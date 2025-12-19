# Order Processing Agents

A progressive demo of an order-processing system built in Java. This project demonstrates evolving from a naive in-process workflow to a sophisticated system with AI agents and Temporal workflows.

## Project Overview

This repository showcases the evolution of an order processing system:

**Current State**: A naive, synchronous order processing workflow without Temporal, HTTP APIs, or retries. This is intentional to highlight problems that Temporal will later solve.

**Future Plans**:
1. Add AI agents for decision support and customer communication
2. Migrate to Temporal workflows for durability, retries, and observability
3. Optionally add HTTP API layer

## Repository Structure

```
order-processing-agents/
├── order-agents/          # Main Java application directory
│   └── CLAUDE.md         # Detailed development guide
└── README.md             # This file
```

## Getting Started

### Prerequisites

- Java 24 or higher
- Maven 3.8+

### Build the Project

```bash
cd order-agents
mvn clean package
```

### Run Scenarios

#### Without Temporal

The application accepts a single CLI argument to control execution paths:

**Mixed scenario** (default - one item in stock, one out of stock):

If one of the items is out of stock, the workflow stops while we resolve the issue. Then we need to re-run the workflow
after the item is back in stock.

```bash 
  # Mixed scenario
  mvn exec:java -Dexec.args="mixed"
```

**All items in stock**:
```bash
  # In-stock scenario
  mvn exec:java -Dexec.args="in-stock"
```

**Out of stock**:
```bash
mvn exec:java -Dexec.args="out-of-stock"
```

**Run with Java directly**:
```bash
java -cp target/order-processing-agents-0.1.0-SNAPSHOT.jar com.nadvolod.order.OrderProcessingApp in-stock
```

#### With Temporal

```bash
# Terminal 1: Start Temporal Worker
mvn exec:java@worker
```

```bash
# Terminal 2: Start Temporal Workflow (in a new terminal)
mvn exec:java@workflow
```

There's also the option to use Temporal CLI (but it's more complicated)

```bash
  temporal workflow start \
    --task-queue order-fulfillment \
    --type OrderFulfillmentWorkflow \
    --workflow-id order-cli-test \
    --input '{"orderId":"order-123","items":[{"sku":"sku-123","quantity":1}]}'
```

```bash
  # Check workflow status
  temporal workflow describe --workflow-id order-cli-test

  # View workflow execution history
  temporal workflow show --workflow-id order-cli-test

```

## Understanding Temporal Value

Temporal saves you from writing thousands of lines of distributed systems infrastructure code. Here's what you'd have to build **without** Temporal vs. **with** Temporal.

### Your Current Workflow (Simplified)

```
OrderRequest → [Process Order] → [AI Analysis] → [Generate Message] → Result
```

Seems simple, right? But here's what's happening behind the scenes...

### Without Temporal - What You'd Have to Build

#### 1. Durable State Management

You'd need database tables to track workflow state:

```sql
CREATE TABLE workflow_state (
    workflow_id VARCHAR PRIMARY KEY,
    status VARCHAR,
    current_step VARCHAR,
    order_data JSON,
    ai_analysis JSON,
    customer_message TEXT,
    retry_count INT,
    last_error TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE activity_executions (
    id VARCHAR PRIMARY KEY,
    workflow_id VARCHAR,
    activity_name VARCHAR,
    status VARCHAR,
    input JSON,
    output JSON,
    attempt_number INT,
    error TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);
```

**Manual Code Required:**
- Save state after every step
- Handle database failures
- Ensure exactly-once semantics
- Clean up old records
- **~200 lines of code**

#### 2. Retry Logic with Exponential Backoff

```java
public AgentAdvice callAiWithRetries(OrderRequest req, OrderResponse resp) {
    int maxAttempts = 3;
    int attempt = 0;
    long backoffMs = 1000; // Start with 1 second

    while (attempt < maxAttempts) {
        try {
            return aiAgent.explain(req, resp);
        } catch (Exception e) {
            attempt++;
            if (attempt >= maxAttempts) {
                throw new RuntimeException("Failed after " + maxAttempts + " attempts", e);
            }
            logFailure(e, attempt);

            try {
                Thread.sleep(backoffMs);
                backoffMs *= 2; // Exponential backoff
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
            }
        }
    }
}
```

**~150 lines per activity** × number of activities

**What Temporal Does:**
```java
// Configure once - Temporal handles everything
ActivityOptions.newBuilder()
    .setRetryOptions(
        RetryOptions.newBuilder()
            .setMaximumAttempts(3)
            .setInitialInterval(Duration.ofSeconds(1))
            .setBackoffCoefficient(2.0)
            .build()
    )
```

#### 3. Timeout Management

