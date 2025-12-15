package com.nadvolod.order.service;

import com.nadvolod.order.domain.PaymentResult;

import java.util.Random;

public class FakePaymentService implements PaymentService {
    private static final int TX_ID_BOUND = 100000;
    
    private final double failRate;
    private final Random random;

    /**
     * @param failRate Probability of payment failure (0.0 to 1.0)
     * @param random Optional seeded Random for deterministic behavior
     */
    public FakePaymentService(double failRate, Random random) {
        if (failRate < 0.0 || failRate > 1.0) {
            throw new IllegalArgumentException("failRate must be between 0.0 and 1.0");
        }
        this.failRate = failRate;
        this.random = random != null ? random : new Random();
    }

    @Override
    public PaymentResult processPayment(String orderId, double amount) {
        boolean shouldFail = random.nextDouble() < failRate;
        
        if (shouldFail) {
            return new PaymentResult(false, null, "Payment declined (simulated)");
        } else {
            String txId = "TX-" + random.nextInt(TX_ID_BOUND);
            return new PaymentResult(true, txId, "Payment successful");
        }
    }
}
