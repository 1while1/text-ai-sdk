package com.textai.sdk.text;

/**
 * Represents token usage observed during or at the end of a text stream.
 *
 * <p>The SDK keeps this value object intentionally small so callers can react
 * to usage information without depending on provider-specific payload shapes.
 */
public record TextStreamUsage(int inputTokens, int outputTokens, int totalTokens) {
}
