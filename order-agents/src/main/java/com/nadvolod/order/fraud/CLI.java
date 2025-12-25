package com.nadvolod.order.fraud;

import java.util.Set;

public class CLI {
    private static final Set<String> VALID_SCENARIOS = Set.of(
        "low-risk",
        "high-risk",
        "fraud-test",
        "payment-flaky",
        "payment-broken"
    );

     public static CLIConfig parseArgs(String[] args) {
        if (args.length == 0) {
            System.err.println("Error: Missing required scenario argument");
            return null;
        }

        String scenario = args[0];

        if (!VALID_SCENARIOS.contains(scenario)) {
            System.err.println("Error: Invalid scenario. Must be one of: " + String.join(", ", VALID_SCENARIOS));
            return null;
        }

        // No additional flags needed - scenario determines all behavior
        if (args.length > 1) {
            System.err.println("Error: Unexpected arguments. Only scenario name is required.");
            printUsage();
            return null;
        }

        CLIConfig config = new CLIConfig();
        config.scenario = scenario;
        return config;
    }

    public static void printUsage() {
        System.err.println("Usage: java FraudOrderProcessingApp <scenario>");
        System.err.println();
        System.err.println("Scenarios:");
        System.err.println("  low-risk         Normal order with low fraud risk (payment always succeeds)");
        System.err.println("  high-risk        Large quantity order that may be flagged (payment always succeeds)");
        System.err.println("  fraud-test       Order with intentional fraud indicators (payment always succeeds)");
        System.err.println("  payment-flaky    Normal order with flaky payment (70% failure rate)");
        System.err.println("                   → Use this to see Temporal's retry benefits!");
        System.err.println("  payment-broken   Normal order with broken payment (100% failure rate)");
        System.err.println("                   → Demonstrates retry exhaustion");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  java FraudOrderProcessingApp low-risk");
        System.err.println("  java FraudOrderProcessingApp payment-flaky");
        System.err.println("  java FraudOrderProcessingApp payment-broken");
    }

    public static class CLIConfig {
        public String scenario;
    }
}
