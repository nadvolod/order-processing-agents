package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.fraud.domain.PaymentChargeResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface PaymentChargeActivity {
    PaymentChargeResult chargeCard(String orderId, double amount);
}
