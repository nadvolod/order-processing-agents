package com.nadvolod.order.domain;

import java.util.List;

public record OrderResponse(
    String orderId,
    String status, // "ACCEPTED" or "REJECTED"
    List<OrderLineStatus> lineStatuses,
    PaymentResult paymentResult,
    ShipmentDetails shipmentDetails
) {}
