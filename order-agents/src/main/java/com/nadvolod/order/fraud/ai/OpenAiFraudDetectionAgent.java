package com.nadvolod.order.fraud.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.fraud.domain.FraudDetectionResult;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenAI-powered fraud detection agent.
 * Analyzes order patterns for fraud indicators.
 */
public final class OpenAiFraudDetectionAgent implements FraudDetectionAgent {

    private static final Logger log = LoggerFactory.getLogger(OpenAiFraudDetectionAgent.class);

    private final OpenAIClient client;
    private final String model;
    private final ObjectMapper om = new ObjectMapper();

    public OpenAiFraudDetectionAgent(String apiKey, String model) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
        this.model = model;
    }

    @Override
    public FraudDetectionResult analyze(OrderRequest request) {
        try {
            String prompt = buildPrompt(request);

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

            // Parse JSON response
            JsonNode parsed = om.readTree(jsonText);
            boolean approved = parsed.get("approved").asBoolean();
            double riskScore = parsed.get("riskScore").asDouble();
            String reason = parsed.get("reason").asText();
            String riskLevel = parsed.get("riskLevel").asText();

            return new FraudDetectionResult(approved, riskScore, reason, riskLevel);

        } catch (Exception e) {
            // Fail-safe fallback: approve with low confidence
            String fallbackReason = "AI fraud detection failed: " + e.getMessage() + ". Defaulting to APPROVE with caution.";

            log.warn("Fallback triggered in OpenAiFraudDetectionAgent.analyze(). " +
                    "Returning safe fallback. Reason: {}",
                    e.getClass().getSimpleName() + ": " + e.getMessage());

            // Conservative fallback: approve to not block legitimate orders
            return new FraudDetectionResult(
                true,    // approve to not block legitimate orders
                0.5,     // medium risk score to flag for review
                fallbackReason,
                "MEDIUM"
            );
        }
    }

    private String buildPrompt(OrderRequest request) {
        return """
        You are a Fraud Detection AI for an e-commerce platform.

        Analyze the following order for fraud indicators:
        - Unusual quantities
        - Suspicious patterns
        - Order ID anomalies
        - Item combinations

        Return your analysis as valid JSON with this exact structure:
        {
          "approved": true/false,
          "riskScore": 0.0-1.0,
          "reason": "brief explanation of the decision",
          "riskLevel": "LOW" | "MEDIUM" | "HIGH"
        }

        OrderRequest:
        %s

        Be conservative: when in doubt, approve with MEDIUM risk for manual review.
        """.formatted(request.toString());
    }
}
