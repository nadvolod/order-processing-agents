package com.nadvolod.order.temporal;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;

import java.util.List;

public class OrderFulfillmentWorkflowImpl implements OrderFulfillmentWorkflow{
    @Override
    public OrderResponse processOrder(OrderRequest request) {
        return new OrderResponse(
                request.orderId(),
                "ACCEPTED",
                List.of(),
                null,
                null
        );
    }
}
