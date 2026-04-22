package com.textai.sdk.demo.examples;

import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextResponse;

/**
 * Minimal provider-facing example for the `/v1/responses` protocol family.
 *
 * <p>Use this when the upstream service exposes OpenAI-compatible responses
 * semantics and you want the smallest possible pure-text call site.
 */
public final class ResponsesProviderExample {

    private ResponsesProviderExample() {
    }

    /**
     * Executes one non-stream text request through the responses API family.
     */
    public static void main(String[] args) {
        TextAiClient client = TextAiClient.forResponses(
                ExampleSupport.baseUrl(),
                ExampleSupport.requireApiKey(),
                ExampleSupport.model()
        );

        TextResponse response = client.chat("Introduce yourself in one short paragraph.");

        System.out.println("success=" + response.isSuccess());
        System.out.println("text=" + response.getText());
        System.out.println("responseId=" + response.getResponseId());
        System.out.println("model=" + response.getModel());
    }
}
