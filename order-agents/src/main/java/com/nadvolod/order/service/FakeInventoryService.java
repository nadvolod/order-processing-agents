package com.nadvolod.order.service;

import java.util.HashMap;
import java.util.Map;

public class FakeInventoryService implements InventoryService {
    private final Map<String, Integer> inventory = new HashMap<>();

    public FakeInventoryService() {
        // Initialize inventory based on scenario descriptions from CLAUDE.md
        inventory.put("SKU-123", 10);
        inventory.put("SKU-456", 0);
        inventory.put("SKU-789", 5);
    }

    @Override
    public boolean checkAvailability(String sku, int quantity) {
        int available = inventory.getOrDefault(sku, 0);
        return available >= quantity;
    }
}
