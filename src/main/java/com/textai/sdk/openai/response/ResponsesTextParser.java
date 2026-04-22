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
 * Extracts strict text results from `/v1/responses` payloads.
 */
public final class ResponsesTextParser {
    private ResponsesTextParser() {
    }

    /**
     * Parses one non-stream responses payload into a text-only result.
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
            String text = extractText(root);
            if (text == null) {
                return TextResponse.failure(statusCode, AiError.of(AiErrorType.UNSUPPORTED_NON_TEXT_RESPONSE, "response does not contain supported text output", rawBody), rawBody,
                        JsonUtils.getString(root, "id"), JsonUtils.getString(root, "model"), rawHeaders);
            }
            if (text.isBlank()) {
                return TextResponse.failure(statusCode, AiError.of(AiErrorType.EMPTY_RESPONSE, "response text is empty", rawBody), rawBody,
                        JsonUtils.getString(root, "id"), JsonUtils.getString(root, "model"), rawHeaders);
            }

            return TextResponse.success(
                    text,
                    JsonUtils.getString(root, "status"),
                    JsonUtils.getInt(usage, "input_tokens"),
                    JsonUtils.getInt(usage, "output_tokens"),
                    JsonUtils.getInt(usage, "total_tokens"),
                    statusCode,
                    rawBody,
                    JsonUtils.getString(root, "id"),
                    JsonUtils.getString(root, "model"),
                    rawHeaders
            );
        } catch (Exception e) {
            return TextResponse.failure(statusCode, AiError.of(AiErrorType.PARSE_ERROR, "failed to parse responses response", rawBody, e), rawBody,
                    null, null, rawHeaders);
        }
    }

    /**
     * Extracts text from a responses-style JSON object.
     *
     * <p>This method supports both `output_text` and the structured
     * `output[].content[]` form used by newer responses payloads.
     */
    public static String extractText(JsonObject root) {
        JsonElement outputText = root == null ? null : root.get("output_text");
        if (outputText != null && !outputText.isJsonNull()) {
            if (outputText.isJsonPrimitive() && outputText.getAsJsonPrimitive().isString()) {
                return outputText.getAsString();
            }
            return null;
        }

        JsonArray output = JsonUtils.getArray(root, "output");
        if (output == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        boolean sawStructuredContent = false;

        for (JsonElement outputElement : output) {
            if (!outputElement.isJsonObject()) {
                continue;
            }

            JsonObject outputObject = outputElement.getAsJsonObject();
            JsonArray contentArray = JsonUtils.getArray(outputObject, "content");
            if (contentArray == null) {
                continue;
            }

            for (JsonElement contentElement : contentArray) {
                if (!contentElement.isJsonObject()) {
                    continue;
                }

                JsonObject contentObject = contentElement.getAsJsonObject();
                sawStructuredContent = true;
                String type = JsonUtils.getString(contentObject, "type");
                JsonElement text = contentObject.get("text");
                if ("output_text".equals(type)
                        && text != null
                        && !text.isJsonNull()
                        && text.isJsonPrimitive()
                        && text.getAsJsonPrimitive().isString()) {
                    builder.append(text.getAsString());
                }
            }
        }

        if (builder.length() > 0) {
            return builder.toString();
        }
        return sawStructuredContent ? null : "";
    }
}
