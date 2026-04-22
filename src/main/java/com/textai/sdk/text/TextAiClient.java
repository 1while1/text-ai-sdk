package com.textai.sdk.text;

import com.google.gson.JsonObject;
import com.textai.sdk.core.http.UrlResolver;
import com.textai.sdk.error.AiError;
import com.textai.sdk.error.AiErrorType;
import com.textai.sdk.openai.TextApiMode;
import com.textai.sdk.openai.request.OpenAiTextRequestMapper;
import com.textai.sdk.openai.response.ChatCompletionsTextParser;
import com.textai.sdk.openai.response.ResponsesTextParser;
import com.textai.sdk.openai.stream.ChatCompletionsStreamParser;
import com.textai.sdk.openai.stream.ResponsesStreamParser;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Public pure-text client facade for OpenAI-style chat and responses endpoints.
 *
 * <p>This class intentionally keeps the external API focused on text-only
 * requests while delegating protocol mapping and parsing to dedicated
 * OpenAI-specific helpers under the hood.
 */
public final class TextAiClient {
    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private final TextAiClientConfig config;
    private final OkHttpClient client;

    /**
     * Creates a client for the supplied configuration.
     */
    public TextAiClient(TextAiClientConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("TextAiClientConfig cannot be null");
        }
        this.config = config;
        this.client = config.getOkHttpClient() != null
                ? config.getOkHttpClient()
                : new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(config.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
    }

    /**
     * Creates a responses-mode client with the common minimum configuration.
     */
    public static TextAiClient forResponses(String baseUrl, String apiKey, String defaultModel) {
        return new TextAiClient(TextAiClientConfig.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .defaultModel(defaultModel)
                .apiMode(TextApiMode.RESPONSES)
                .build());
    }

    /**
     * Creates a chat/completions-mode client with the common minimum configuration.
     */
    public static TextAiClient forChatCompletions(String baseUrl, String apiKey, String defaultModel) {
        return new TextAiClient(TextAiClientConfig.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .defaultModel(defaultModel)
                .apiMode(TextApiMode.CHAT_COMPLETIONS)
                .build());
    }

    /**
     * Sends one non-stream text request.
     */
    public TextResponse chat(TextRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("TextRequest cannot be null");
        }

        String model = resolveModel(request);
        if (model == null) {
            return TextResponse.failure(0, AiError.of(AiErrorType.VALIDATION_ERROR, "No model configured on request or client"), null);
        }

