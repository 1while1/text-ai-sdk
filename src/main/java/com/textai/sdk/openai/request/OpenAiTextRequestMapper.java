package com.textai.sdk.openai.request;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.textai.sdk.openai.TextApiMode;
import com.textai.sdk.text.TextRequest;

/**
 * Maps a text-only SDK request into the selected OpenAI-style payload shape.
 */
public final class OpenAiTextRequestMapper {
    private OpenAiTextRequestMapper() {
    }

    /**
     * Builds the JSON payload for the selected protocol family.
     */
    public static JsonObject map(TextApiMode apiMode, String model, TextRequest request, boolean stream) {
        return apiMode == TextApiMode.CHAT_COMPLETIONS
                ? buildChatCompletionsPayload(model, request, stream)
                : buildResponsesPayload(model, request, stream);
    }

    private static JsonObject buildChatCompletionsPayload(String model, TextRequest request, boolean stream) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);
        payload.addProperty("stream", stream);
        payload.addProperty("temperature", request.getTemperature());
        payload.addProperty("max_tokens", request.getMaxTokens());

        JsonArray messages = new JsonArray();
        if (notBlank(request.getSystemPrompt())) {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", request.getSystemPrompt());
            messages.add(systemMessage);
        }

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", request.getUserInput());
        messages.add(userMessage);

        payload.add("messages", messages);
        return payload;
    }

    private static JsonObject buildResponsesPayload(String model, TextRequest request, boolean stream) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);
        payload.addProperty("stream", stream);
        payload.addProperty("temperature", request.getTemperature());
        payload.addProperty("max_output_tokens", request.getMaxTokens());

        if (notBlank(request.getSystemPrompt())) {
            payload.addProperty("instructions", request.getSystemPrompt());
        }
        payload.addProperty("input", request.getUserInput());
        return payload;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
