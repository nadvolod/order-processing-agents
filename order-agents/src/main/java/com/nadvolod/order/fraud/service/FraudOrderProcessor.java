package com.nadvolod.order.fraud.service;

import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.ai.ConfirmationMessageAgent;
import com.nadvolod.order.fraud.ai.FraudDetectionAgent;
import com.nadvolod.order.fraud.domain.*;

/**
 * Orchestrates the fraud detection workflow WITHOUT Temporal.
 *
 * Workflow: OrderRequest → Fraud Detection → Payment Charge → Confirmation Message
 *
 * This is intentionally simple to demonstrate Temporal's benefits:
 * - No retry logic
 * - No state persistence
 * - No visibility into failures
 * - No automatic recovery
 */
public class FraudOrderProcessor {

    private static final double PRICE_PER_ITEM = 10.0;

    private final FraudDetectionAgent fraudAgent;
    private final FraudPaymentService paymentService;
    private final ConfirmationMessageAgent confirmationAgent;

    public FraudOrderProcessor(
            FraudDetectionAgent fraudAgent,
            FraudPaymentService paymentService,
            ConfirmationMessageAgent confirmationAgent) {
        this.fraudAgent = fraudAgent;
        this.paymentService = paymentService;
        this.confirmationAgent = confirmationAgent;
    }

    public FraudOrderResponse processOrder(OrderRequest request) {
        System.out.println("\n=== Processing Fraud Detection Order: " + request.orderId() + " ===\n");

        // STEP 1: AI Fraud Detection
        System.out.println("STEP 1: AI Fraud Detection");
        FraudDetectionResult fraudCheck = fraudAgent.analyze(request);

        System.out.println("  Risk Score: " + fraudCheck.riskScore());
        System.out.println("  Risk Level: " + fraudCheck.riskLevel());
        System.out.println("  Decision: " + (fraudCheck.approved() ? "APPROVED" : "REJECTED"));
        System.out.println("  Reason: " + fraudCheck.reason());

        if (!fraudCheck.approved()) {
            System.out.println("\nRESULT: REJECTED_FRAUD");
            System.out.println("Order rejected due to fraud detection.\n");

            // Build partial response - no payment or confirmation
            FraudOrderResponse partialResponse = new FraudOrderResponse(
                request.orderId(),
                "REJECTED_FRAUD",
                fraudCheck,
                null,  // no payment attempted
                null   // no confirmation generated yet
            );

            // Generate rejection message
            ConfirmationMessage rejectionMessage = confirmationAgent.generate(partialResponse);

            return new FraudOrderResponse(
                request.orderId(),
                "REJECTED_FRAUD",
                fraudCheck,
                null,
                rejectionMessage
            );
        }

        System.out.println("STEP 1: ✓ PASSED\n");

        // STEP 2: Charge Payment Card
        System.out.println("STEP 2: Charge Payment Card");
        double totalAmount = request.items().stream()
            .mapToDouble(item -> item.quantity() * PRICE_PER_ITEM)
            .sum();

        System.out.println("  Amount: $" + String.format("%.2f", totalAmount));

        PaymentChargeResult paymentResult = paymentService.chargeCard(request.orderId(), totalAmount);

        System.out.println("  Result: " + (paymentResult.success() ? "SUCCESS" : "FAILED"));
        System.out.println("  Message: " + paymentResult.message());
        if (paymentResult.chargeId() != null) {
            System.out.println("  Charge ID: " + paymentResult.chargeId());
        }

        if (!paymentResult.success()) {
            System.out.println("\nRESULT: REJECTED_PAYMENT");
            System.out.println("Order rejected due to payment failure.\n");

            // Build partial response
            FraudOrderResponse partialResponse = new FraudOrderResponse(
                request.orderId(),
                "REJECTED_PAYMENT",
                fraudCheck,
                paymentResult,
                null
            );

            // Generate failure message
            ConfirmationMessage failureMessage = confirmationAgent.generate(partialResponse);

            return new FraudOrderResponse(
                request.orderId(),
                "REJECTED_PAYMENT",
                fraudCheck,
                paymentResult,
                failureMessage
            );
        }

        System.out.println("STEP 2: ✓ PASSED\n");

        // STEP 3: Generate Confirmation Message
        System.out.println("STEP 3: Generate Confirmation Message");

        // Build successful response
        FraudOrderResponse successResponse = new FraudOrderResponse(
            request.orderId(),
            "APPROVED",
            fraudCheck,
            paymentResult,
            null  // will be filled next
        );

        ConfirmationMessage confirmation = confirmationAgent.generate(successResponse);

        System.out.println("  Subject: " + confirmation.subject());
        System.out.println("  Tone: " + confirmation.tone());
        System.out.println("  Body: " + confirmation.body());

        System.out.println("STEP 3: ✓ PASSED\n");

        System.out.println("RESULT: APPROVED\n");

        return new FraudOrderResponse(
            request.orderId(),
            "APPROVED",
            fraudCheck,
            paymentResult,
            confirmation
        );
    }
}
