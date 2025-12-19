package com.nadvolod.order.temporal;

import com.nadvolod.order.ai.CustomerMessageAgent;
import com.nadvolod.order.ai.OrderDecisionExplainerAgent;
import com.nadvolod.order.domain.AgentAdvice;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;

public final class AiActivitiesImpl implements AiActivities{

    private final OrderDecisionExplainerAgent decisionAgent;
    private final CustomerMessageAgent messageAgent;

    public AiActivitiesImpl(OrderDecisionExplainerAgent decisionExplainerAgent, CustomerMessageAgent customerMessageAgent) {
        this.decisionAgent = decisionExplainerAgent;
        this.messageAgent = customerMessageAgent;

    }
    @Override
    public AgentAdvice explain(OrderRequest request, OrderResponse response) {
        return decisionAgent.explain(request, response);
    }

    @Override
    public String generateCustomerMessage(AgentAdvice advice) {
        return messageAgent.generateMessage(advice);
    }
}
