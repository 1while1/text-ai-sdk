package com.textai.sdk.core.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Small JSON helper methods shared by protocol parsers.
 *
 * <p>Keeping these lookups in one place reduces parser noise and makes it
 * easier to keep null-handling consistent across both protocol families.
 */
public final class JsonUtils {
    private JsonUtils() {
    }

    /**
     * Returns the named object field or {@code null} when the field is missing or not an object.
     */
    public static JsonObject getObject(JsonObject root, String key) {
        JsonElement element = root == null ? null : root.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    /**
     * Returns the named array field or {@code null} when the field is missing or not an array.
     */
    public static JsonArray getArray(JsonObject root, String key) {
        JsonElement element = root == null ? null : root.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    /**
     * Returns the named string field or {@code null} when it is absent or not a string primitive.
     */
    public static String getString(JsonObject root, String key) {
        JsonElement element = root == null ? null : root.get(key);
        return element != null && !element.isJsonNull() && element.isJsonPrimitive()
                ? element.getAsString()
                : null;
    }

    /**
     * Returns the named integer field or {@code 0} when it is absent.
     */
    public static int getInt(JsonObject root, String key) {
        JsonElement element = root == null ? null : root.get(key);
        return element != null && !element.isJsonNull() ? element.getAsInt() : 0;
    }
}
