package com.nadvolod.order.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

public class OpenAiClientFactory {
    
    private OpenAiClientFactory() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Creates an OpenAI client using the API key from the OPENAI_API_KEY environment variable.
     * 
     * @return configured OpenAIClient instance
     * @throws IllegalStateException if OPENAI_API_KEY environment variable is not set
     */
    public static OpenAIClient create() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is not set");
        }
        
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
