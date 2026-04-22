package com.textai.sdk.openai.stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.textai.sdk.core.http.ResponseHeaderUtils;
import com.textai.sdk.core.stream.StreamCallbackDispatcher;
import com.textai.sdk.core.stream.StreamPayloadBatch;
import com.textai.sdk.core.util.JsonUtils;
import com.textai.sdk.error.AiError;
import com.textai.sdk.error.AiErrorType;
import com.textai.sdk.openai.response.ResponsesTextParser;
import com.textai.sdk.text.DeltaCallback;
import com.textai.sdk.text.TextStreamListener;
import com.textai.sdk.text.TextStreamResponse;
import com.textai.sdk.text.TextStreamUsage;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

/**
 * Parses text deltas from `/v1/responses` SSE streams.
 */
public final class ResponsesStreamParser {
    private ResponsesStreamParser() {
    }

    /**
     * Parses one responses-style SSE stream into a text-only stream result.
     */
    public static TextStreamResponse parse(Response response, Executor callbackExecutor, DeltaCallback callback) throws IOException {
        return parse(response, callbackExecutor, callback, null);
    }

    /**
     * Parses one responses-style SSE stream into a text-only stream result with lifecycle callbacks.
     */
    public static TextStreamResponse parse(
            Response response,
            Executor callbackExecutor,
            DeltaCallback callback,
            TextStreamListener listener
    ) throws IOException {
        int statusCode = response.code();
        var rawHeaders = ResponseHeaderUtils.toRawHeaderMap(response.headers());
        if (!response.isSuccessful() || response.body() == null) {
            String rawBody = response.body() != null ? response.body().string() : "";
            AiError error = AiError.of(AiErrorType.HTTP_ERROR, "HTTP " + statusCode, rawBody);
            StreamCallbackDispatcher.emitError(callbackExecutor, listener, error, "", null);
            return TextStreamResponse.failure(statusCode, error, "", null, null, null, rawHeaders);
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8)
        );

        StringBuilder text = new StringBuilder();
        String finishReason = null;
        int inputTokens = 0;
        int outputTokens = 0;
        int totalTokens = 0;
        String lastEventType = null;
        String currentEventType = "";
        String responseId = null;
        String model = null;

        StreamCallbackDispatcher.emitStart(callbackExecutor, listener);

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("event: ")) {
                currentEventType = line.substring(7).trim();
                continue;
            }
            if (!line.startsWith("data: ")) {
                continue;
            }

            String data = line.substring(6).trim();
            if ("[DONE]".equals(data)) {
                break;
            }

            StreamPayloadBatch batch = StreamPayloadBatch.parse(data);
            if (!batch.valid()) {
                AiError error = AiError.of(AiErrorType.STREAM_PROTOCOL_ERROR, batch.errorMessage(), data);
                StreamCallbackDispatcher.emitError(callbackExecutor, listener, error, text.toString(), lastEventType);
                return TextStreamResponse.failure(statusCode, error, text.toString(), lastEventType, responseId, model, rawHeaders);
            }

            for (JsonObject eventObject : batch.objects()) {
                String eventType = currentEventType != null && !currentEventType.isBlank()
                        ? currentEventType
                        : JsonUtils.getString(eventObject, "type");
                lastEventType = eventType;

                if ("response.output_text.delta".equals(eventType)) {
                    JsonElement delta = eventObject.get("delta");
                    if (delta != null && !delta.isJsonNull()) {
                        if (!delta.isJsonPrimitive() || !delta.getAsJsonPrimitive().isString()) {
                            AiError error = AiError.of(AiErrorType.UNSUPPORTED_NON_TEXT_RESPONSE, "response delta is not text", eventObject.toString());
                            StreamCallbackDispatcher.emitError(callbackExecutor, listener, error, text.toString(), lastEventType);
                            return TextStreamResponse.failure(statusCode, error, text.toString(), lastEventType);
                        }

                        String piece = delta.getAsString();
                        if (!piece.isEmpty()) {
                            StreamCallbackDispatcher.emit(callbackExecutor, callback, piece);
                            StreamCallbackDispatcher.emitDelta(callbackExecutor, listener, piece);
                            text.append(piece);
                        }
                    }
                } else if ("response.completed".equals(eventType)) {
                    JsonObject responseObject = JsonUtils.getObject(eventObject, "response");
                    if (responseObject != null) {
                        String completedId = JsonUtils.getString(responseObject, "id");
                        if (completedId != null && !completedId.isBlank()) {
                            responseId = completedId;
                        }

                        String completedModel = JsonUtils.getString(responseObject, "model");
                        if (completedModel != null && !completedModel.isBlank()) {
                            model = completedModel;
                        }

                        JsonObject usage = JsonUtils.getObject(responseObject, "usage");
                        if (usage != null) {
                            inputTokens = JsonUtils.getInt(usage, "input_tokens");
                            outputTokens = JsonUtils.getInt(usage, "output_tokens");
                            totalTokens = JsonUtils.getInt(usage, "total_tokens");
                            StreamCallbackDispatcher.emitUsage(
                                    callbackExecutor,
                                    listener,
                                    new TextStreamUsage(inputTokens, outputTokens, totalTokens)
                            );
                        }

                        if (text.length() == 0) {
                            String fallbackText = ResponsesTextParser.extractText(responseObject);
                            if (fallbackText == null) {
                                AiError error = AiError.of(AiErrorType.UNSUPPORTED_NON_TEXT_RESPONSE, "completed response does not contain supported text output", eventObject.toString());
                                StreamCallbackDispatcher.emitError(callbackExecutor, listener, error, "", lastEventType);
                                return TextStreamResponse.failure(statusCode, error, "", lastEventType, responseId, model, rawHeaders);
                            }
                            if (!fallbackText.isBlank()) {
                                StreamCallbackDispatcher.emit(callbackExecutor, callback, fallbackText);
                                StreamCallbackDispatcher.emitDelta(callbackExecutor, listener, fallbackText);
                                text.append(fallbackText);
                            }
                        }
                    }
                    finishReason = "completed";
                } else if ("error".equals(eventType)) {
                    AiError error = AiError.of(AiErrorType.STREAM_PROTOCOL_ERROR, "server sent error event", eventObject.toString());
                    StreamCallbackDispatcher.emitError(callbackExecutor, listener, error, text.toString(), lastEventType);
                    return TextStreamResponse.failure(statusCode, error, text.toString(), lastEventType, responseId, model, rawHeaders);
                }
            }

            currentEventType = "";
        }

        if (text.length() == 0) {
            AiError error = AiError.of(AiErrorType.EMPTY_RESPONSE, "stream completed without text output");
            StreamCallbackDispatcher.emitError(callbackExecutor, listener, error, "", lastEventType);
            return TextStreamResponse.failure(statusCode, error, "", lastEventType, responseId, model, rawHeaders);
        }

        TextStreamResponse result = TextStreamResponse.success(text.toString(), finishReason, inputTokens, outputTokens, totalTokens,
                statusCode, lastEventType, responseId, model, rawHeaders);
        StreamCallbackDispatcher.emitComplete(callbackExecutor, listener, result);
        return result;
    }
}
