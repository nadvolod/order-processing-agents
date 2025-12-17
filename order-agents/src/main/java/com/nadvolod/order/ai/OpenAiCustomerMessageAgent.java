package com.nadvolod.order.ai;

import com.nadvolod.order.domain.AgentAdvice;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import java.util.Objects;

public final class OpenAiCustomerMessageAgent implements CustomerMessageAgent {

    private final OpenAIClient client;
    private final String model;

    public OpenAiCustomerMessageAgent(OpenAIClient client, String model) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.model = Objects.requireNonNull(model, "model must not be null");
    }

    @Override
    public String generateMessage(AgentAdvice advice) {
        try {
            String prompt = buildPrompt(advice);

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .addUserMessage(prompt)
                    .model(model)
                    .build();

            ChatCompletion completion = client.chat().completions().create(params);
            String message = completion.choices().get(0).message().content().orElse("");

            // Return the message or fallback if empty
            return message.isBlank() ? safeFallback(advice, "Empty response") : message;

        } catch (Exception e) {
            return safeFallback(advice, "AI call failed: " + e.getClass().getSimpleName());
        }
    }

    private String buildPrompt(AgentAdvice advice) {
        return """
        You are a Customer Communications Specialist for an e-commerce store.

        Convert this internal order analysis into a clear, friendly customer message.
        Use simple language, no technical jargon, and provide actionable next steps.

        Internal Summary: %s
        Internal Actions: %s

        Generate a brief, customer-ready message (2-3 sentences).
        """.formatted(advice.summary(), advice.recommendedActions());
    }

    private String safeFallback(AgentAdvice advice, String reason) {
        // Context-aware fallback based on summary content
        String summaryLower = advice.summary().toLowerCase();

        if (summaryLower.contains("accept") || summaryLower.contains("success")) {
            return "Thank you for your order! We're processing it and will send updates soon.";
        } else if (summaryLower.contains("reject") || summaryLower.contains("fail")) {
            return "We're unable to complete your order at this time. Please contact support for assistance.";
        } else {
            return "We're reviewing your order. You'll receive an update shortly.";
        }
    }
}
