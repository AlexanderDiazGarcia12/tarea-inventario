package com.store.inventory.port;

import com.store.inventory.api.ProductCategory;

public interface StockRepository {

    void registerProduct(String sku, ProductCategory category);
    void addStock(String sku, int quantity);
    int available(String sku);
    ProductCategory categoryOf(String sku);
    void reserveUnits(String sku, int quantity);
    void confirmUnits(String sku, int quantity);
}
