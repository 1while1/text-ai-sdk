package com.textai.sdk.text;

import com.textai.sdk.error.AiError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the final result of a streaming text request.
 */
public final class TextStreamResponse {
    private final boolean success;
    private final String fullText;
    private final String finishReason;
    private final int inputTokens;
    private final int outputTokens;
    private final int totalTokens;
    private final int statusCode;
    private final AiError error;
    private final String lastEventType;
    private final String responseId;
    private final String model;
    private final Map<String, List<String>> rawHeaders;

    private TextStreamResponse(boolean success, String fullText, String finishReason,
                               int inputTokens, int outputTokens, int totalTokens,
                               int statusCode, AiError error, String lastEventType,
                               String responseId, String model, Map<String, List<String>> rawHeaders) {
        this.success = success;
        this.fullText = fullText;
        this.finishReason = finishReason;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.statusCode = statusCode;
        this.error = error;
        this.lastEventType = lastEventType;
        this.responseId = responseId;
        this.model = model;
        this.rawHeaders = immutableHeaders(rawHeaders);
    }

    /**
     * Creates a successful streaming response summary.
     */
    public static TextStreamResponse success(String fullText, String finishReason,
                                             int inputTokens, int outputTokens, int totalTokens,
                                             int statusCode, String lastEventType) {
        return success(fullText, finishReason, inputTokens, outputTokens, totalTokens, statusCode,
                lastEventType, null, null, null);
    }

    /**
     * Creates a successful streaming response summary with enriched metadata.
     */
    public static TextStreamResponse success(String fullText, String finishReason,
                                             int inputTokens, int outputTokens, int totalTokens,
                                             int statusCode, String lastEventType,
                                             String responseId, String model, Map<String, List<String>> rawHeaders) {
        return new TextStreamResponse(true, fullText, finishReason, inputTokens, outputTokens, totalTokens,
                statusCode, null, lastEventType, responseId, model, rawHeaders);
    }

    /**
     * Creates a failed streaming response summary.
     */
    public static TextStreamResponse failure(int statusCode, AiError error, String partialText, String lastEventType) {
        return failure(statusCode, error, partialText, lastEventType, null, null, null);
    }

    /**
     * Creates a failed streaming response summary with metadata captured before the failure.
     */
    public static TextStreamResponse failure(int statusCode, AiError error, String partialText, String lastEventType,
                                             String responseId, String model, Map<String, List<String>> rawHeaders) {
        return new TextStreamResponse(false, partialText, null, 0, 0, 0, statusCode, error, lastEventType,
                responseId, model, rawHeaders);
    }

    /**
     * Returns whether the stream completed successfully.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the fully accumulated text, or the partial text collected before a failure.
     */
    public String getFullText() {
        return fullText;
    }

    /**
     * Returns the final finish reason when one is available.
     */
    public String getFinishReason() {
        return finishReason;
    }

    /**
     * Returns the input token count when provided by the upstream service.
     */
    public int getInputTokens() {
        return inputTokens;
    }

    /**
     * Returns the output token count when provided by the upstream service.
     */
    public int getOutputTokens() {
        return outputTokens;
    }

    /**
     * Returns the total token count when provided by the upstream service.
     */
    public int getTotalTokens() {
        return totalTokens;
    }

    /**
     * Returns the final HTTP status code for the stream.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the structured error when the stream failed.
     */
    public AiError getError() {
        return error;
    }

    /**
     * Returns the last observed stream event type for responses-style streams.
     */
    public String getLastEventType() {
        return lastEventType;
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
