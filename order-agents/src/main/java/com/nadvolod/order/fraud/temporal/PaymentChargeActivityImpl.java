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
        PaymentChargeResult result = paymentService.chargeCard(orderId, amount);

        // If payment fails, throw an exception to trigger Temporal retries
        if (!result.success()) {
            throw new PaymentFailedException(
                orderId,
                1, // Temporal will handle retry count
                result.message()
            );
        }

        return result;
    }
}
