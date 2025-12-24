package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.fraud.ai.*;
import com.nadvolod.order.fraud.service.FakeCardPaymentService;
import com.nadvolod.order.fraud.service.FraudPaymentService;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

import java.util.Random;

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

        // Create payment service with configurable fail rate and seed from environment
        double paymentFailRate = 0.0;
        String failRateEnv = System.getenv("PAYMENT_FAIL_RATE");
        if (failRateEnv != null && !failRateEnv.isBlank()) {
            try {
                paymentFailRate = Double.parseDouble(failRateEnv);
                if (paymentFailRate < 0.0 || paymentFailRate > 1.0) {
                    System.err.println("[WARNING] PAYMENT_FAIL_RATE must be between 0.0 and 1.0, using default: 0.0");
                    paymentFailRate = 0.0;
                }
            } catch (NumberFormatException e) {
                System.err.println("[WARNING] Invalid PAYMENT_FAIL_RATE value, using default: 0.0");
            }
        }

        Random random;
        String seedEnv = System.getenv("RANDOM_SEED");
        if (seedEnv != null && !seedEnv.isBlank()) {
            try {
                long seed = Long.parseLong(seedEnv);
                random = new Random(seed);
                System.out.println("[INFO] Using random seed: " + seed);
            } catch (NumberFormatException e) {
                System.err.println("[WARNING] Invalid RANDOM_SEED value, using unseeded random");
                random = new Random();
            }
        } else {
            random = new Random();
        }

        System.out.println("[INFO] Payment fail rate: " + (paymentFailRate * 100) + "%");
        FraudPaymentService paymentService = new FakeCardPaymentService(paymentFailRate, random);

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
