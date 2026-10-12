package com.store.inventory;

import com.store.inventory.adapter.InMemoryReservationRepository;
import com.store.inventory.adapter.InMemoryStockRepository;
import com.store.inventory.api.InventoryService;
import com.store.inventory.api.StockAlertListener;
import com.store.inventory.domain.rules.CategoryRules;
import com.store.inventory.port.ReservationRepository;
import com.store.inventory.port.StockRepository;
import com.store.inventory.service.DefaultInventoryService;
import com.store.inventory.usecase.ConfirmReservation;
import com.store.inventory.usecase.ReleaseExpiredReservations;
import com.store.inventory.usecase.ReserveUnits;

import java.time.Clock;

/**
 * Entry point used by our automated tests. Keep this signature exactly as it is,
 * and build your implementation here.
 */
public final class Inventory {

    private Inventory() {
    }

    public static InventoryService create(Clock clock, StockAlertListener alertListener) {
        StockRepository stockRepository = new InMemoryStockRepository(alertListener);
        ReservationRepository reservationRepository = new InMemoryReservationRepository();
        CategoryRules categoryRules = CategoryRules.defaults();
        ReleaseExpiredReservations releaseExpiredReservations = new ReleaseExpiredReservations(clock, stockRepository, reservationRepository);

        return new DefaultInventoryService(
            stockRepository,
            new ReserveUnits(clock, categoryRules, stockRepository, reservationRepository, releaseExpiredReservations),
            new ConfirmReservation(clock, stockRepository, reservationRepository, releaseExpiredReservations),
            releaseExpiredReservations);
    }
}
