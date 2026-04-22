package com.textai.sdk.core.http;

import okhttp3.HttpUrl;

import java.util.Map;

import com.textai.sdk.openai.TextApiMode;

/**
 * Resolves the final request URL for the selected OpenAI-style protocol family.
 */
public final class UrlResolver {
    private UrlResolver() {
    }

    /**
     * Appends the correct protocol path to the supplied base URL.
     */
    public static String resolve(String baseUrl, TextApiMode apiMode) {
        return resolve(baseUrl, apiMode, null, Map.of());
    }

    /**
     * Builds the final request URL from the base URL, selected protocol path, optional override, and query params.
     */
    public static String resolve(
            String baseUrl,
            TextApiMode apiMode,
            String endpointPathOverride,
            Map<String, String> queryParams
    ) {
        HttpUrl.Builder builder = HttpUrl.get(baseUrl).newBuilder();
        builder.encodedPath(endpointPathOverride != null
                ? endpointPathOverride
                : defaultPath(apiMode));

        if (queryParams != null) {
            queryParams.forEach(builder::addQueryParameter);
        }
        return builder.build().toString();
    }

    private static String defaultPath(TextApiMode apiMode) {
        return apiMode == TextApiMode.CHAT_COMPLETIONS
                ? "/v1/chat/completions"
                : "/v1/responses";
    }
}
