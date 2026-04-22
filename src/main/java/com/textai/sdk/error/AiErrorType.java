package com.textai.sdk.error;

/**
 * Enumerates the failure categories surfaced by the text SDK.
 *
 * <p>The SDK is intentionally strict about text-only contracts, so protocol
 * mismatches and unsupported non-text outputs are represented explicitly
 * instead of being silently ignored.
 */
public enum AiErrorType {
    VALIDATION_ERROR,
    NETWORK_ERROR,
    HTTP_ERROR,
    PARSE_ERROR,
    EMPTY_RESPONSE,
    EMPTY_CHOICES,
    MISSING_MESSAGE,
    UNSUPPORTED_NON_TEXT_RESPONSE,
    STREAM_PROTOCOL_ERROR,
    CANCELLED,
    UNKNOWN_ERROR
}
