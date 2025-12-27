package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.fraud.ai.*;
import com.nadvolod.order.fraud.service.FakeCardPaymentService;
import com.nadvolod.order.fraud.service.FraudPaymentService;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class WorkerApp {
    // 1. select a task queue name
    static final String TASK_QUEUE = "fraud-order-process";

    public static void main(String[] args) {

        //2. connect to temporal
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        System.out.println("Connected to Temporal server.");

        //3. create worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);
        System.out.println("Registered worker for task queue: " + TASK_QUEUE);

        //4. register workflow
        worker.registerWorkflowImplementationTypes(FraudOrderWorkflowImpl.class);
        System.out.println("Registered workflow");

        //5. Create activity dependencies
        // Create AI agents (OpenAI or Stub)
        var apiKey = System.getenv("OPENAI_API_KEY");
        FraudDetectionAgent fraudAgent;
        ConfirmationMessageAgent confirmationAgent;

        if (apiKey != null && !apiKey.isBlank()) {
            // Create OpenAI versions
            fraudAgent = new OpenAiFraudDetectionAgent(apiKey, "gpt-4o-mini");
            confirmationAgent = new OpenAiConfirmationMessageAgent(apiKey, "gpt-4o-mini");
            System.out.println("[INFO] Using OpenAI API for AI agents");
        } else {
            // Create stub versions
            fraudAgent = new StubFraudDetectionAgent();
            confirmationAgent = new StubConfirmationMessageAgent();
            System.out.println("[INFO] OPENAI_API_KEY not set - using stub AI agents");
        }

        // Create payment service with configurable failure rate
        // Read from environment variable, default to 0.7 (70%) if not set
        String failRateEnv = System.getenv("PAYMENT_FAILURE_RATE");
        double paymentFailRate = 0.7; // default
        if (failRateEnv != null && !failRateEnv.isBlank()) {
            try {
                paymentFailRate = Double.parseDouble(failRateEnv);
                if (paymentFailRate < 0.0 || paymentFailRate > 1.0) {
                    System.err.println("[WARN] PAYMENT_FAILURE_RATE must be between 0.0 and 1.0. Using default: 0.7");
                    paymentFailRate = 0.7;
                }
            } catch (NumberFormatException e) {
                System.err.println("[WARN] Invalid PAYMENT_FAILURE_RATE value. Using default: 0.7");
                paymentFailRate = 0.7;
            }
        }
        System.out.println("[INFO] Payment failure rate: " + (paymentFailRate * 100) + "% (configurable via PAYMENT_FAILURE_RATE env var)");
        FraudPaymentService paymentService = new FakeCardPaymentService(paymentFailRate);

        //6. Create activity implementations
        FraudDetectionActivityImpl fraudActivity = new FraudDetectionActivityImpl(fraudAgent);
        PaymentChargeActivityImpl paymentChargeActivity = new PaymentChargeActivityImpl(paymentService);
        ConfirmationMessageActivityImpl confirmationActivity = new ConfirmationMessageActivityImpl(confirmationAgent);

        //7. Register activities
        worker.registerActivitiesImplementations(fraudActivity, paymentChargeActivity, confirmationActivity);

        //8. Start the worker
        factory.start();
        System.out.println("Started worker.");
        System.out.println("task queue: " + TASK_QUEUE);
    }
}
