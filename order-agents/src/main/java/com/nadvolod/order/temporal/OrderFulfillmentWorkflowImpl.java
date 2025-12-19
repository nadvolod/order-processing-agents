package com.nadvolod.order.temporal;

import com.nadvolod.order.domain.AgentAdvice;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class OrderFulfillmentWorkflowImpl implements OrderFulfillmentWorkflow {

    // Configure activity options with timeouts and retry policy
    private final AiActivities aiActivities = Workflow.newActivityStub(
            AiActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))  // Max time for activity to complete
                    .setRetryOptions(
                            RetryOptions.newBuilder()
                                    .setMaximumAttempts(3)  // Retry up to 3 times on failure
                                    .setInitialInterval(Duration.ofSeconds(1))
                                    .setMaximumInterval(Duration.ofSeconds(10))
                                    .setBackoffCoefficient(2.0)  // Exponential backoff
                                    .build()
                    )
                    .build()
    );

    @Override
    public com.nadvolod.order.domain.WorkflowResult processOrder(OrderRequest request) {
        // Log workflow start
        Workflow.getLogger(OrderFulfillmentWorkflowImpl.class)
                .info("Processing order: " + request.orderId());

        // Simulate order processing (replace with actual order processing logic)
        OrderResponse response = new OrderResponse(
                request.orderId(),
                "ACCEPTED",
                java.util.List.of(),
                null,
                null
        );

        // Activity 1: Generate internal analysis with AI
        AgentAdvice advice = aiActivities.explain(request, response);
        Workflow.getLogger(OrderFulfillmentWorkflowImpl.class)
                .info("AI Analysis - Summary: " + advice.summary());

        // Activity 2: Generate customer message with AI
        String customerMessage = aiActivities.generateCustomerMessage(advice);
        Workflow.getLogger(OrderFulfillmentWorkflowImpl.class)
                .info("Customer Message: " + customerMessage);

        // Return complete result with AI-generated content
        return new com.nadvolod.order.domain.WorkflowResult(response, advice, customerMessage);
    }
}
