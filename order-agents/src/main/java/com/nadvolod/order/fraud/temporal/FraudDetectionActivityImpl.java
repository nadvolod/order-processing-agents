package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.ai.FraudDetectionAgent;
import com.nadvolod.order.fraud.domain.FraudDetectionResult;

public class FraudDetectionActivityImpl implements FraudDetectionActivity{
    private final FraudDetectionAgent fraudAgent;

    public FraudDetectionActivityImpl(FraudDetectionAgent fraudDetectionAgent) {
        this.fraudAgent = fraudDetectionAgent;
    }

    @Override
    public FraudDetectionResult detectFraud(OrderRequest request) {
        return fraudAgent.analyze(request);
    }
}
