package com.store.inventory.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import com.store.inventory.api.OrderLimitExceededException;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;
import com.store.inventory.domain.rules.CategoryRule;
import com.store.inventory.domain.rules.CategoryRules;
import com.store.inventory.port.ReservationRepository;
import com.store.inventory.port.StockRepository;

public final class ReserveUnits {

    private final Clock clock;
    private final CategoryRules categoryRules;
    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;
    private final ReleaseExpiredReservations releaseExpiredReservations;

    public ReserveUnits(Clock clock, CategoryRules categoryRules, StockRepository stockRepository, ReservationRepository reservationRepository, ReleaseExpiredReservations releaseExpiredReservations) {
        this.clock = clock;
        this.categoryRules = categoryRules;
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
        this.releaseExpiredReservations = releaseExpiredReservations;
    }

    public Reservation reserve(String orderId, String sku, int quantity) {
        requireNonBlank(orderId, "orderId");
        requireNonBlank(sku, "sku");
        requirePositiveQuantity(quantity);

        Instant now = clock.instant();
        releaseExpiredReservations.releaseDueAt(now);

        Optional<Reservation> existing = reservationRepository.activeReservationFor(orderId);
        if (existing.isPresent()) {
            return resolveRetry(existing.get(), sku, quantity);
        }

        ProductCategory category = stockRepository.categoryOf(sku);
        if (category != null) {
            enforceOrderLimit(sku, category, quantity);
        }

        stockRepository.reserveUnits(sku, quantity);

        Instant expiresAt = now.plus(categoryRules.ruleFor(category).timeToPay());
        Reservation reservation = new Reservation(orderId, sku, quantity, expiresAt);
        Reservation saved = reservationRepository.saveIfAbsent(reservation);

        if (saved == reservation) {
            return saved;
        }

        stockRepository.reserveUnits(sku, quantity);
        return resolveRetry(saved, sku, quantity);
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

    private static Reservation resolveRetry(Reservation existing, String sku, int quantity) {
        if (existing.sku().equals(sku) && existing.quantity() == quantity) {
            return existing;
        }
        throw new IllegalStateException(
                "Order " + existing.orderId() + " already has an active reservation of "
                        + existing.quantity() + " units of " + existing.sku()
                        + " (expires at " + existing.expiresAt() + "); refusing a conflicting request for "
                        + quantity + " units of " + sku);
    }
}
