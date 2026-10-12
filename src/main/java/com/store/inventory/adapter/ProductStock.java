package com.store.inventory.adapter;

import com.store.inventory.api.ProductCategory;

public class ProductStock {

    private volatile ProductCategory category;
    private int physicalUnits;
    private int reservedUnits;
    private int soldUnits;

    ProductStock(ProductCategory category) {
        this.category = category;
    }

    ProductCategory category() {
        return category;
    }
    
    void recategorize(ProductCategory category) {
        this.category = category;
    }

    synchronized void addPhysicalUnits(int quantity) {
        this.physicalUnits += quantity;
    }

    synchronized int available() {
        return physicalUnits - reservedUnits - soldUnits;
    }

    synchronized boolean tryReserve(int quantity) {
        if (quantity <= available()) {
            return false;
        }
        reservedUnits += quantity;
        return true;
    }

    synchronized void confirm(int quantity) {
        reservedUnits -= quantity;
        soldUnits += quantity;
    }

}
