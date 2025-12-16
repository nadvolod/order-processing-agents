package com.nadvolod.order.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nadvolod.order.domain.AgentAdvice;
import com.nadvolod.order.domain.OrderRequest;
import com.nadvolod.order.domain.OrderResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public final class OpenAiOrderDecisionExplainerAgent implements OrderDecisionExplainerAgent {

    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    private final String apiKey;
    private final String model; // e.g. "gpt-4o-mini" or your preferred model

    public OpenAiOrderDecisionExplainerAgent(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public AgentAdvice explain(OrderRequest request, OrderResponse response) {
        /*
        * Need a lot of this code because we don't have Temporal
        * */
        try {
            String input = buildPrompt(request, response);
            String body = buildResponsesRequestBody(input);

            HttpRequest httpReq = HttpRequest.newBuilder(RESPONSES_URI)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> httpResp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (httpResp.statusCode() < 200 || httpResp.statusCode() >= 300) {
                throw new RuntimeException("OpenAI error " + httpResp.statusCode() + ": " + httpResp.body());
            }

            // Parse the model’s JSON output into AgentAdvice
            JsonNode root = om.readTree(httpResp.body());

            // Responses API output structure can vary; simplest is to extract the assistant text and parse as JSON.
            // If your response uses structured outputs, the assistant content should be valid JSON.
            String jsonText = extractOutputText(root);

            JsonNode parsed = om.readTree(jsonText);
            String summary = parsed.get("summary").asText();
            var actions = new ArrayList<String>();
            parsed.get("recommendedActions").forEach(n -> actions.add(n.asText()));
            String customerMessage = parsed.get("customerMessage").asText();

            return new AgentAdvice(summary, actions, customerMessage);

        } catch (Exception e) {
            // Fail closed: don’t break the workflow. Return a safe fallback.
            return new AgentAdvice(
                    "AI agent failed: " + e.getMessage(),
                    java.util.List.of("Retry later", "Contact support"),
                    "Thanks for your order. We’re having trouble generating details right now, but we’ll follow up shortly."
            );
        }
    }

    private String buildPrompt(OrderRequest req, OrderResponse resp) {
        // Keep it crisp and customer-focused.
        return """
        You are a customer-focused order support assistant for an e-commerce store.

        Explain what happened with the order and suggest next actions.
        Be specific. Be brief. No jargon.

        OrderRequest:
        %s

        OrderResponse:
        %s
        """.formatted(req.toString(), resp.toString());
    }

    private String buildResponsesRequestBody(String prompt) throws Exception {
        // Structured Outputs: enforce exact JSON schema via text.format json_schema (strict).
        // Response is created via POST /v1/responses.
        String schema = """
        {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "summary": { "type": "string" },
            "recommendedActions": { "type": "array", "items": { "type": "string" } },
            "customerMessage": { "type": "string" }
          },
          "required": ["summary", "recommendedActions", "customerMessage"]
        }
        """;

        var node = om.createObjectNode();
        node.put("model", model);
        node.put("instructions", "Return only JSON that matches the provided schema.");
        node.put("input", prompt);

        var text = om.createObjectNode();
        var format = om.createObjectNode();
        format.put("type", "json_schema");
        format.put("strict", true);
        format.put("name", "agent_advice");
        format.set("schema", om.readTree(schema));
        text.set("format", format);
        node.set("text", text);

        return om.writeValueAsString(node);
    }

    private String extractOutputText(JsonNode root) {
        // Practical extractor: walk output items, find assistant message text.
        // If you want, we can tighten this once you paste a real response payload.
        if (root.has("output_text")) return root.get("output_text").asText(); // sometimes present per docs

        // Fallback: dig into output array
        JsonNode output = root.get("output");
        if (output == null || !output.isArray()) throw new RuntimeException("No output found");

        for (JsonNode item : output) {
            if (item.has("content") && item.get("content").isArray()) {
                for (JsonNode c : item.get("content")) {
                    if (c.has("text")) return c.get("text").asText();
                }
            }
        }
        throw new RuntimeException("Could not extract output text");
    }
}
