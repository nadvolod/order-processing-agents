package com.nadvolod.order.domain;

public record PaymentResult(boolean success, String transactionId, String message) {}
