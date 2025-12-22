package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.fraud.domain.PaymentChargeResult;
import com.nadvolod.order.fraud.service.FraudPaymentService;

public class PaymentChargeImpl implements PaymentChargeActivity{
    private final FraudPaymentService paymentService;

    public PaymentChargeImpl(FraudPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public PaymentChargeResult chargeCard(String orderId, double amount) {
        return paymentService.chargeCard(orderId, amount);
    }
}
