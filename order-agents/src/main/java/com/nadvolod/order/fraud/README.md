# Fraud Detection Order Processing Workflow (V2)

## Overview

This package implements a simplified, AI-powered order processing workflow focused on fraud detection and payment processing. It's designed as a learning tool to demonstrate the value of Temporal workflow orchestration.

**Workflow Steps:**
```
OrderRequest → AI Fraud Detection → Charge Payment Card → Generate Confirmation Message → Result
```

## Why V2?

The original v1 workflow (`com.nadvolod.order`) focuses on inventory management with a 3-step process (inventory → payment → shipping). This v2 workflow is:

- **Simpler**: Only 3 steps instead of 5
- **More realistic**: Real-world fraud detection and payment scenarios
- **AI-first**: Uses OpenAI for fraud analysis and customer communication
- **Learning-focused**: Intentionally naive implementation to demonstrate Temporal's benefits

## Quick Start

### Prerequisites

- Java 24 or later
- Maven 3.x
- (Optional) OpenAI API key for AI-powered agents

### Running the Workflow

```bash
# Basic usage - low-risk order
mvn exec:java@fraud-app -Dexec.args="low-risk"

# High-risk order (large quantities)
mvn exec:java@fraud-app -Dexec.args="high-risk"

# Fraud test (intentionally suspicious order ID)
mvn exec:java@fraud-app -Dexec.args="fraud-test"

# Test payment failures (80% failure rate, seed for determinism)
mvn exec:java@fraud-app -Dexec.args="low-risk --payment-fail-rate 0.8 --seed 42"

# Use stub agents (no OpenAI API required)
OPENAI_API_KEY= mvn exec:java@fraud-app -Dexec.args="low-risk"
```

### Available Scenarios

| Scenario | Description | Expected Outcome |
|----------|-------------|------------------|
| `low-risk` | Normal order (2-3 items) | Should pass fraud check, payment succeeds (if fail rate = 0) |
| `high-risk` | Large quantity order (50+ items) | May be flagged by AI, or pass with medium risk |
| `fraud-test` | Order ID contains "FRAUD-TEST-" | Always rejected by fraud detection |

### CLI Options

| Option | Description | Example |
|--------|-------------|---------|
| `--payment-fail-rate <double>` | Probability of payment failure (0.0-1.0) | `--payment-fail-rate 0.5` |
| `--seed <long>` | Random seed for deterministic behavior | `--seed 12345` |

## Architecture

### Package Structure

```
com.nadvolod.order.fraud/
├── domain/              # Data models
│   ├── FraudDetectionResult
│   ├── PaymentChargeResult
│   ├── ConfirmationMessage
│   └── FraudOrderResponse
├── service/             # Business logic
│   ├── FraudDetectionService (interface)
│   ├── FraudPaymentService (interface)
│   ├── FakeCardPaymentService
│   └── FraudOrderProcessor (orchestrator)
├── ai/                  # AI agents
│   ├── FraudDetectionAgent (interface)
│   ├── OpenAiFraudDetectionAgent
│   ├── StubFraudDetectionAgent
│   ├── ConfirmationMessageAgent (interface)
│   ├── OpenAiConfirmationMessageAgent
│   └── StubConfirmationMessageAgent
└── FraudOrderProcessingApp (CLI entry point)
```

### Workflow Orchestration

**FraudOrderProcessor** is the core orchestrator that chains the 3 steps:

```java
public FraudOrderResponse processOrder(OrderRequest request) {
    // STEP 1: AI Fraud Detection
    FraudDetectionResult fraudCheck = fraudAgent.analyze(request);
    if (!fraudCheck.approved()) {
        return rejectedFraudResponse;
    }

    // STEP 2: Charge Payment Card
    PaymentChargeResult payment = paymentService.chargeCard(orderId, amount);
    if (!payment.success()) {
        return rejectedPaymentResponse;
    }

    // STEP 3: Generate Confirmation Message
    ConfirmationMessage confirmation = confirmationAgent.generate(response);

    return approvedResponse;
}
```

**Key Design Note**: This implementation is **intentionally naive**:
- ❌ No retry logic
- ❌ No state persistence
- ❌ No visibility into failures
- ❌ No automatic recovery

This makes Temporal's benefits obvious when you add it later!

### AI Agents

#### Fraud Detection Agent