```java
public AgentAdvice callAiWithTimeout(OrderRequest req, OrderResponse resp) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<AgentAdvice> future = executor.submit(() -> aiAgent.explain(req, resp));

    try {
        return future.get(30, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        future.cancel(true);
        // Now what? Retry? Save state? Notify user?
        throw new RuntimeException("AI call timed out", e);
    } finally {
        executor.shutdown();
    }
}
```

**~100 lines per activity**

**What Temporal Does:**
```java
.setStartToCloseTimeout(Duration.ofSeconds(30))  // Done!
```

#### 4. Recovery from Failures

**Without Temporal:**
```java
// On server restart, you need to:
public void recoverWorkflows() {
    // 1. Load all in-progress workflows from database
    List<WorkflowState> incompleteWorkflows =
        db.query("SELECT * FROM workflow_state WHERE status != 'COMPLETED'");

    // 2. Determine where each workflow left off
    for (WorkflowState state : incompleteWorkflows) {
        switch (state.getCurrentStep()) {
            case "process_order_complete":
                retryAiAnalysis(state);
                break;
            case "ai_analysis_complete":
                retryMessageGeneration(state);
                break;
            // ... more cases
        }
    }
}
```

**~200 lines of recovery logic**

**What Temporal Does:**
- Workflow state is **automatically** persisted
- On failure/restart, workflows **automatically** resume
- **Zero code needed**

### Code Size Comparison

#### Without Temporal
~1000+ lines of infrastructure code:
- State persistence layer: **200+ lines**
- Retry mechanisms: **150+ lines**
- Timeout handling: **100+ lines**
- Recovery logic: **200+ lines**
- Queue management: **150+ lines**
- Monitoring/observability: **200+ lines**
- **Plus ongoing maintenance!**

#### With Temporal
**~30 lines** of business logic:
```java
public WorkflowResult processOrder(OrderRequest request) {
    OrderResponse response = processOrderLogic(request);
    AgentAdvice advice = aiActivities.explain(request, response);
    String message = aiActivities.generateCustomerMessage(advice);
    return new WorkflowResult(response, advice, message);
}
```

Temporal handles:
- ✓ State persistence
- ✓ Automatic retries with backoff
- ✓ Timeout management
- ✓ Failure recovery
- ✓ Distributed coordination
- ✓ Monitoring and observability

### When Temporal Really Shines

1. **Long-running workflows** (hours, days, months)
   - Customer onboarding flows
   - Subscription billing cycles
   - Multi-day approval processes

2. **Complex error handling**
   - Payment processing with retries
   - External API calls that can fail
   - Multi-step sagas with compensation

3. **Human-in-the-loop**
   - Approval workflows
   - Customer support escalations
   - Manual review steps

4. **High reliability requirements**
   - Financial transactions
   - Critical business processes
   - Compliance-heavy workflows

### Your AI Workflow: What Temporal Saves You

- ✓ **If OpenAI API is down** → Automatic retries with backoff
- ✓ **If server crashes mid-workflow** → Resumes from last activity
- ✓ **If AI call times out** → Automatic timeout handling
- ✓ **Full execution history** → See exactly what happened and when
- ✓ **Built-in monitoring** → UI to visualize workflow execution

**Without writing:**
- Database schema for state
- Retry loops
- Timeout wrappers
- Recovery procedures
- Monitoring dashboards

**That's the value:** You write **business logic**, Temporal handles **distributed systems complexity**.




## Architecture Overview

### Event Flow Diagram

The following diagram illustrates the complete order processing workflow with all events and decision points:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              Order Processing Workflow                          │
└─────────────────────────────────────────────────────────────────────────────────┘

                                  ┌─────────────┐
                                  │ CLI Command │
                                  │  (Scenario) │
                                  └──────┬──────┘
                                         │
                                         ▼
                              ┌────────────────────┐
                              │  OrderRequest      │
                              │  - orderId         │
                              │  - items[]         │
                              │    (SKU, quantity) │
                              └─────────┬──────────┘
                                        │
                                        ▼
                        ┌───────────────────────────────┐
                        │  STEP 1: Inventory Check      │
                        │  (InventoryService)           │
                        │  Check each SKU availability  │
                        └──────────┬────────────────────┘
                                   │
                        ┌──────────┴──────────┐
                        │                     │
                   All Available        Any Unavailable
                        │                     │
                        ▼                     ▼
              ┌──────────────────┐   ┌────────────────────┐
              │ STEP 2: Payment  │   │  Return Response   │
              │ (PaymentService) │   │  Status: REJECTED  │
              │ Process $amount  │   │  - lineStatuses    │
              └────────┬─────────┘   │  - payment: null   │
                       │             │  - shipment: null  │
            ┌──────────┴────────┐   └──────────┬─────────┘
            │                   │               │
       Success             Random Fail          │
            │                   │               │
            ▼                   ▼               │
   ┌──────────────────┐ ┌────────────────────┐ │
   │ STEP 3: Shipping │ │  Return Response   │ │
   │ (ShippingService)│ │  Status: REJECTED  │ │
   │ Create shipment  │ │  - lineStatuses    │ │
   └────────┬─────────┘ │  - paymentResult   │ │
            │           │  - shipment: null  │ │
  ┌─────────┴─────────┐ └──────────┬─────────┘ │
  │                   │            │            │
