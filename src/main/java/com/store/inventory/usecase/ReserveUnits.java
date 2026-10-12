package com.store.inventory.usecase;

import java.time.Clock;
import java.time.Instant;

import com.store.inventory.api.OrderLimitExceededException;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;
import com.store.inventory.domain.rules.CategoryRule;
import com.store.inventory.domain.rules.CategoryRules;
import com.store.inventory.port.ReservationRepository;
import com.store.inventory.port.StockRepository;

public class ReserveUnits {

    private final Clock clock;
    private final CategoryRules categoryRules;
    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;

    public ReserveUnits(Clock clock, CategoryRules categoryRules, StockRepository stockRepository, ReservationRepository reservationRepository) {
        this.clock = clock;
        this.categoryRules = categoryRules;
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    public Reservation reserve(String orderId, String sku, int quantity) {
        requireNonBlank(orderId, "orderId");
        requireNonBlank(sku, "sku");
        requirePositiveQuantity(quantity);

        ProductCategory category = stockRepository.categoryOf(sku);
        if (category != null) {
            enforceOrderLimit(sku, category, quantity);
        }
        stockRepository.reserveUnits(sku, quantity);
        Instant expiresAt = clock.instant().plus(categoryRules.ruleFor(category).timeToPay());
        Reservation reservation = new Reservation(orderId, sku, quantity, expiresAt);
        reservationRepository.save(reservation);
        return reservation;
    }

    private void enforceOrderLimit(String sku, ProductCategory category, int quantity) {
        CategoryRule rule = categoryRules.ruleFor(category);
        if (rule.exceedsOrderLimit(quantity)) {
            throw new OrderLimitExceededException(sku, quantity, rule.maxUnitsPerOrder().orElseThrow());
        }
    }

    private static void requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
