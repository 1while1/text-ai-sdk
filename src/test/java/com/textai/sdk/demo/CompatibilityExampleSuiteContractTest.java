package com.textai.sdk.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Verifies that the documented compatibility example suite entry points exist.
 */
class CompatibilityExampleSuiteContractTest {

    @Test
    void compatibilityExampleClassesExist() {
        assertDoesNotThrow(() -> Class.forName("com.textai.sdk.demo.examples.ResponsesProviderExample"));
        assertDoesNotThrow(() -> Class.forName("com.textai.sdk.demo.examples.ChatCompletionsProviderExample"));
        assertDoesNotThrow(() -> Class.forName("com.textai.sdk.demo.examples.GatewayCustomizationExample"));
        assertDoesNotThrow(() -> Class.forName("com.textai.sdk.demo.examples.ResponsesStreamListenerExample"));
        assertDoesNotThrow(() -> Class.forName("com.textai.sdk.demo.examples.RetryAndMetadataExample"));
    }
}
