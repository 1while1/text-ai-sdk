# Text AI SDK Guide

## 1. Overview

`text-ai-sdk` is a pure-text Java SDK that talks to OpenAI-style HTTP APIs while keeping its public API intentionally narrow.

If you are completely new to this SDK, read this first:

- `docs/text-ai-sdk-beginner-tutorial.md`

It currently supports:

- non-stream text requests
- stream text requests
- synchronous calls
- asynchronous calls
- `/v1/chat/completions`
- `/v1/responses`

It does not currently support:

- tool calling
- multimodal inputs
- structured outputs

Those capabilities are intentionally excluded so the text client can stay simple and predictable.

## 2. Design Philosophy

The package is built around one core rule:

`TextAiClient` is a pure-text facade, not a generic OpenAI SDK.

That means:

- the public request model is optimized for text use cases
- the SDK rejects non-text protocol shapes explicitly
- the internals are layered so future capability packages can reuse transport and protocol logic

## 2.1 Product Positioning

The package sits between low-level provider SDKs and heavier AI application frameworks.

It is not trying to be:

- a full OpenAI Java SDK replacement
- a Spring AI style framework abstraction
- a LangChain-style orchestration toolkit

It is trying to be:

> a lightweight, strict, OpenAI-compatible text facade for Java applications

This positioning matters because it explains why the public API stays narrow and why non-text responses fail instead of being loosely tolerated.

## 3. Package Structure

### `com.textai.sdk.text`

Public text-facing API.

- `TextAiClient`
- `TextAiClientConfig`
- `TextRequest`
- `TextResponse`
- `TextStreamResponse`
- `DeltaCallback`

Use this package first when integrating the SDK.

### `com.textai.sdk.error`

Shared typed error model.

- `AiError`
- `AiErrorType`

### `com.textai.sdk.core`

Cross-cutting helpers that are not text-specific.

- `core.http.UrlResolver`
- `core.stream.StreamCallbackDispatcher`
- `core.stream.StreamPayloadBatch`
- `core.util.JsonUtils`

### `com.textai.sdk.openai`

OpenAI-style protocol support for the text client.

- `TextApiMode`
- request mapping
- non-stream parsing
- stream parsing

## 4. Public API

### 4.1 Factory startup

Use a factory when you want the smallest amount of setup:

```java
TextAiClient client = TextAiClient.forResponses(
        "http://127.0.0.1:48760",
        System.getenv("AI_API_KEY"),
        "gpt-5.4"
);
```

or:

```java
TextAiClient client = TextAiClient.forChatCompletions(
        "http://127.0.0.1:48760",
        System.getenv("AI_API_KEY"),
        "gpt-5.4"
);
```

### 4.2 Full configuration

Use `TextAiClientConfig` when you need more control:

```java
TextAiClientConfig config = TextAiClientConfig.builder()
        .baseUrl("http://127.0.0.1:48760")
        .apiKey(System.getenv("AI_API_KEY"))
        .defaultModel("gpt-5.4")
        .defaultSystemPrompt("You are a helpful assistant.")
        .apiMode(TextApiMode.RESPONSES)
        .connectTimeoutSeconds(30)
        .readTimeoutSeconds(120)
        .build();

TextAiClient client = new TextAiClient(config);
```

### 4.2.1 Transport customization for OpenAI-compatible providers

When an OpenAI-compatible provider needs a custom path, query parameter, or extra headers, keep using `TextAiClientConfig` instead of changing the text request model.

```java
TextAiClientConfig config = TextAiClientConfig.builder()
        .baseUrl("http://127.0.0.1:48760")
        .apiKey(System.getenv("AI_API_KEY"))
        .defaultModel("gpt-5.4")
        .apiMode(TextApiMode.RESPONSES)
        .endpointPathOverride("/openai/custom/responses")
        .queryParam("api-version", "2025-04-01")
        .extraHeader("X-Provider", "gateway-a")
        .build();
```

These settings are useful for:

- provider-specific routing
- gateway-specific headers
- compatibility parameters that should apply to every request

