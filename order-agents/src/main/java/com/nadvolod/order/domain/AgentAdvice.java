package com.nadvolod.order.domain;

import java.util.List;

/**
 * Represents advice from an AI agent about an order decision.
 * 
 * @param summary A brief summary of what happened with the order
 * @param recommendedActions A list of recommended next actions
 * @param customerMessage A friendly message for the customer
 */
public record AgentAdvice(
    String summary,
    List<String> recommendedActions,
    String customerMessage
) {}
