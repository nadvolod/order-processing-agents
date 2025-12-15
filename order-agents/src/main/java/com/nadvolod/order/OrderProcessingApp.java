package com.nadvolod.order;

import com.nadvolod.order.domain.*;
import com.nadvolod.order.service.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * CLI-based order processing demo.
 * 
 * Usage: java OrderProcessingApp <scenario> [--payment-fail-rate <rate>] [--shipping-fail-rate <rate>] [--seed <long>]
 * 
 * Scenarios:
 *   in-stock    - All items available
 *   out-of-stock - No items available
 *   mixed       - Mix of available and unavailable items
 */
public class OrderProcessingApp {
    
    public static void main(String[] args) {
        // Parse CLI arguments
        CLIConfig config = parseArgs(args);
        
        if (config == null) {
            printUsage();
            System.exit(1);
        }
        
        // Create services with configuration
        Random random = config.seed != null ? new Random(config.seed) : new Random();
        
        InventoryService inventoryService = new FakeInventoryService();
        PaymentService paymentService = new FakePaymentService(config.paymentFailRate, random);
        ShippingService shippingService = new FakeShippingService(config.shippingFailRate, random);
        
        // Create processor
        OrderProcessor processor = new OrderProcessor(inventoryService, paymentService, shippingService);
        
        // Create order request based on scenario
        OrderRequest request = createOrderForScenario(config.scenario);
        
        // Process order
        OrderResponse response = processor.processOrder(request);
        
        System.out.println("\n=== Order Complete ===");
        System.out.println("Final Status: " + response.status());
    }
    
    private static OrderRequest createOrderForScenario(String scenario) {
        List<OrderLine> items = new ArrayList<>();
        
        switch (scenario.toLowerCase()) {
            case "in-stock" -> {
                items.add(new OrderLine("SKU-123", 2));  // 10 available
                items.add(new OrderLine("SKU-789", 1));  // 5 available
            }
            case "out-of-stock" -> {
                items.add(new OrderLine("SKU-456", 1));  // 0 available
            }
            case "mixed" -> {
                items.add(new OrderLine("SKU-123", 2));  // 10 available
                items.add(new OrderLine("SKU-456", 1));  // 0 available
            }
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }
        
        return new OrderRequest("ORDER-" + System.currentTimeMillis(), items);
    }
    
    private static CLIConfig parseArgs(String[] args) {
        if (args.length == 0) {
            System.err.println("Error: Missing required scenario argument");
            return null;
        }
        
        String scenario = args[0];
        
        // Validate scenario
        if (!scenario.equals("in-stock") && !scenario.equals("out-of-stock") && !scenario.equals("mixed")) {
            System.err.println("Error: Invalid scenario. Must be one of: in-stock, out-of-stock, mixed");
            return null;
        }
        
        CLIConfig config = new CLIConfig();
        config.scenario = scenario;
        config.paymentFailRate = 0.0;  // Default: never fail
        config.shippingFailRate = 0.0; // Default: never fail
        config.seed = null;            // Default: no seed
        
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
            } else if (arg.equals("--shipping-fail-rate")) {
                if (i + 1 >= args.length) {
                    System.err.println("Error: --shipping-fail-rate requires a value");
                    return null;
                }
                try {
                    config.shippingFailRate = Double.parseDouble(args[++i]);
                    if (config.shippingFailRate < 0.0 || config.shippingFailRate > 1.0) {
                        System.err.println("Error: --shipping-fail-rate must be between 0.0 and 1.0");
                        return null;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error: --shipping-fail-rate must be a number");
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
        System.err.println("Usage: java OrderProcessingApp <scenario> [options]");
        System.err.println();
        System.err.println("Scenarios:");
        System.err.println("  in-stock      All items are available in inventory");
        System.err.println("  out-of-stock  No items are available");
        System.err.println("  mixed         Mix of available and unavailable items");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  --payment-fail-rate <double>   Probability of payment failure (0.0-1.0, default: 0.0)");
        System.err.println("  --shipping-fail-rate <double>  Probability of shipping failure (0.0-1.0, default: 0.0)");
        System.err.println("  --seed <long>                  Random seed for deterministic behavior");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  java OrderProcessingApp in-stock");
        System.err.println("  java OrderProcessingApp in-stock --payment-fail-rate 0.5 --seed 12345");
        System.err.println("  java OrderProcessingApp mixed --payment-fail-rate 1 --shipping-fail-rate 0");
    }
    
    private static class CLIConfig {
        String scenario;
        double paymentFailRate;
        double shippingFailRate;
        Long seed;
    }
}
