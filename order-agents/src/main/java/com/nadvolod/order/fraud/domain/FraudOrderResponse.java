package com.nadvolod.order.fraud.domain;

/**
 * Complete response from fraud detection workflow.
 * Contains results from all three steps.
 */
public record FraudOrderResponse(
    String orderId,
    String status,                      // "APPROVED", "REJECTED_FRAUD", "REJECTED_PAYMENT"
    FraudDetectionResult fraudCheck,
    PaymentChargeResult paymentCharge,  // null if fraud detected
    ConfirmationMessage confirmation    // null if rejected
) {}
