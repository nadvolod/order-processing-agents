package com.nadvolod.order.service;

import com.nadvolod.order.domain.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Naive workflow orchestrator - intentionally fragile with no retry logic.
 * This will later map to a Temporal Workflow.
 */
public class OrderProcessor {
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final ShippingService shippingService;

    public OrderProcessor(InventoryService inventoryService, 
                         PaymentService paymentService, 
                         ShippingService shippingService) {
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.shippingService = shippingService;
    }

    public OrderResponse processOrder(OrderRequest request) {
        System.out.println("=== Processing Order: " + request.orderId() + " ===");
        
        // STEP 1: Check inventory
        List<OrderLineStatus> lineStatuses = new ArrayList<>();
        boolean allItemsAvailable = true;
        
        for (OrderLine item : request.items()) {
            boolean available = inventoryService.checkAvailability(item.sku(), item.quantity());
            lineStatuses.add(new OrderLineStatus(item.sku(), item.quantity(), available));
            if (!available) {
                allItemsAvailable = false;
            }
        }
        
        if (!allItemsAvailable) {
            System.out.println("STEP 1 Inventory: FAILED");
            System.out.println("RESULT: REJECTED");
            return new OrderResponse(request.orderId(), "REJECTED", lineStatuses, null, null);
        }
        
        System.out.println("STEP 1 Inventory: OK");
        
        // STEP 2: Process payment
        // Simple calculation: assume $10 per item
        double amount = request.items().stream()
            .mapToDouble(item -> item.quantity() * 10.0)
            .sum();
        
        PaymentResult paymentResult = paymentService.processPayment(request.orderId(), amount);
        
        if (!paymentResult.success()) {
            System.out.println("STEP 2 Payment: FAILED (simulated)");
            System.out.println("RESULT: REJECTED");
            return new OrderResponse(request.orderId(), "REJECTED", lineStatuses, paymentResult, null);
        }
        
        System.out.println("STEP 2 Payment: OK (" + paymentResult.transactionId() + ")");
        
        // STEP 3: Create shipment
        ShipmentDetails shipmentDetails = shippingService.createShipment(request.orderId());
        
        if (!shipmentDetails.success()) {
            System.out.println("STEP 3 Shipping: FAILED (simulated)");
            System.out.println("RESULT: REJECTED");
            return new OrderResponse(request.orderId(), "REJECTED", lineStatuses, paymentResult, shipmentDetails);
        }
        
        System.out.println("STEP 3 Shipping: OK (" + shipmentDetails.trackingNumber() + ")");
        System.out.println("RESULT: ACCEPTED");
        
        return new OrderResponse(request.orderId(), "ACCEPTED", lineStatuses, paymentResult, shipmentDetails);
    }
}
