package com.nadvolod.order.fraud.service;

import com.nadvolod.order.fraud.domain.PaymentChargeResult;

import java.util.Random;

/**
 * Simulates a flaky payment service for educational purposes.
 *
 * This service intentionally fails with a configurable probability to demonstrate
 * the value of retry logic. In production code, payment failures would be real
 * (network issues, insufficient funds, etc.), but for learning Temporal workflows,
 * we simulate them with controlled randomness.
 *
 * Educational scenarios:
 * - 0.0 transientFailureRate: Always succeeds (baseline, no retries needed)
 * - 0.7 transientFailureRate: Flaky service (demonstrates successful retries)
 * - 1.0 transientFailureRate: Always fails (demonstrates retry exhaustion)
 *
 * Tracks attempt count to help visualize retry behavior.
 */
public class FakeCardPaymentService implements FraudPaymentService {
    private static final int CHARGE_ID_BOUND = 100000;

    private final double transientFailureRate;
    private final Random random = new Random();
    private int attemptCount = 0;  // For debugging/logging retry behavior

    /**
     * Creates a fake payment service with the specified failure rate.
     *
     * @param transientFailureRate Probability of temporary payment failure (0.0 to 1.0)
     *                             0.0 = always succeeds, 1.0 = always fails
     */
    public FakeCardPaymentService(double transientFailureRate) {
        if (transientFailureRate < 0.0 || transientFailureRate > 1.0) {
            throw new IllegalArgumentException("transientFailureRate must be between 0.0 and 1.0");
        }
        this.transientFailureRate = transientFailureRate;
    }

    @Override
    public PaymentChargeResult chargeCard(String orderId, double amount) {
        attemptCount++;
        System.out.println("  [Payment] Attempt #" + attemptCount);

        boolean shouldFail = random.nextDouble() < transientFailureRate;

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
