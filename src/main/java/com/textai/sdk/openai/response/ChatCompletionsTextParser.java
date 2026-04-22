package com.textai.sdk.openai.response;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.textai.sdk.core.http.ResponseHeaderUtils;
import com.textai.sdk.core.util.JsonUtils;
import com.textai.sdk.error.AiError;
import com.textai.sdk.error.AiErrorType;
import com.textai.sdk.text.TextResponse;
import okhttp3.Response;

import java.io.IOException;

/**
 * Extracts strict text results from `/v1/chat/completions` responses.
 */
public final class ChatCompletionsTextParser {
    private ChatCompletionsTextParser() {
    }

    /**
     * Parses one non-stream chat/completions response into a text-only result.
     */
    public static TextResponse parse(Response response) throws IOException {
        int statusCode = response.code();
        String rawBody = response.body() != null ? response.body().string() : "";
        var rawHeaders = ResponseHeaderUtils.toRawHeaderMap(response.headers());

        if (!response.isSuccessful()) {
            return TextResponse.failure(statusCode, AiError.of(AiErrorType.HTTP_ERROR, "HTTP " + statusCode, rawBody), rawBody,
                    null, null, rawHeaders);
        }

        try {
            JsonObject root = JsonParser.parseString(rawBody).getAsJsonObject();
            JsonObject usage = JsonUtils.getObject(root, "usage");
            JsonArray choices = JsonUtils.getArray(root, "choices");
            if (choices == null || choices.size() == 0) {
                return TextResponse.failure(statusCode, AiError.of(AiErrorType.EMPTY_CHOICES, "choices array is empty", rawBody), rawBody,
                        JsonUtils.getString(root, "id"), JsonUtils.getString(root, "model"), rawHeaders);
            }

            JsonElement firstChoiceElement = choices.get(0);
            if (!firstChoiceElement.isJsonObject()) {
                return TextResponse.failure(statusCode, AiError.of(AiErrorType.PARSE_ERROR, "first choice is not an object", rawBody), rawBody,
                        JsonUtils.getString(root, "id"), JsonUtils.getString(root, "model"), rawHeaders);
            }

            JsonObject firstChoice = firstChoiceElement.getAsJsonObject();
            JsonObject message = JsonUtils.getObject(firstChoice, "message");
            if (message == null) {
                return TextResponse.failure(statusCode, AiError.of(AiErrorType.MISSING_MESSAGE, "missing message object", rawBody), rawBody,
                        JsonUtils.getString(root, "id"), JsonUtils.getString(root, "model"), rawHeaders);
            }

            JsonElement content = message.get("content");
            if (content == null || content.isJsonNull()) {
                return TextResponse.failure(statusCode, AiError.of(AiErrorType.UNSUPPORTED_NON_TEXT_RESPONSE, "missing text content", rawBody), rawBody,
                        JsonUtils.getString(root, "id"), JsonUtils.getString(root, "model"), rawHeaders);
            }
            if (!content.isJsonPrimitive() || !content.getAsJsonPrimitive().isString()) {
                return TextResponse.failure(statusCode, AiError.of(AiErrorType.UNSUPPORTED_NON_TEXT_RESPONSE, "non-text content is not supported", rawBody), rawBody,
                        JsonUtils.getString(root, "id"), JsonUtils.getString(root, "model"), rawHeaders);
            }

            String text = content.getAsString();
            if (text.isBlank()) {
                return TextResponse.failure(statusCode, AiError.of(AiErrorType.EMPTY_RESPONSE, "text content is empty", rawBody), rawBody,
                        JsonUtils.getString(root, "id"), JsonUtils.getString(root, "model"), rawHeaders);
            }

            return TextResponse.success(
                    text,
                    JsonUtils.getString(firstChoice, "finish_reason"),
                    JsonUtils.getInt(usage, "prompt_tokens"),
                    JsonUtils.getInt(usage, "completion_tokens"),
                    JsonUtils.getInt(usage, "total_tokens"),
                    statusCode,
                    rawBody,
                    JsonUtils.getString(root, "id"),
                    JsonUtils.getString(root, "model"),
                    rawHeaders
            );
        } catch (Exception e) {
            return TextResponse.failure(statusCode, AiError.of(AiErrorType.PARSE_ERROR, "failed to parse chat/completions response", rawBody, e), rawBody,
                    null, null, rawHeaders);
        }
    }
}
