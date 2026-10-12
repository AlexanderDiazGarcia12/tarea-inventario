package com.store.inventory.domain.rules;

import java.time.Duration;
import java.util.Map;

import com.store.inventory.api.ProductCategory;

public final class CategoryRules {
    private final Map<ProductCategory, CategoryRule> rules;

    public CategoryRules(Map<ProductCategory, CategoryRule> rules) {
        this.rules = Map.copyOf(rules);
    }

    public static CategoryRules defaults() {
        return new CategoryRules(Map.of(
            ProductCategory.STANDARD, CategoryRule.unlimited(Duration.ofMinutes(15)),
            ProductCategory.PRE_ORDER, CategoryRule.unlimited(Duration.ofHours(24)),
            ProductCategory.FLASH_SALE, CategoryRule.withOrderLimit(Duration.ofMinutes(5), 2)
        ));
    }

    public CategoryRule ruleFor(ProductCategory category) {
        CategoryRule rule = rules.get(category);
        if (rule == null) {
            throw new IllegalArgumentException("No rule found for category " + category);
        }
        return rule;
    }
}
