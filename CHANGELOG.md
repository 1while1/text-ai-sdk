# Changelog

All notable changes to `text-ai-sdk` are documented in this file.

## 1.0.0-SNAPSHOT

### Added

- pure-text `TextAiClient` facade for `/v1/chat/completions` and `/v1/responses`
- strict text-only parsing with typed SDK errors
- sync, async, stream, and async-stream APIs
- `TextStreamListener` lifecycle callbacks for start, delta, usage, complete, and error events
- transport customization through endpoint path overrides, query parameters, and extra headers
- response metadata exposure via `responseId`, `model`, and raw headers
- opt-in retry policy for transient non-stream failures
- compatibility example suite for provider and gateway scenarios

### Changed

- public package structure was reorganized into `text`, `error`, `core`, and `openai`
- listener-based streaming APIs now use explicit method names to avoid `null` overload ambiguity
- demo coverage now includes async lifecycle-listener streaming

### Known Limits

- this package is intentionally limited to pure text requests and text responses
- stream retries are not implemented
- tool calling, multimodal inputs, and structured output contracts are intentionally out of scope for this module
- public-release metadata such as license and SCM coordinates still needs maintainer-provided values before publishing to a public Maven repository
