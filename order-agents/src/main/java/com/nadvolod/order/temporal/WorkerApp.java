package com.nadvolod.order.temporal;

import com.nadvolod.order.ai.CustomerMessageAgent;
import com.nadvolod.order.ai.OpenAiClientFactory;
import com.nadvolod.order.ai.OpenAiCustomerMessageAgent;
import com.nadvolod.order.ai.OpenAiOrderDecisionExplainerAgent;
import com.nadvolod.order.ai.OrderDecisionExplainerAgent;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;


public class WorkerApp {
    public static final String TASK_QUEUE = "order-fulfillment";

    public static void main(String[] args) {
        // 1. Connect to Temporal server
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

        // 2. Create workflow client
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 3. Create worker factory and worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        // 4. Register workflow implementation
        worker.registerWorkflowImplementationTypes(OrderFulfillmentWorkflowImpl.class);

        // 5. Create and register AI activities with agent implementations
        String apiKey = System.getenv("OPENAI_API_KEY");
        String model = "gpt-4o-mini";

        OrderDecisionExplainerAgent decisionAgent = new OpenAiOrderDecisionExplainerAgent(apiKey, model);
        CustomerMessageAgent messageAgent = new OpenAiCustomerMessageAgent(
                OpenAiClientFactory.create(),
                model
        );

        AiActivitiesImpl aiActivities = new AiActivitiesImpl(decisionAgent, messageAgent);
        worker.registerActivitiesImplementations(aiActivities);

        // 6. Start polling for tasks
        factory.start();
        System.out.println("Worker started on queue: " + TASK_QUEUE);
        System.out.println("Registered workflows: OrderFulfillmentWorkflow");
        System.out.println("Registered activities: AiActivities");
    }
}
