package com.nadvolod.order.fraud.ai;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.domain.FraudDetectionResult;

/**
 * Stub implementation for testing without OpenAI API.
 * Uses simple heuristics based on order ID and quantity.
 */
public class StubFraudDetectionAgent implements FraudDetectionAgent {

    @Override
    public FraudDetectionResult analyze(OrderRequest request) {
        // Simple heuristic: reject if order ID contains "FRAUD"
        String orderId = request.orderId().toLowerCase();

        if (orderId.contains("fraud") || orderId.contains("test-fraud")) {
            return new FraudDetectionResult(
                false,
                0.95,
                "Order ID contains fraud indicators",
                "HIGH"
            );
        }

        // Check for unusual quantities (>100 items total)
        int totalQuantity = request.items().stream()
            .mapToInt(item -> item.quantity())
            .sum();

        if (totalQuantity > 100) {
            return new FraudDetectionResult(
                false,
                0.85,
                "Unusually high quantity: " + totalQuantity + " items",
                "HIGH"
            );
        }

        // Default: approve with low risk
        return new FraudDetectionResult(
            true,
            0.1,
            "No fraud indicators detected",
            "LOW"
        );
    }
}
