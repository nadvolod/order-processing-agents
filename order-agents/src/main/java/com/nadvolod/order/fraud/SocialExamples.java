package com.nadvolod.order.fraud;

import com.nadvolod.order.domain.AgentAdvice;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;

import java.time.Duration;

public class SocialExamples {
    public AgentAdvice callAiWithRetries(OrderRequest req, OrderResponse resp) {
        int maxAttempts = 3;
        int attempt = 0;
        long backoffMs = 1000; // Start with 1 second

        while (attempt < maxAttempts) {
            try {
                // return aiAgent.explain(req, resp);
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    throw new RuntimeException("Failed after " + maxAttempts + " attempts", e);
                }
                // logFailure(e, attempt);

                try {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }

        }
        return null;
    }

    public void withTemporal() {
        // Configure once - Temporal handles everything
        ActivityOptions.newBuilder()
                .setRetryOptions(
                        RetryOptions.newBuilder()
                                .setMaximumAttempts(3)
                                .setInitialInterval(Duration.ofSeconds(1))
                                .setBackoffCoefficient(2.0)
                                .build()
                );
    }
}