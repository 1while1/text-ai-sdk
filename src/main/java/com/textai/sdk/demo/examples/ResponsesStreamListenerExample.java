package com.textai.sdk.demo.examples;

import com.textai.sdk.openai.TextApiMode;
import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextStreamListener;
import com.textai.sdk.text.TextStreamResponse;
import com.textai.sdk.text.TextStreamUsage;

/**
 * Demonstrates responses-style streaming with the richer lifecycle listener API.
 *
 * <p>This is the compatibility example to copy when you need start, delta,
 * usage, completion, and error hooks in one place.
 */
public final class ResponsesStreamListenerExample {

    private ResponsesStreamListenerExample() {
    }

    /**
     * Executes one async streaming request and prints lifecycle events.
     */
    public static void main(String[] args) {
        TextAiClient client = new TextAiClient(
                ExampleSupport.baseConfig(TextApiMode.RESPONSES).build()
        );

        TextStreamResponse response = client.streamAsyncWithListener(
                "Write a short streamed greeting.",
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
                        System.out.println("[listener] totalTokens=" + usage.totalTokens());
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

        System.out.println("success=" + response.isSuccess());
        System.out.println("fullText=" + response.getFullText());
        System.out.println("lastEventType=" + response.getLastEventType());
    }
}
