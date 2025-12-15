package com.nadvolod.order.domain;

public record OrderLineStatus(String sku, int quantity, boolean available) {}
