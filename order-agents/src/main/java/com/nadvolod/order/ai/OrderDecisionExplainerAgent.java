package com.nadvolod.order.ai;

import com.nadvolod.order.domain.AgentAdvice;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;

/**
 * Interface for agents that explain order processing decisions.
 */
public interface OrderDecisionExplainerAgent {
    
    /**
     * Explains what happened with an order and provides advice.
     * 
     * @param request The original order request
     * @param response The order processing response
     * @return Agent advice including summary, recommended actions, and customer message
     */
    AgentAdvice explain(OrderRequest request, OrderResponse response);
}
