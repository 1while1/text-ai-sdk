# Text AI SDK 中文通用文档

这是一份面向通用使用场景的中文说明文档，用来帮助你快速理解并使用 `text-ai-sdk`。

它适合：

- 第一次接触这个 SDK 的开发者
- 希望快速集成纯文本模型调用能力的 Java 开发者
- 想先了解 SDK 边界、调用方式和协议模式的使用者

这份文档不追求展开所有实现细节，而是重点说明：

- 这个 SDK 是做什么的
- 你应该如何调用它
- 它支持哪些能力
- 什么场景下该用什么方式

---

## 1. SDK 简介

`text-ai-sdk` 是一个面向 Java 的纯文本调用 SDK，用来通过 OpenAI 风格 HTTP 接口与模型进行文本交互。

当前重点支持：

- 文本问答
- 同步请求
- 异步请求
- 流式输出
- OpenAI 风格协议兼容

已支持的协议入口：

- `/v1/chat/completions`
- `/v1/responses`

它的定位是：

> 用简单、稳定、明确的方式完成纯文本请求，而不是做一个大而全的 AI 框架。

---

## 2. 适用范围

这个 SDK 适合：

- 普通聊天问答
- 文本生成
- 文本翻译、总结、润色
- Java 后端中的模型调用
- GUI 或桌面程序中的文本能力接入
- RAG 系统中的生成层

当前不负责：

- tool calling
- 多模态输入输出
- 结构化输出协议
- agent 编排
- 检索系统本身

如果你后续要做：

- RAG：可以把这个 SDK 作为生成层使用
- Agent：可以把这个 SDK 作为底层文本能力使用

---

## 3. 核心概念

### 3.1 `TextAiClient`

这是最核心的客户端对象。

它负责：

- 持有基础配置
- 发起请求
- 返回结果

几乎所有调用都从它开始。

### 3.2 `TextAiClientConfig`

这是客户端级配置对象。

它通常用于配置：

- `baseUrl`
- `apiKey`
- `defaultModel`
- `defaultSystemPrompt`
- 协议模式
- 超时
- 重试
- 额外 header / query 参数

你可以把它理解为“客户端默认设置”。

### 3.3 `TextRequest`

这是单次请求的参数对象。

它描述本次请求本身，例如：

- `systemPrompt`
- `userInput`
- `temperature`
- `maxTokens`

如果只是最简单调用，不一定非要手动创建它；但一旦你需要更多控制，就建议使用它。

### 3.4 `TextResponse`

这是非流式请求的结果对象。

你通常会从中读取：

- 最终文本回答
- token 数量
- 模型名
- 错误信息

### 3.5 `TextStreamResponse`

这是流式请求的最终汇总结果对象。

它与流式 callback 不冲突：

- callback 负责边生成边输出
- `TextStreamResponse` 负责在流结束后给出完整汇总结果

### 3.6 `DeltaCallback`

这是最简单的流式文本回调。

作用是：

> 每收到一段文本，就立即交给你处理一次。

---

## 4. 两种协议模式

这个 SDK 当前支持两种协议：

### 4.1 `/v1/responses`

通过：

```java
TextAiClient.forResponses(...)
```

创建客户端。

适合：

- 直接对接 `responses` 风格服务
- 与当前 SDK 的文本门面配合使用

### 4.2 `/v1/chat/completions`

通过：

```java
TextAiClient.forChatCompletions(...)
```

创建客户端。

适合：

- 需要兼容老一些的 OpenAI 风格调用方式
- provider 更偏向 `chat/completions` 协议

---

## 5. 调用方式总览

### 5.1 非流式

```java
TextResponse response = client.chat("你好");
```

### 5.2 流式

```java
TextStreamResponse response = client.stream(
        "你好",
        delta -> System.out.print(delta)
);
```

### 5.3 异步非流式

```java
client.chatAsync("你好");
```

### 5.4 异步流式

```java
client.streamAsync("你好", delta -> System.out.print(delta));
```

---

## 6. system prompt 怎么传

非流式：

```java
TextResponse response = client.chat(
        "你是一个专业助手。",
        "请介绍一下你自己。"
);
```

流式：

```java
TextStreamResponse response = client.stream(
        "你是一个专业助手。",
        "请流式介绍一下你自己。",
        delta -> System.out.print(delta)
);
```

如果配置了 `defaultSystemPrompt`，单参数重载也会自动复用默认 system prompt。

---

## 7. 返回结果里可以拿到什么

非流式 `TextResponse` 常用方法：

- `isSuccess()`
- `getText()`
- `getFinishReason()`
- `getInputTokens()`
- `getOutputTokens()`
- `getTotalTokens()`
- `getResponseId()`
- `getModel()`
- `getRawHeaders()`
- `getError()`

流式 `TextStreamResponse` 常用方法：

- `isSuccess()`
- `getFullText()`
- `getFinishReason()`
- `getInputTokens()`
- `getOutputTokens()`
- `getTotalTokens()`
- `getResponseId()`
- `getModel()`
- `getRawHeaders()`
- `getLastEventType()`
- `getError()`

---

## 8. 错误与重试

SDK 使用类型化错误模型。

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

对于非流式请求，支持通过 `maxRetries(int)` 启用瞬时错误重试。

---

## 9. OpenAI-compatible 传输层配置

当 provider 或网关需要自定义 path、query 参数或额外请求头时，可以使用：

- `endpointPathOverride(...)`
- `queryParam(...)`
- `extraHeader(...)`

这些配置属于传输层，不属于文本请求模型本身。

---

## 10. 适用与不适用场景

适合：

- 纯文本问答
- 文本生成
- 文本工具型后端
- 作为 RAG 的生成层

不适合：

- tool calling
- multimodal
- structured output
- 完整 agent SDK

---

## 11. 继续阅读

- [零基础教学文档](text-ai-sdk-beginner-tutorial.md)
- [详细使用指南](text-ai-sdk-guide.md)
- [兼容性示例说明](text-ai-sdk-compatibility-examples.md)
- [发布检查清单](text-ai-sdk-release-checklist.md)
- [后续任务清单](text-ai-sdk-backlog.md)

