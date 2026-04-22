# Text AI SDK 详细使用指南

## 1. 文档定位

这份文档用于系统说明 `text-ai-sdk` 的公开能力、配置方式、调用方式和常见用法。

如果你是第一次接触这个 SDK，建议先看：

- [零基础教学文档](text-ai-sdk-beginner-tutorial.md)

如果你希望快速了解 SDK 的整体定位，也可以先看：

- [中文通用文档](text-ai-sdk-general-guide-zh.md)

---

## 2. SDK 简介

`text-ai-sdk` 是一个面向 Java 的纯文本调用 SDK，用来通过 OpenAI 风格 HTTP 接口完成文本请求和文本回答。

当前支持：

- 非流式文本请求
- 流式文本请求
- 同步调用
- 异步调用
- `/v1/chat/completions`
- `/v1/responses`

当前不支持：

- tool calling
- 多模态输入输出
- 结构化输出协议

这些能力被有意排除在当前模块之外，以保持文本门面的简单性和稳定性。

---

## 3. 设计原则

这个 SDK 的核心原则是：

> `TextAiClient` 是纯文本门面，不是一个全功能 OpenAI SDK。

这意味着：

- 公共请求模型以文本场景为中心
- 非文本响应会明确失败
- 流式协议异常会明确失败
- 内部实现按职责分层，便于未来拆分更多能力包

---

## 4. 包结构

### 4.1 `com.textai.sdk.text`

面向业务调用方的纯文本 API：

- `TextAiClient`
- `TextAiClientConfig`
- `TextRequest`
- `TextResponse`
- `TextStreamResponse`
- `DeltaCallback`
- `TextStreamListener`
- `TextStreamUsage`

### 4.2 `com.textai.sdk.error`

统一错误模型：

- `AiError`
- `AiErrorType`

### 4.3 `com.textai.sdk.core`

与具体文本协议无关的公共基础能力：

- URL 解析
- HTTP 请求构建
- SSE 处理
- JSON 辅助逻辑

### 4.4 `com.textai.sdk.openai`

OpenAI 风格协议相关能力：

- `TextApiMode`
- 请求映射
- 非流式解析
- 流式解析

---

## 5. 启动方式

### 5.1 工厂方法启动

这是最简单的接入方式。

走 `/v1/responses`：

```java
TextAiClient client = TextAiClient.forResponses(
        "http://127.0.0.1:48760",
        System.getenv("AI_API_KEY"),
        "gpt-5.4"
);
```

走 `/v1/chat/completions`：

```java
TextAiClient client = TextAiClient.forChatCompletions(
        "http://127.0.0.1:48760",
        System.getenv("AI_API_KEY"),
        "gpt-5.4"
);
```

### 5.2 使用完整配置对象

当你需要更多控制时，使用 `TextAiClientConfig`：

```java
TextAiClientConfig config = TextAiClientConfig.builder()
        .baseUrl("http://127.0.0.1:48760")
        .apiKey(System.getenv("AI_API_KEY"))
        .defaultModel("gpt-5.4")
        .defaultSystemPrompt("你是一个专业助手。")
        .apiMode(TextApiMode.RESPONSES)
        .connectTimeoutSeconds(30)
        .readTimeoutSeconds(120)
        .maxRetries(2)
        .build();

TextAiClient client = new TextAiClient(config);
```

---

## 6. 网关与 OpenAI-compatible 配置

如果你的服务不是标准公开端点，而是自定义网关、代理层或 Azure 风格兼容层，可以在配置对象上调整传输层参数，而不用污染文本请求模型。

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

适用场景：

- provider 自定义路径
- 网关附加 header
- 兼容层需要固定 query 参数

---

## 7. 非流式调用

### 7.1 最短写法

```java
TextResponse response = client.chat("你好，请介绍一下你自己。");
```

### 7.2 显式传入 system prompt

```java
TextResponse response = client.chat(
        "你是一个专业、简洁的助手。",
        "请介绍一下你自己。"
);
```

### 7.3 使用 `TextRequest`

```java
TextRequest request = TextRequest.builder()
        .systemPrompt("你是一个专业、简洁的助手。")
        .userInput("请介绍一下你自己。")
        .temperature(0)
        .maxTokens(256)
        .build();

TextResponse response = client.chat(request);
```

