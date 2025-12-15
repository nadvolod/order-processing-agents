package com.nadvolod.order.domain;

import java.util.List;

public record OrderRequest(String orderId, List<OrderLine> items) {}
