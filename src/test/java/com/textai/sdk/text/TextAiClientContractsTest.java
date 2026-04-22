package com.textai.sdk.text;

import com.textai.sdk.openai.TextApiMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers configuration and request object contracts for the text SDK.
 */
class TextAiClientContractsTest {

    @Test
    void configBuilderRequiresBaseUrl() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TextAiClientConfig.builder()
                        .apiKey("key")
                        .build()
        );

        assertEquals("TextAiClientConfig.baseUrl cannot be blank", exception.getMessage());
    }

    @Test
    void requestBuilderRequiresUserInput() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TextRequest.builder().build()
        );

        assertEquals("TextRequest.userInput cannot be blank", exception.getMessage());
    }

    @Test
    void requestBuilderRequiresPositiveMaxTokens() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TextRequest.builder()
                        .userInput("hello")
                        .maxTokens(0)
                        .build()
        );

        assertEquals("TextRequest.maxTokens must be greater than 0", exception.getMessage());
    }

    @Test
    void configBuilderSupportsDefaultSystemPrompt() {
        TextAiClientConfig config = TextAiClientConfig.builder()
                .baseUrl("http://127.0.0.1:48760")
                .apiKey("key")
                .defaultModel("gpt-5.4")
                .defaultSystemPrompt("You are a helpful assistant.")
                .apiMode(TextApiMode.RESPONSES)
                .build();

        assertEquals("You are a helpful assistant.", config.getDefaultSystemPrompt());
    }

    @Test
    void configBuilderStoresTransportCustomization() {
        TextAiClientConfig config = TextAiClientConfig.builder()
                .baseUrl("http://127.0.0.1:48760")
                .apiKey("key")
                .defaultModel("gpt-5.4")
                .apiMode(TextApiMode.RESPONSES)
                .endpointPathOverride("/custom/responses")
                .queryParam("api-version", "2025-04-01")
                .extraHeader("X-Provider", "gateway-a")
                .build();

        assertEquals("/custom/responses", config.getEndpointPathOverride());
        assertEquals(Map.of("api-version", "2025-04-01"), config.getQueryParams());
        assertEquals(Map.of("X-Provider", "gateway-a"), config.getExtraHeaders());
    }

    @Test
    void configBuilderStoresMaxRetries() {
        TextAiClientConfig config = TextAiClientConfig.builder()
                .baseUrl("http://127.0.0.1:48760")
                .apiKey("key")
                .defaultModel("gpt-5.4")
                .maxRetries(2)
                .build();

        assertEquals(2, config.getMaxRetries());
    }

    @Test
    void publicStreamingApiUsesDistinctListenerMethodNames() {
        Method[] methods = TextAiClient.class.getMethods();

        boolean hasAmbiguousSyncListenerOverload = Arrays.stream(methods)
                .anyMatch(method -> method.getName().equals("stream")
                        && method.getParameterCount() >= 2
                        && method.getParameterTypes()[method.getParameterCount() - 1] == TextStreamListener.class);

        boolean hasAmbiguousAsyncListenerOverload = Arrays.stream(methods)
                .anyMatch(method -> method.getName().equals("streamAsync")
                        && method.getParameterCount() >= 2
                        && method.getParameterTypes()[method.getParameterCount() - 1] == TextStreamListener.class);

        boolean hasNamedSyncListenerEntry = Arrays.stream(methods)
                .anyMatch(method -> method.getName().equals("streamWithListener")
                        && method.getParameterCount() == 2
                        && method.getParameterTypes()[1] == TextStreamListener.class);

        boolean hasNamedAsyncListenerEntry = Arrays.stream(methods)
                .anyMatch(method -> method.getName().equals("streamAsyncWithListener")
                        && method.getParameterCount() == 2
                        && method.getParameterTypes()[1] == TextStreamListener.class);

        assertFalse(hasAmbiguousSyncListenerOverload);
        assertFalse(hasAmbiguousAsyncListenerOverload);
        assertTrue(hasNamedSyncListenerEntry);
        assertTrue(hasNamedAsyncListenerEntry);
    }
}
