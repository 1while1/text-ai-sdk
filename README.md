# Text AI SDK

`text-ai-sdk` is a standalone Java SDK for pure text Q&A over OpenAI-style HTTP APIs.

Today it focuses on one thing: strict, predictable text requests over:

- `/v1/chat/completions`
- `/v1/responses`

The SDK is intentionally narrow:

- it only accepts text input/output semantics
- non-text responses fail explicitly
- malformed stream payloads fail explicitly
- async APIs support real cancellation through `CompletableFuture.cancel()`

## Release readiness

The module is now organized for local packaging and handoff:

- the Maven build is kept self-contained inside `standalone-text-ai-sdk`
- package-time artifacts are intended to include the main jar, source jar, and javadoc jar
- maintainer-owned metadata such as license, SCM URL, and final publish coordinates should still be finalized before any public Maven release

## Positioning

This package is not trying to replace:

- the official OpenAI Java SDK as a low-level provider client
- Spring AI as a framework-oriented integration layer
- LangChain4j as an orchestration toolkit

Its role is narrower:

> a lightweight Java facade for simple, strict, controllable OpenAI-compatible text calls

That means the package is optimized for:

- pure text requests
- predictable stream handling
- clear error boundaries
- reuse across OpenAI-compatible gateways and providers

## Package Layout

The module is still a single Maven package, but the source tree is now split by responsibility so it can later evolve into multiple publishable packages without rewriting the public text API.

- `com.textai.sdk.text`
  Pure-text public API
- `com.textai.sdk.error`
  Typed SDK errors
- `com.textai.sdk.core`
  Shared transport/stream/json helpers
- `com.textai.sdk.openai`
  OpenAI-style protocol mapping and parsing

## Maven

```xml
<dependency>
    <groupId>com.textai</groupId>
    <artifactId>text-ai-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Main Types

- `com.textai.sdk.text.TextAiClient`
- `com.textai.sdk.text.TextAiClientConfig`
- `com.textai.sdk.text.TextRequest`
- `com.textai.sdk.text.TextResponse`
- `com.textai.sdk.text.TextStreamResponse`
- `com.textai.sdk.openai.TextApiMode`
- `com.textai.sdk.text.DeltaCallback`
- `com.textai.sdk.text.TextStreamListener`
- `com.textai.sdk.text.TextStreamUsage`
- `com.textai.sdk.error.AiError`
- `com.textai.sdk.error.AiErrorType`

## Quick Start

### Option 1: Minimal factory-based startup

```java
TextAiClient client = TextAiClient.forResponses(
        "http://127.0.0.1:48760",
        System.getenv("AI_API_KEY"),
        "gpt-5.4"
);

TextResponse response = client.chat("Hello, please introduce yourself.");
if (response.isSuccess()) {
    System.out.println(response.getText());
}
```

### Option 2: Full configuration

```java
TextAiClientConfig config = TextAiClientConfig.builder()
        .baseUrl("http://127.0.0.1:48760")
        .apiKey(System.getenv("AI_API_KEY"))
        .defaultModel("gpt-5.4")
        .defaultSystemPrompt("You are a helpful assistant.")
        .apiMode(TextApiMode.RESPONSES)
        .build();

TextAiClient client = new TextAiClient(config);

TextRequest request = TextRequest.builder()
        .userInput("Summarize what you can help with.")
        .maxTokens(256)
        .build();

TextResponse response = client.chat(request);
```

## Convenience Overloads

For common pure-text workflows you do not need to build a `TextRequest` manually.

Supported overloads include:

- `chat(String userInput)`
- `chat(String systemPrompt, String userInput)`
- `chatAsync(String userInput)`
- `chatAsync(String systemPrompt, String userInput)`
- `stream(String userInput, DeltaCallback callback)`
- `stream(String systemPrompt, String userInput, DeltaCallback callback)`
- `streamAsync(String userInput, DeltaCallback callback)`
- `streamAsync(String systemPrompt, String userInput, DeltaCallback callback)`
- `streamWithListener(String userInput, TextStreamListener listener)`
- `streamWithListener(String systemPrompt, String userInput, TextStreamListener listener)`
- `streamAsyncWithListener(String userInput, TextStreamListener listener)`
- `streamAsyncWithListener(String systemPrompt, String userInput, TextStreamListener listener)`

If a default system prompt is configured on `TextAiClientConfig`, the single-string overloads reuse it automatically.

## Streaming

```java
TextStreamResponse response = client.stream(
        "Give me a short greeting.",
        delta -> {
            System.out.print(delta);
            System.out.flush();
        }
);
```

For richer stream instrumentation, you can also use `TextStreamListener`:

```java
TextStreamResponse response = client.streamWithListener(
        "Give me a short greeting.",
        new TextStreamListener() {
            @Override
            public void onStart() {
                System.out.println("stream started");
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
                System.out.println("\\nstream done");
            }
        }
);
```

`DeltaCallback` remains the smallest API for plain text printing. `TextStreamListener` is the richer lifecycle-oriented option, and it uses distinct method names to avoid `null` overload ambiguity.

## Async

```java
client.chatAsync("Hello")
        .thenAccept(response -> {
            if (response.isSuccess()) {
                System.out.println(response.getText());
            }
        });
