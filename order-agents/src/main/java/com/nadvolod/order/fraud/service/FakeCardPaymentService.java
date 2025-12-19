package com.nadvolod.order.fraud.service;

import com.nadvolod.order.fraud.domain.PaymentChargeResult;

import java.util.Random;

/**
 * Fake payment service that demonstrates retry scenarios.
 * - Configurable fail rate
 * - Tracks attempt count (useful for demonstrating Temporal retries later)
 * - Supports seeded randomness for deterministic testing
 */
public class FakeCardPaymentService implements FraudPaymentService {
    private static final int CHARGE_ID_BOUND = 100000;

    private final double failRate;
    private final Random random;
    private int attemptCount = 0;  // For debugging/logging retry behavior

    /**
     * @param failRate Probability of payment failure (0.0 to 1.0)
     * @param random Optional seeded Random for deterministic behavior
     */
    public FakeCardPaymentService(double failRate, Random random) {
        if (failRate < 0.0 || failRate > 1.0) {
            throw new IllegalArgumentException("failRate must be between 0.0 and 1.0");
        }
        this.failRate = failRate;
        this.random = random != null ? random : new Random();
    }

    @Override
    public PaymentChargeResult chargeCard(String orderId, double amount) {
        attemptCount++;
        System.out.println("  [Payment] Attempt #" + attemptCount);

        boolean shouldFail = random.nextDouble() < failRate;

        if (shouldFail) {
            return new PaymentChargeResult(
                false,
                null,
                "Card declined (simulated failure #" + attemptCount + ")",
                0.0
            );
        } else {
            String chargeId = "CHG-" + Math.abs(random.nextInt(CHARGE_ID_BOUND));
            return new PaymentChargeResult(
                true,
                chargeId,
                "Charge successful on attempt #" + attemptCount,
                amount
            );
        }
    }

    /**
     * Getter for testing/debugging
     */
    public int getAttemptCount() {
        return attemptCount;
    }
}
