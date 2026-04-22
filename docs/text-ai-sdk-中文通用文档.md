# Text AI SDK 中文通用文档

本文档是一份面向通用使用场景的中文说明文档，用于帮助你快速理解并使用 `text-ai-sdk`。

它适合以下读者：

- 第一次接触该 SDK 的开发者
- 需要快速集成纯文本模型调用能力的 Java 开发者
- 希望了解 SDK 支持范围、调用方式、协议模式和常见配置的使用者

本文档不强调底层实现细节，而是重点说明：

- 这个 SDK 是做什么的
- 你应该如何调用它
- 它支持哪些能力
- 什么时候该用哪种方式

---

## 1. SDK 简介

`text-ai-sdk` 是一个面向 Java 的 **纯文本调用 SDK**，用于通过 OpenAI 风格的 HTTP 接口与大模型进行文本交互。

当前版本重点支持：

- 文本问答
- 同步请求
- 异步请求
- 流式输出
- OpenAI 风格接口兼容

已支持的协议入口：

- `/v1/chat/completions`
- `/v1/responses`

这个 SDK 的定位是：

> 用简单、稳定、明确的方式完成纯文本请求，而不是做一个“大而全”的 AI 框架。

---

## 2. 适用范围

这个 SDK 适合：

- 普通聊天问答
- 文本生成
- 文本改写
- 总结、翻译、润色等文本任务
- Java 后端服务中的模型调用
- 桌面工具、GUI 工具中的文本模型接入
- RAG 系统中的“生成层”

这个 SDK 当前不负责：

- 工具调用
- 多模态输入输出
- 结构化输出协议
- Agent 编排
- 检索系统本身

如果你后续要做：

- RAG：可以把这个 SDK 作为生成层使用
- Agent：可以把这个 SDK 作为底层文本能力使用

---

## 3. 核心概念

使用这个 SDK 时，最重要的几个类如下。

### 3.1 `TextAiClient`

这是最核心的客户端对象。

它负责：

- 持有基础配置
- 发送请求
- 返回结果

几乎所有实际调用都从它开始。

---

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

你可以理解为：

> 这是“客户端的默认设置”

---

### 3.3 `TextRequest`

这是一次文本请求的参数对象。

它用于描述本次请求本身，比如：

- `systemPrompt`
- `userInput`
- `temperature`
- `maxTokens`

如果只是最简单的调用，不一定要手动创建它；但一旦你需要更多控制，就建议使用它。

---

### 3.4 `TextResponse`

这是 **非流式请求** 的结果对象。

常见用途：

- 读取最终回答
- 读取 token 数量
- 读取模型名
- 读取错误信息

---

### 3.5 `TextStreamResponse`

这是 **流式请求** 的最终汇总结果对象。

它和流式回调不是一回事。

你可以这样理解：

- 流式回调：边生成边输出
- `TextStreamResponse`：整个流结束后的汇总结果

---

### 3.6 `DeltaCallback`

这是最简单的流式文本回调。

作用是：

> 每收到一段文本，就立即处理一次

适合：

- 控制台打印
- 简单流式 UI
- 快速输出

---

### 3.7 `TextStreamListener`

这是更完整的流式生命周期监听器。

它比 `DeltaCallback` 更丰富，支持：

- `onStart()`
- `onDelta(...)`
- `onUsage(...)`
- `onComplete(...)`
- `onError(...)`

适合：

- GUI 场景
- 带状态管理的流式输出
- 需要统计 token 或监听完成事件的场景

---

## 4. 两个关键维度：协议模式 与 请求方式

学习这个 SDK 时，最容易混淆的是这两个维度。

### 4.1 协议模式

由创建 `TextAiClient` 的方式决定。

#### 模式 A：Responses

```java
TextAiClient client = TextAiClient.forResponses(
        "http://127.0.0.1:48760",
        "你的API_KEY",
        "gpt-5.4"
);
```

表示底层走：

```text
/v1/responses
```

#### 模式 B：Chat Completions

```java
TextAiClient client = TextAiClient.forChatCompletions(
        "http://127.0.0.1:48760",
        "你的API_KEY",
        "gpt-5.4"
);
```

表示底层走：

```text
/v1/chat/completions
```

---

### 4.2 请求方式

由调用的方法名决定。

#### 非流式

```java
client.chat(...)
client.chatAsync(...)
```

#### 流式

```java
client.stream(...)
client.streamAsync(...)
client.streamWithListener(...)
client.streamAsyncWithListener(...)
```

记忆方法：

- `chat` = 非流式
- `stream` = 流式
- `Async` = 异步

