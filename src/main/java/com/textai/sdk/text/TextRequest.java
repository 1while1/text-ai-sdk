package com.textai.sdk.text;

/**
 * Models one text-only request sent through {@link TextAiClient}.
 *
 * <p>The request intentionally stays narrow: one optional system prompt,
 * one user input, a temperature, and a single token ceiling that the SDK
 * maps to the protocol-specific field name internally.
 */
public final class TextRequest {
    private final String model;
    private final String systemPrompt;
    private final String userInput;
    private final double temperature;
    private final int maxTokens;

    private TextRequest(Builder builder) {
        if (builder.userInput == null || builder.userInput.trim().isEmpty()) {
            throw new IllegalArgumentException("TextRequest.userInput cannot be blank");
        }
        if (builder.maxTokens <= 0) {
            throw new IllegalArgumentException("TextRequest.maxTokens must be greater than 0");
        }

        this.model = builder.model;
        this.systemPrompt = builder.systemPrompt;
        this.userInput = builder.userInput;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
    }

    /**
     * Starts building a text request.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the request-level model override, or {@code null} when the client default should be used.
     */
    public String getModel() {
        return model;
    }

    /**
     * Returns the request-level system prompt, or {@code null} when no explicit prompt is supplied.
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * Returns the required user input text.
     */
    public String getUserInput() {
        return userInput;
    }

    /**
     * Returns the sampling temperature.
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Returns the unified token ceiling for this request.
     */
    public int getMaxTokens() {
        return maxTokens;
    }

    /**
     * Builder for immutable {@link TextRequest} objects.
     */
    public static final class Builder {
        private String model;
        private String systemPrompt;
        private String userInput;
        private double temperature = 0;
        private int maxTokens = 2048;

        private Builder() {
        }

        /**
         * Sets a request-level model override.
         */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the system prompt for this request.
         */
        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        /**
         * Sets the required user input text.
         */
        public Builder userInput(String userInput) {
            this.userInput = userInput;
            return this;
        }

        /**
         * Sets the sampling temperature.
         */
        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        /**
         * Sets the unified token ceiling for this request.
         */
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * Creates the immutable request instance.
         */
        public TextRequest build() {
            return new TextRequest(this);
        }
    }
}
