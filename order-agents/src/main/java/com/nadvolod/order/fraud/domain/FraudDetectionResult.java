package com.nadvolod.order.fraud.domain;

/**
 * Result of AI-powered fraud detection analysis.
 * Contains risk score, decision, and explanation.
 */
public record FraudDetectionResult(
    boolean approved,           // true = safe to proceed, false = reject as fraud
    double riskScore,          // 0.0 (safe) to 1.0 (definite fraud)
    String reason,             // AI-generated explanation
    String riskLevel           // "LOW", "MEDIUM", "HIGH"
) {}
