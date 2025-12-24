package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.CLI;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import static com.nadvolod.order.fraud.FraudOrderProcessingApp.createOrderForScenario;

public class Starter {

    //A. Task Queue - must match worker
    private static final String TASK_QUEUE = "fraud-order-process";

    public static void main(String[] args) {
        //B. Parse CLI arguments
        // Parse CLI arguments
        CLI.CLIConfig config = CLI.parseArgs(args);

        if (config == null) {
            CLI.printUsage();
            System.exit(1);
        }

        OrderRequest request = createOrderForScenario(config.scenario);

        //1. Connect to Temporal
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        //2. Create workflow stub
        FraudOrderWorkflow workflow = client.newWorkflowStub(
                FraudOrderWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId("fraud-workflow-" + request.orderId())
                        .build()
        );

        // Step 3: Start the workflow!
        System.out.println("=== Fraud Detection Workflow Demo (Temporal) ===");
        System.out.println("Scenario: " + config.scenario);
        System.out.println("Payment Fail Rate: " + (config.paymentFailRate * 100) + "%");
        if (config.seed != null) {
            System.out.println("Random Seed: " + config.seed);
        }

        FraudOrderResponse response = workflow.processOrder(request);

        // Step 4: Display results (similar to FraudOrderProcessingApp)
        System.out.println("\n=== Final Results ===");
        System.out.println("Order ID: " + response.orderId());
        System.out.println("Status: " + response.status());

        if (response.confirmation() != null) {
            System.out.println("\n=== Customer Message ===");
            System.out.println("Subject: " + response.confirmation().subject());
            System.out.println("Tone: " + response.confirmation().tone());
            System.out.println("\nMessage:");
            System.out.println(response.confirmation().body());
        }

        System.out.println("\n=== Workflow Complete ===");
    }
}