They are intentionally transport-scoped, not request-scoped.

### 4.3 Request object usage

Use `TextRequest` when you need request-level control:

```java
TextRequest request = TextRequest.builder()
        .systemPrompt("You are a helpful assistant.")
        .userInput("Write a short greeting.")
        .temperature(0)
        .maxTokens(256)
        .build();

TextResponse response = client.chat(request);
```

### 4.4 Convenience overload usage

Use overloads when you want the shortest call site:

```java
TextResponse response = client.chat("Hello");
TextResponse response2 = client.chat("You are concise.", "Explain what you do.");
```

If `defaultSystemPrompt` is configured, the one-argument methods will automatically reuse it.

## 5. Sync and Async APIs

### Synchronous

```java
TextResponse response = client.chat("Hello");
TextStreamResponse streamResponse = client.stream(
        "Give me a short greeting.",
        delta -> System.out.print(delta)
);
```

### Asynchronous

```java
client.chatAsync("Hello")
        .thenAccept(response -> {
            if (response.isSuccess()) {
                System.out.println(response.getText());
            }
        });
```

```java
client.streamAsync("Stream a short greeting.", delta -> {
    System.out.print(delta);
    System.out.flush();
});
```

## 6. Streaming Model

Streaming callbacks are text-only.

```java
TextStreamResponse response = client.stream(
        "Write a short message.",
        delta -> {
            System.out.print(delta);
            System.out.flush();
        }
);
```

Behavior:

- deltas are emitted as plain strings
- the final `TextStreamResponse` contains the full accumulated text
- responses-style streams also surface the last observed event type

### 6.1 Lifecycle listener

When plain delta delivery is not enough, use `TextStreamListener`.

```java
TextStreamResponse response = client.streamWithListener(
        "Write a short message.",
        new TextStreamListener() {
            @Override
            public void onStart() {
                System.out.println("started");
            }

            @Override
            public void onDelta(String delta) {
                System.out.print(delta);
            }

            @Override
            public void onUsage(TextStreamUsage usage) {
                System.out.println("\\nusage total=" + usage.totalTokens());
            }

            @Override
            public void onComplete(TextStreamResponse response) {
                System.out.println("\\ncomplete");
            }

            @Override
            public void onError(com.textai.sdk.error.AiError error, String partialText, String lastEventType) {
                System.out.println("\\nfailed: " + error.getType());
            }
        }
);
```

The listener currently exposes:

- `onStart()`
- `onDelta(String delta)`
- `onUsage(TextStreamUsage usage)`
- `onComplete(TextStreamResponse response)`
- `onError(AiError error, String partialText, String lastEventType)`

Use `DeltaCallback` when you only care about text chunks. Use `TextStreamListener` when you need lifecycle visibility. The listener API intentionally uses `streamWithListener(...)` and `streamAsyncWithListener(...)` so literal `null` arguments cannot collide with `DeltaCallback` overloads.

## 7. Error Handling

All failures use `AiError`.

Example:

```java
TextResponse response = client.chat("Hello");
if (!response.isSuccess()) {
    System.out.println(response.getError().getType());
    System.out.println(response.getError().getMessage());
}
```

Important categories:

- `VALIDATION_ERROR`
  Missing required client or request data
- `NETWORK_ERROR`
  Transport failure
- `HTTP_ERROR`
  Upstream returned a non-2xx response
- `PARSE_ERROR`
  Upstream returned a body that did not match the expected JSON shape
- `UNSUPPORTED_NON_TEXT_RESPONSE`
  Upstream returned a non-text result shape
- `STREAM_PROTOCOL_ERROR`
  Stream payload was malformed or unsupported
- `CANCELLED`
  Caller cancelled the async request

## 7.1 Response metadata

Successful text results also expose lightweight provider metadata:

- `getResponseId()`
- `getModel()`
- `getRawHeaders()`

These fields are intentionally small and read-only. They help with:

