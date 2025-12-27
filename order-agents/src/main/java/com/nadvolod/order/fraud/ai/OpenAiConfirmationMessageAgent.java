package com.nadvolod.order.fraud.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nadvolod.order.fraud.domain.ConfirmationMessage;
import com.nadvolod.order.fraud.domain.FraudOrderResponse;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

/**
 * OpenAI-powered confirmation message generator.
 * Creates customer-friendly messages based on order status.
 */
public final class OpenAiConfirmationMessageAgent implements ConfirmationMessageAgent {

    private final OpenAIClient client;
    private final String model;
    private final ObjectMapper om = new ObjectMapper();

    public OpenAiConfirmationMessageAgent(String apiKey, String model) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
        this.model = model;
    }

    @Override
    public ConfirmationMessage generate(FraudOrderResponse response) {
        try {
            String prompt = buildPrompt(response);

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .addUserMessage(prompt)
                    .model(model)
                    .build();

            ChatCompletion completion = client.chat().completions().create(params);
            String jsonText = completion.choices().get(0).message().content().orElse("");

            // Strip markdown code blocks
            jsonText = jsonText.trim();
            if (jsonText.startsWith("```")) {
                int start = jsonText.indexOf('\n') + 1;
                int end = jsonText.lastIndexOf("```");
                if (end > start) {
                    jsonText = jsonText.substring(start, end).trim();
                }
            }

            JsonNode parsed = om.readTree(jsonText);
            String subject = parsed.get("subject").asText();
            String body = parsed.get("body").asText();
            String tone = parsed.get("tone").asText();

            return new ConfirmationMessage(subject, body, tone);

        } catch (Exception e) {
            // Fallback to simple message
            return createFallbackMessage(response);
        }
    }

    private String buildPrompt(FraudOrderResponse response) {
        return """
        You are a Customer Communications Specialist for an e-commerce platform.

        Generate a confirmation message for this order:

        Order ID: %s
        Status: %s

        Return as JSON with this structure:
        {
          "subject": "email subject line",
          "body": "2-3 sentence message body",
          "tone": "positive" | "neutral" | "apologetic"
        }

        Guidelines:
        - APPROVED: Enthusiastic, confirm order and next steps
        - REJECTED_FRAUD: Apologetic, mention security review needed
        - REJECTED_PAYMENT: Helpful, suggest checking payment method
        """.formatted(response.orderId(), response.status());
    }

    private ConfirmationMessage createFallbackMessage(FraudOrderResponse response) {
        return switch (response.status()) {
            case "APPROVED" -> new ConfirmationMessage(
                "Order Confirmed: " + response.orderId(),
                "Thank you for your order! We've received it and will send updates soon.",
                "positive"
            );
            case "REJECTED_FRAUD" -> new ConfirmationMessage(
                "Order Under Review: " + response.orderId(),
                "We need to review your order for security. Our team will contact you within 24 hours.",
                "apologetic"
            );
            case "REJECTED_PAYMENT" -> new ConfirmationMessage(
                "Payment Issue: " + response.orderId(),
                "We couldn't process your payment. Please verify your payment method and try again.",
                "apologetic"
            );
            default -> new ConfirmationMessage(
                "Order Status: " + response.orderId(),
                "We're processing your order. You'll receive an update shortly.",
                "neutral"
            );
        };
    }
}
