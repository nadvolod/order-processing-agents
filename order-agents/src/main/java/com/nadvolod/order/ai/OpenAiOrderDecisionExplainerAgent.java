package com.nadvolod.order.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nadvolod.order.domain.AgentAdvice;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import java.util.ArrayList;

public final class OpenAiOrderDecisionExplainerAgent implements OrderDecisionExplainerAgent {

    private final OpenAIClient client;
    private final ObjectMapper om = new ObjectMapper();
    private final String model;

    public OpenAiOrderDecisionExplainerAgent(String apiKey, String model) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
        this.model = model;
    }

    @Override
    public AgentAdvice explain(OrderRequest request, OrderResponse response) {
        try {
            String prompt = buildPrompt(request, response);

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .addUserMessage(prompt)
                    .model(model)
                    .build();

            ChatCompletion completion = client.chat().completions().create(params);
            String jsonText = completion.choices().get(0).message().content().orElse("");

            // Parse the model's JSON output into AgentAdvice
            JsonNode parsed = om.readTree(jsonText);
            String summary = parsed.get("summary").asText();
            var actions = new ArrayList<String>();
            parsed.get("recommendedActions").forEach(n -> actions.add(n.asText()));
            String customerMessage = parsed.get("customerMessage").asText();

            return new AgentAdvice(summary, actions, customerMessage);

        } catch (Exception e) {
            // Fail closed: don't break the workflow. Return a safe fallback.
            return new AgentAdvice(
                    "AI agent failed: " + e.getMessage(),
                    java.util.List.of("Retry later", "Contact support"),
                    "Thanks for your order. We're having trouble generating details right now, but we'll follow up shortly."
            );
        }
    }

    private String buildPrompt(OrderRequest req, OrderResponse resp) {
        return """
        You are a customer-focused order support assistant for an e-commerce store.

        Explain what happened with the order and suggest next actions.
        Be specific. Be brief. No jargon.

        Return your response as valid JSON with this exact structure:
        {
          "summary": "brief summary of what happened",
          "recommendedActions": ["action 1", "action 2"],
          "customerMessage": "friendly message for the customer"
        }

        OrderRequest:
        %s

        OrderResponse:
        %s
        """.formatted(req.toString(), resp.toString());
    }
}
