package com.nadvolod.order.ai;

import com.nadvolod.order.domain.AgentAdvice;

/**
 * AI agent that converts internal order analysis into customer-ready messages.
 * Focuses on tone, clarity, and hiding internal system details.
 */
public interface CustomerMessageAgent {
    String generateMessage(AgentAdvice advice);
}
