package com.textai.sdk.demo.examples;

import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextResponse;

/**
 * Minimal provider-facing example for the `/v1/chat/completions` protocol family.
 *
 * <p>Use this when an upstream service expects chat-style OpenAI-compatible
 * payloads and you still want to stay inside the pure-text SDK surface.
 */
public final class ChatCompletionsProviderExample {

    private ChatCompletionsProviderExample() {
    }

    /**
     * Executes one chat/completions request with an explicit system prompt.
     */
    public static void main(String[] args) {
        TextAiClient client = TextAiClient.forChatCompletions(
                ExampleSupport.baseUrl(),
                ExampleSupport.requireApiKey(),
                ExampleSupport.model()
        );

        TextResponse response = client.chat(
                "You are a concise assistant.",
                "Explain what this SDK is for in two sentences."
        );

        System.out.println("success=" + response.isSuccess());
        System.out.println("text=" + response.getText());
        System.out.println("finishReason=" + response.getFinishReason());
        System.out.println("responseId=" + response.getResponseId());
    }
}
