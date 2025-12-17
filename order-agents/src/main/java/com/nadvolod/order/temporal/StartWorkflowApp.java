package com.nadvolod.order.temporal;

import com.nadvolod.order.domain.OrderLine;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.util.List;

public final class StartWorkflowApp {
    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(WorkerApp.TASK_QUEUE)
                .setWorkflowId("order-" + System.currentTimeMillis())
                .build();

        OrderFulfillmentWorkflow workflow = client.newWorkflowStub(OrderFulfillmentWorkflow.class, options);

        OrderRequest request = new OrderRequest("order-123", List.of(new OrderLine("sku-123", 1)));

        OrderResponse response = workflow.processOrder(request);

        System.out.println("workflow result:");
        System.out.println(response);
    }
}
