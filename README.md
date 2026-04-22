# Text AI SDK

[![Java](https://img.shields.io/badge/Java-17%2B-2f7ed8)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-ready-c71a36)](https://maven.apache.org/)
[![协议](https://img.shields.io/badge/OpenAI-compatible-0a7f5a)](#)
[![范围](https://img.shields.io/badge/Text-only-444444)](#)
[![JitPack](https://jitpack.io/v/1while1/text-ai-sdk.svg)](https://jitpack.io/#1while1/text-ai-sdk)

`text-ai-sdk` 是一个面向 Java 的轻量级纯文本 SDK，用来通过 OpenAI 风格 HTTP 接口发起文本请求、接收文本回答。

它当前只专注一件事：

> 用简单、稳定、可控的方式完成纯文本问答调用。

支持的协议入口：

- `/v1/responses`
- `/v1/chat/completions`

支持的调用形态：

- 非流式
- 流式
- 同步
- 异步

---

## 1. 为什么用它

这个 SDK 不试图替代：

- OpenAI 官方 Java SDK
- Spring AI
- LangChain4j

它的定位更轻、更窄：

> 一个面向 Java 的纯文本门面，专门解决 OpenAI-compatible 文本调用体验问题。

适合你在这些场景使用：

- 普通聊天问答
- 文本生成、翻译、润色、总结
- Java 后端中的纯文本模型调用
- GUI / 桌面程序中的文本能力接入
- OpenAI-compatible 网关或代理层接入
- RAG 系统中的生成层

当前明确不做：

- tool calling
- 多模态输入输出
- 结构化输出协议
- agent 编排

---

## 2. 最短可复制示例

### 非流式

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

        TextResponse response = client.chat("请介绍一下你自己。");

        if (response.isSuccess()) {
            System.out.println(response.getText());
        } else {
            System.out.println(response.getError().getType());
            System.out.println(response.getError().getMessage());
        }
    }
}
```

### 流式

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

## 3. 一眼看懂怎么调用

这个 SDK 有两个维度：

### 3.1 先看协议

- `TextAiClient.forResponses(...)`
  - 底层走 `/v1/responses`
- `TextAiClient.forChatCompletions(...)`
  - 底层走 `/v1/chat/completions`

### 3.2 再看是否流式

- `chat(...)` / `chatAsync(...)`
  - 非流式
- `stream(...)` / `streamAsync(...)`
  - 流式
- `streamWithListener(...)` / `streamAsyncWithListener(...)`
  - 也是流式

一句话记忆：

- 看 `forResponses / forChatCompletions`：判断协议
- 看 `chat / stream`：判断是不是流式
- 看 `Async`：判断是不是异步

---

## 4. 常用公开 API

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

## 5. Maven 依赖

```xml
<dependency>
    <groupId>com.textai</groupId>
    <artifactId>text-ai-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

如果你希望通过公开 Git 仓库直接使用这个 SDK，推荐使用 JitPack。

### 5.1 通过 GitHub + JitPack 使用

先添加 JitPack 仓库：

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

然后添加依赖：

```xml
<dependency>
    <groupId>com.github.1while1</groupId>
    <artifactId>text-ai-sdk</artifactId>
    <version>v0.1.2</version>
</dependency>
```

JitPack 页面：

- [https://jitpack.io/#1while1/text-ai-sdk](https://jitpack.io/#1while1/text-ai-sdk)

适合场景：

- 你想直接从公开 GitHub 仓库使用 SDK
- 你还没有发布到 Maven Central
- 你希望先用最轻量的公开分发方式验证 SDK 可用性

---

## 6. 适用边界

当前版本已经支持：

- 严格的纯文本输入输出约束
- OpenAI-compatible 协议接入
- 网关路径覆盖
- 额外 query 参数
- 额外请求头
- 流式生命周期监听
- 响应元数据提取
- 非流式瞬时错误重试
- provider / gateway 示例套件

如果你未来要做：

- RAG：这个 SDK 适合作为生成层
- Agent：这个 SDK 适合作为文本能力底座

---

## 7. 文档导航

- [中文通用文档](docs/text-ai-sdk-general-guide-zh.md)
- [零基础教学文档](docs/text-ai-sdk-beginner-tutorial.md)
- [详细使用指南](docs/text-ai-sdk-guide.md)
- [兼容性示例说明](docs/text-ai-sdk-compatibility-examples.md)
- [更新记录](CHANGELOG.md)

---

## 8. 发布状态

当前仓库已经整理为可本地打包、可继续演进的独立 SDK 项目：

- `mvn test` 可用于验证
- `mvn package` 可生成主 jar、sources jar、javadoc jar
- 如果未来要发布到公共 Maven 仓库，仍建议补齐 license、SCM、最终版本号等维护者元数据

---

## 9. 代码结构

当前源码按职责拆分为：

- `com.textai.sdk.text`
  - 面向调用方的纯文本 API
- `com.textai.sdk.error`
  - 错误类型和错误对象
- `com.textai.sdk.core`
  - HTTP、SSE、JSON 等公共基础能力
- `com.textai.sdk.openai`
  - OpenAI 风格协议映射与解析

这套结构方便后续继续扩展更多能力包，同时不破坏当前 `TextAiClient` 的纯文本定位。
