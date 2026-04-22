# Text AI SDK Compatibility Examples

This page collects the focused compatibility examples that ship with the SDK.

The goal of the suite is to answer one practical question quickly:

> "Which example should I copy for my OpenAI-compatible text integration?"

All examples live under:

- `src/main/java/com/textai/sdk/demo/examples`

They are intentionally small, heavily commented, and centered on one scenario each.

## Example index

### 1. `ResponsesProviderExample`

File:

- `src/main/java/com/textai/sdk/demo/examples/ResponsesProviderExample.java`

Use this when:

- your provider exposes `/v1/responses`
- you want the smallest non-stream pure-text example
- you want to see `responseId` and `model` extraction in a basic call

### 2. `ChatCompletionsProviderExample`

File:

- `src/main/java/com/textai/sdk/demo/examples/ChatCompletionsProviderExample.java`

Use this when:

- your provider expects `/v1/chat/completions`
- you want a text-only system prompt plus user prompt example
- you want to compare chat-style and responses-style integration

### 3. `GatewayCustomizationExample`

File:

- `src/main/java/com/textai/sdk/demo/examples/GatewayCustomizationExample.java`

Use this when:

- your OpenAI-compatible gateway needs a custom path
- you must attach query parameters such as `api-version`
- you must attach provider-specific headers

This is the example to copy for:

- internal gateways
- Azure-style compatibility layers
- proxy routers with custom routing rules

### 4. `ResponsesStreamListenerExample`

File:

- `src/main/java/com/textai/sdk/demo/examples/ResponsesStreamListenerExample.java`

Use this when:

- you need responses-style streaming
- `DeltaCallback` is not enough
- you want lifecycle hooks for:
  - start
  - delta
  - usage
  - complete
  - error

This is the best example for GUI and observable stream integrations.

### 5. `RetryAndMetadataExample`

File:

- `src/main/java/com/textai/sdk/demo/examples/RetryAndMetadataExample.java`

Use this when:

- you want non-stream retries for transient failures
- you want to inspect provider metadata after a request
- you are integrating the SDK into a production-oriented backend service

## Environment variables used by the examples

The example suite reads:

- `AI_API_KEY`
- `AI_BASE_URL`
- `AI_MODEL`

Defaults:

- `AI_BASE_URL` defaults to `http://127.0.0.1:48760`
- `AI_MODEL` defaults to `gpt-5.4`

`AI_API_KEY` is required.

## Suggested reading order

If you are new to the SDK:

1. `ResponsesProviderExample`
2. `ChatCompletionsProviderExample`
3. `GatewayCustomizationExample`
4. `ResponsesStreamListenerExample`
5. `RetryAndMetadataExample`

This order moves from the smallest text call site to the more operational compatibility scenarios.

## Relationship to the main demo

The existing `TextAiClientDemo` remains the all-in-one walkthrough.

The compatibility example suite is different:

- each file focuses on one integration scenario
- each file is easier to copy into a real project
- each file maps to one OpenAI-compatible compatibility question
