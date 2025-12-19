package com.nadvolod.order.temporal;

import com.nadvolod.order.domain.AgentAdvice;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/*
* We put this into an Activity because it's slow and can fail nondeterministically
* */
@ActivityInterface
public interface AiActivities {
    @ActivityMethod
    AgentAdvice explain(OrderRequest request, OrderResponse response);

    @ActivityMethod
    String generateCustomerMessage(AgentAdvice advice);
}
