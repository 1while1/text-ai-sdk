package com.textai.sdk.demo.examples;

import com.textai.sdk.openai.TextApiMode;
import com.textai.sdk.text.TextAiClientConfig;

/**
 * Small helper methods shared by the compatibility example suite.
 *
 * <p>The examples intentionally read configuration from environment variables
 * so they can be copied into real projects with minimal editing.
 */
final class ExampleSupport {
    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:48760";
    private static final String DEFAULT_MODEL = "gpt-5.4";

    private ExampleSupport() {
    }

    /**
     * Returns the configured API key or fails with a clear message.
     */
    static String requireApiKey() {
        String apiKey = System.getenv("AI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Please set AI_API_KEY before running this example.");
        }
        return apiKey;
    }

    /**
     * Returns the example base URL, defaulting to the local compatibility gateway.
     */
    static String baseUrl() {
        String value = System.getenv("AI_BASE_URL");
        return value == null || value.isBlank() ? DEFAULT_BASE_URL : value;
    }

    /**
     * Returns the example model name, defaulting to the local sample model.
     */
    static String model() {
        String value = System.getenv("AI_MODEL");
        return value == null || value.isBlank() ? DEFAULT_MODEL : value;
    }

    /**
     * Creates a pre-populated builder for the chosen protocol family.
     */
    static TextAiClientConfig.Builder baseConfig(TextApiMode apiMode) {
        return TextAiClientConfig.builder()
                .baseUrl(baseUrl())
                .apiKey(requireApiKey())
                .defaultModel(model())
                .apiMode(apiMode);
    }
}