```

## Threading

- `chatAsync()` and `streamAsync()` use OkHttp async calls
- if `callbackExecutor` is configured, stream callbacks run there
- otherwise stream callbacks run on OkHttp worker threads
- GUI callers must marshal UI updates back to the correct UI thread
- the same threading rule applies to both `DeltaCallback` and `TextStreamListener`

## Error Model

The SDK uses a typed error model instead of string-only failures.

Common error types:

- `VALIDATION_ERROR`
- `NETWORK_ERROR`
- `HTTP_ERROR`
- `PARSE_ERROR`
- `EMPTY_RESPONSE`
- `EMPTY_CHOICES`
- `MISSING_MESSAGE`
- `UNSUPPORTED_NON_TEXT_RESPONSE`
- `STREAM_PROTOCOL_ERROR`
- `CANCELLED`

## Response Metadata

Both `TextResponse` and `TextStreamResponse` now expose lightweight provider metadata:

- `getResponseId()`
- `getModel()`
- `getRawHeaders()`

This is useful for:

- debugging provider behavior
- correlating logs with upstream request IDs
- auditing which model actually served a request behind a gateway

Example:

```java
TextResponse response = client.chat("Hello");
if (response.isSuccess()) {
    System.out.println("responseId=" + response.getResponseId());
    System.out.println("model=" + response.getModel());
    System.out.println("headers=" + response.getRawHeaders());
}
```

## Retry policy

The SDK now supports an opt-in retry policy for transient non-stream failures.

Configure it with:

- `maxRetries(int)`

Example:

```java
TextAiClientConfig config = TextAiClientConfig.builder()
        .baseUrl("http://127.0.0.1:48760")
        .apiKey(System.getenv("AI_API_KEY"))
        .defaultModel("gpt-5.4")
        .apiMode(TextApiMode.RESPONSES)
        .maxRetries(2)
        .build();
```

Current retry boundaries:

- retries apply to `chat(...)` and `chatAsync(...)`
- retries do not apply to stream methods
- retries are attempted for transient network failures
- retries are attempted for HTTP `429` and HTTP `5xx`
- retries are not attempted for HTTP `4xx` client errors such as `400`
- the default is `0`, which keeps retries disabled

## Cancellation

Async methods return `CompletableFuture`.

Calling `cancel(true)` on the returned future also cancels the underlying OkHttp `Call`.

## Where It Fits

### Good fit

- pure text chat applications
- desktop tools and GUI clients
- lightweight backend services
- OpenAI-compatible gateways and proxies
- the generation layer of a RAG pipeline

### Partial fit

- agent systems

The package can serve as the text generation layer inside an agent, but it is not an agent SDK. Tool calling, planning loops, and structured action handling should live in separate packages later.

### Not the current goal

- tool calling
- multimodal input/output
- structured output contracts
- workflow orchestration
- RAG retrieval infrastructure

## OpenAI-Compatible Transport Customization

Some OpenAI-compatible providers need small transport-level tweaks without changing the pure-text API.

`TextAiClientConfig` now supports:

- `endpointPathOverride(...)`
- `queryParam(...)`
- `extraHeader(...)`

Example:

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

These knobs are intended for OpenAI-compatible routing differences. They do not turn the package into a generic provider SDK.

## Detailed Guide

For a package-by-package walkthrough, architecture notes, and usage examples, see:

- `docs/text-ai-sdk-中文通用文档.md`
- `docs/text-ai-sdk-beginner-tutorial.md`
- `docs/text-ai-sdk-guide.md`
- `docs/text-ai-sdk-backlog.md`
- `docs/text-ai-sdk-compatibility-examples.md`
- `docs/text-ai-sdk-release-checklist.md`
- `CHANGELOG.md`

## Demo

See:

- `src/main/java/com/textai/sdk/demo/TextAiClientDemo.java`
- `src/main/java/com/textai/sdk/demo/examples/*.java`

The demo includes:

- synchronous text request
- synchronous delta streaming
- asynchronous text request
- asynchronous lifecycle-listener streaming

The compatibility example suite adds focused examples for:

- responses providers
- chat/completions providers
- gateway customization
- responses stream listeners
- retry plus metadata usage

Set `AI_API_KEY` before running the demo.