Success          Random Fail       │            │
  │                   │            │            │
  ▼                   ▼            │            │
┌─────────────┐ ┌─────────────┐   │            │
│  Response   │ │  Response   │   │            │
│  ACCEPTED   │ │  REJECTED   │   │            │
│ All fields  │ │  Partial    │   │            │
│  populated  │ │   fields    │   │            │
└──────┬──────┘ └──────┬──────┘   │            │
       │               │           │            │
       └───────────────┴───────────┴────────────┘
                       │
                       ▼
            ┌─────────────────────────┐
            │  AI Agent (OpenAI)      │
            │  Analyze order outcome  │
            │  Generate explanation   │
            └──────────┬──────────────┘
                       │
                       ▼
            ┌─────────────────────────┐
            │  AgentAdvice            │
            │  - summary              │
            │  - recommendedActions[] │
            │  - customerMessage      │
            └──────────┬──────────────┘
                       │
                       ▼
            ┌─────────────────────────┐
            │  Console Output         │
            │  Display final status   │
            │  and AI recommendations │
            └─────────────────────────┘

Legend:
  ┌─────┐
  │ Box │  = Processing step or data structure
  └─────┘
    ▼     = Flow direction
  ───┬──  = Decision point (branch)
```

### Core Workflow Pattern

The system implements a naive three-step orchestration pattern:

```
OrderRequest → Inventory Check → Payment Processing → Shipment Creation → OrderResponse
```

Each step can fail and will halt the entire workflow. This fragility is intentional to demonstrate the problems that Temporal workflows solve.

### Key Components

**OrderProcessor**: The central naive workflow orchestrator containing all control flow. This class will map 1:1 to a Temporal Workflow in future iterations. Currently has:
- No retry logic
- No state persistence
- No crash recovery

**Service Layer**: Three services following interface-based design:
- `InventoryService` - checks SKU availability (in-memory implementation)
- `PaymentService` - simulates payment gateway (intentionally flaky with random failures)
- `ShippingService` - simulates shipment creation (mostly succeeds)

**Domain Models**: Java records representing order state:
- `OrderRequest` / `OrderResponse` - input/output contracts
- `OrderLineStatus` - per-SKU inventory results
- `PaymentResult` / `ShipmentDetails` - step-specific outcomes

### Partial Response Pattern

`OrderResponse` uses nullable fields to show workflow progress:
- `lineStatuses` - always populated
- `paymentResult` - null if inventory failed
- `shipmentDetails` - null if inventory or payment failed

This makes it easy to see exactly where the workflow stopped.

### Important Notes

When adding new code to the workflows:
1. Stop the worker
2. `mvn clean compile`
3. Restart the worker
4. Run the workflow

### Scenario-Driven Testing

The application provides three hardcoded scenarios using in-memory inventory data:
- **SKU-123**: 10 units in stock
- **SKU-456**: 0 units in stock
- **SKU-789**: 5 units in stock

## Technology Stack

- Java 24
- Maven
- Jackson for JSON
- OkHttp for HTTP client
- Spring Boot Web (for future API layer)
- Lombok (optional)

## Known Limitations (Intentional)

These are teaching points, not bugs:
- ❌ No retries on failures
- ❌ No state persistence
- ❌ No recovery from partial failures (e.g., payment succeeds but shipping fails)
- ❌ No long-running workflow support
- ❌ No visibility beyond console logs
- ❌ No transaction rollback or compensation logic

These limitations will be addressed when migrating to Temporal workflows.

## What NOT to Add

⚠️ **Important**: Do not "fix" the naive implementation unless explicitly requested:
- ❌ No retry logic
- ❌ No state persistence
- ❌ No transaction handling
- ❌ No HTTP endpoints (unless part of planned evolution)
- ❌ No complex error handling

Keep it simple and fragile as a baseline before introducing Temporal.

## Future Roadmap

1. **Phase 2**: Add AI agents for decision support and customer communication
2. **Phase 3**: Migrate to Temporal workflows for durability and retries
3. **Phase 4**: Add HTTP API layer (optional)

## Documentation

For detailed development guidance, see [`order-agents/CLAUDE.md`](order-agents/CLAUDE.md).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
