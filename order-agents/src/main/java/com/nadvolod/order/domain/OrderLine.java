package com.nadvolod.order.domain;

public record OrderLine(String sku, int quantity) {
    public OrderLine {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }
}
