package com.store.inventory.adapter;

import java.util.concurrent.ConcurrentHashMap;

import com.store.inventory.api.InsufficientStockException;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.port.StockRepository;

public class InMemoryStockRepository implements StockRepository {
    
    private final ConcurrentHashMap<String, ProductStock> products = new ConcurrentHashMap<>();

    @Override
    public void registerProduct(String sku, ProductCategory category) {
        requireNonBlank(sku, "sku is required");
        products.compute(sku, (key, existing) -> {
            if (existing == null) {
                return new ProductStock(category);
            }
            existing.recategorize(category);
            return existing;
        });
    }

    @Override
    public void addStock(String sku, int quantity) {
        requireNonBlank(sku, "sku is required");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        ProductStock stock = products.get(sku);
        if (stock == null) {
            throw new IllegalStateException("Product not registered: " + sku);
        }
        stock.addPhysicalUnits(quantity);
    }

    @Override
    public int available(String sku) {
        ProductStock stock = products.get(sku);
        return stock == null ? 0 : stock.available();
    }

    @Override
    public ProductCategory categoryOf(String sku) {
        ProductStock stock = products.get(sku);
        return stock == null ? null : stock.category();
    }

    @Override
    public void reserveUnits(String sku, int quantity) {
        ProductStock stock = products.get(sku);
        if (stock == null) {
            throw new InsufficientStockException(sku, quantity, 0);
        }
        if (!stock.tryReserve(quantity)) {
            throw new InsufficientStockException(sku, quantity, stock.available());
        }
    }

    @Override
    public void confirmUnits(String sku, int quantity) {
        products.get(sku).confirm(quantity);
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

}
