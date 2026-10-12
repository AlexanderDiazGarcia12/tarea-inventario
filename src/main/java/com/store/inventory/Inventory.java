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
        StockRepository stockRepository = new InMemoryStockRepository();
        ReservationRepository reservationRepository = new InMemoryReservationRepository();
        CategoryRules categoryRules = CategoryRules.defaults();

        return new DefaultInventoryService(
            alertListener,
            stockRepository,
            new ReserveUnits(clock, categoryRules, stockRepository, reservationRepository),
            new ConfirmReservation(stockRepository, reservationRepository));
    }
}
