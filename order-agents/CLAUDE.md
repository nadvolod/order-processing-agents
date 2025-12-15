# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a progressive demo of an order-processing system built in Java 25. The project demonstrates evolving from a naive in-process workflow to a sophisticated system with AI agents and Temporal workflows.

**Current State**: Naive, synchronous order processing without Temporal, HTTP APIs, or retries. This is intentional to highlight problems that Temporal will later solve.

**Future Plans**:
1. Add AI agents for decision support and customer communication
2. Migrate to Temporal workflows for durability, retries, and observability
3. Optionally add HTTP API layer

## Build and Run Commands

### Build the project
```bash
mvn clean package
```

### Run scenarios (CLI-driven demo)

The application accepts a single CLI argument to control execution paths:

**Mixed scenario** (default - one item in stock, one out of stock):
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

### Core Workflow Pattern

The system implements a naive three-step orchestration pattern in `OrderProcessor`:

```text
OrderRequest → Inventory Check → Payment Processing → Shipment Creation → OrderResponse
```

Each step can fail and will halt the entire workflow. This fragility is intentional.

### Key Architectural Components

**OrderProcessor** (`service/OrderProcessor.java`): The central naive workflow orchestrator containing all control flow. This class will map 1:1 to a Temporal Workflow in future iterations. Currently has no retry logic, state persistence, or crash recovery.

**Service Layer**: Three injected services following interface-based design:
- `InventoryService` - checks SKU availability (in-memory implementation)
- `PaymentService` - simulates payment gateway (intentionally flaky with random failures)
- `ShippingService` - simulates shipment creation (mostly succeeds)

**Domain Models** (`domain/`): Java records representing order state:
- `OrderRequest` / `OrderResponse` - input/output contracts
- `OrderLineStatus` - per-SKU inventory results
- `PaymentResult` / `ShipmentDetails` - step-specific outcomes

### Important Architectural Patterns

**Partial Response Pattern**: `OrderResponse` uses nullable fields to show workflow progress:
- `lineStatuses` - always populated
- `paymentResult` - null if inventory failed
- `shipmentDetails` - null if inventory or payment failed

This makes it easy to see exactly where the workflow stopped.

**Fake Service Pattern**: Services have "Fake" implementations (`FakePaymentService`, `FakeShippingService`) that simulate external system behavior including failures. These will become Temporal Activities.

**Scenario-Driven Testing**: `OrderProcessingApp` provides three hardcoded scenarios using in-memory inventory data:
- SKU-123: 10 units in stock
- SKU-456: 0 units in stock
- SKU-789: 5 units in stock

## Development Context

### Technology Stack
- Java 24
- Maven
- Jackson for JSON
- OkHttp for HTTP client
- Spring Boot Web (added for future API layer, currently unused)
- Lombok (optional)

### Known Limitations (Intentional)
These are teaching points, not bugs:
- No retries on failures
- No state persistence
- No recovery from partial failures (e.g., payment succeeds but shipping fails)
- No long-running workflow support
- No visibility beyond console logs
- No transaction rollback or compensation logic

### What NOT to Add
When making changes, avoid "fixing" the naive implementation unless explicitly requested:
- Do not add retry logic
- Do not add state persistence
- Do not add transaction handling
- Do not add HTTP endpoints (unless part of planned evolution)
- Do not add complex error handling

The goal is to keep this simple and fragile as a baseline before introducing Temporal.

## Code Style Notes

- Uses Java 24 features including records and pattern matching
- Modern switch expressions with yield
- Service injection via constructor
- Interface-based abstractions for all external dependencies