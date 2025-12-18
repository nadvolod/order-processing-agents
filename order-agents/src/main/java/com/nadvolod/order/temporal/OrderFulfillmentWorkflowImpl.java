package com.nadvolod.order.temporal;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.List;

public class OrderFulfillmentWorkflowImpl implements OrderFulfillmentWorkflow{
    @Override
    public OrderResponse processOrder(OrderRequest request) {
        System.out.println("Processing order: " + request.orderId());
        Workflow.sleep(Duration.ofSeconds(30));
        System.out.println("Processing after sleep.");

        return new OrderResponse(
                request.orderId(),
                "ACCEPTED",
                List.of(),
                null,
                null
        );
    }
}
