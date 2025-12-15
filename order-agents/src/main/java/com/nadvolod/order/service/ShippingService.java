package com.nadvolod.order.service;

import com.nadvolod.order.domain.ShipmentDetails;

public interface ShippingService {
    ShipmentDetails createShipment(String orderId);
}
