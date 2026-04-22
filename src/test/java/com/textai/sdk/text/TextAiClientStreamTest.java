package com.textai.sdk.text;

import com.textai.sdk.error.AiErrorType;
import com.textai.sdk.openai.TextApiMode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies streaming text behavior and strict stream failure handling.
 */
class TextAiClientStreamTest {

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
    void chatStreamParsesObjectChunks() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"choices\":[{\"delta\":{\"content\":\"Hel\"}}]}\n\n"
                        + "data: {\"choices\":[{\"delta\":{\"content\":\"lo\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":2,\"total_tokens\":3}}\n\n"
                        + "data: [DONE]\n\n"));

        TextAiClient client = TextAiClient.forChatCompletions(server.url("/").toString(), "test-key", "gpt-5.4");
        AtomicReference<String> callbackText = new AtomicReference<>("");

        TextStreamResponse response = client.stream(
                TextRequest.builder().userInput("hi").build(),
                delta -> callbackText.set(callbackText.get() + delta)
        );

        assertTrue(response.isSuccess());
        assertEquals("Hello", response.getFullText());
        assertEquals("Hello", callbackText.get());
        assertEquals("stop", response.getFinishReason());
    }

    @Test
    void chatStreamParsesArrayChunks() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .setBody("data: [{\"choices\":[{\"delta\":{\"content\":\"He\"}}]},{\"choices\":[{\"delta\":{\"content\":\"y\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":2,\"total_tokens\":3}}]\n\n"
                        + "data: [DONE]\n\n"));

        TextAiClient client = TextAiClient.forChatCompletions(server.url("/").toString(), "test-key", "gpt-5.4");
        TextStreamResponse response = client.stream(TextRequest.builder().userInput("hi").build(), null);

        assertTrue(response.isSuccess());
        assertEquals("Hey", response.getFullText());
        assertEquals(3, response.getTotalTokens());
    }

    @Test
    void responsesStreamParsesEvents() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .setBody("event: response.output_text.delta\n"
                        + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Hi\"}\n\n"
                        + "event: response.output_text.delta\n"
                        + "data: {\"type\":\"response.output_text.delta\",\"delta\":\" there\"}\n\n"
                        + "event: response.completed\n"
                        + "data: {\"type\":\"response.completed\",\"response\":{\"usage\":{\"input_tokens\":2,\"output_tokens\":2,\"total_tokens\":4}}}\n\n"));

        TextAiClient client = TextAiClient.forResponses(server.url("/").toString(), "test-key", "gpt-5.4");
        TextStreamResponse response = client.stream(TextRequest.builder().userInput("hi").build(), null);

        assertTrue(response.isSuccess());
        assertEquals("Hi there", response.getFullText());
        assertEquals(4, response.getTotalTokens());
        assertEquals("response.completed", response.getLastEventType());
    }

    @Test
    void responsesStreamExposesMetadataAndRawHeaders() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .addHeader("X-Request-Id", "req-stream-123")
                .addHeader("X-Provider", "gateway-a")
                .setBody("event: response.output_text.delta\n"
                        + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Hi\"}\n\n"
                        + "event: response.completed\n"
                        + "data: {\"type\":\"response.completed\",\"response\":{\"id\":\"resp-123\",\"model\":\"gpt-5.4\",\"usage\":{\"input_tokens\":2,\"output_tokens\":1,\"total_tokens\":3}}}\n\n"));

        TextAiClient client = TextAiClient.forResponses(server.url("/").toString(), "test-key", "gpt-5.4");
        TextStreamResponse response = client.stream(TextRequest.builder().userInput("hi").build(), null);

        assertTrue(response.isSuccess());
        assertEquals("resp-123", response.getResponseId());
        assertEquals("gpt-5.4", response.getModel());
        assertEquals(List.of("req-stream-123"), response.getRawHeaders().get("X-Request-Id"));
        assertEquals(List.of("gateway-a"), response.getRawHeaders().get("X-Provider"));
    }

    @Test
    void streamFailsOnInvalidProtocolPayload() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .setBody("data: not-json\n\n"));

        TextAiClient client = TextAiClient.forChatCompletions(server.url("/").toString(), "test-key", "gpt-5.4");
        TextStreamResponse response = client.stream(TextRequest.builder().userInput("hi").build(), null);

        assertFalse(response.isSuccess());
        assertEquals(AiErrorType.STREAM_PROTOCOL_ERROR, response.getError().getType());
    }

    @Test
    void streamStringOverloadUsesDefaultSystemPrompt() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .setBody("event: response.output_text.delta\n"
                        + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Hi\"}\n\n"
                        + "event: response.completed\n"
                        + "data: {\"type\":\"response.completed\",\"response\":{\"usage\":{\"input_tokens\":1,\"output_tokens\":1,\"total_tokens\":2}}}\n\n"));

        TextAiClient client = new TextAiClient(TextAiClientConfig.builder()
                .baseUrl(server.url("/").toString())
                .apiKey("test-key")
                .defaultModel("gpt-5.4")
                .defaultSystemPrompt("Use the configured prompt.")
                .apiMode(TextApiMode.RESPONSES)
                .build());

        TextStreamResponse response = client.stream("hello", null);

        assertTrue(response.isSuccess());
        assertEquals("Hi", response.getFullText());
    }

    @Test
    void responsesStreamListenerReceivesLifecycleEventsInOrder() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .setBody("event: response.output_text.delta\n"
                        + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Hi\"}\n\n"
                        + "event: response.output_text.delta\n"
                        + "data: {\"type\":\"response.output_text.delta\",\"delta\":\" there\"}\n\n"
                        + "event: response.completed\n"
                        + "data: {\"type\":\"response.completed\",\"response\":{\"usage\":{\"input_tokens\":2,\"output_tokens\":2,\"total_tokens\":4}}}\n\n"));

        TextAiClient client = TextAiClient.forResponses(server.url("/").toString(), "test-key", "gpt-5.4");
        List<String> events = new ArrayList<>();

        TextStreamResponse response = client.streamWithListener("hi", new TextStreamListener() {
            @Override
            public void onStart() {
                events.add("start");
            }

            @Override
            public void onDelta(String delta) {
                events.add("delta:" + delta);
            }

            @Override
            public void onUsage(TextStreamUsage usage) {
                events.add("usage:" + usage.totalTokens());
            }

            @Override
            public void onComplete(TextStreamResponse response) {
                events.add("complete:" + response.getFullText());
            }
        });

        assertTrue(response.isSuccess());
        assertEquals(List.of("start", "delta:Hi", "delta: there", "usage:4", "complete:Hi there"), events);
    }

    @Test
    void streamListenerReceivesErrorOnMalformedPayload() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .setBody("data: not-json\n\n"));

        TextAiClient client = TextAiClient.forChatCompletions(server.url("/").toString(), "test-key", "gpt-5.4");
        List<String> events = new ArrayList<>();

        TextStreamResponse response = client.streamWithListener("hi", new TextStreamListener() {
            @Override
            public void onStart() {
                events.add("start");
            }

            @Override
            public void onError(com.textai.sdk.error.AiError error, String partialText, String lastEventType) {
                events.add("error:" + error.getType());
            }
        });

        assertFalse(response.isSuccess());
        assertEquals(List.of("start", "error:STREAM_PROTOCOL_ERROR"), events);
    }
}
