package com.textai.sdk.demo.examples;

import com.textai.sdk.openai.TextApiMode;
import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextAiClientConfig;
import com.textai.sdk.text.TextResponse;

/**
 * Demonstrates the production-oriented combination of retries and metadata.
 *
 * <p>Copy this example when you need a pure-text request that can survive a
 * small number of transient failures and still expose provider diagnostics.
 */
public final class RetryAndMetadataExample {

    private RetryAndMetadataExample() {
    }

    /**
     * Executes one non-stream text request with retries enabled.
     */
    public static void main(String[] args) {
        TextAiClientConfig config = ExampleSupport.baseConfig(TextApiMode.RESPONSES)
                .maxRetries(2)
                .build();

        TextAiClient client = new TextAiClient(config);
        TextResponse response = client.chat("Explain why retries and metadata help operations teams.");

        System.out.println("success=" + response.isSuccess());
        System.out.println("text=" + response.getText());
        System.out.println("responseId=" + response.getResponseId());
        System.out.println("model=" + response.getModel());
        System.out.println("rawHeaders=" + response.getRawHeaders());
    }
}
