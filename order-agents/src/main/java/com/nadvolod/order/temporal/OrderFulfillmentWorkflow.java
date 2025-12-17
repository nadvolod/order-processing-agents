package com.nadvolod.order.temporal;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderFulfillmentWorkflow {
    @WorkflowMethod
    OrderResponse processOrder(OrderRequest request);
}
