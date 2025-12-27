package com.nadvolod.order.fraud.domain;

/**
 * Result of charging a payment card.
 * Similar to PaymentResult but specific to card charging.
 */
public record PaymentChargeResult(
    boolean success,
    String chargeId,           // e.g., "CHG-12345"
    String message,            // "Charge successful" or error message
    double amountCharged
) {}
