package com.textai.sdk.text;

import com.textai.sdk.openai.TextApiMode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies async request behavior and cancellation semantics.
 */
class TextAiClientAsyncTest {

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
    void chatAsyncReturnsResponse() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"output_text\":\"async hello\",\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3}}"));

        TextAiClient client = TextAiClient.forResponses(server.url("/").toString(), "test-key", "gpt-5.4");
        TextResponse response = client.chatAsync(TextRequest.builder().userInput("hi").build()).join();

        assertTrue(response.isSuccess());
        assertEquals("async hello", response.getText());
    }

    @Test
    void streamAsyncReturnsResponse() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .setBody("event: response.output_text.delta\n"
                        + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"async\"}\n\n"
                        + "event: response.completed\n"
                        + "data: {\"type\":\"response.completed\",\"response\":{\"usage\":{\"input_tokens\":1,\"output_tokens\":1,\"total_tokens\":2}}}\n\n"));

        TextAiClient client = TextAiClient.forResponses(server.url("/").toString(), "test-key", "gpt-5.4");
        TextStreamResponse response = client.streamAsync(TextRequest.builder().userInput("hi").build(), null).join();

        assertTrue(response.isSuccess());
        assertEquals("async", response.getFullText());
    }

    @Test
    void streamAsyncSupportsCancellation() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBodyDelay(5, TimeUnit.SECONDS)
                .addHeader("Content-Type", "text/event-stream")
                .setBody("event: response.output_text.delta\n"
                        + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"slow\"}\n\n"));

        TextAiClient client = TextAiClient.forResponses(server.url("/").toString(), "test-key", "gpt-5.4");
        var future = client.streamAsync(TextRequest.builder().userInput("hi").build(), null);

        future.cancel(true);

        assertTrue(future.isCancelled());
        assertThrows(CancellationException.class, future::join);
    }

    @Test
    void chatAsyncRetries429WhenConfigured() {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"rate limited\"}}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"output_text\":\"async retried\",\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3}}"));

        TextAiClient client = new TextAiClient(TextAiClientConfig.builder()
                .baseUrl(server.url("/").toString())
                .apiKey("test-key")
                .defaultModel("gpt-5.4")
                .apiMode(TextApiMode.RESPONSES)
                .maxRetries(1)
                .build());

        TextResponse response = client.chatAsync("hello").join();

        assertTrue(response.isSuccess());
        assertEquals("async retried", response.getText());
        assertEquals(2, server.getRequestCount());
    }

    @Test
    void chatAsyncStringOverloadUsesDefaultSystemPrompt() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"output_text\":\"async overload\",\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3}}"));

        TextAiClient client = new TextAiClient(TextAiClientConfig.builder()
                .baseUrl(server.url("/").toString())
                .apiKey("test-key")
                .defaultModel("gpt-5.4")
                .defaultSystemPrompt("Use the configured prompt.")
                .apiMode(TextApiMode.RESPONSES)
                .build());

        TextResponse response = client.chatAsync("hello").join();

        assertTrue(response.isSuccess());
        assertEquals("async overload", response.getText());
    }
}
