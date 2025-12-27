package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.fraud.domain.ConfirmationMessage;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ConfirmationMessageActivity {
    ConfirmationMessage generateMessage(FraudOrderResponse result);
}
