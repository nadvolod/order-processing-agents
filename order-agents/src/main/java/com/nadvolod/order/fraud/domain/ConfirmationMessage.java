package com.nadvolod.order.fraud.domain;

/**
 * AI-generated confirmation message for customer.
 * Tailored based on fraud check and payment results.
 */
public record ConfirmationMessage(
    String subject,            // Email subject line
    String body,               // Main message content
    String tone                // "positive", "neutral", "apologetic"
) {}
