package com.textai.sdk.demo.examples;

import com.textai.sdk.openai.TextApiMode;
import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextAiClientConfig;
import com.textai.sdk.text.TextResponse;

/**
 * Shows how to adapt the text SDK to gateway-specific routing differences.
 *
 * <p>This example is useful when a provider is OpenAI-compatible at the JSON
 * layer but still requires a custom path, query string, or extra headers.
 */
public final class GatewayCustomizationExample {

    private GatewayCustomizationExample() {
    }

    /**
     * Executes one request with custom path, query parameter, and headers.
     */
    public static void main(String[] args) {
        TextAiClientConfig config = ExampleSupport.baseConfig(TextApiMode.RESPONSES)
                .endpointPathOverride("/openai/custom/responses")
                .queryParam("api-version", "2025-04-01")
                .extraHeader("X-Provider", "gateway-a")
                .extraHeader("X-Workspace", "demo")
                .build();

        TextAiClient client = new TextAiClient(config);
        TextResponse response = client.chat("Describe why transport customization is useful.");

        System.out.println("success=" + response.isSuccess());
        System.out.println("text=" + response.getText());
        System.out.println("headers=" + response.getRawHeaders());
    }
}
