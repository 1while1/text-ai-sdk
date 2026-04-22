package com.textai.sdk.text;

import com.textai.sdk.openai.TextApiMode;
import okhttp3.OkHttpClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Holds client-wide configuration for {@link TextAiClient}.
 *
 * <p>This object carries stable transport and default request settings so
 * individual calls can stay small and focused on pure text input.
 */
public final class TextAiClientConfig {
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final String defaultSystemPrompt;
    private final TextApiMode apiMode;
    private final long connectTimeoutSeconds;
    private final long readTimeoutSeconds;
    private final Executor callbackExecutor;
    private final OkHttpClient okHttpClient;
    private final String endpointPathOverride;
    private final Map<String, String> queryParams;
    private final Map<String, String> extraHeaders;
    private final int maxRetries;

    private TextAiClientConfig(Builder builder) {
        this.baseUrl = requireNotBlank(builder.baseUrl, "baseUrl");
        this.apiKey = requireNotBlank(builder.apiKey, "apiKey");
        this.defaultModel = builder.defaultModel;
        this.defaultSystemPrompt = builder.defaultSystemPrompt;
        this.apiMode = builder.apiMode == null ? TextApiMode.RESPONSES : builder.apiMode;
        this.connectTimeoutSeconds = requirePositive(builder.connectTimeoutSeconds, "connectTimeoutSeconds");
        this.readTimeoutSeconds = requirePositive(builder.readTimeoutSeconds, "readTimeoutSeconds");
        this.callbackExecutor = builder.callbackExecutor;
        this.okHttpClient = builder.okHttpClient;
        this.endpointPathOverride = normalizeOptionalPath(builder.endpointPathOverride);
        this.queryParams = Map.copyOf(builder.queryParams);
        this.extraHeaders = Map.copyOf(builder.extraHeaders);
        this.maxRetries = requireNonNegative(builder.maxRetries, "maxRetries");
    }

    /**
     * Starts building a client configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the API base URL without the protocol-specific path suffix.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Returns the API key used for Bearer authentication.
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Returns the client-level default model, or {@code null} when every request must provide one.
     */
    public String getDefaultModel() {
        return defaultModel;
    }

    /**
     * Returns the client-level default system prompt, or {@code null} when none is configured.
     */
    public String getDefaultSystemPrompt() {
        return defaultSystemPrompt;
    }

    /**
     * Returns which OpenAI-style protocol family this client targets.
     */
    public TextApiMode getApiMode() {
        return apiMode;
    }

    /**
     * Returns the connection timeout in seconds.
     */
    public long getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    /**
     * Returns the read timeout in seconds.
     */
    public long getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    /**
     * Returns the optional executor used to dispatch stream callbacks.
     */
    public Executor getCallbackExecutor() {
        return callbackExecutor;
    }

    /**
     * Returns a caller-supplied OkHttp client when one is configured.
     */
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    /**
     * Returns an optional path override used instead of the default protocol path.
     */
    public String getEndpointPathOverride() {
        return endpointPathOverride;
    }

    /**
     * Returns additional query parameters appended to every request.
     */
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    /**
     * Returns additional headers attached to every request.
     */
    public Map<String, String> getExtraHeaders() {
        return extraHeaders;
    }

    /**
     * Returns how many additional non-stream retry attempts are allowed for
     * transient failures such as network disconnects, HTTP 429, and HTTP 5xx.
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Builder for immutable {@link TextAiClientConfig} objects.
     */
    public static final class Builder {
        private String baseUrl;
        private String apiKey;
        private String defaultModel;
        private String defaultSystemPrompt;
        private TextApiMode apiMode = TextApiMode.RESPONSES;
        private long connectTimeoutSeconds = 30;
        private long readTimeoutSeconds = 120;
        private Executor callbackExecutor;
        private OkHttpClient okHttpClient;
        private String endpointPathOverride;
        private final Map<String, String> queryParams = new LinkedHashMap<>();
        private final Map<String, String> extraHeaders = new LinkedHashMap<>();
        private int maxRetries = 0;

        private Builder() {
        }

        /**
         * Sets the base URL for the upstream service.
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the API key used for Bearer authentication.
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the default model used when a request does not override it.
         */
        public Builder defaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
            return this;
        }

        /**
         * Sets the default system prompt used by string-based convenience methods.
         */
        public Builder defaultSystemPrompt(String defaultSystemPrompt) {
            this.defaultSystemPrompt = defaultSystemPrompt;
            return this;
        }

        /**
         * Selects the OpenAI-style protocol family used by this client.
         */
        public Builder apiMode(TextApiMode apiMode) {
            this.apiMode = apiMode;
            return this;
        }

        /**
         * Sets the connection timeout in seconds.
         */
        public Builder connectTimeoutSeconds(long connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
            return this;
        }

        /**
         * Sets the read timeout in seconds.
         */
        public Builder readTimeoutSeconds(long readTimeoutSeconds) {
            this.readTimeoutSeconds = readTimeoutSeconds;
            return this;
        }

        /**
         * Sets the executor used for streaming callback dispatch.
         */
        public Builder callbackExecutor(Executor callbackExecutor) {
            this.callbackExecutor = callbackExecutor;
            return this;
        }

        /**
         * Injects a caller-supplied OkHttp client.
         */
        public Builder okHttpClient(OkHttpClient okHttpClient) {
            this.okHttpClient = okHttpClient;
            return this;
        }

        /**
         * Overrides the default protocol path suffix for every request.
         */
        public Builder endpointPathOverride(String endpointPathOverride) {
            this.endpointPathOverride = endpointPathOverride;
            return this;
        }

        /**
         * Adds one extra query parameter to every request.
         */
        public Builder queryParam(String key, String value) {
            this.queryParams.put(requireNotBlank(key, "queryParam.key"), requireNotBlank(value, "queryParam.value"));
            return this;
        }

        /**
         * Adds multiple query parameters to every request.
         */
        public Builder queryParams(Map<String, String> queryParams) {
            if (queryParams == null) {
                return this;
            }
            queryParams.forEach(this::queryParam);
            return this;
        }

        /**
         * Adds one extra header to every request.
         */
        public Builder extraHeader(String key, String value) {
            this.extraHeaders.put(requireNotBlank(key, "extraHeader.key"), requireNotBlank(value, "extraHeader.value"));
            return this;
        }

        /**
         * Adds multiple extra headers to every request.
         */
        public Builder extraHeaders(Map<String, String> extraHeaders) {
            if (extraHeaders == null) {
                return this;
            }
            extraHeaders.forEach(this::extraHeader);
            return this;
        }

        /**
         * Sets how many additional retry attempts are allowed for transient
         * non-stream failures. The default is {@code 0}, which disables retries.
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Creates the immutable config instance.
         */
        public TextAiClientConfig build() {
            return new TextAiClientConfig(this);
        }
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("TextAiClientConfig." + fieldName + " cannot be blank");
        }
        return value;
    }

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException("TextAiClientConfig." + fieldName + " must be greater than 0");
        }
        return value;
    }

    private static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException("TextAiClientConfig." + fieldName + " must not be negative");
        }
        return value;
    }

    private static String normalizeOptionalPath(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.startsWith("/") ? value : "/" + value;
    }
}
