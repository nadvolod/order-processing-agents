package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface FraudOrderWorkflow {
    @WorkflowMethod
    FraudOrderResponse processOrder(OrderRequest order);
}
