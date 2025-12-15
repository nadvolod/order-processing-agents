# Order Processing Agents - Java Application

A progressive demo of an order-processing system built in Java 24, demonstrating the evolution from a naive in-process workflow to a sophisticated system with AI agents and Temporal workflows.

## Quick Start

### Build
```bash
mvn clean package
```

### Run
```bash
# Mixed scenario (one item in stock, one out of stock)
mvn -q exec:java -Dexec.mainClass="com.nadvolod.order.OrderProcessingApp" -Dexec.args="mixed"

# All items in stock
mvn -q exec:java -Dexec.mainClass="com.nadvolod.order.OrderProcessingApp" -Dexec.args="in-stock"

# Out of stock
mvn -q exec:java -Dexec.mainClass="com.nadvolod.order.OrderProcessingApp" -Dexec.args="out-of-stock"
```

### Run with Java directly
```bash
java -cp target/order-processing-agents-0.1.0-SNAPSHOT.jar com.nadvolod.order.OrderProcessingApp <scenario>
```

## Project Overview

This is a **naive implementation** of an order processing workflow. It intentionally lacks:
- Retry logic
- State persistence
- Error recovery
- Transaction handling

These limitations demonstrate the problems that Temporal workflows solve.

## Architecture

### Workflow
```
OrderRequest 
  → Inventory Check 
  → Payment Processing 
  → Shipment Creation 
  → OrderResponse
```

### Components

**OrderProcessor** (`service/OrderProcessor.java`)
- Central workflow orchestrator
- Synchronous execution
- No error recovery
- Will become a Temporal Workflow in future iterations

**Services** (interface-based design)
- `InventoryService` - In-memory stock checking
- `PaymentService` - Simulated payment gateway (with intentional random failures)
- `ShippingService` - Simulated shipment creation

**Domain Models** (`domain/`)
- `OrderRequest` / `OrderResponse` - I/O contracts
- `OrderLineStatus` - Per-SKU inventory results
- `PaymentResult` / `ShipmentDetails` - Step outcomes

### Partial Response Pattern

`OrderResponse` uses nullable fields to show workflow progress:
- `lineStatuses` - always populated
- `paymentResult` - null if inventory failed
- `shipmentDetails` - null if inventory or payment failed

This makes it easy to see exactly where the workflow stopped.

## Test Scenarios

The application uses hardcoded inventory data:
- **SKU-123**: 10 units in stock
- **SKU-456**: 0 units in stock (triggers out-of-stock path)
- **SKU-789**: 5 units in stock

## Technology Stack

- **Java 24** - Modern Java features (records, pattern matching, switch expressions)
- **Maven** - Build and dependency management
- **Jackson** - JSON serialization
- **OkHttp** - HTTP client (for future use)
- **Spring Boot Web** - Framework for future API layer (currently unused)
- **Lombok** - Optional boilerplate reduction

## Development Guide

For comprehensive development guidance including:
- Detailed architectural patterns
- Code style notes
- What NOT to add (important!)
- Future evolution plans

See [CLAUDE.md](CLAUDE.md) for full details.

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

## License

Apache License 2.0 - see [LICENSE](LICENSE) file for details.
