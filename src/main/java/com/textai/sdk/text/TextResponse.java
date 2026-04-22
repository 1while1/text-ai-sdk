package com.textai.sdk.text;

import com.textai.sdk.error.AiError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the final result of a non-stream text request.
 */
public final class TextResponse {
    private final boolean success;
    private final String text;
    private final String finishReason;
    private final int inputTokens;
    private final int outputTokens;
    private final int totalTokens;
    private final int statusCode;
    private final AiError error;
    private final String rawBody;
    private final String responseId;
    private final String model;
    private final Map<String, List<String>> rawHeaders;

    private TextResponse(boolean success, String text, String finishReason, int inputTokens,
                         int outputTokens, int totalTokens, int statusCode, AiError error, String rawBody,
                         String responseId, String model, Map<String, List<String>> rawHeaders) {
        this.success = success;
        this.text = text;
        this.finishReason = finishReason;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.statusCode = statusCode;
        this.error = error;
        this.rawBody = rawBody;
        this.responseId = responseId;
        this.model = model;
        this.rawHeaders = immutableHeaders(rawHeaders);
    }

    /**
     * Creates a successful text response.
     */
    public static TextResponse success(String text, String finishReason, int inputTokens,
                                       int outputTokens, int totalTokens, int statusCode, String rawBody) {
        return success(text, finishReason, inputTokens, outputTokens, totalTokens, statusCode, rawBody, null, null, null);
    }

    /**
     * Creates a successful text response with enriched metadata.
     */
    public static TextResponse success(String text, String finishReason, int inputTokens,
                                       int outputTokens, int totalTokens, int statusCode, String rawBody,
                                       String responseId, String model, Map<String, List<String>> rawHeaders) {
        return new TextResponse(true, text, finishReason, inputTokens, outputTokens, totalTokens, statusCode,
                null, rawBody, responseId, model, rawHeaders);
    }

    /**
     * Creates a failed text response.
     */
    public static TextResponse failure(int statusCode, AiError error, String rawBody) {
        return failure(statusCode, error, rawBody, null, null, null);
    }

    /**
     * Creates a failed text response with metadata captured before the failure.
     */
    public static TextResponse failure(int statusCode, AiError error, String rawBody,
                                       String responseId, String model, Map<String, List<String>> rawHeaders) {
        return new TextResponse(false, null, null, 0, 0, 0, statusCode, error, rawBody,
                responseId, model, rawHeaders);
    }

    /**
     * Returns whether the request completed successfully.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the extracted text when the request succeeded.
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the upstream finish reason when one is available.
     */
    public String getFinishReason() {
        return finishReason;
    }

    /**
     * Returns the input token count when the upstream service provided it.
     */
    public int getInputTokens() {
        return inputTokens;
    }

    /**
     * Returns the output token count when the upstream service provided it.
     */
    public int getOutputTokens() {
        return outputTokens;
    }

    /**
     * Returns the total token count when the upstream service provided it.
     */
    public int getTotalTokens() {
        return totalTokens;
    }

    /**
     * Returns the HTTP status code that completed the request.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the structured error when the request failed.
     */
    public AiError getError() {
        return error;
    }

    /**
     * Returns the raw HTTP body when one is available.
     */
    public String getRawBody() {
        return rawBody;
    }

    /**
     * Returns the provider response identifier when one is available.
     */
    public String getResponseId() {
        return responseId;
    }

    /**
     * Returns the provider-reported model name when one is available.
     */
    public String getModel() {
        return model;
    }

    /**
     * Returns an immutable copy of the raw HTTP response headers.
     */
    public Map<String, List<String>> getRawHeaders() {
        return rawHeaders;
    }

    private static Map<String, List<String>> immutableHeaders(Map<String, List<String>> rawHeaders) {
        if (rawHeaders == null || rawHeaders.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : rawHeaders.entrySet()) {
            List<String> values = entry.getValue() == null ? List.of() : List.copyOf(new ArrayList<>(entry.getValue()));
            copy.put(entry.getKey(), values);
        }
        return Map.copyOf(copy);
    }
}
