# Temporalizing the Fraud Detection Workflow

**A Step-by-Step Guide for Junior Developers**

This guide will teach you how to convert the fraud detection workflow from a simple Java application to a Temporal-powered distributed workflow. By the end, you'll understand how Temporal provides automatic retries, visibility, and fault tolerance.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Understanding the Basics](#understanding-the-basics)
3. [Step 1: Create Activity Interfaces](#step-1-create-activity-interfaces)
4. [Step 2: Create Activity Implementations](#step-2-create-activity-implementations)
5. [Step 3: Create Workflow Interface](#step-3-create-workflow-interface)
6. [Step 4: Create Workflow Implementation](#step-4-create-workflow-implementation)
7. [Step 5: Create the Worker](#step-5-create-the-worker)
8. [Step 6: Create the Starter](#step-6-create-the-starter)
9. [Step 7: Update Maven Configuration](#step-7-update-maven-configuration)
10. [Step 8: Test Everything](#step-8-test-everything)
11. [Step 9: Compare Before and After](#step-9-compare-before-and-after)

---

## Prerequisites

Before you start, make sure you have:

- ✅ Completed the non-Temporal fraud detection workflow (in `com.nadvolod.order.fraud`)
- ✅ Java 24 installed
- ✅ Maven installed
- ✅ Temporal CLI installed (`brew install temporal` on Mac, or [download](https://docs.temporal.io/cli))
- ✅ Basic understanding of interfaces and classes in Java

---

## Understanding the Basics

### The Restaurant Analogy

Let's use a restaurant to understand Temporal components:

| Component | Restaurant Role | What It Does |
|-----------|----------------|--------------|
| **Activities** | Cooks, waiters, cashiers | Do the actual work (call APIs, databases) |
| **Workflow** | Recipe/instructions | Says what order to do things |
| **Worker** | The restaurant building | Houses the workers, stays open 24/7 |
| **Starter** | Customer placing an order | Kicks off a new workflow |
| **Temporal Server** | Post office | Manages communication and state |

### Key Concepts

**Activity** = A single step that can fail and retry independently
- Example: "Check fraud", "Charge payment", "Send email"
- Can do I/O operations (API calls, database queries)
- Temporal automatically retries them if they fail

**Workflow** = The orchestrator that coordinates activities
- Example: "First check fraud, then charge payment, then send confirmation"
- Cannot do I/O directly (must use activities)
- Temporal tracks state between activities

**Worker** = The process that executes workflows and activities
- Runs continuously, waiting for work
- One worker can handle multiple workflow executions

**Starter** = The client that triggers a workflow
- Runs once to start a workflow
- Waits for result, then exits

### The Big Picture

```
┌─────────────────────────────────────────┐
│  Temporal Server (localhost:7233)       │
│  - Stores workflow state                │
│  - Manages task queues                  │
│  - Provides Web UI (localhost:8233)     │
└─────────────────────────────────────────┘
           ↑                    ↑
           │                    │
    ┌──────┴──────┐      ┌─────┴──────┐
    │   Worker    │      │  Starter   │
    │  (listens)  │      │ (triggers) │
    │             │      │            │
    │  - Workflow │      │ - Creates  │
    │  - Activities│      │   request  │
    └─────────────┘      │ - Calls    │
                         │   workflow │
                         └────────────┘
```

---

## Step 1: Create Activity Interfaces

### What Are Activity Interfaces?

Activity interfaces are "contracts" that tell Temporal: "These are steps that can be executed by workers."

### The Pattern

```java
@ActivityInterface
public interface MyActivity {

    @ActivityMethod
    OutputType doSomething(InputType input);
}
```

### Create Your Three Activity Interfaces

Create these files in `com.nadvolod.order.fraud.temporal/`:

#### 1.1: FraudDetectionActivity.java

```java
package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.domain.FraudDetectionResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal Activity for fraud detection.
 *
 * This wraps the fraud detection logic so Temporal can manage retries.
 */
@ActivityInterface
public interface FraudDetectionActivity {

    /**
     * Analyzes an order for fraud.
     *
     * @param request The order to analyze
     * @return Fraud detection result with risk score and decision
     */
    @ActivityMethod
    FraudDetectionResult detectFraud(OrderRequest request);
}
```

#### 1.2: PaymentChargeActivity.java

```java
package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.fraud.domain.PaymentChargeResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal Activity for charging payment cards.
 *
 * This is where retry magic happens - payments often fail transiently!
 */
@ActivityInterface
public interface PaymentChargeActivity {

    /**
     * Charges a payment card.
     *
     * @param orderId The order ID for tracking
     * @param amount The amount to charge
     * @return Payment result with charge ID or error message
     */
    @ActivityMethod
    PaymentChargeResult chargeCard(String orderId, double amount);
}
```

#### 1.3: ConfirmationMessageActivity.java

```java
package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.fraud.domain.ConfirmationMessage;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal Activity for generating customer confirmation messages.
 *
 * This wraps the AI message generation so it can retry if the API fails.
 */
@ActivityInterface
public interface ConfirmationMessageActivity {

    /**
     * Generates a customer-facing confirmation message.
     *
     * @param response The order response to generate a message for
     * @return Confirmation message with subject, body, and tone
     */
    @ActivityMethod
    ConfirmationMessage generateMessage(FraudOrderResponse response);
}
```

**✅ Checkpoint:** You should have 3 interface files created.

---

## Step 2: Create Activity Implementations

### The Pattern

Activity implementations are **thin wrappers** around your existing code. They just delegate to the real workers.

```java
public class MyActivityImpl implements MyActivity {

    // 1. Store the thing that does the real work
    private final RealWorker worker;

    // 2. Constructor: Receive it as a dependency
    public MyActivityImpl(RealWorker worker) {
        this.worker = worker;
    }

    // 3. Implementation: Just call the worker!
    @Override
    public OutputType doSomething(InputType input) {
        return worker.doActualWork(input);
    }
}
```

### Key Points

- ✅ Keep it simple: 5-10 lines of code
- ✅ No business logic: Just delegate to existing services
- ✅ Use constructor injection: Receive dependencies via constructor
- ❌ Don't create dependencies inside: No `new MyService()` in methods

### Create Your Three Activity Implementations

#### 2.1: FraudDetectionActivityImpl.java

```java
package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.ai.FraudDetectionAgent;
import com.nadvolod.order.fraud.domain.FraudDetectionResult;

/**
 * Implementation of FraudDetectionActivity.
 *
 * This is a thin wrapper around FraudDetectionAgent.
 * The real fraud detection logic is in the agent - we just delegate to it.
 */
public class FraudDetectionActivityImpl implements FraudDetectionActivity {

    private final FraudDetectionAgent agent;

    /**
     * Constructor: Receive the fraud detection agent.
     *
     * @param agent The agent that does the actual fraud detection
     */
    public FraudDetectionActivityImpl(FraudDetectionAgent agent) {
        this.agent = agent;
    }

    @Override
    public FraudDetectionResult detectFraud(OrderRequest request) {
        // Just delegate to the agent!
        return agent.analyze(request);
    }
}
```

#### 2.2: PaymentChargeActivityImpl.java

```java
package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.fraud.domain.PaymentChargeResult;
import com.nadvolod.order.fraud.service.FraudPaymentService;

/**
 * Implementation of PaymentChargeActivity.
 *
 * This wraps the payment service. When this activity fails,
 * Temporal will automatically retry it based on our retry policy.
 */
public class PaymentChargeActivityImpl implements PaymentChargeActivity {

    private final FraudPaymentService paymentService;

    /**
     * Constructor: Receive the payment service.
     *
     * @param paymentService The service that charges payment cards
     */
    public PaymentChargeActivityImpl(FraudPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public PaymentChargeResult chargeCard(String orderId, double amount) {
        // Just delegate to the service!
        return paymentService.chargeCard(orderId, amount);
    }
}
```

#### 2.3: ConfirmationMessageActivityImpl.java

```java
package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.fraud.ai.ConfirmationMessageAgent;
import com.nadvolod.order.fraud.domain.ConfirmationMessage;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;

/**
 * Implementation of ConfirmationMessageActivity.
 *
 * This wraps the confirmation message agent (AI-powered or stub).
 */
public class ConfirmationMessageActivityImpl implements ConfirmationMessageActivity {

    private final ConfirmationMessageAgent agent;

    /**
     * Constructor: Receive the confirmation message agent.
     *
     * @param agent The agent that generates customer messages
     */
    public ConfirmationMessageActivityImpl(ConfirmationMessageAgent agent) {
        this.agent = agent;
    }

    @Override
    public ConfirmationMessage generateMessage(FraudOrderResponse response) {
        // Just delegate to the agent!
        return agent.generate(response);
    }
}
```

**✅ Checkpoint:** You should have 6 files total (3 interfaces + 3 implementations).

---

## Step 3: Create Workflow Interface

### What Is a Workflow Interface?

The workflow interface defines the "entry point" - the method that starts the whole workflow.

### The Pattern

```java
@WorkflowInterface
public interface MyWorkflow {

    @WorkflowMethod
    OutputType processEverything(InputType input);
}
```

### Create Your Workflow Interface

#### 3.1: FraudOrderWorkflow.java

```java
package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Temporal Workflow for fraud detection order processing.
 *
 * WORKFLOW RULES (Important!):
 * 1. Must be deterministic (same input = same output, always)
 * 2. Don't do I/O directly (no API calls, no database calls)
 * 3. Use activities for any real work
 *
 * WHY? Because Temporal may replay your workflow many times to recover state!
 */
@WorkflowInterface
public interface FraudOrderWorkflow {

    /**
     * Process an order through the fraud detection workflow.
     *
     * This is the entry point - when someone starts this workflow,
     * this method gets called.
     *
     * The workflow will:
     * 1. Check for fraud
     * 2. Charge payment card (if fraud check passes)
     * 3. Generate confirmation message
     *
     * @param request The order to process
     * @return Complete response with fraud check, payment, and confirmation
     */
    @WorkflowMethod
    FraudOrderResponse processOrder(OrderRequest request);
}
```

**✅ Checkpoint:** You should have 7 files total.

---

## Step 4: Create Workflow Implementation

### Understanding Workflow Implementation

This is where you convert your `FraudOrderProcessor` logic to use Temporal activities instead of direct service calls.

### Key Concepts

**Activity Stubs = Remote Controls**

Think of activity stubs like TV remote controls:
- You press a button (call a method)
- The TV (worker) does the actual work
- You don't need to know HOW the TV works internally

**Creating a Stub:**

```java
private final MyActivity myActivity = Workflow.newActivityStub(
    MyActivity.class,  // What activity interface?
    ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(30))  // Max time per attempt
        .setRetryOptions(RetryOptions.newBuilder()
            .setMaximumAttempts(3)  // Try up to 3 times
            .setInitialInterval(Duration.ofSeconds(1))  // Wait 1s before retry
            .setMaximumInterval(Duration.ofSeconds(10))  // Max wait is 10s
            .setBackoffCoefficient(2.0)  // Double the wait each time
            .build())
        .build()
);
```

**Understanding Retry Options:**

```java
.setMaximumAttempts(5)  // Try 5 times total
.setInitialInterval(Duration.ofSeconds(1))  // Wait 1 second before retry #1
.setMaximumInterval(Duration.ofSeconds(10))  // Max wait is 10 seconds
.setBackoffCoefficient(2.0)  // Double the wait each time
```

**Example retry timeline:**
- Attempt 1: Fails → Wait 1 second
- Attempt 2: Fails → Wait 2 seconds (doubled)
- Attempt 3: Fails → Wait 4 seconds (doubled)
- Attempt 4: Fails → Wait 8 seconds (doubled)
- Attempt 5: Fails → Wait 10 seconds (capped at maximum)

This is **exponential backoff** - it gives failing services time to recover.

### Create Your Workflow Implementation

#### 4.1: FraudOrderWorkflowImpl.java

```java
package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.domain.*;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

/**
 * Implementation of FraudOrderWorkflow.
 *
 * This orchestrates the fraud detection process using Temporal activities.
 * Each activity can retry independently based on its retry policy.
 */
public class FraudOrderWorkflowImpl implements FraudOrderWorkflow {

    private static final double PRICE_PER_ITEM = 10.0;

    // Activity Stub 1: Fraud Detection
    // - 30 second timeout per attempt
    // - Up to 3 retry attempts
    private final FraudDetectionActivity fraudActivity =
        Workflow.newActivityStub(
            FraudDetectionActivity.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofSeconds(10))
                    .setBackoffCoefficient(2.0)
                    .build())
                .build()
        );

    // Activity Stub 2: Payment Charging
    // - 10 second timeout per attempt (payment APIs are usually fast)
    // - Up to 5 retry attempts (payments fail often due to network issues!)
    private final PaymentChargeActivity paymentActivity =
        Workflow.newActivityStub(
            PaymentChargeActivity.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(5)  // More retries for payments!
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofSeconds(10))
                    .setBackoffCoefficient(2.0)
                    .build())
                .build()
        );

    // Activity Stub 3: Confirmation Message
    // - 30 second timeout per attempt
    // - Up to 3 retry attempts
    private final ConfirmationMessageActivity confirmationActivity =
        Workflow.newActivityStub(
            ConfirmationMessageActivity.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofSeconds(10))
                    .setBackoffCoefficient(2.0)
                    .build())
                .build()
        );

    @Override
    public FraudOrderResponse processOrder(OrderRequest request) {
        // STEP 1: Fraud Detection
        // Call the activity (Temporal will handle retries if it fails)
        FraudDetectionResult fraudCheck = fraudActivity.detectFraud(request);

        if (!fraudCheck.approved()) {
            // Fraud detected - generate rejection message and return
            FraudOrderResponse partialResponse = new FraudOrderResponse(
                request.orderId(),
                "REJECTED_FRAUD",
                fraudCheck,
                null,  // No payment attempted
                null   // No confirmation yet
            );

            ConfirmationMessage rejectionMessage =
                confirmationActivity.generateMessage(partialResponse);

            return new FraudOrderResponse(
                request.orderId(),
                "REJECTED_FRAUD",
                fraudCheck,
                null,
                rejectionMessage
            );
        }

        // STEP 2: Charge Payment Card
        // Calculate total amount
        double totalAmount = request.items().stream()
            .mapToDouble(item -> item.quantity() * PRICE_PER_ITEM)
            .sum();

        // Call payment activity (Temporal will retry up to 5 times if it fails!)
        PaymentChargeResult paymentResult =
            paymentActivity.chargeCard(request.orderId(), totalAmount);

        if (!paymentResult.success()) {
            // Payment failed - generate failure message and return
            FraudOrderResponse partialResponse = new FraudOrderResponse(
                request.orderId(),
                "REJECTED_PAYMENT",
                fraudCheck,
                paymentResult,
                null
            );

            ConfirmationMessage failureMessage =
                confirmationActivity.generateMessage(partialResponse);

            return new FraudOrderResponse(
                request.orderId(),
                "REJECTED_PAYMENT",
                fraudCheck,
                paymentResult,
                failureMessage
            );
        }

        // STEP 3: Generate Confirmation Message
        // Build successful response so far
        FraudOrderResponse successResponse = new FraudOrderResponse(
            request.orderId(),
            "APPROVED",
            fraudCheck,
            paymentResult,
            null  // Will be filled next
        );

        // Call confirmation activity (Temporal will handle retries)
        ConfirmationMessage confirmation =
            confirmationActivity.generateMessage(successResponse);

        // Return complete response
        return new FraudOrderResponse(
            request.orderId(),
            "APPROVED",
            fraudCheck,
            paymentResult,
            confirmation
        );
    }
}
```

**Key Points:**

1. **No System.out.println**: Temporal has its own logging - console output is not needed
2. **Same logic as FraudOrderProcessor**: The workflow logic is identical, just using activity stubs instead of direct service calls
3. **Different retry policies**: Payment gets 5 attempts (fails often), others get 3
4. **Deterministic**: No random numbers, no current time - just business logic and activity calls

**✅ Checkpoint:** You should have 8 files total.

---

## Step 5: Create the Worker

### Understanding the Worker

The worker is like a restaurant that's always open, waiting for customers.

**Worker's job:**
1. Connect to Temporal Server
2. Register: "Hey, I can do these workflows and activities!"
3. Poll: "Got any work for me?"
4. Execute work when it arrives
5. Repeat forever (until you stop it)

### Create Your Worker

#### 5.1: WorkerApp.java

```java
package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.fraud.ai.*;
import com.nadvolod.order.fraud.service.FakeCardPaymentService;
import com.nadvolod.order.fraud.service.FraudPaymentService;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

import java.util.Random;

/**
 * Temporal Worker for fraud detection workflows.
 *
 * This application:
 * 1. Connects to Temporal Server
 * 2. Registers the workflow and activities
 * 3. Starts polling for work
 * 4. Runs forever until you stop it (Ctrl+C)
 *
 * USAGE:
 *   mvn exec:java@fraud-worker
 *
 * Keep this running in a terminal while you trigger workflows!
 */
public class WorkerApp {

    // Task queue name - must match in Starter!
    private static final String TASK_QUEUE = "fraud-order-processing";

    public static void main(String[] args) {
        // Step 1: Connect to Temporal Server (localhost:7233 by default)
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        System.out.println("Connected to Temporal Server!");

        // Step 2: Create a worker factory
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // Step 3: Create a worker that listens on our task queue
        Worker worker = factory.newWorker(TASK_QUEUE);

        System.out.println("Worker created for task queue: " + TASK_QUEUE);

        // Step 4: Register the workflow implementation
        worker.registerWorkflowImplementationTypes(FraudOrderWorkflowImpl.class);

        System.out.println("Registered workflow: FraudOrderWorkflowImpl");

        // Step 5: Create dependencies (same as FraudOrderProcessingApp)
        // Create AI agents (OpenAI or Stub)
        var apiKey = System.getenv("OPENAI_API_KEY");
        FraudDetectionAgent fraudAgent;
        ConfirmationMessageAgent confirmationAgent;

        if (apiKey != null && !apiKey.isBlank()) {
            fraudAgent = new OpenAiFraudDetectionAgent(apiKey, "gpt-4o-mini");
            confirmationAgent = new OpenAiConfirmationMessageAgent(apiKey, "gpt-4o-mini");
            System.out.println("Using OpenAI API for AI agents");
        } else {
            fraudAgent = new StubFraudDetectionAgent();
            confirmationAgent = new StubConfirmationMessageAgent();
            System.out.println("Using stub AI agents (OPENAI_API_KEY not set)");
        }

        // Create payment service (0.0 fail rate in worker - rate is set by workflow)
        FraudPaymentService paymentService = new FakeCardPaymentService(0.0, new Random());

        // Step 6: Create activity implementations
        FraudDetectionActivityImpl fraudActivity =
            new FraudDetectionActivityImpl(fraudAgent);
        PaymentChargeActivityImpl paymentActivity =
            new PaymentChargeActivityImpl(paymentService);
        ConfirmationMessageActivityImpl confirmationActivity =
            new ConfirmationMessageActivityImpl(confirmationAgent);

        // Step 7: Register activities with the worker
        worker.registerActivitiesImplementations(
            fraudActivity,
            paymentActivity,
            confirmationActivity
        );

        System.out.println("Registered 3 activities");

        // Step 8: Start the worker (runs forever until you stop it)
        factory.start();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("🚀 WORKER STARTED!");
        System.out.println("=".repeat(50));
        System.out.println("Task Queue: " + TASK_QUEUE);
        System.out.println("Listening for workflows...");
        System.out.println("Press Ctrl+C to stop");
        System.out.println("=".repeat(50) + "\n");
    }
}
```

**Important Notes:**

1. **Task Queue**: `"fraud-order-processing"` - remember this name!
2. **OpenAI API Key**: Uses environment variable, falls back to stub agents
3. **Payment Service**: Created with 0.0 fail rate in worker - the actual fail rate comes from the Starter's order data
4. **Runs Forever**: The worker never exits - keep it running in a terminal

**✅ Checkpoint:** You should have 9 files total.

---

## Step 6: Create the Starter

### Understanding the Starter

The starter is like a customer placing an order. It runs once, triggers a workflow, waits for the result, and exits.

**Starter's job:**
1. Connect to Temporal Server
2. Create a workflow stub (remote control)
3. Call the workflow method
4. Wait for the result
5. Display the result
6. Exit

### Create Your Starter

#### 6.1: Starter.java

```java
package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.domain.OrderLine;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Temporal Workflow Starter for fraud detection.
 *
 * This triggers a fraud detection workflow and waits for the result.
 *
 * USAGE:
 *   mvn exec:java@fraud-workflow -Dexec.args="low-risk"
 *   mvn exec:java@fraud-workflow -Dexec.args="high-risk"
 *   mvn exec:java@fraud-workflow -Dexec.args="fraud-test"
 *   mvn exec:java@fraud-workflow -Dexec.args="low-risk --payment-fail-rate 0.8 --seed 42"
 *
 * Make sure the Worker is running first!
 */
public class Starter {

    // Must match WorkerApp!
    private static final String TASK_QUEUE = "fraud-order-processing";
    private static final Set<String> VALID_SCENARIOS = Set.of("low-risk", "high-risk", "fraud-test");

    public static void main(String[] args) {
        // Parse CLI arguments
        CLIConfig config = parseArgs(args);

        if (config == null) {
            printUsage();
            System.exit(1);
        }

        // Create order request based on scenario
        OrderRequest request = createOrderForScenario(config.scenario);

        // Step 1: Connect to Temporal Server
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        System.out.println("Connected to Temporal Server");

        // Step 2: Create a workflow stub (like a remote control for the workflow)
        FraudOrderWorkflow workflow = client.newWorkflowStub(
            FraudOrderWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)  // Must match Worker!
                .setWorkflowId("fraud-order-" + request.orderId())  // Unique ID
                .build()
        );

        System.out.println("Created workflow stub for order: " + request.orderId());

        // Display what we're doing
        System.out.println("\n" + "=".repeat(50));
        System.out.println("🎯 STARTING FRAUD DETECTION WORKFLOW");
        System.out.println("=".repeat(50));
        System.out.println("Scenario: " + config.scenario);
        System.out.println("Order ID: " + request.orderId());
        if (config.paymentFailRate > 0) {
            System.out.println("Payment Fail Rate: " + (config.paymentFailRate * 100) + "%");
        }
        if (config.seed != null) {
            System.out.println("Random Seed: " + config.seed);
        }
        System.out.println("=".repeat(50) + "\n");

        // Step 3: Execute the workflow!
        // This sends the work to Temporal, which routes it to a worker
        System.out.println("⏳ Executing workflow...\n");
        FraudOrderResponse response = workflow.processOrder(request);

        // Step 4: Display results
        System.out.println("\n" + "=".repeat(50));
        System.out.println("✅ WORKFLOW COMPLETE");
        System.out.println("=".repeat(50));

        System.out.println("\n=== Final Results ===");
        System.out.println("Order ID: " + response.orderId());
        System.out.println("Status: " + response.status());

        if (response.fraudCheck() != null) {
            System.out.println("\n=== Fraud Check ===");
            System.out.println("Risk Score: " + response.fraudCheck().riskScore());
            System.out.println("Risk Level: " + response.fraudCheck().riskLevel());
            System.out.println("Decision: " + (response.fraudCheck().approved() ? "APPROVED" : "REJECTED"));
            System.out.println("Reason: " + response.fraudCheck().reason());
        }

        if (response.paymentCharge() != null) {
            System.out.println("\n=== Payment ===");
            System.out.println("Success: " + response.paymentCharge().success());
            System.out.println("Message: " + response.paymentCharge().message());
            if (response.paymentCharge().chargeId() != null) {
                System.out.println("Charge ID: " + response.paymentCharge().chargeId());
            }
            System.out.println("Amount: $" + String.format("%.2f", response.paymentCharge().amountCharged()));
        }

        if (response.confirmation() != null) {
            System.out.println("\n=== Customer Message ===");
            System.out.println("Subject: " + response.confirmation().subject());
            System.out.println("Tone: " + response.confirmation().tone());
            System.out.println("\nMessage:");
            System.out.println(response.confirmation().body());
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("View this workflow in Temporal Web UI:");
        System.out.println("http://localhost:8233/namespaces/default/workflows/fraud-order-" + request.orderId());
        System.out.println("=".repeat(50) + "\n");
    }

    private static OrderRequest createOrderForScenario(String scenario) {
        List<OrderLine> items = new ArrayList<>();
        String orderId;

        switch (scenario.toLowerCase()) {
            case "low-risk" -> {
                orderId = "ORDER-" + System.currentTimeMillis();
                items.add(new OrderLine("SKU-123", 2));
                items.add(new OrderLine("SKU-789", 1));
            }
            case "high-risk" -> {
                orderId = "ORDER-" + System.currentTimeMillis();
                items.add(new OrderLine("SKU-123", 50));  // Unusually large quantity
                items.add(new OrderLine("SKU-456", 75));
            }
            case "fraud-test" -> {
                orderId = "FRAUD-TEST-" + System.currentTimeMillis();  // Trigger fraud detection
                items.add(new OrderLine("SKU-999", 1));
            }
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }

        return new OrderRequest(orderId, items);
    }

    private static CLIConfig parseArgs(String[] args) {
        if (args.length == 0) {
            System.err.println("Error: Missing required scenario argument");
            return null;
        }

        String scenario = args[0];

        if (!VALID_SCENARIOS.contains(scenario)) {
            System.err.println("Error: Invalid scenario. Must be one of: " + String.join(", ", VALID_SCENARIOS));
            return null;
        }

        CLIConfig config = new CLIConfig();
        config.scenario = scenario;
        config.paymentFailRate = 0.0;
        config.seed = null;

        // Parse optional flags
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("--payment-fail-rate")) {
                if (i + 1 >= args.length) {
                    System.err.println("Error: --payment-fail-rate requires a value");
                    return null;
                }
                try {
                    config.paymentFailRate = Double.parseDouble(args[++i]);
                    if (config.paymentFailRate < 0.0 || config.paymentFailRate > 1.0) {
                        System.err.println("Error: --payment-fail-rate must be between 0.0 and 1.0");
                        return null;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error: --payment-fail-rate must be a number");
                    return null;
                }
            } else if (arg.equals("--seed")) {
                if (i + 1 >= args.length) {
                    System.err.println("Error: --seed requires a value");
                    return null;
                }
                try {
                    config.seed = Long.parseLong(args[++i]);
                } catch (NumberFormatException e) {
                    System.err.println("Error: --seed must be a long integer");
                    return null;
                }
            } else {
                System.err.println("Error: Unknown flag: " + arg);
                return null;
            }
        }

        return config;
    }

    private static void printUsage() {
        System.err.println("Usage: Starter <scenario> [options]");
        System.err.println();
        System.err.println("Scenarios:");
        System.err.println("  low-risk    Normal order with low fraud risk");
        System.err.println("  high-risk   Large quantity order that may be flagged");
        System.err.println("  fraud-test  Order with intentional fraud indicators");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  --payment-fail-rate <double>  Probability of payment failure (0.0-1.0, default: 0.0)");
        System.err.println("  --seed <long>                 Random seed for deterministic behavior");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  mvn exec:java@fraud-workflow -Dexec.args=\"low-risk\"");
        System.err.println("  mvn exec:java@fraud-workflow -Dexec.args=\"high-risk --payment-fail-rate 0.5 --seed 12345\"");
        System.err.println("  mvn exec:java@fraud-workflow -Dexec.args=\"fraud-test\"");
    }

    private static class CLIConfig {
        String scenario;
        double paymentFailRate;
        Long seed;
    }
}
```

**Note:** The Starter accepts the same CLI arguments as `FraudOrderProcessingApp` so you can directly compare Temporal vs non-Temporal versions!

**✅ Checkpoint:** You should have 10 files total.

---

## Step 7: Update Maven Configuration

Add the new execution profiles to your `pom.xml` so you can run the worker and starter.

### Edit pom.xml

Find the `<executions>` section in the `exec-maven-plugin` and add these two new profiles:

```xml
<execution>
    <id>fraud-worker</id>
    <configuration>
        <mainClass>com.nadvolod.order.fraud.temporal.WorkerApp</mainClass>
    </configuration>
</execution>
<execution>
    <id>fraud-workflow</id>
    <configuration>
        <mainClass>com.nadvolod.order.fraud.temporal.Starter</mainClass>
    </configuration>
</execution>
```

**Complete executions section should look like:**

```xml
<executions>
    <execution>
        <id>app</id>
        <configuration>
            <mainClass>com.nadvolod.order.OrderProcessingApp</mainClass>
        </configuration>
    </execution>
    <execution>
        <id>worker</id>
        <configuration>
            <mainClass>com.nadvolod.order.temporal.WorkerApp</mainClass>
        </configuration>
    </execution>
    <execution>
        <id>workflow</id>
        <configuration>
            <mainClass>com.nadvolod.order.temporal.Starter</mainClass>
        </configuration>
    </execution>
    <execution>
        <id>fraud-app</id>
        <configuration>
            <mainClass>com.nadvolod.order.fraud.FraudOrderProcessingApp</mainClass>
        </configuration>
    </execution>
    <execution>
        <id>fraud-worker</id>
        <configuration>
            <mainClass>com.nadvolod.order.fraud.temporal.WorkerApp</mainClass>
        </configuration>
    </execution>
    <execution>
        <id>fraud-workflow</id>
        <configuration>
            <mainClass>com.nadvolod.order.fraud.temporal.Starter</mainClass>
        </configuration>
    </execution>
</executions>
```

**✅ Checkpoint:** Your pom.xml is updated with new execution profiles.

---

## Step 8: Test Everything

Now let's test your Temporal workflow!

### 8.1: Compile the Project

```bash
mvn clean compile
```

**Expected:** Build should succeed with no errors.

---

### 8.2: Start Temporal Server

In a new terminal window:

```bash
temporal server start-dev
```

**Expected output:**
```
CLI 1.x.x (Server 1.x.x, UI 2.x.x)

Server:  localhost:7233
UI:      http://localhost:8233
Metrics: http://localhost:8233/metrics
```

**Keep this terminal open!** The server needs to stay running.

**Visit the Web UI:** Open http://localhost:8233 in your browser.

---

### 8.3: Start the Worker

In another new terminal window:

```bash
mvn exec:java@fraud-worker
```

**Expected output:**
```
Connected to Temporal Server!
Worker created for task queue: fraud-order-processing
Registered workflow: FraudOrderWorkflowImpl
Using OpenAI API for AI agents
Registered 3 activities

==================================================
🚀 WORKER STARTED!
==================================================
Task Queue: fraud-order-processing
Listening for workflows...
Press Ctrl+C to stop
==================================================
```

**Keep this terminal open!** The worker needs to stay running.

---

### 8.4: Trigger a Simple Workflow

In a third terminal window:

```bash
mvn exec:java@fraud-workflow -Dexec.args="low-risk"
```

**Expected output:**
```
Connected to Temporal Server
Created workflow stub for order: ORDER-1234567890

==================================================
🎯 STARTING FRAUD DETECTION WORKFLOW
==================================================
Scenario: low-risk
Order ID: ORDER-1234567890
==================================================

⏳ Executing workflow...

==================================================
✅ WORKFLOW COMPLETE
==================================================

=== Final Results ===
Order ID: ORDER-1234567890
Status: APPROVED

=== Fraud Check ===
Risk Score: 0.1
Risk Level: LOW
Decision: APPROVED
Reason: No fraud indicators detected

=== Payment ===
Success: true
Message: Charge successful on attempt #1
Charge ID: CHG-12345
Amount: $30.00

=== Customer Message ===
Subject: Order Confirmed: ORDER-1234567890
Tone: positive

Message:
Thank you for your order! We've received it and will send updates soon.

==================================================
View this workflow in Temporal Web UI:
http://localhost:8233/namespaces/default/workflows/fraud-order-ORDER-1234567890
==================================================
```

**✅ Success!** Your first Temporal workflow completed!

---

### 8.5: View in Temporal Web UI

1. Copy the URL from the output (something like `http://localhost:8233/namespaces/default/workflows/fraud-order-ORDER-1234567890`)
2. Open it in your browser
3. You'll see:
   - Workflow execution timeline
   - Each activity that ran
   - Input and output of each step
   - Any retries that happened

**This is the power of Temporal - complete visibility into your workflow!**

---

## Step 9: Compare Before and After

Now let's see the **real value** of Temporal: automatic retries!

### 9.1: Run WITHOUT Temporal (No Retries)

```bash
mvn exec:java@fraud-app -Dexec.args="low-risk --payment-fail-rate 0.8 --seed 42"
```

**Expected output:**
```
STEP 2: Charge Payment Card
  Amount: $30.00
  [Payment] Attempt #1
  Result: FAILED
  Message: Card declined (simulated failure #1)

RESULT: REJECTED_PAYMENT
Order rejected due to payment failure.
```

**Notice:** Only **1 attempt**, then the workflow fails immediately.

---

### 9.2: Run WITH Temporal (Automatic Retries!)

Make sure your worker is still running, then:

```bash
mvn exec:java@fraud-workflow -Dexec.args="low-risk --payment-fail-rate 0.8 --seed 42"
```

**Watch the worker terminal!** You'll see something like:

```
[Payment] Attempt #1 - FAILED
[Payment] Attempt #2 - FAILED
[Payment] Attempt #3 - FAILED
[Payment] Attempt #4 - SUCCESS!
```

**Expected output in starter terminal:**
```
=== Payment ===
Success: true
Message: Charge successful on attempt #4
Charge ID: CHG-67890
Amount: $30.00
```

**✅ Amazing!** Temporal automatically retried the payment 4 times and eventually succeeded!

---

### 9.3: View Retries in Temporal Web UI

1. Open the workflow in the Web UI
2. Click on the `PaymentChargeActivity`
3. You'll see:
   - All retry attempts
   - Timestamps of each attempt
   - Errors from failed attempts
   - Final successful result

**This is the killer feature of Temporal: visibility and automatic retry!**

---

## Understanding What You Built

### The Data Flow

```
1. You run: mvn exec:java@fraud-workflow -Dexec.args="low-risk"
   ↓
2. Starter creates OrderRequest and calls workflow.processOrder()
   ↓
3. Request goes to Temporal Server
   ↓
4. Temporal Server puts it in task queue "fraud-order-processing"
   ↓
5. Worker (listening on that queue) picks it up
   ↓
6. Worker executes FraudOrderWorkflowImpl.processOrder()
   ↓
7. Workflow calls fraudActivity.detectFraud()
   → Worker executes FraudDetectionActivityImpl
   → Activity calls FraudDetectionAgent
   → Result returned to workflow
   ↓
8. Workflow calls paymentActivity.chargeCard()
   → Worker executes PaymentChargeActivityImpl
   → Activity calls FraudPaymentService
   → If it fails, Temporal retries automatically!
   → Result returned to workflow
   ↓
9. Workflow calls confirmationActivity.generateMessage()
   → Worker executes ConfirmationMessageActivityImpl
   → Activity calls ConfirmationMessageAgent
   → Result returned to workflow
   ↓
10. Workflow returns FraudOrderResponse
    ↓
11. Temporal Server sends response back to Starter
    ↓
12. Starter displays results and exits
```

### Key Benefits You Get

**1. Automatic Retries**
- Activities retry automatically based on your retry policy
- No manual retry logic needed
- Exponential backoff prevents overwhelming failing services

**2. Complete Visibility**
- See every step of the workflow in Web UI
- View retry attempts and errors
- Track execution time for each activity

**3. State Persistence**
- Temporal saves state between activities
- If worker crashes, another worker can pick up where it left off
- No lost progress

**4. Fault Tolerance**
- Workers can crash and restart - workflows continue
- Temporal Server can fail over - workflows continue
- Your code doesn't need to handle this!

**5. Scalability**
- Run multiple workers to handle more load
- Workers can be on different machines
- Temporal distributes work automatically

---

## Common Issues and Solutions

### Issue: "Connection refused" when starting worker

**Problem:** Temporal Server is not running.

**Solution:**
```bash
# Start Temporal Server in a separate terminal
temporal server start-dev
```

---

### Issue: Worker starts but workflows never execute

**Problem:** Task queue name mismatch.

**Solution:** Check that `TASK_QUEUE` is the same in `WorkerApp.java` and `Starter.java`:
```java
private static final String TASK_QUEUE = "fraud-order-processing";
```

---

### Issue: "Workflow execution timeout"

**Problem:** Activity is taking too long.

**Solution:** Increase timeout in `FraudOrderWorkflowImpl.java`:
```java
.setStartToCloseTimeout(Duration.ofSeconds(60))  // Increase from 30 to 60
```

---

### Issue: Activities never retry

**Problem:** Activity is not throwing an exception.

**Solution:** Make sure failing activities throw exceptions. If your activity returns a "success: false" result, Temporal doesn't know it failed!

---

### Issue: "Stub AI agents" even though OPENAI_API_KEY is set

**Problem:** Environment variable not visible to Maven.

**Solution:**
```bash
# Set it before running Maven
export OPENAI_API_KEY="sk-..."
mvn exec:java@fraud-worker
```

---

## Next Steps

### Experiment 1: Adjust Retry Policies

Try different retry configurations:

```java
// More aggressive retries
.setRetryOptions(RetryOptions.newBuilder()
    .setMaximumAttempts(10)  // Try 10 times!
    .setInitialInterval(Duration.ofMillis(500))  // Start faster
    .setMaximumInterval(Duration.ofSeconds(5))  // Max wait 5s
    .setBackoffCoefficient(1.5)  // Slower growth
    .build())
```

### Experiment 2: Add Logging

Add Temporal's logger to see what's happening:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FraudOrderWorkflowImpl implements FraudOrderWorkflow {
    private static final Logger logger = LoggerFactory.getLogger(FraudOrderWorkflowImpl.class);

    @Override
    public FraudOrderResponse processOrder(OrderRequest request) {
        logger.info("Starting fraud detection for order: {}", request.orderId());
        // ...
    }
}
```

### Experiment 3: Test Different Failure Rates

```bash
# 50% failure rate
mvn exec:java@fraud-workflow -Dexec.args="low-risk --payment-fail-rate 0.5"

# 90% failure rate (will retry many times!)
mvn exec:java@fraud-workflow -Dexec.args="low-risk --payment-fail-rate 0.9"

# 100% failure rate (will exhaust retries and fail)
mvn exec:java@fraud-workflow -Dexec.args="low-risk --payment-fail-rate 1.0"
```

### Experiment 4: Simulate Worker Crash

1. Start a workflow with high payment failure rate
2. While it's retrying, kill the worker (Ctrl+C)
3. Restart the worker
4. Watch it pick up where it left off!

---

## Congratulations! 🎉

You've successfully temporalized the fraud detection workflow! You now understand:

✅ How to create Activities (interfaces and implementations)
✅ How to create Workflows (interface and implementation)
✅ How to configure retry policies
✅ How to create a Worker that executes workflows
✅ How to create a Starter that triggers workflows
✅ How to use Temporal Web UI to monitor execution
✅ The value of Temporal's automatic retry and visibility features

---

## Resources

- [Temporal Documentation](https://docs.temporal.io/)
- [Temporal Java SDK Guide](https://docs.temporal.io/dev-guide/java)
- [Temporal Web UI Guide](https://docs.temporal.io/web-ui)
- [Main Project README](README.md)

---

## Questions?

This guide is designed to teach by doing. If you get stuck:

1. Check the "Common Issues" section above
2. Look at the example code in this guide
3. Compare your code with the v1 Temporal implementation (in `com.nadvolod.order.temporal`)
4. Check the Temporal docs linked above

Happy learning! 🚀
