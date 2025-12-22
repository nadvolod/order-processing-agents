package com.nadvolod.order.fraud.ai;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.domain.FraudDetectionResult;
import io.temporal.activity.ActivityInterface;

/**
 * AI agent that analyzes orders for fraud indicators.
 * Returns risk assessment with explanation.
 */
public interface FraudDetectionAgent {
    FraudDetectionResult analyze(OrderRequest request);
}
