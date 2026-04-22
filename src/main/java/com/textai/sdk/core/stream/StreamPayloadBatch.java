package com.textai.sdk.core.stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Normalizes one SSE payload into a list of JSON objects.
 *
 * <p>Some gateways emit a single object per `data:` line, while others batch
 * multiple objects into one JSON array. This helper makes both shapes look the
 * same to higher-level stream parsers.
 */
public record StreamPayloadBatch(boolean valid, List<JsonObject> objects, String errorMessage) {

    /**
     * Parses one raw SSE payload string into a normalized batch.
     */
    public static StreamPayloadBatch parse(String data) {
        try {
            JsonElement payload = JsonParser.parseString(data);
            if (payload.isJsonObject()) {
                return new StreamPayloadBatch(true, Collections.singletonList(payload.getAsJsonObject()), null);
            }
            if (payload.isJsonArray()) {
                List<JsonObject> objects = new ArrayList<>();
                for (JsonElement element : payload.getAsJsonArray()) {
                    if (!element.isJsonObject()) {
                        return new StreamPayloadBatch(false, Collections.emptyList(), "stream array contains non-object element");
                    }
                    objects.add(element.getAsJsonObject());
                }
                return new StreamPayloadBatch(true, objects, null);
            }
            return new StreamPayloadBatch(false, Collections.emptyList(), "stream payload is not an object or array");
        } catch (Exception e) {
            return new StreamPayloadBatch(false, Collections.emptyList(), "stream payload is not valid JSON");
        }
    }
}
