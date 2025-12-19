package com.nadvolod.order.fraud.service;

import com.nadvolod.order.fraud.domain.PaymentChargeResult;

/**
 * Service for charging payment cards.
 * Designed to demonstrate retry behavior.
 */
public interface FraudPaymentService {
    PaymentChargeResult chargeCard(String orderId, double amount);
}
