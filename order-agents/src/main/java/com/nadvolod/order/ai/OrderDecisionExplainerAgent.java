package com.nadvolod.order.ai;

import com.nadvolod.order.domain.AgentAdvice;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;

public interface OrderDecisionExplainerAgent {
    AgentAdvice explain(OrderRequest request, OrderResponse response);
}
