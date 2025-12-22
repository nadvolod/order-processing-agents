package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.fraud.ai.ConfirmationMessageAgent;
import com.nadvolod.order.fraud.domain.ConfirmationMessage;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;

public class ConfirmationMessageImpl implements ConfirmationMessageActivity{

    private final ConfirmationMessageAgent messageAgent;

    public ConfirmationMessageImpl(ConfirmationMessageAgent messageAgent){
        this.messageAgent = messageAgent;
    }
    @Override
    public ConfirmationMessage generateMessage(FraudOrderResponse result) {
        return messageAgent.generate(result);
    }
}
