package com.nadvolod.order.ai;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;
import com.nadvolod.order.domain.AgentAdvice;

/**
 * AI agent that explains what happened with an order and suggests next actions.
 */
public interface OrderDecisionExplainerAgent {
    AgentAdvice explain(OrderRequest request, OrderResponse response);
}
