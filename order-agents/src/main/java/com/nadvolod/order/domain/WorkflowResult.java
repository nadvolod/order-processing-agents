package com.nadvolod.order.domain;

/**
 * Complete result from order fulfillment workflow including AI-generated content.
 */
public record WorkflowResult(
        OrderResponse orderResponse,
        AgentAdvice aiAnalysis,
        String customerMessage
) {
    @Override
    public String toString() {
        return """

                === Order Fulfillment Result ===
                Order ID: %s
                Status: %s

                === AI Internal Analysis ===
                Summary: %s
                Recommended Actions: %s

                === Customer Message ===
                %s
                """.formatted(
                orderResponse.orderId(),
                orderResponse.status(),
                aiAnalysis.summary(),
                aiAnalysis.recommendedActions(),
                customerMessage
        );
    }
}
