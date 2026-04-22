package com.textai.sdk.text;

import com.textai.sdk.error.AiErrorType;
import com.textai.sdk.openai.TextApiMode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies non-stream text behavior across both supported protocol families.
 */
class TextAiClientNonStreamTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void chatReturnsTextForChatCompletions() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"hello\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":3,\"completion_tokens\":4,\"total_tokens\":7}}"));

        TextAiClient client = TextAiClient.forChatCompletions(server.url("/").toString(), "test-key", "gpt-5.4");
        TextResponse response = client.chat(TextRequest.builder().userInput("hi").build());

        assertTrue(response.isSuccess());
        assertEquals("hello", response.getText());
        assertEquals("stop", response.getFinishReason());
        assertEquals(7, response.getTotalTokens());
    }

    @Test
    void responsesReturnsTextForResponsesApi() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"output_text\":\"hello from responses\",\"usage\":{\"input_tokens\":5,\"output_tokens\":6,\"total_tokens\":11}}"));

        TextAiClient client = TextAiClient.forResponses(server.url("/").toString(), "test-key", "gpt-5.4");
        TextResponse response = client.chat(TextRequest.builder().userInput("hi").build());

        assertTrue(response.isSuccess());
        assertEquals("hello from responses", response.getText());
        assertEquals(11, response.getTotalTokens());
    }

    @Test
    void chatResponseExposesMetadataAndRawHeaders() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Request-Id", "req-chat-123")
                .addHeader("X-Provider", "gateway-a")
                .setBody("{\"id\":\"chatcmpl-123\",\"model\":\"gpt-5.4\",\"choices\":[{\"message\":{\"content\":\"hello\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":3,\"completion_tokens\":4,\"total_tokens\":7}}"));

        TextAiClient client = TextAiClient.forChatCompletions(server.url("/").toString(), "test-key", "gpt-5.4");
        TextResponse response = client.chat(TextRequest.builder().userInput("hi").build());

        assertTrue(response.isSuccess());
        assertEquals("chatcmpl-123", response.getResponseId());
        assertEquals("gpt-5.4", response.getModel());
        assertEquals(List.of("req-chat-123"), response.getRawHeaders().get("X-Request-Id"));
        assertEquals(List.of("gateway-a"), response.getRawHeaders().get("X-Provider"));
    }

    @Test
    void chatRetriesTransientNetworkFailureWhenConfigured() {
        server.enqueue(new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"output_text\":\"retried ok\",\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3}}"));

        TextAiClient client = new TextAiClient(TextAiClientConfig.builder()
                .baseUrl(server.url("/").toString())
                .apiKey("test-key")
                .defaultModel("gpt-5.4")
                .apiMode(TextApiMode.RESPONSES)
                .maxRetries(1)
                .build());

        TextResponse response = client.chat("hello");

        assertTrue(response.isSuccess());
        assertEquals("retried ok", response.getText());
        assertEquals(2, server.getRequestCount());
    }

    @Test
    void chatDoesNotRetryClientErrors() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"bad request\"}}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"output_text\":\"should not be used\",\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3}}"));

        TextAiClient client = new TextAiClient(TextAiClientConfig.builder()
                .baseUrl(server.url("/").toString())
                .apiKey("test-key")
                .defaultModel("gpt-5.4")
                .apiMode(TextApiMode.RESPONSES)
                .maxRetries(3)
                .build());

        TextResponse response = client.chat("hello");

        assertFalse(response.isSuccess());
        assertEquals(AiErrorType.HTTP_ERROR, response.getError().getType());
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void chatFailsOnEmptyChoices() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":0,\"total_tokens\":1}}"));

        TextAiClient client = TextAiClient.forChatCompletions(server.url("/").toString(), "test-key", "gpt-5.4");
        TextResponse response = client.chat(TextRequest.builder().userInput("hi").build());

        assertFalse(response.isSuccess());
        assertEquals(AiErrorType.EMPTY_CHOICES, response.getError().getType());
    }

    @Test
    void chatFailsOnMissingMessage() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"finish_reason\":\"stop\"}]}"));

        TextAiClient client = TextAiClient.forChatCompletions(server.url("/").toString(), "test-key", "gpt-5.4");
        TextResponse response = client.chat(TextRequest.builder().userInput("hi").build());

        assertFalse(response.isSuccess());
        assertEquals(AiErrorType.MISSING_MESSAGE, response.getError().getType());
    }

    @Test
    void chatFailsOnNonTextContent() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":[{\"type\":\"output_text\",\"text\":\"hello\"}]},\"finish_reason\":\"stop\"}]}"));

        TextAiClient client = TextAiClient.forChatCompletions(server.url("/").toString(), "test-key", "gpt-5.4");
        TextResponse response = client.chat(TextRequest.builder().userInput("hi").build());

        assertFalse(response.isSuccess());
        assertEquals(AiErrorType.UNSUPPORTED_NON_TEXT_RESPONSE, response.getError().getType());
    }

    @Test
    void chatStringOverloadUsesDefaultSystemPrompt() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"output_text\":\"hello from overload\",\"usage\":{\"input_tokens\":2,\"output_tokens\":3,\"total_tokens\":5}}"));

        TextAiClient client = new TextAiClient(TextAiClientConfig.builder()
                .baseUrl(server.url("/").toString())
                .apiKey("test-key")
                .defaultModel("gpt-5.4")
                .defaultSystemPrompt("Use the configured prompt.")
                .apiMode(TextApiMode.RESPONSES)
                .build());

        TextResponse response = client.chat("say hi");
        RecordedRequest recordedRequest = server.takeRequest();

        assertTrue(response.isSuccess());
        assertEquals("/v1/responses", recordedRequest.getPath());
        assertTrue(recordedRequest.getBody().readUtf8().contains("\"instructions\":\"Use the configured prompt.\""));
    }

    @Test
    void chatOverloadAllowsExplicitSystemPromptOverride() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"hello\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":2,\"completion_tokens\":3,\"total_tokens\":5}}"));

        TextAiClient client = TextAiClient.forChatCompletions(server.url("/").toString(), "test-key", "gpt-5.4");
        TextResponse response = client.chat("Act like a mentor.", "say hi");
        RecordedRequest recordedRequest = server.takeRequest();

        assertTrue(response.isSuccess());
        assertEquals("/v1/chat/completions", recordedRequest.getPath());
        assertTrue(recordedRequest.getBody().readUtf8().contains("\"role\":\"system\""));
    }

    @Test
    void chatUsesEndpointPathOverrideAndQueryParams() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"output_text\":\"override ok\",\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3}}"));

        TextAiClient client = new TextAiClient(TextAiClientConfig.builder()
                .baseUrl(server.url("/").toString())
                .apiKey("test-key")
                .defaultModel("gpt-5.4")
                .apiMode(TextApiMode.RESPONSES)
                .endpointPathOverride("/openai/custom/responses")
                .queryParam("api-version", "2025-04-01")
                .build());

        TextResponse response = client.chat("hello");
        RecordedRequest recordedRequest = server.takeRequest();

        assertTrue(response.isSuccess());
        assertEquals("/openai/custom/responses?api-version=2025-04-01", recordedRequest.getPath());
    }

    @Test
    void chatAddsConfiguredExtraHeaders() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"output_text\":\"headers ok\",\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3}}"));

        TextAiClient client = new TextAiClient(TextAiClientConfig.builder()
                .baseUrl(server.url("/").toString())
                .apiKey("test-key")
                .defaultModel("gpt-5.4")
                .apiMode(TextApiMode.RESPONSES)
                .extraHeader("X-Provider", "gateway-a")
                .extraHeader("X-Workspace", "demo")
                .build());

        TextResponse response = client.chat("hello");
        RecordedRequest recordedRequest = server.takeRequest();

        assertTrue(response.isSuccess());
        assertEquals("gateway-a", recordedRequest.getHeader("X-Provider"));
        assertEquals("demo", recordedRequest.getHeader("X-Workspace"));
    }
}
