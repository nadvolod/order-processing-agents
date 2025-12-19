package com.nadvolod.order.fraud.service;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.domain.FraudDetectionResult;

/**
 * Service for detecting fraudulent orders.
 * Delegates to AI agent for analysis.
 */
public interface FraudDetectionService {
    FraudDetectionResult analyze(OrderRequest request);
}
