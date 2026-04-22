# Text AI SDK

`text-ai-sdk` 是一个面向 Java 的轻量级纯文本 SDK，用来通过 OpenAI 风格 HTTP 接口发起文本请求、接收文本回答。

它当前专注一件事：

> 用简单、稳定、可控的方式完成纯文本问答调用。

当前支持的协议入口：

- `/v1/responses`
- `/v1/chat/completions`

当前支持的调用形态：

- 非流式
- 流式
- 同步
- 异步

---

## 1. 这个 SDK 适合什么场景

适合：

- 普通聊天问答
- 文本生成、润色、翻译、总结
- Java 后端中的纯文本模型调用
- 桌面程序或 GUI 工具中的文本能力接入
- 自建网关、代理层、OpenAI-compatible 服务的文本对接
- RAG 系统中的生成层

不适合：

- tool calling
- 多模态输入输出
- 结构化输出协议
- agent 编排
- 记忆、工作流、RAG 全链路框架

如果你未来要做这些能力，建议以当前 SDK 作为“文本能力底座”，再拆出新的能力包。

---

## 2. 它和其他 SDK 的关系

这个 SDK 不是要替代：

- OpenAI 官方 Java SDK
- Spring AI
- LangChain4j

它的定位更窄、更轻：

> 一个面向 Java 的纯文本门面，专门解决 OpenAI-compatible 文本调用体验问题。

你可以把它理解成：

- 官方 SDK：底层客户端
- Spring AI / LangChain4j：框架和编排层
- `text-ai-sdk`：纯文本调用门面

---

## 3. 30 秒快速开始

### 3.1 Maven 依赖

```xml
<dependency>
    <groupId>com.textai</groupId>
    <artifactId>text-ai-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 3.2 最小非流式示例

```java
import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextResponse;

public class QuickStart {
    public static void main(String[] args) {
        TextAiClient client = TextAiClient.forResponses(
                "http://127.0.0.1:48760",
                System.getenv("AI_API_KEY"),
                "gpt-5.4"
        );

        TextResponse response = client.chat(
                "你是一个专业、简洁的助手。",
                "请介绍一下你自己。"
        );

        if (response.isSuccess()) {
            System.out.println("回答: " + response.getText());
        } else {
            System.out.println("失败类型: " + response.getError().getType());
            System.out.println("失败信息: " + response.getError().getMessage());
        }
    }
}
```

### 3.3 最小流式示例

```java
import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextStreamResponse;

public class StreamQuickStart {
    public static void main(String[] args) {
        TextAiClient client = TextAiClient.forResponses(
                "http://127.0.0.1:48760",
                System.getenv("AI_API_KEY"),
                "gpt-5.4"
        );

        TextStreamResponse response = client.stream(
                "你是一个专业、简洁的助手。",
                "请流式介绍一下你自己。",
                delta -> {
                    System.out.print(delta);
                    System.out.flush();
                }
        );

        System.out.println();
        System.out.println("完整回答: " + response.getFullText());
    }
}
```

---

## 4. 怎么看当前请求属于哪一种方式

这个 SDK 有两个维度：

### 4.1 协议维度

由创建客户端的方法决定：

- `TextAiClient.forResponses(...)`
  - 底层走 `/v1/responses`
- `TextAiClient.forChatCompletions(...)`
  - 底层走 `/v1/chat/completions`

### 4.2 流式维度

由调用的方法名决定：

- `chat(...)` / `chatAsync(...)`
  - 非流式
- `stream(...)` / `streamAsync(...)`
  - 流式
- `streamWithListener(...)` / `streamAsyncWithListener(...)`
  - 也是流式

一句话记忆：

- 看 `forResponses / forChatCompletions`：判断协议
- 看 `chat / stream`：判断是否流式
- 看 `Async`：判断是否异步

---

## 5. 常用公开 API

核心类型：

- `com.textai.sdk.text.TextAiClient`
- `com.textai.sdk.text.TextAiClientConfig`
- `com.textai.sdk.text.TextRequest`
- `com.textai.sdk.text.TextResponse`
- `com.textai.sdk.text.TextStreamResponse`
- `com.textai.sdk.text.DeltaCallback`
- `com.textai.sdk.text.TextStreamListener`
- `com.textai.sdk.text.TextStreamUsage`
- `com.textai.sdk.openai.TextApiMode`
- `com.textai.sdk.error.AiError`
- `com.textai.sdk.error.AiErrorType`

常用方法：

- `chat(String userInput)`
- `chat(String systemPrompt, String userInput)`
- `chat(TextRequest request)`
- `chatAsync(...)`
- `stream(String userInput, DeltaCallback callback)`
- `stream(String systemPrompt, String userInput, DeltaCallback callback)`
- `streamWithListener(...)`
- `streamAsync(...)`
- `streamAsyncWithListener(...)`

---

## 6. 适合公开接入的能力边界

当前版本已经支持：

- 严格的纯文本输入输出约束
- OpenAI-compatible 协议接入
- 网关路径覆盖
- 额外 query 参数
- 额外请求头
- 流式生命周期监听
- 响应元数据提取
- 非流式瞬时错误重试
- 示例套件

当前明确不做：

- tool calling
- multimodal
- structured output
- agent workflow

---

## 7. 文档导航

- [中文通用文档](docs/text-ai-sdk-general-guide-zh.md)
- [零基础教学文档](docs/text-ai-sdk-beginner-tutorial.md)
- [详细使用指南](docs/text-ai-sdk-guide.md)
- [兼容性示例说明](docs/text-ai-sdk-compatibility-examples.md)
- [下一阶段 backlog](docs/text-ai-sdk-backlog.md)
- [发布检查清单](docs/text-ai-sdk-release-checklist.md)
- [变更记录](CHANGELOG.md)

---

## 8. 发布状态

当前仓库已经整理为可本地打包、可继续演进的独立 SDK 项目：

- `mvn test` 可用于验证
- `mvn package` 可生成主 jar、sources jar、javadoc jar
- 公开发布前仍建议补齐维护者元数据，例如 license、SCM、最终版本号

---

## 9. 代码结构

当前单模块源码按职责拆分为：

- `com.textai.sdk.text`
  - 面向调用方的纯文本 API
- `com.textai.sdk.error`
  - 错误类型和错误对象
- `com.textai.sdk.core`
  - HTTP、SSE、JSON 等公共基础能力
- `com.textai.sdk.openai`
  - OpenAI 风格协议映射与解析

这套结构方便后续继续演进出更多能力包，同时不破坏当前 `TextAiClient` 的纯文本定位。

