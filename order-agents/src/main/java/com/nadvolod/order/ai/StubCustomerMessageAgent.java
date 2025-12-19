package com.nadvolod.order.ai;

import com.nadvolod.order.domain.AgentAdvice;

public class StubCustomerMessageAgent implements CustomerMessageAgent {
    @Override
    public String generateMessage(AgentAdvice advice) {
        // Simple context-aware stub for testing
        return switch (CustomerMessageUtils.determineMessageType(advice)) {
            case SUCCESS -> "Thank you for your order! We've got it and are getting it ready to ship.";
            case FAILURE -> "We encountered an issue with your order. Our team will contact you shortly.";
            case NEUTRAL -> "We're reviewing your order. You'll receive an update shortly.";
        };
    }
}
