package com.textai.sdk.demo;

import com.textai.sdk.openai.TextApiMode;
import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextAiClientConfig;
import com.textai.sdk.text.TextStreamListener;
import com.textai.sdk.text.TextStreamUsage;
import com.textai.sdk.text.TextRequest;
import com.textai.sdk.text.TextResponse;
import com.textai.sdk.text.TextStreamResponse;

/**
 * Small demo entry point showing the most common ways to use the text SDK.
 */
public final class TextAiClientDemo {

    private TextAiClientDemo() {
    }

    /**
     * Runs a short sync, stream, async, and async-listener demonstration using
     * environment-provided credentials.
     */
    public static void main(String[] args) {
        String apiKey = System.getenv("AI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Please set AI_API_KEY before running the demo.");
        }

        TextAiClientConfig config = TextAiClientConfig.builder()
                .baseUrl("http://127.0.0.1:48760")
                .apiKey(apiKey)
                .defaultModel("gpt-5.4")
                .defaultSystemPrompt("You are a helpful assistant.")
                .apiMode(TextApiMode.RESPONSES)
                .build();

        TextAiClient client = new TextAiClient(config);

        TextResponse syncResponse = client.chat("Please introduce yourself in one short paragraph.");
        System.out.println("sync success: " + syncResponse.isSuccess());
        System.out.println("sync text: " + syncResponse.getText());

        TextStreamResponse streamResponse = client.stream(
                TextRequest.builder().userInput("Give me a short greeting.").build(),
                delta -> {
                    System.out.print(delta);
                    System.out.flush();
                }
        );
        System.out.println();
        System.out.println("stream success: " + streamResponse.isSuccess());
        System.out.println("stream full text: " + streamResponse.getFullText());

        TextResponse asyncResponse = client.chatAsync("Summarize your role in one sentence.").join();
        System.out.println("async text: " + asyncResponse.getText());

        System.out.println("async listener stream:");
        TextStreamResponse asyncListenerResponse = client.streamAsyncWithListener(
                "Stream a short closing sentence.",
                new TextStreamListener() {
                    @Override
                    public void onStart() {
                        System.out.println("[listener] start");
                    }

                    @Override
                    public void onDelta(String delta) {
                        System.out.print(delta);
                        System.out.flush();
                    }

                    @Override
                    public void onUsage(TextStreamUsage usage) {
                        System.out.println();
                        System.out.println("[listener] usage total=" + usage.totalTokens());
                    }

                    @Override
                    public void onComplete(TextStreamResponse response) {
                        System.out.println("[listener] complete");
                    }

                    @Override
                    public void onError(com.textai.sdk.error.AiError error, String partialText, String lastEventType) {
                        System.out.println("[listener] error=" + error.getType());
                    }
                }
        ).join();
        System.out.println("async listener success: " + asyncListenerResponse.isSuccess());
        System.out.println("async listener full text: " + asyncListenerResponse.getFullText());
    }
}
