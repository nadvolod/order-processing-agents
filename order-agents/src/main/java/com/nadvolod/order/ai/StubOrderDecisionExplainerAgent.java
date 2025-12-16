package com.nadvolod.order.ai;

import com.nadvolod.order.domain.AgentAdvice;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;

import java.util.List;

public class StubOrderDecisionExplainerAgent implements OrderDecisionExplainerAgent{
    @Override
    public AgentAdvice explain(OrderRequest request, OrderResponse response) {
        return new AgentAdvice(
                "Stub AI: explanation not enabled yet.",
                List.of("No recommended actions (stub)."),
                "Thanks for your order. We’ll update you shortly.");
    }
}
