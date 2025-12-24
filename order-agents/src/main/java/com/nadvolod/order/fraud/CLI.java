package com.nadvolod.order.fraud;

import java.util.Set;

public class CLI {
    private static final Set<String> VALID_SCENARIOS = Set.of("low-risk", "high-risk", "fraud-test");

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

    public static void printUsage() {
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

    public static class CLIConfig {
        public String scenario;
        public double paymentFailRate;
        public Long seed;
    }
}
