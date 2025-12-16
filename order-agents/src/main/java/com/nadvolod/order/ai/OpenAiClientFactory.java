package com.nadvolod.order.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

public final class OpenAiClientFactory {

    private OpenAiClientFactory() {}

    public static OpenAIClient create() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY not set");
        }
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
