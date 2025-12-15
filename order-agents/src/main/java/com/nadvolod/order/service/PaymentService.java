package com.nadvolod.order.service;

import com.nadvolod.order.domain.PaymentResult;

public interface PaymentService {
    PaymentResult processPayment(String orderId, double amount);
}
