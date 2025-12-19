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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public final class OpenAiOrderDecisionExplainerAgent implements OrderDecisionExplainerAgent {

    private static final Logger log = LoggerFactory.getLogger(OpenAiOrderDecisionExplainerAgent.class);

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

            // Strip markdown code blocks if present (```json ... ```)
            jsonText = jsonText.trim();
            if (jsonText.startsWith("```")) {
                int start = jsonText.indexOf('\n') + 1;
                int end = jsonText.lastIndexOf("```");
                if (end > start) {
                    jsonText = jsonText.substring(start, end).trim();
                }
            }

            // Parse the model's JSON output into AgentAdvice
            JsonNode parsed = om.readTree(jsonText);
            String summary = parsed.get("summary").asText();
            var actions = new ArrayList<String>();
            parsed.get("recommendedActions").forEach(n -> actions.add(n.asText()));

            return new AgentAdvice(summary, actions);

        } catch (Exception e) {
            // Fail closed: don't break the workflow. Return a safe fallback.
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            String fallbackSummary = "AI agent failed: " + errorMessage;
            
            log.warn("Fallback triggered in OpenAiOrderDecisionExplainerAgent.explain(). " +
                    "Returning safe fallback with summary: '{}'. Reason: {}", 
                    fallbackSummary, e.getClass().getSimpleName() + ": " + errorMessage);
            
            return new AgentAdvice(
                    "AI agent failed: " + e.getMessage(),
                    java.util.List.of("Retry later", "Contact support")
            );
        }
    }

    private String buildPrompt(OrderRequest req, OrderResponse resp) {
        return """
        You are an internal Order Decision Explainer for an e-commerce system.

        Analyze what happened during order processing and provide internal notes for engineers.
        Be specific. Be technical. This is for internal use only.

        Return your response as valid JSON with this exact structure:
        {
          "summary": "brief technical summary of what happened",
          "recommendedActions": ["action 1", "action 2"]
        }

        OrderRequest:
        %s

        OrderResponse:
        %s
        """.formatted(req.toString(), resp.toString());
    }
}
