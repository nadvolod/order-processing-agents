package com.nadvolod.order.service;

public interface InventoryService {
    boolean checkAvailability(String sku, int quantity);
}