---

## 5. 快速开始

### 5.1 引入依赖

```xml
<dependency>
    <groupId>com.textai</groupId>
    <artifactId>text-ai-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

### 5.2 最小可运行示例

```java
import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextResponse;

public class QuickStart {
    public static void main(String[] args) {
        TextAiClient client = TextAiClient.forResponses(
                "http://127.0.0.1:48760",
                "你的API_KEY",
                "gpt-5.4"
        );

        TextResponse response = client.chat("你好，请介绍一下你自己。");

        if (response.isSuccess()) {
            System.out.println("回答: " + response.getText());
        } else {
            System.out.println("错误类型: " + response.getError().getType());
            System.out.println("错误信息: " + response.getError().getMessage());
        }
    }
}
```

---

## 6. 常见调用方式

### 6.1 最简单的非流式请求

```java
TextResponse response = client.chat("你好，请介绍一下你自己。");
```

适合：

- 快速调用
- 最少代码
- 不需要复杂请求参数

---

### 6.2 带 `systemPrompt` 的非流式请求

```java
TextResponse response = client.chat(
        "你是一个专业的 Java 助手。",
        "请解释一下什么是线程安全。"
);
```

适合：

- 明确设定角色
- 控制回答风格
- 控制语言和表达方式

---

### 6.3 使用 `TextRequest` 的非流式请求

```java
import com.textai.sdk.text.TextRequest;

TextRequest request = TextRequest.builder()
        .systemPrompt("你是一个专业的 Java 助手。")
        .userInput("请解释一下什么是线程安全。")
        .temperature(0)
        .maxTokens(512)
        .build();

TextResponse response = client.chat(request);
```

适合：

- 需要更多参数控制
- 需要统一封装请求
- 需要在业务代码里明确请求结构

---

### 6.4 最简单的流式请求

```java
import com.textai.sdk.text.TextStreamResponse;

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

注意：

- `delta` 是流式过程中的片段文本
- `response.getFullText()` 是流结束后的完整回答

---

### 6.5 带 `systemPrompt` 的流式请求

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

说明：

- 流式请求也是支持 `systemPrompt` 的
- 不是只有非流式才能传系统提示词

---

### 6.6 使用 `TextStreamListener` 的流式请求

```java
import com.textai.sdk.text.TextStreamListener;
import com.textai.sdk.text.TextStreamResponse;
import com.textai.sdk.text.TextStreamUsage;

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

适合：

- 需要生命周期管理
- 需要 token 统计
- 需要更完整的流式交互逻辑

---

### 6.7 异步非流式请求

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

适合：

- 后台执行请求
- 不想阻塞当前线程

---

### 6.8 异步流式请求

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

---

## 7. 推荐使用顺序

如果你是第一次接入这个 SDK，建议这样学习和使用：

### 第一步：最简单非流式

先用：

```java
client.chat("你好")
```

目标：

- 跑通最小调用
- 理解 `TextResponse`

---

### 第二步：加入 `systemPrompt`

再用：

```java
client.chat("你是一个专业助手", "你好")
```

目标：

- 理解系统提示词
- 理解请求基本结构

---

### 第三步：使用 `TextRequest`

目标：

- 学会管理参数
- 学会控制 `temperature` 和 `maxTokens`

---

### 第四步：尝试流式

先用：

```java
client.stream(..., delta -> ...)
```

目标：

- 理解流式输出
- 理解 `getFullText()`

---

### 第五步：最后再用 listener 和 async

因为这两部分会引入更多概念，例如：

- 生命周期
- 回调
- 异步线程

---

## 8. 常见配置方式

如果你不想每次都写完整参数，可以使用 `TextAiClientConfig`：

```java
import com.textai.sdk.openai.TextApiMode;
import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextAiClientConfig;

TextAiClientConfig config = TextAiClientConfig.builder()
        .baseUrl("http://127.0.0.1:48760")
        .apiKey("你的API_KEY")
        .defaultModel("gpt-5.4")
        .defaultSystemPrompt("你是一个专业助手。")
        .apiMode(TextApiMode.RESPONSES)
        .maxRetries(2)
        .build();

