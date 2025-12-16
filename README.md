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

The application accepts a single CLI argument to control execution paths:

**Mixed scenario** (default - one item in stock, one out of stock):

If one of the items is out of stock, the workflow stops while we resolve the issue. Then we need to re-run the workflow
after the item is back in stock.

```bash
mvn -q exec:java -Dexec.mainClass="com.nadvolod.order.OrderProcessingApp" -Dexec.args="mixed"
```

**All items in stock**:
```bash
mvn -q exec:java -Dexec.mainClass="com.nadvolod.order.OrderProcessingApp" -Dexec.args="in-stock"
```

**Out of stock**:
```bash
mvn -q exec:java -Dexec.mainClass="com.nadvolod.order.OrderProcessingApp" -Dexec.args="out-of-stock"
```

**Run with Java directly**:
```bash
java -cp target/order-processing-agents-0.1.0-SNAPSHOT.jar com.nadvolod.order.OrderProcessingApp in-stock
```

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