---

## 8. 流式调用

### 8.1 使用 `DeltaCallback`

适合直接打印增量文本：

```java
TextStreamResponse response = client.stream(
        "请流式介绍一下你自己。",
        delta -> {
            System.out.print(delta);
            System.out.flush();
        }
);

System.out.println();
System.out.println("完整回答: " + response.getFullText());
```

### 8.2 流式请求也可以传 `systemPrompt`

```java
TextStreamResponse response = client.stream(
        "你是一个专业、简洁的助手。",
        "请流式介绍一下你自己。",
        delta -> {
            System.out.print(delta);
            System.out.flush();
        }
);
```

### 8.3 使用 `TextStreamListener`

当你不仅需要增量文本，还需要开始、完成、usage、错误等生命周期事件时，使用 listener。

```java
TextStreamResponse response = client.streamWithListener(
        "请流式介绍一下你自己。",
        new TextStreamListener() {
            @Override
            public void onStart() {
                System.out.println("开始流式输出");
            }

            @Override
            public void onDelta(String delta) {
                System.out.print(delta);
            }

            @Override
            public void onUsage(TextStreamUsage usage) {
                System.out.println();
                System.out.println("token 总数: " + usage.totalTokens());
            }

            @Override
            public void onComplete(TextStreamResponse response) {
                System.out.println();
                System.out.println("流式完成");
            }
        }
);
```

---

## 9. 异步调用

### 9.1 非流式异步

```java
client.chatAsync("你好，请介绍一下你自己。")
        .thenAccept(response -> {
            if (response.isSuccess()) {
                System.out.println(response.getText());
            } else {
                System.out.println(response.getError().getMessage());
            }
        });
```

### 9.2 流式异步

```java
client.streamAsync(
        "请流式介绍一下你自己。",
        delta -> {
            System.out.print(delta);
            System.out.flush();
        }
).thenAccept(response -> {
    System.out.println();
    System.out.println("完整回答: " + response.getFullText());
});
```

### 9.3 异步 listener

```java
client.streamAsyncWithListener(
        "请流式介绍一下你自己。",
        new TextStreamListener() {
            @Override
            public void onDelta(String delta) {
                System.out.print(delta);
            }
        }
);
```

---

## 10. 线程说明

- `chatAsync()` 和 `streamAsync()` 基于 OkHttp 的异步调用
- 如果配置了 `callbackExecutor`，流式回调会切换到该 executor
- 如果没有配置，流式回调默认运行在 OkHttp 工作线程
- GUI 场景下，如果你要更新界面控件，应自行切回 UI 线程

---

## 11. 错误模型

SDK 使用类型化错误，而不是仅返回字符串消息。

常见错误类型包括：

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

你可以通过：

```java
response.getError()
```

读取错误对象，再通过：

```java
response.getError().getType()
response.getError().getMessage()
```

获取错误类型和错误信息。

---

## 12. 响应元数据

非流式和流式结果对象都支持读取轻量元数据：

- `getResponseId()`
- `getModel()`
- `getRawHeaders()`

适用场景：

- 排查网关行为
- 对接 provider request ID
- 记录真实响应模型

示例：

```java
TextResponse response = client.chat("你好");
if (response.isSuccess()) {
    System.out.println("responseId=" + response.getResponseId());
    System.out.println("model=" + response.getModel());
    System.out.println("headers=" + response.getRawHeaders());
}
```

---

## 13. 重试策略

SDK 支持可选的非流式重试策略：

- 通过 `maxRetries(int)` 配置
- 仅作用于非流式请求
- 对瞬时网络错误、`429`、`5xx` 生效
- 不对流式请求生效

示例：

```java
TextAiClientConfig config = TextAiClientConfig.builder()
        .baseUrl("http://127.0.0.1:48760")
        .apiKey(System.getenv("AI_API_KEY"))
        .defaultModel("gpt-5.4")
        .apiMode(TextApiMode.RESPONSES)
        .maxRetries(2)
        .build();
```

---

## 14. 示例与扩展阅读

- [零基础教学文档](text-ai-sdk-beginner-tutorial.md)
- [中文通用文档](text-ai-sdk-general-guide-zh.md)
- [兼容性示例说明](text-ai-sdk-compatibility-examples.md)
