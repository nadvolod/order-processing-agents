package com.nadvolod.order.service;

import com.nadvolod.order.domain.ShipmentDetails;

import java.util.Random;

public class FakeShippingService implements ShippingService {
    private static final int TRACKING_NUM_BOUND = 100000;
    
    private final double failRate;
    private final Random random;

    /**
     * @param failRate Probability of shipping failure (0.0 to 1.0)
     * @param random Optional seeded Random for deterministic behavior
     */
    public FakeShippingService(double failRate, Random random) {
        if (failRate < 0.0 || failRate > 1.0) {
            throw new IllegalArgumentException("failRate must be between 0.0 and 1.0");
        }
        this.failRate = failRate;
        this.random = random != null ? random : new Random();
    }

    @Override
    public ShipmentDetails createShipment(String orderId) {
        boolean shouldFail = random.nextDouble() < failRate;
        
        if (shouldFail) {
            return new ShipmentDetails(false, null, "Shipping unavailable (simulated)");
        } else {
            String trackingNum = "TRACK-" + random.nextInt(TRACKING_NUM_BOUND);
            return new ShipmentDetails(true, trackingNum, "Shipment created");
        }
    }
}
