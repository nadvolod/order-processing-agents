package com.nadvolod.order.fraud.temporal;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.domain.ConfirmationMessage;
import com.nadvolod.order.fraud.domain.FraudDetectionResult;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;
import com.nadvolod.order.fraud.domain.PaymentChargeResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class FraudOrderWorkflowImpl implements FraudOrderWorkflow {
    private static final double PRICE_PER_ITEM = 10.0;
    //1. Create activity "stubs"
    // Fraud Detection (AI-based, may be slow)
    // Fail → wait 1s → retry → fail → wait 2s → retry → fail → done.
    private final FraudDetectionActivity fraudDetectionActivity = Workflow.newActivityStub(FraudDetectionActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)  //Total tries (1 original + 2 retries)
                            .setInitialInterval(Duration.ofSeconds(1))  //Wait before first retry
                            .setMaximumInterval(Duration.ofSeconds(10)) //Cap on wait time between retries
                            .setBackoffCoefficient(2.0) //Multiplier for exponential backoff
                            .build())
                    .build()
    );
    private final ConfirmationMessageActivity confirmationMessageActivity = Workflow.newActivityStub(ConfirmationMessageActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(10))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build());
    private final PaymentChargeActivity paymentChargeActivity = Workflow.newActivityStub(PaymentChargeActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(5))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(5)  // Retry up to 5 times total
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(5))
                            .setBackoffCoefficient(2.0)  // Exponential backoff: 1s, 2s, 4s, 5s
                            .build())
                    .build());

    @Override
    public FraudOrderResponse processOrder(OrderRequest order) {
        System.out.println("\n=== Processing Fraud Detection Order: " + order.orderId() + " ===\n");

        // STEP 1: AI Fraud Detection
        System.out.println("STEP 1: AI Fraud Detection");
        FraudDetectionResult fraudCheck = fraudDetectionActivity.detectFraud(order);

        System.out.println("  Risk Score: " + fraudCheck.riskScore());
        System.out.println("  Risk Level: " + fraudCheck.riskLevel());
        System.out.println("  Decision: " + (fraudCheck.approved() ? "APPROVED" : "REJECTED"));
        System.out.println("  Reason: " + fraudCheck.reason());

        if (!fraudCheck.approved()) {
            System.out.println("\nRESULT: REJECTED_FRAUD");
            System.out.println("Order rejected due to fraud detection.\n");

            // Build partial response - no payment or confirmation
            FraudOrderResponse partialResponse = new FraudOrderResponse(
                    order.orderId(),
                    "REJECTED_FRAUD",
                    fraudCheck,
                    null,  // no payment attempted
                    null   // no confirmation generated yet
            );
            // Generate rejection message
            ConfirmationMessage rejectionMessage = confirmationMessageActivity.generateMessage(partialResponse);

            return new FraudOrderResponse(
                    order.orderId(),
                    "REJECTED_FRAUD",
                    fraudCheck,
                    null,
                    rejectionMessage
            );
        }

        System.out.println("STEP 1: ✓ PASSED\n");

        // STEP 2: Charge Payment Card
        System.out.println("STEP 2: Charge Payment Card");
        double totalAmount = order.items().stream()
                .mapToDouble(item -> item.quantity() * PRICE_PER_ITEM)
                .sum();

        System.out.println("  Amount: $" + String.format("%.2f", totalAmount));

        PaymentChargeResult paymentResult;
        try {
            // This will retry automatically if payment fails (throws PaymentFailedException)
            paymentResult = paymentChargeActivity.chargeCard(order.orderId(), totalAmount);

            System.out.println("  Result: SUCCESS");
            System.out.println("  Message: " + paymentResult.message());
            if (paymentResult.chargeId() != null) {
                System.out.println("  Charge ID: " + paymentResult.chargeId());
            }
            System.out.println("STEP 2: ✓ PASSED\n");

        } catch (Exception e) {
            // Retries exhausted - payment failed after all attempts
            System.out.println("  Result: FAILED");
            System.out.println("  Message: Payment failed after all retry attempts");
            System.out.println("\nRESULT: REJECTED_PAYMENT");
            System.out.println("Order rejected due to payment failure after retries.\n");

            // Create a failed payment result for the response
            PaymentChargeResult failedResult = new PaymentChargeResult(
                    false,
                    null,
                    e.getMessage(),
                    0.0
            );

            // Build partial response
            FraudOrderResponse partialResponse = new FraudOrderResponse(
                    order.orderId(),
                    "REJECTED_PAYMENT",
                    fraudCheck,
                    failedResult,
                    null
            );

            // Generate failure message
            ConfirmationMessage failureMessage = confirmationMessageActivity.generateMessage(partialResponse);

            return new FraudOrderResponse(
                    order.orderId(),
                    "REJECTED_PAYMENT",
                    fraudCheck,
                    failedResult,
                    failureMessage
            );
        }

        // STEP 3: Generate Confirmation Message
        System.out.println("STEP 3: Generate Confirmation Message");

        // Build the response so far
        FraudOrderResponse partialResponse = new FraudOrderResponse(
                order.orderId(),
                "APPROVED",
                fraudCheck,
                paymentResult,
                null  // No confirmation yet
        );

        ConfirmationMessage confirmation = confirmationMessageActivity.generateMessage(partialResponse);

        System.out.println("  Subject: " + confirmation.subject());
        System.out.println("  Tone: " + confirmation.tone());
        System.out.println("  Body: " + confirmation.body());
        System.out.println("STEP 3: ✓ PASSED\n");

        System.out.println("RESULT: APPROVED\n");

        // Return complete response
        return new FraudOrderResponse(
                order.orderId(),
                "APPROVED",
                fraudCheck,
                paymentResult,
                confirmation
        );
    }
}