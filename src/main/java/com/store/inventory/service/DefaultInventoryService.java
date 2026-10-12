package com.store.inventory.service;

import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;
import com.store.inventory.api.StockAlertListener;
import com.store.inventory.port.StockRepository;
import com.store.inventory.usecase.ConfirmReservation;
import com.store.inventory.usecase.ReleaseExpiredReservations;
import com.store.inventory.usecase.ReserveUnits;

public class DefaultInventoryService implements InventoryService {
    private final StockAlertListener alertListener;
    private final StockRepository stockRepository;
    private final ReserveUnits reserveUnits;
    private final ConfirmReservation confirmReservation;
    private final ReleaseExpiredReservations releaseExpiredReservations;

    public DefaultInventoryService(
            StockAlertListener alertListener,
            StockRepository stockRepository,
            ReserveUnits reserveUnits,
            ConfirmReservation confirmReservation,
            ReleaseExpiredReservations releaseExpiredReservations) {
        this.alertListener = alertListener;
        this.stockRepository = stockRepository;
        this.reserveUnits = reserveUnits;
        this.confirmReservation = confirmReservation;
        this.releaseExpiredReservations = releaseExpiredReservations;
    }

    @Override
    public void registerProduct(String sku, ProductCategory category) {
        stockRepository.registerProduct(sku, category);
    }

    @Override
    public void addStock(String sku, int quantity) {
        stockRepository.addStock(sku, quantity);
    }

    @Override
    public Reservation reserve(String orderId, String sku, int quantity) {
        return reserveUnits.reserve(orderId, sku, quantity);
    }

    @Override
    public void confirm(String orderId) {
        confirmReservation.confirm(orderId);
    }

    @Override
    public int available(String sku) {
        releaseExpiredReservations.releaseDue();
        return stockRepository.available(sku);
    }

}