- correlating SDK results with upstream request IDs
- confirming which model a gateway actually used
- capturing provider headers for debugging and audit trails

Example:

```java
TextResponse response = client.chat("Hello");
if (response.isSuccess()) {
    System.out.println(response.getResponseId());
    System.out.println(response.getModel());
    System.out.println(response.getRawHeaders());
}
```

`TextStreamResponse` exposes the same metadata for stream calls.

## 7.2 Retry policy

The SDK now provides a narrow, opt-in retry policy for transient non-stream failures.

Configure it on `TextAiClientConfig`:

```java
TextAiClientConfig config = TextAiClientConfig.builder()
        .baseUrl("http://127.0.0.1:48760")
        .apiKey(System.getenv("AI_API_KEY"))
        .defaultModel("gpt-5.4")
        .apiMode(TextApiMode.RESPONSES)
        .maxRetries(2)
        .build();
```

Behavior in the current version:

- retries apply to `chat(...)` and `chatAsync(...)`
- retries do not apply to `stream(...)` or `streamAsync(...)`
- retries are attempted for transient network failures
- retries are attempted for HTTP `429` and HTTP `5xx`
- retries are not attempted for HTTP `4xx` client errors such as `400`
- `maxRetries(0)` is the default and disables retries

This keeps the first resilience layer predictable without replaying streams automatically.

## 8. Threading and GUI Use

Async APIs use OkHttp async execution.

For stream callbacks:

- if `callbackExecutor` is configured, callbacks run on that executor
- otherwise callbacks run on OkHttp worker threads
- the same rule applies to `DeltaCallback` and `TextStreamListener`

This matters for GUI frameworks:

- Swing, JavaFX, Android, and similar UI stacks require you to marshal updates onto the UI thread

## 9. Cancellation

Async methods return `CompletableFuture`.

Calling:

```java
future.cancel(true);
```

will also cancel the underlying OkHttp `Call`.

This is important for:

- GUI screens that are closing
- request abandonment
- user-triggered stop actions

## 10. Protocol Compatibility Boundary

The SDK already speaks OpenAI-style protocol shapes internally, but only for text workflows.

That means the package is:

- compatible with OpenAI-style text request/response patterns
- not yet a general-purpose OpenAI SDK

This is intentional. Future capability packages can reuse the internal layering without bloating the pure-text facade.

## 10.1 RAG and Agent Usage

### RAG

This SDK is a good fit for the generation layer of a RAG system.

A typical RAG stack still needs:

- retrieval
- context assembly
- prompt composition
- text generation

`TextAiClient` can cleanly own the final text generation step.

### Agent

This SDK is only a partial fit for agent systems.

It can be used as:

- the text generation layer inside an agent

It should not be treated as:

- the full agent runtime

Agent-specific features such as tool invocation, action planning, structured tool arguments, and iterative control loops should live in separate packages later.

## 11. Extension Strategy

The current layering is designed so later packages can be added without replacing `TextAiClient`.

Planned extension direction:

- pure text package stays focused
- future tool/multimodal/structured packages reuse:
  - error model
  - URL resolution
  - JSON helpers
  - stream normalization
  - protocol mapping patterns

The intended long-term package family looks like:

- `text-ai-sdk`
- a future tool-calling package
- a future multimodal package
- a future structured-output package

Those packages should share the same core protocol and transport foundations instead of duplicating them.

## 12. Source Code Notes

The SDK source is heavily commented on purpose.

The comments are meant to help readers understand:

- where the public text boundary is
- which code is transport-related
- which code is protocol-specific
- why strict text-only failures are intentional

## 13. Demo Entry Point

For a runnable example that shows the main calling styles in one place, see:

- `src/main/java/com/textai/sdk/demo/TextAiClientDemo.java`
- `docs/text-ai-sdk-compatibility-examples.md`

The demo covers:

- synchronous text request
- synchronous delta streaming
- asynchronous text request
- asynchronous lifecycle-listener streaming

The compatibility example suite complements the main demo with smaller files
that each focus on one OpenAI-compatible integration scenario.
