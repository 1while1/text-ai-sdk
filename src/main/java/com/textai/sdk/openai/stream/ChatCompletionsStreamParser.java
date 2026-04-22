package com.textai.sdk.openai.stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.textai.sdk.core.http.ResponseHeaderUtils;
import com.textai.sdk.core.stream.StreamCallbackDispatcher;
import com.textai.sdk.core.stream.StreamPayloadBatch;
import com.textai.sdk.core.util.JsonUtils;
import com.textai.sdk.error.AiError;
import com.textai.sdk.error.AiErrorType;
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
 * Parses text deltas from `/v1/chat/completions` SSE streams.
 */
public final class ChatCompletionsStreamParser {
    private ChatCompletionsStreamParser() {
    }

    /**
     * Parses one chat/completions SSE stream into a text-only stream result.
     */
    public static TextStreamResponse parse(Response response, Executor callbackExecutor, DeltaCallback callback) throws IOException {
        return parse(response, callbackExecutor, callback, null);
    }

    /**
     * Parses one chat/completions SSE stream into a text-only stream result with lifecycle callbacks.
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
        String responseId = null;
        String model = null;

        StreamCallbackDispatcher.emitStart(callbackExecutor, listener);

        String line;
        while ((line = reader.readLine()) != null) {
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
                StreamCallbackDispatcher.emitError(callbackExecutor, listener, error, text.toString(), null);
                return TextStreamResponse.failure(statusCode, error, text.toString(), null);
            }

            for (JsonObject chunk : batch.objects()) {
                String chunkId = JsonUtils.getString(chunk, "id");
                if (chunkId != null && !chunkId.isBlank()) {
                    responseId = chunkId;
                }

                String chunkModel = JsonUtils.getString(chunk, "model");
                if (chunkModel != null && !chunkModel.isBlank()) {
                    model = chunkModel;
                }

                JsonObject usage = JsonUtils.getObject(chunk, "usage");
                if (usage != null) {
                    inputTokens = JsonUtils.getInt(usage, "prompt_tokens");
                    outputTokens = JsonUtils.getInt(usage, "completion_tokens");
                    totalTokens = JsonUtils.getInt(usage, "total_tokens");
                    StreamCallbackDispatcher.emitUsage(
                            callbackExecutor,
                            listener,
                            new TextStreamUsage(inputTokens, outputTokens, totalTokens)
                    );
                }

                JsonArray choices = JsonUtils.getArray(chunk, "choices");
                if (choices == null || choices.size() == 0) {
                    continue;
                }

                JsonElement firstChoiceElement = choices.get(0);
                if (!firstChoiceElement.isJsonObject()) {
                    AiError error = AiError.of(AiErrorType.STREAM_PROTOCOL_ERROR, "first choice is not an object", chunk.toString());
                    StreamCallbackDispatcher.emitError(callbackExecutor, listener, error, text.toString(), null);
                    return TextStreamResponse.failure(statusCode, error, text.toString(), null, responseId, model, rawHeaders);
                }

                JsonObject choice = firstChoiceElement.getAsJsonObject();
                JsonObject delta = JsonUtils.getObject(choice, "delta");
                if (delta != null) {
                    JsonElement content = delta.get("content");
                    if (content != null && !content.isJsonNull()) {
                        if (!content.isJsonPrimitive() || !content.getAsJsonPrimitive().isString()) {
                            AiError error = AiError.of(AiErrorType.UNSUPPORTED_NON_TEXT_RESPONSE, "stream delta content is not text", chunk.toString());
                            StreamCallbackDispatcher.emitError(callbackExecutor, listener, error, text.toString(), null);
                            return TextStreamResponse.failure(statusCode, error, text.toString(), null, responseId, model, rawHeaders);
                        }

                        String piece = content.getAsString();
                        if (!piece.isEmpty()) {
                            StreamCallbackDispatcher.emit(callbackExecutor, callback, piece);
                            StreamCallbackDispatcher.emitDelta(callbackExecutor, listener, piece);
                            text.append(piece);
                        }
                    }
                }

                String currentFinishReason = JsonUtils.getString(choice, "finish_reason");
                if (currentFinishReason != null && !currentFinishReason.isBlank()) {
                    finishReason = currentFinishReason;
                }
            }
        }

        if (text.length() == 0) {
            AiError error = AiError.of(AiErrorType.EMPTY_RESPONSE, "stream completed without text output");
            StreamCallbackDispatcher.emitError(callbackExecutor, listener, error, "", null);
            return TextStreamResponse.failure(statusCode, error, "", null, responseId, model, rawHeaders);
        }

        TextStreamResponse result = TextStreamResponse.success(text.toString(), finishReason, inputTokens, outputTokens, totalTokens,
                statusCode, null, responseId, model, rawHeaders);
        StreamCallbackDispatcher.emitComplete(callbackExecutor, listener, result);
        return result;
    }
}