        for (int attempt = 0; ; attempt++) {
            Request httpRequest = buildRequest(model, request, false);
            try (Response response = client.newCall(httpRequest).execute()) {
                TextResponse parsedResponse = config.getApiMode() == TextApiMode.CHAT_COMPLETIONS
                        ? ChatCompletionsTextParser.parse(response)
                        : ResponsesTextParser.parse(response);

                if (!shouldRetry(parsedResponse, attempt)) {
                    return parsedResponse;
                }
            } catch (IOException e) {
                if (!shouldRetryNetworkFailure(attempt)) {
                    return TextResponse.failure(0, AiError.of(AiErrorType.NETWORK_ERROR, "Network request failed", null, e), null);
                }
            }
        }
    }

    /**
     * Sends one non-stream request using the configured default system prompt.
     */
    public TextResponse chat(String userInput) {
        return chat(buildConvenienceRequest(config.getDefaultSystemPrompt(), userInput));
    }

    /**
     * Sends one non-stream request with an explicit system prompt override.
     */
    public TextResponse chat(String systemPrompt, String userInput) {
        return chat(buildConvenienceRequest(systemPrompt, userInput));
    }

    /**
     * Sends one async non-stream text request.
     */
    public CompletableFuture<TextResponse> chatAsync(TextRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("TextRequest cannot be null");
        }

        String model = resolveModel(request);
        if (model == null) {
            return CompletableFuture.completedFuture(
                    TextResponse.failure(0, AiError.of(AiErrorType.VALIDATION_ERROR, "No model configured on request or client"), null)
            );
        }

        AtomicReference<Call> currentCallRef = new AtomicReference<>();
        CompletableFuture<TextResponse> future = createCallAwareFuture(currentCallRef);
        enqueueChatAttempt(model, request, 0, future, currentCallRef);
        return future;
    }

    /**
     * Sends one async non-stream request using the configured default system prompt.
     */
    public CompletableFuture<TextResponse> chatAsync(String userInput) {
        return chatAsync(buildConvenienceRequest(config.getDefaultSystemPrompt(), userInput));
    }

    /**
     * Sends one async non-stream request with an explicit system prompt override.
     */
    public CompletableFuture<TextResponse> chatAsync(String systemPrompt, String userInput) {
        return chatAsync(buildConvenienceRequest(systemPrompt, userInput));
    }

    /**
     * Sends one synchronous streaming text request.
     */
    public TextStreamResponse stream(TextRequest request, DeltaCallback callback) {
        return stream(request, callback, null);
    }

    /**
     * Sends one synchronous streaming text request with lifecycle callbacks.
     *
     * <p>This method intentionally uses a distinct name so literal {@code null}
     * arguments cannot become ambiguous between {@link DeltaCallback} and
     * {@link TextStreamListener} overloads.
     */
    public TextStreamResponse streamWithListener(TextRequest request, TextStreamListener listener) {
        return stream(request, null, listener);
    }

    private TextStreamResponse stream(TextRequest request, DeltaCallback callback, TextStreamListener listener) {
        if (request == null) {
            throw new IllegalArgumentException("TextRequest cannot be null");
        }

        String model = resolveModel(request);
        if (model == null) {
            return TextStreamResponse.failure(0, AiError.of(AiErrorType.VALIDATION_ERROR, "No model configured on request or client"), "", null);
        }

        Request httpRequest = buildRequest(model, request, true);
        try (Response response = client.newCall(httpRequest).execute()) {
            return config.getApiMode() == TextApiMode.CHAT_COMPLETIONS
                    ? ChatCompletionsStreamParser.parse(response, config.getCallbackExecutor(), callback, listener)
                    : ResponsesStreamParser.parse(response, config.getCallbackExecutor(), callback, listener);
        } catch (IOException e) {
            AiError error = AiError.of(AiErrorType.NETWORK_ERROR, "Network stream request failed", null, e);
            if (listener != null) {
                listener.onError(error, "", null);
            }
            return TextStreamResponse.failure(0, error, "", null);
        }
    }

    /**
     * Sends one streaming request using the configured default system prompt.
     */
    public TextStreamResponse stream(String userInput, DeltaCallback callback) {
        return stream(buildConvenienceRequest(config.getDefaultSystemPrompt(), userInput), callback);
    }

    /**
     * Sends one streaming request using the configured default system prompt and lifecycle callbacks.
     */
    public TextStreamResponse streamWithListener(String userInput, TextStreamListener listener) {
        return streamWithListener(buildConvenienceRequest(config.getDefaultSystemPrompt(), userInput), listener);
    }

    /**
     * Sends one streaming request with an explicit system prompt override.
     */
    public TextStreamResponse stream(String systemPrompt, String userInput, DeltaCallback callback) {
        return stream(buildConvenienceRequest(systemPrompt, userInput), callback);
    }

    /**
     * Sends one streaming request with an explicit system prompt override and lifecycle callbacks.
     */
    public TextStreamResponse streamWithListener(String systemPrompt, String userInput, TextStreamListener listener) {
        return streamWithListener(buildConvenienceRequest(systemPrompt, userInput), listener);
    }

    /**
     * Sends one async streaming text request.
     */
    public CompletableFuture<TextStreamResponse> streamAsync(TextRequest request, DeltaCallback callback) {
        return streamAsync(request, callback, null);
    }

    /**
     * Sends one async streaming text request with lifecycle callbacks.
     *
     * <p>This method intentionally uses a distinct name so literal {@code null}
     * arguments cannot become ambiguous between {@link DeltaCallback} and
     * {@link TextStreamListener} overloads.
     */
    public CompletableFuture<TextStreamResponse> streamAsyncWithListener(TextRequest request, TextStreamListener listener) {
        return streamAsync(request, null, listener);
    }

    private CompletableFuture<TextStreamResponse> streamAsync(TextRequest request, DeltaCallback callback, TextStreamListener listener) {
        if (request == null) {
            throw new IllegalArgumentException("TextRequest cannot be null");
        }

        String model = resolveModel(request);
        if (model == null) {
            return CompletableFuture.completedFuture(
                    TextStreamResponse.failure(0, AiError.of(AiErrorType.VALIDATION_ERROR, "No model configured on request or client"), "", null)
            );
        }

        Request httpRequest = buildRequest(model, request, true);
        Call call = client.newCall(httpRequest);
        CompletableFuture<TextStreamResponse> future = createCallAwareFuture(call);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (future.isCancelled()) {
                    return;
                }
                if (call.isCanceled()) {
                    AiError error = AiError.of(AiErrorType.CANCELLED, "Request was cancelled");
                    if (listener != null) {
                        listener.onError(error, "", null);
                    }
                    future.complete(TextStreamResponse.failure(0, error, "", null));
                    return;
                }
                AiError error = AiError.of(AiErrorType.NETWORK_ERROR, "Network stream request failed", null, e);
                if (listener != null) {
                    listener.onError(error, "", null);
                }
                future.complete(TextStreamResponse.failure(0, error, "", null));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (future.isCancelled()) {
                    response.close();
                    return;
                }
                try (Response ignored = response) {
                    future.complete(config.getApiMode() == TextApiMode.CHAT_COMPLETIONS
                            ? ChatCompletionsStreamParser.parse(response, config.getCallbackExecutor(), callback, listener)
                            : ResponsesStreamParser.parse(response, config.getCallbackExecutor(), callback, listener));
                } catch (Exception e) {
                    AiError error = AiError.of(AiErrorType.UNKNOWN_ERROR, "Unexpected async stream failure", null, e);
                    if (listener != null) {
                        listener.onError(error, "", null);
                    }
                    future.complete(TextStreamResponse.failure(0, error, "", null));
                }
            }
        });
        return future;
    }

    /**
     * Sends one async streaming request using the configured default system prompt.
     */
    public CompletableFuture<TextStreamResponse> streamAsync(String userInput, DeltaCallback callback) {
        return streamAsync(buildConvenienceRequest(config.getDefaultSystemPrompt(), userInput), callback);
    }

    /**
     * Sends one async streaming request using the configured default system prompt and lifecycle callbacks.
     */
    public CompletableFuture<TextStreamResponse> streamAsyncWithListener(String userInput, TextStreamListener listener) {
        return streamAsyncWithListener(buildConvenienceRequest(config.getDefaultSystemPrompt(), userInput), listener);
    }

    /**
     * Sends one async streaming request with an explicit system prompt override.
     */
    public CompletableFuture<TextStreamResponse> streamAsync(String systemPrompt, String userInput, DeltaCallback callback) {
        return streamAsync(buildConvenienceRequest(systemPrompt, userInput), callback);
    }

    /**
     * Sends one async streaming request with an explicit system prompt override and lifecycle callbacks.
     */
    public CompletableFuture<TextStreamResponse> streamAsyncWithListener(String systemPrompt, String userInput, TextStreamListener listener) {
        return streamAsyncWithListener(buildConvenienceRequest(systemPrompt, userInput), listener);
    }

    /**
     * Returns the immutable client configuration.
     */
    public TextAiClientConfig getConfig() {
        return config;
    }

    private Request buildRequest(String model, TextRequest request, boolean stream) {
        JsonObject payload = OpenAiTextRequestMapper.map(config.getApiMode(), model, request, stream);
        RequestBody requestBody = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);

        Request.Builder builder = new Request.Builder()
                .url(UrlResolver.resolve(
                        config.getBaseUrl(),
                        config.getApiMode(),
                        config.getEndpointPathOverride(),
                        config.getQueryParams()
                ))
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json");

        config.getExtraHeaders().forEach(builder::addHeader);

        return builder
                .post(requestBody)
                .build();
    }

    private TextRequest buildConvenienceRequest(String systemPrompt, String userInput) {
        TextRequest.Builder builder = TextRequest.builder()
                .userInput(userInput);

        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            builder.systemPrompt(systemPrompt);
        }
        return builder.build();
    }

    private String resolveModel(TextRequest request) {
        String requestModel = request.getModel();
        if (requestModel != null && !requestModel.trim().isEmpty()) {
            return requestModel;
        }

        String defaultModel = config.getDefaultModel();
        if (defaultModel != null && !defaultModel.trim().isEmpty()) {
            return defaultModel;
        }
        return null;
    }

    private void enqueueChatAttempt(
            String model,
            TextRequest request,
            int attempt,
            CompletableFuture<TextResponse> future,
            AtomicReference<Call> currentCallRef
    ) {
        if (future.isCancelled()) {
            return;
        }

        Request httpRequest = buildRequest(model, request, false);
        Call call = client.newCall(httpRequest);
        currentCallRef.set(call);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (future.isCancelled()) {
                    return;
                }
                if (call.isCanceled()) {
                    future.complete(TextResponse.failure(0, AiError.of(AiErrorType.CANCELLED, "Request was cancelled"), null));
                    return;
                }
                if (shouldRetryNetworkFailure(attempt)) {
                    enqueueChatAttempt(model, request, attempt + 1, future, currentCallRef);
                    return;
                }
                future.complete(TextResponse.failure(0, AiError.of(AiErrorType.NETWORK_ERROR, "Network request failed", null, e), null));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (future.isCancelled()) {
                    response.close();
                    return;
                }
                try (Response ignored = response) {
                    TextResponse parsedResponse = config.getApiMode() == TextApiMode.CHAT_COMPLETIONS
                            ? ChatCompletionsTextParser.parse(response)
                            : ResponsesTextParser.parse(response);

                    if (shouldRetry(parsedResponse, attempt)) {
                        enqueueChatAttempt(model, request, attempt + 1, future, currentCallRef);
                        return;
                    }

                    future.complete(parsedResponse);
                } catch (Exception e) {
                    future.complete(TextResponse.failure(0, AiError.of(AiErrorType.UNKNOWN_ERROR, "Unexpected async parsing failure", null, e), null));
                }
            }
        });
    }

    private boolean shouldRetry(TextResponse response, int attempt) {
        if (response == null || response.isSuccess() || attempt >= config.getMaxRetries()) {
            return false;
        }
        if (response.getError() == null || response.getError().getType() != AiErrorType.HTTP_ERROR) {
            return false;
        }

        int statusCode = response.getStatusCode();
        return statusCode == 429 || statusCode >= 500;
    }

    private boolean shouldRetryNetworkFailure(int attempt) {
        return attempt < config.getMaxRetries();
    }

    private static <T> CompletableFuture<T> createCallAwareFuture(Call call) {
        AtomicReference<Call> callRef = new AtomicReference<>(call);
        return createCallAwareFuture(callRef);
    }

    private static <T> CompletableFuture<T> createCallAwareFuture(AtomicReference<Call> callRef) {
        return new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                Call currentCall = callRef.get();
                if (currentCall != null) {
                    currentCall.cancel();
                }
                return super.cancel(mayInterruptIfRunning);
            }
        };
    }
}