- **OpenAI Implementation**: Uses GPT-4o-mini to analyze orders for fraud indicators
- **Stub Implementation**: Simple rule-based heuristics (order ID contains "fraud", quantity > 100)
- **Fallback Strategy**: On error, approves with MEDIUM risk (doesn't block legitimate orders)

**Input**: `OrderRequest`
**Output**: `FraudDetectionResult` (approved, riskScore, reason, riskLevel)

#### Confirmation Message Agent

- **OpenAI Implementation**: Generates customer-friendly confirmation messages based on order status
- **Stub Implementation**: Template-based messages for each status type
- **Fallback Strategy**: Simple switch statement based on status

**Input**: `FraudOrderResponse`
**Output**: `ConfirmationMessage` (subject, body, tone)

### Payment Service

**FakeCardPaymentService** simulates payment gateway behavior:

- Configurable failure rate (0.0 to 1.0)
- Tracks attempt count (useful for demonstrating Temporal retries)
- Supports seeded `Random` for deterministic testing
- Logs each attempt: `[Payment] Attempt #1`

## Understanding the Output

### Successful Order

```
=== Processing Fraud Detection Order: ORDER-1766181159899 ===

STEP 1: AI Fraud Detection
  Risk Score: 0.1
  Risk Level: LOW
  Decision: APPROVED
  Reason: No fraud indicators detected
STEP 1: ✓ PASSED

STEP 2: Charge Payment Card
  Amount: $30.00
  [Payment] Attempt #1
  Result: SUCCESS
  Message: Charge successful on attempt #1
  Charge ID: CHG-12345
STEP 2: ✓ PASSED

STEP 3: Generate Confirmation Message
  Subject: Order Confirmed: ORDER-1766181159899
  Tone: positive
  Body: Thank you for your order! We've received it...
STEP 3: ✓ PASSED

RESULT: APPROVED
```

### Fraud Rejected

```
STEP 1: AI Fraud Detection
  Risk Score: 0.95
  Risk Level: HIGH
  Decision: REJECTED
  Reason: Order ID contains fraud indicators

RESULT: REJECTED_FRAUD
Order rejected due to fraud detection.
```

### Payment Failure (No Retries!)

```
STEP 2: Charge Payment Card
  Amount: $30.00
  [Payment] Attempt #1
  Result: FAILED
  Message: Card declined (simulated failure #1)

RESULT: REJECTED_PAYMENT
Order rejected due to payment failure.
```

**Notice**: Only 1 attempt! This is intentional to show the difference when Temporal is added.

## OpenAI API Setup

### Setting the API Key

```bash
# Set environment variable
export OPENAI_API_KEY="sk-..."

# Or run with inline variable
OPENAI_API_KEY="sk-..." mvn exec:java@fraud-app -Dexec.args="low-risk"
```

### What OpenAI Does

1. **Fraud Detection**: Analyzes order for suspicious patterns
   - Unusual quantities
   - Order ID anomalies
   - Item combinations
   - Returns risk score (0.0-1.0) and explanation

2. **Confirmation Messages**: Generates customer-friendly messages
   - Tailored to order status (APPROVED, REJECTED_FRAUD, REJECTED_PAYMENT)
   - Professional tone and formatting
   - 2-3 sentence message body

### Fallback Without OpenAI

If `OPENAI_API_KEY` is not set, the application automatically uses **stub agents**:

- Simple rule-based fraud detection
- Template-based confirmation messages
- No API calls, no costs, fully deterministic

## Learning Path: Adding Temporal

### Current Limitations (By Design)

The current implementation has several "problems" that Temporal solves:

1. **No Retries**: Payment fails once → entire workflow fails
2. **No Visibility**: Can't see where it failed or why
3. **No State Persistence**: If the process crashes, all progress is lost
4. **No Recovery**: Must manually re-run the entire workflow

### Temporalization Roadmap

When you're ready to add Temporal, here's the path:

#### Step 1: Create Activities (3 files)

Activities wrap each workflow step for distributed execution:

```java
@ActivityInterface
public interface FraudDetectionActivity {
    FraudDetectionResult detectFraud(OrderRequest request);
}

@ActivityInterface
public interface PaymentChargeActivity {
    PaymentChargeResult chargeCard(String orderId, double amount);
}

@ActivityInterface
public interface ConfirmationMessageActivity {
    ConfirmationMessage generateMessage(FraudOrderResponse response);
}
```

**Implementation**: Thin wrappers around existing agents/services.

#### Step 2: Create Workflow (2 files)

The workflow replaces `FraudOrderProcessor`:

```java
@WorkflowInterface
public interface FraudOrderWorkflow {
    @WorkflowMethod
    FraudOrderResponse processOrder(OrderRequest request);
}
```

**Implementation**: Same logic as `FraudOrderProcessor`, but calls activity stubs instead of services directly.

#### Step 3: Configure Retries

Key difference - add retry policies to activities:

```java
// Payment activity with aggressive retries
ActivityOptions paymentOptions = ActivityOptions.newBuilder()
    .setStartToCloseTimeout(Duration.ofSeconds(10))
    .setRetryOptions(RetryOptions.newBuilder()
        .setInitialInterval(Duration.ofSeconds(1))
        .setMaximumInterval(Duration.ofSeconds(10))
        .setBackoffCoefficient(2.0)
        .setMaximumAttempts(5)  // 5 attempts!
        .build())
    .build();
```

#### Step 4: Create Worker & Starter

- **Worker**: Registers activities and workflows, polls for tasks
- **Starter**: Triggers workflow execution

#### Step 5: Demo the Difference

**Before Temporal (current implementation):**
```bash
mvn exec:java@fraud-app -Dexec.args="low-risk --payment-fail-rate 0.8"
# Result: Likely fails on first payment attempt
# Output: [Payment] Attempt #1 → FAILED → REJECTED_PAYMENT
```

**After Temporal:**
```bash
# Start worker
mvn exec:java@fraud-worker

# In another terminal, start workflow
mvn exec:java@fraud-workflow -Dexec.args="low-risk --payment-fail-rate 0.8"
# Result: Multiple retries, eventually succeeds!
# Output:
#   [Payment] Attempt #1 → FAILED
#   [Payment] Attempt #2 → FAILED
#   [Payment] Attempt #3 → FAILED
#   [Payment] Attempt #4 → SUCCESS → APPROVED
```

**Additional Benefits:**
- View retry history in Temporal Web UI (`http://localhost:8233`)
- See exact step that's executing/failing
- Workflow state persisted - survives crashes
- Can pause, resume, or cancel workflows

### Example Temporal Workflow Implementation

Here's a preview of what the temporalized version would look like:

```java
public class FraudOrderWorkflowImpl implements FraudOrderWorkflow {

    private final FraudDetectionActivity fraudActivity =
        Workflow.newActivityStub(FraudDetectionActivity.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .build())
                .build());

    private final PaymentChargeActivity paymentActivity =
        Workflow.newActivityStub(PaymentChargeActivity.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(5)  // More retries for payments!
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofSeconds(10))
                    .setBackoffCoefficient(2.0)
                    .build())
                .build());

    private final ConfirmationMessageActivity confirmationActivity =
        Workflow.newActivityStub(ConfirmationMessageActivity.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .build())
                .build());

    @Override
    public FraudOrderResponse processOrder(OrderRequest request) {
        // Step 1: Fraud detection (with automatic retries)
        FraudDetectionResult fraudCheck = fraudActivity.detectFraud(request);

        if (!fraudCheck.approved()) {
            // Generate rejection message and return
            // ...
            return rejectedResponse;
        }

        // Step 2: Charge payment (with 5 retry attempts!)
        double amount = calculateAmount(request);
        PaymentChargeResult payment = paymentActivity.chargeCard(request.orderId(), amount);

        if (!payment.success()) {
            // Generate failure message and return
            // ...
            return failedResponse;
        }

        // Step 3: Generate confirmation (with automatic retries)
        FraudOrderResponse tempResponse = new FraudOrderResponse(...);
        ConfirmationMessage confirmation = confirmationActivity.generateMessage(tempResponse);

        return new FraudOrderResponse(
            request.orderId(),
            "APPROVED",
            fraudCheck,
            payment,
            confirmation
        );
    }
}
```

**Key Differences:**
- Activities instead of direct service calls
- Automatic retry configuration per activity
- State persisted between each activity
- Visible in Temporal Web UI

## Troubleshooting

### Build Issues

```bash
# Clean and rebuild
mvn clean compile

# Run tests
mvn test
```

### OpenAI API Issues

**Problem**: `Using stub AI agents` even though API key is set

**Solution**: Check that API key is not empty:
```bash
echo $OPENAI_API_KEY  # Should print your key
```

**Problem**: OpenAI API rate limits or errors

**Solution**: The application gracefully falls back to safe defaults:
- Fraud detection: Approves with MEDIUM risk
- Confirmation messages: Uses template-based fallback

### Payment Always Fails

**Problem**: Payment fails every time even with `--payment-fail-rate 0.0`

**Solution**: Check the random seed - some seeds may produce unlucky sequences. Try different seeds:
```bash
mvn exec:java@fraud-app -Dexec.args="low-risk --payment-fail-rate 0.0 --seed 99"
```

## Comparison with V1

| Feature | V1 (com.nadvolod.order) | V2 (com.nadvolod.order.fraud) |
|---------|-------------------------|-------------------------------|
| **Focus** | Inventory management | Fraud detection & payment |
| **Steps** | Inventory → Payment → Shipping | Fraud Detection → Payment → Confirmation |
| **AI Usage** | Post-processing analysis | Core workflow step |
| **Complexity** | 5 steps | 3 steps |
| **Realism** | Demo-focused | Production-like |
| **Temporal** | Partial integration | Not yet (by design) |

## Next Steps

1. **Run All Scenarios**: Try all three scenarios to understand the workflow
2. **Test Failure Cases**: Experiment with different `--payment-fail-rate` values
3. **Examine the Code**: Read through `FraudOrderProcessor.java` to understand the orchestration
4. **Add Temporal**: Follow the temporalization roadmap above
5. **Compare Before/After**: Run the same scenario with and without Temporal to see the benefits

## Resources

- [Temporal Documentation](https://docs.temporal.io/)
- [OpenAI API Documentation](https://platform.openai.com/docs)
- [V1 Workflow](../README.md) (for comparison)
- [Main Project README](../../../../../../../CLAUDE.md)

## Questions?

This is a learning demo. Feel free to:
- Modify the failure rates to see different behaviors
- Add logging to understand the flow better
- Implement your own AI agent variations
- Experiment with the Temporal integration

Happy learning!
