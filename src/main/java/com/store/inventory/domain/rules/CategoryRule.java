package com.store.inventory.domain.rules;

import java.time.Duration;
import java.util.OptionalInt;

public record CategoryRule(Duration timeToPay, OptionalInt maxUnitsPerOrder) {

    public static CategoryRule unlimited(Duration timeToPay) {
        return new CategoryRule(timeToPay, OptionalInt.empty());
    }

    public static CategoryRule withOrderLimit(Duration timeToPay, int maxUnitsPerOrder) {
        return new CategoryRule(timeToPay, OptionalInt.of(maxUnitsPerOrder));
    }

    public  boolean exceedsOrderLimit(int quantity) {
        return maxUnitsPerOrder.isPresent() && quantity > maxUnitsPerOrder.getAsInt();
    }
}
