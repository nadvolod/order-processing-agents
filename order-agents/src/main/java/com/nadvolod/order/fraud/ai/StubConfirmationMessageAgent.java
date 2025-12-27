package com.nadvolod.order.fraud.ai;

import com.nadvolod.order.fraud.domain.ConfirmationMessage;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;

/**
 * Stub implementation for testing without OpenAI API.
 */
public class StubConfirmationMessageAgent implements ConfirmationMessageAgent {

    @Override
    public ConfirmationMessage generate(FraudOrderResponse response) {
        return switch (response.status()) {
            case "APPROVED" -> new ConfirmationMessage(
                "Order Confirmed: " + response.orderId(),
                "Your order has been confirmed and is being processed.",
                "positive"
            );
            case "REJECTED_FRAUD" -> new ConfirmationMessage(
                "Order Review Required: " + response.orderId(),
                "Your order requires additional security verification.",
                "apologetic"
            );
            case "REJECTED_PAYMENT" -> new ConfirmationMessage(
                "Payment Failed: " + response.orderId(),
                "We couldn't process your payment. Please try again.",
                "apologetic"
            );
            default -> new ConfirmationMessage(
                "Order Status: " + response.orderId(),
                "We're reviewing your order.",
                "neutral"
            );
        };
    }
}
