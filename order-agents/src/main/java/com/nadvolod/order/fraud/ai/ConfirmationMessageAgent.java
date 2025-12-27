package com.nadvolod.order.fraud.ai;

import com.nadvolod.order.fraud.domain.ConfirmationMessage;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;

/**
 * AI agent that generates customer confirmation messages.
 * Tailors message based on fraud check and payment results.
 */
public interface ConfirmationMessageAgent {
    ConfirmationMessage generate(FraudOrderResponse response);
}
