package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.fraud.domain.PaymentChargeResult;
import com.nadvolod.order.fraud.service.FraudPaymentService;

public class PaymentChargeActivityImpl implements PaymentChargeActivity{
    private final FraudPaymentService paymentService;

    public PaymentChargeActivityImpl(FraudPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public PaymentChargeResult chargeCard(String orderId, double amount) {
        return paymentService.chargeCard(orderId, amount);
    }
}
