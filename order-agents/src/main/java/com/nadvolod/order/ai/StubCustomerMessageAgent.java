package com.nadvolod.order.ai;

import com.nadvolod.order.domain.AgentAdvice;

public class StubCustomerMessageAgent implements CustomerMessageAgent {
    @Override
    public String generateMessage(AgentAdvice advice) {
        // Simple context-aware stub for testing
        if (advice.summary().toLowerCase().contains("accept")) {
            return "Thank you for your order! We've got it and are getting it ready to ship.";
        } else {
            return "We encountered an issue with your order. Our team will contact you shortly.";
        }
    }
}
