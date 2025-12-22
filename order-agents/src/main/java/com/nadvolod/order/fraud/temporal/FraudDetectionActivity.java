package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.domain.FraudDetectionResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface FraudDetectionActivity {
    FraudDetectionResult detectFraud(OrderRequest request);
}
