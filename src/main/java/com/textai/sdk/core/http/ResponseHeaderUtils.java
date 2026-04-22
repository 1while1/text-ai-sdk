package com.textai.sdk.core.http;

import okhttp3.Headers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts OkHttp headers into immutable SDK-friendly maps.
 */
public final class ResponseHeaderUtils {
    private ResponseHeaderUtils() {
    }

    /**
     * Returns an immutable copy of all response headers while preserving the
     * order in which header names first appeared.
     */
    public static Map<String, List<String>> toRawHeaderMap(Headers headers) {
        if (headers == null || headers.size() == 0) {
            return Map.of();
        }

        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String name = headers.name(index);
            String value = headers.value(index);
            copy.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
        }

        Map<String, List<String>> immutableCopy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : copy.entrySet()) {
            immutableCopy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(immutableCopy);
    }
}