TextAiClient client = new TextAiClient(config);
```

这样后面就可以直接：

```java
TextResponse response = client.chat("你好");
```

---

## 9. 常见返回信息

### 9.1 非流式：`TextResponse`

常用方法：

```java
response.isSuccess();
response.getText();
response.getFinishReason();
response.getInputTokens();
response.getOutputTokens();
response.getTotalTokens();
response.getResponseId();
response.getModel();
response.getRawHeaders();
response.getError();
```

---

### 9.2 流式：`TextStreamResponse`

常用方法：

```java
response.isSuccess();
response.getFullText();
response.getFinishReason();
response.getInputTokens();
response.getOutputTokens();
response.getTotalTokens();
response.getResponseId();
response.getModel();
response.getRawHeaders();
response.getLastEventType();
response.getError();
```

---

## 10. OpenAI 兼容配置

如果你接入的是 OpenAI 风格兼容网关，而不是最标准的默认路径，可以通过 `TextAiClientConfig` 进行 transport 级配置：

```java
TextAiClientConfig config = TextAiClientConfig.builder()
        .baseUrl("http://127.0.0.1:48760")
        .apiKey("你的API_KEY")
        .defaultModel("gpt-5.4")
        .apiMode(TextApiMode.RESPONSES)
        .endpointPathOverride("/openai/custom/responses")
        .queryParam("api-version", "2025-04-01")
        .extraHeader("X-Provider", "gateway-a")
        .build();
```

适合：

- 自建网关
- Azure 风格兼容路径
- 代理层定制 header

---

## 11. 重试策略

当前 SDK 提供的是 **窄范围、可控的非流式重试能力**。

配置方式：

```java
TextAiClientConfig config = TextAiClientConfig.builder()
        .baseUrl("http://127.0.0.1:48760")
        .apiKey("你的API_KEY")
        .defaultModel("gpt-5.4")
        .apiMode(TextApiMode.RESPONSES)
        .maxRetries(2)
        .build();
```

当前版本的行为：

- 只作用于 `chat(...)` 和 `chatAsync(...)`
- 不作用于 `stream(...)` 或 `streamAsync(...)`
- 会重试瞬时网络异常
- 会重试 HTTP `429`
- 会重试 HTTP `5xx`
- 不会重试 HTTP `400` 这类客户端错误

---

## 12. 常见问题

### 12.1 我怎么看出当前走的是哪种协议？

看 client 的创建方式：

- `TextAiClient.forResponses(...)` -> `responses`
- `TextAiClient.forChatCompletions(...)` -> `chat/completions`

---

### 12.2 我怎么看出是不是流式？

看方法名：

- `chat...` = 非流式
- `stream...` = 流式

---

### 12.3 流式为什么也有返回值？

因为：

- 流过程中通过 callback / listener 不断输出
- 流结束后 SDK 再返回一个 `TextStreamResponse`

所以：

```java
response.getFullText()
```

就是流结束后的完整回答。

---

### 12.4 流式也能传 `systemPrompt` 吗？

能。

例如：

```java
client.stream("系统提示", "用户输入", delta -> {
    System.out.print(delta);
});
```

---

### 12.5 异步和流式是同一回事吗？

不是。

- `Async` = 是否异步
- `stream` = 是否流式

这两个维度是独立的。

---

## 13. 最容易犯的错误

### 错误 1：把 `Async` 当成流式

不是。

`chatAsync(...)` 是异步非流式。

---

### 错误 2：只看流式 callback，不看最终 `TextStreamResponse`

记住：

```java
response.getFullText()
```

是完整最终结果。

---

### 错误 3：不知道 `systemPrompt` 可以用于流式

其实流式和非流式都支持。

---

### 错误 4：不判断 `isSuccess()` 就直接读结果

建议先判断：

```java
if (response.isSuccess()) {
    ...
} else {
    ...
}
```

---

## 14. 进阶阅读建议

如果你已经能看懂并使用本篇文档，后面可以继续看这些资料：

- [README.md](</D:/Codex-Manager-main (1)/Codex-Manager-main/standalone-text-ai-sdk/README.md>)
- [text-ai-sdk-guide.md](</D:/Codex-Manager-main (1)/Codex-Manager-main/standalone-text-ai-sdk/docs/text-ai-sdk-guide.md>)
- [text-ai-sdk-compatibility-examples.md](</D:/Codex-Manager-main (1)/Codex-Manager-main/standalone-text-ai-sdk/docs/text-ai-sdk-compatibility-examples.md>)
- [text-ai-sdk-release-checklist.md](</D:/Codex-Manager-main (1)/Codex-Manager-main/standalone-text-ai-sdk/docs/text-ai-sdk-release-checklist.md>)

---

## 15. 一句话总结

如果你只记住一句话，请记住这句：

> `TextAiClient` 是一个面向 Java 的纯文本模型调用器；先看它用的是哪种协议，再看你调用的是 `chat` 还是 `stream`。
