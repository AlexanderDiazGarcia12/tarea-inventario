package com.store.inventory.adapter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.store.inventory.api.StockAlertListener;

final class LowStockAlerter {

    static final int LOW_STOCK_THRESHOLD = 5;

    private final StockAlertListener listener;
    private final Set<String> skusInAlert = ConcurrentHashMap.newKeySet();

    LowStockAlerter(StockAlertListener listener) {
        this.listener = listener;
    }
    void afterAvailabilityChange(String sku, int availableUnits) {
        if (availableUnits > LOW_STOCK_THRESHOLD) {
            skusInAlert.remove(sku);
            return;
        }
        boolean newlyInAlert = skusInAlert.add(sku);
        if (newlyInAlert) {
            listener.onLowStock(sku, availableUnits);
        }
    }

}
