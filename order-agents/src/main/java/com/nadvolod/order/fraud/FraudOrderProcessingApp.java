package com.nadvolod.order.fraud;

import com.nadvolod.order.domain.OrderLine;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.ai.*;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;
import com.nadvolod.order.fraud.service.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
public class FraudOrderProcessingApp {

    private static final Set<String> VALID_SCENARIOS = Set.of("low-risk", "high-risk", "fraud-test");

    public static void main(String[] args) {
        // Parse CLI arguments
        CLIConfig config = parseArgs(args);

        if (config == null) {
            printUsage();
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

    private static OrderRequest createOrderForScenario(String scenario) {
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
                items.add(new OrderLine("SKU-123", 50));  // Unusually large quantity
                items.add(new OrderLine("SKU-456", 75));
            }
            case "fraud-test" -> {
                orderId = "FRAUD-TEST-" + System.currentTimeMillis();  // Trigger fraud detection
                items.add(new OrderLine("SKU-999", 1));
            }
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }

        return new OrderRequest(orderId, items);
    }

    private static CLIConfig parseArgs(String[] args) {
        if (args.length == 0) {
            System.err.println("Error: Missing required scenario argument");
            return null;
        }

        String scenario = args[0];

        if (!VALID_SCENARIOS.contains(scenario)) {
            System.err.println("Error: Invalid scenario. Must be one of: " + String.join(", ", VALID_SCENARIOS));
            return null;
        }

        CLIConfig config = new CLIConfig();
        config.scenario = scenario;
        config.paymentFailRate = 0.0;
        config.seed = null;

        // Parse optional flags
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("--payment-fail-rate")) {
                if (i + 1 >= args.length) {
                    System.err.println("Error: --payment-fail-rate requires a value");
                    return null;
                }
                try {
                    config.paymentFailRate = Double.parseDouble(args[++i]);
                    if (config.paymentFailRate < 0.0 || config.paymentFailRate > 1.0) {
                        System.err.println("Error: --payment-fail-rate must be between 0.0 and 1.0");
                        return null;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error: --payment-fail-rate must be a number");
                    return null;
                }
            } else if (arg.equals("--seed")) {
                if (i + 1 >= args.length) {
                    System.err.println("Error: --seed requires a value");
                    return null;
                }
                try {
                    config.seed = Long.parseLong(args[++i]);
                } catch (NumberFormatException e) {
                    System.err.println("Error: --seed must be a long integer");
                    return null;
                }
            } else {
                System.err.println("Error: Unknown flag: " + arg);
                return null;
            }
        }

        return config;
    }

    private static void printUsage() {
        System.err.println("Usage: java FraudOrderProcessingApp <scenario> [options]");
        System.err.println();
        System.err.println("Scenarios:");
        System.err.println("  low-risk    Normal order with low fraud risk");
        System.err.println("  high-risk   Large quantity order that may be flagged");
        System.err.println("  fraud-test  Order with intentional fraud indicators");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  --payment-fail-rate <double>  Probability of payment failure (0.0-1.0, default: 0.0)");
        System.err.println("  --seed <long>                 Random seed for deterministic behavior");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  java FraudOrderProcessingApp low-risk");
        System.err.println("  java FraudOrderProcessingApp high-risk --payment-fail-rate 0.5 --seed 12345");
        System.err.println("  java FraudOrderProcessingApp fraud-test");
    }

    private static class CLIConfig {
        String scenario;
        double paymentFailRate;
        Long seed;
    }
}
