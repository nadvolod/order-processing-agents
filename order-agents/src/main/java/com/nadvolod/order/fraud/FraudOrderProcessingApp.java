package com.nadvolod.order.fraud;

import com.nadvolod.order.domain.OrderLine;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.ai.*;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;
import com.nadvolod.order.fraud.service.FakeCardPaymentService;
import com.nadvolod.order.fraud.service.FraudOrderProcessor;
import com.nadvolod.order.fraud.service.FraudPaymentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * CLI application for fraud detection order processing.
 *
 * Demonstrates a 3-step workflow:
 * 1. AI Fraud Detection
 * 2. Payment Card Charging
 * 3. Confirmation Message Generation
 *
 * Usage: java FraudOrderProcessingApp <scenario> [--payment-fail-rate <rate>] [--seed <long>]
 *
 * Scenarios:
 *   low-risk   - Normal order, should pass fraud check
 *   high-risk  - Large quantity, may be flagged
 *   fraud-test - Intentionally suspicious, should fail fraud check
 */
public class FraudOrderProcessingApp extends CLI {

    public static void main(String[] args) {
        // Parse CLI arguments
        CLIConfig config = CLI.parseArgs(args);

        if (config == null) {
            CLI.printUsage();
            System.exit(1);
        }

        // Create services
        Random random = config.seed != null ? new Random(config.seed) : new Random();

        // Initialize AI agents (OpenAI if available, otherwise stub)
        FraudDetectionAgent fraudAgent;
        ConfirmationMessageAgent confirmationAgent;

        var apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) {
            fraudAgent = new OpenAiFraudDetectionAgent(apiKey, "gpt-4o-mini");
            confirmationAgent = new OpenAiConfirmationMessageAgent(apiKey, "gpt-4o-mini");
            System.out.println("[INFO] Using OpenAI API for AI agents\n");
        } else {
            // Fallback to stub if no API key
            fraudAgent = new StubFraudDetectionAgent();
            confirmationAgent = new StubConfirmationMessageAgent();
            System.out.println("[INFO] OPENAI_API_KEY not set - using stub AI agents\n");
        }

        FraudPaymentService paymentService = new FakeCardPaymentService(
            config.paymentFailRate,
            random
        );

        // Create processor
        FraudOrderProcessor processor = new FraudOrderProcessor(
            fraudAgent,
            paymentService,
            confirmationAgent
        );

        // Create order request based on scenario
        // Doesn't need Temporal
        OrderRequest request = createOrderForScenario(config.scenario);

        System.out.println("=== Fraud Detection Workflow Demo ===");
        System.out.println("Scenario: " + config.scenario);
        System.out.println("Payment Fail Rate: " + (config.paymentFailRate * 100) + "%");
        if (config.seed != null) {
            System.out.println("Random Seed: " + config.seed);
        }

        // Process order
        FraudOrderResponse response = processor.processOrder(request);

        // Display final results
        System.out.println("\n=== Final Results ===");
        System.out.println("Order ID: " + response.orderId());
        System.out.println("Status: " + response.status());

        if (response.confirmation() != null) {
            System.out.println("\n=== Customer Message ===");
            System.out.println("Subject: " + response.confirmation().subject());
            System.out.println("Tone: " + response.confirmation().tone());
            System.out.println("\nMessage:");
            System.out.println(response.confirmation().body());
        }

        System.out.println("\n=== Workflow Complete ===");
    }

    public static OrderRequest createOrderForScenario(String scenario) {
        List<OrderLine> items = new ArrayList<>();
        String orderId;

        switch (scenario.toLowerCase()) {
            case "low-risk" -> {
                orderId = "ORDER-" + System.currentTimeMillis();
                items.add(new OrderLine("SKU-123", 2));
                items.add(new OrderLine("SKU-789", 1));
            }
            case "high-risk" -> {
                orderId = "ORDER-" + System.currentTimeMillis();
                items.add(new OrderLine("SKU-123", 125));  // Unusually large quantity
                items.add(new OrderLine("SKU-456", 150));
            }
            case "fraud-test" -> {
                orderId = "FRAUD-TEST-" + System.currentTimeMillis();  // Trigger fraud detection
                items.add(new OrderLine("SKU-999", 1));
            }
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }

        return new OrderRequest(orderId, items);
    }

}
