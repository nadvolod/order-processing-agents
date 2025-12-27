package com.nadvolod.order.fraud.temporal;

/**
 * Exception thrown when a payment charge fails.
 * This triggers Temporal's retry mechanism for transient failures.
 */
public class PaymentFailedException extends RuntimeException {
    private final String orderId;
    private final int attemptNumber;

    public PaymentFailedException(String orderId, int attemptNumber, String message) {
        super(message);
        this.orderId = orderId;
        this.attemptNumber = attemptNumber;
    }

    public String getOrderId() {
        return orderId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }
}
