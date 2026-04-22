# Text AI SDK 零基础教学文档

这份文档是给第一次接触 `text-ai-sdk` 的同学准备的。

目标不是把所有细节一次讲完，而是让你先做到这三件事：

1. 知道这个 SDK 是干什么的
2. 能看懂最基本的调用代码
3. 能自己写出一个“能跑起来”的文本请求

如果你现在对下面这些词还不熟，也没关系：

- OpenAI 接口协议
- `/v1/responses`
- `/v1/chat/completions`
- 流式请求
- 异步请求

这份文档会一步一步讲。

---

## 1. 先用一句话理解这个 SDK

`text-ai-sdk` 是一个 **Java 纯文本调用 SDK**。

它帮你做的事情是：

- 向大模型发送文本问题
- 接收文本回答
- 支持同步、异步、流式
- 支持两种常见 OpenAI 风格接口：
  - `/v1/responses`
  - `/v1/chat/completions`

你可以把它理解成：

> 一个专门帮你“发文字请求、收文字回答”的 Java 工具包。

它现在 **不负责**：

- 工具调用
- 多模态输入输出
- 结构化输出
- Agent 编排

所以你现在可以放心地把它当成：

> 文本版模型调用器

---

## 2. 你需要先知道的 5 个核心概念

在开始写代码前，先记住下面 5 个词。

### 2.1 `TextAiClient`

这是最核心的对象。

你可以把它理解成：

> 连接模型服务的客户端

你后面所有调用，几乎都是从它开始。

例如：

```java
TextAiClient client = TextAiClient.forResponses(
        "http://127.0.0.1:48760",
        "你的API_KEY",
        "gpt-5.4"
);
```

这行代码的意思是：

- 我要创建一个客户端
- 它连接到 `http://127.0.0.1:48760`
- 它使用指定的 `API_KEY`
- 默认模型是 `gpt-5.4`
- 它走的是 `responses` 协议

---

### 2.2 `TextRequest`

这是“单次请求”的参数对象。

当你只想简单发一句话时，可以不用它。  
但如果你想控制更多内容，就会用到它。

它通常用来装这些信息：

- `systemPrompt`
- `userInput`
- `temperature`
- `maxTokens`

例如：

```java
TextRequest request = TextRequest.builder()
        .systemPrompt("你是一个专业助手")
        .userInput("请介绍一下你自己")
        .temperature(0)
        .maxTokens(512)
        .build();
```

---

### 2.3 `TextResponse`

这是 **非流式请求** 的返回结果。

你可以从它里面拿到：

- 最终文本回答
- token 统计
- 模型名
- responseId
- 错误信息

最常用的是：

```java
response.getText()
```

---

### 2.4 `TextStreamResponse`

这是 **流式请求** 的最终汇总结果。

注意：

流式请求在过程中会一段一段返回文本，  
但请求结束后，SDK 还是会给你一个最终对象，里面带完整回答。

最常用的是：

```java
response.getFullText()
```

---

### 2.5 `DeltaCallback`

这是流式回调。

它的作用是：

> 每来一小段文本，就立刻告诉你

例如：

```java
delta -> {
    System.out.print(delta);
    System.out.flush();
}
```

这段代码的意思就是：

- 模型每生成一点内容
- 就马上打印到控制台

---

## 3. 先分清两个维度：协议 和 是否流式

初学者最容易混的地方就在这里。

你要把 SDK 的调用拆成两个维度看。

### 3.1 第一维：走哪种协议

由 `client` 的创建方式决定。

#### 方式 A：`responses`

```java
TextAiClient client = TextAiClient.forResponses(...);
```

表示底层走：

```text
/v1/responses
```

#### 方式 B：`chat/completions`

```java
TextAiClient client = TextAiClient.forChatCompletions(...);
```

表示底层走：

```text
/v1/chat/completions
```

所以：

- `forResponses(...)` = `responses` 协议
- `forChatCompletions(...)` = `chat/completions` 协议

---

### 3.2 第二维：是不是流式

由你调用的方法名决定。

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

所以你只要记住：

- `chat` 开头 = 非流式
- `stream` 开头 = 流式

---

### 3.3 最重要的总规则

以后你看代码时，只要按这个规则判断：

- `forResponses / forChatCompletions`：看协议
- `chat / stream`：看是不是流式
- `Async`：看是不是异步

---

## 4. 第一个能跑起来的例子

这是最推荐你先抄下来跑一遍的代码。

```java
import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextResponse;

public class FirstDemo {
    public static void main(String[] args) {
        TextAiClient client = TextAiClient.forResponses(
                "http://127.0.0.1:48760",
                "你的API_KEY",
                "gpt-5.4"
        );

        TextResponse response = client.chat("你好，请介绍一下你自己");

        if (response.isSuccess()) {
            System.out.println("回答: " + response.getText());
        } else {
            System.out.println("失败类型: " + response.getError().getType());
            System.out.println("失败信息: " + response.getError().getMessage());
        }
    }
}
```

---

## 5. 上面这段代码是怎么工作的

我们逐行看。

### 5.1 创建客户端

```java
TextAiClient client = TextAiClient.forResponses(
        "http://127.0.0.1:48760",
        "你的API_KEY",
        "gpt-5.4"
);
```

意思是：

- 使用 `responses` 协议
- 请求地址是 `http://127.0.0.1:48760`
- 使用这个 key 做认证
- 默认模型是 `gpt-5.4`

---

### 5.2 发一个非流式请求

```java
TextResponse response = client.chat("你好，请介绍一下你自己");
```

意思是：

- 发送一句用户输入
- 等模型完整回答完
- 一次性拿到结果

所以这是一种：

> `responses + 非流式`

调用方式。

---

### 5.3 读取回答

```java
response.getText()
```

这是非流式最常用的读取方式。

它表示：

> 拿到完整文本回答

---

## 6. 如果我想加 `systemPrompt`

你可以直接传两个字符串：

```java
TextResponse response = client.chat(
        "你是一个专业的 Java 助手。",
        "请介绍一下你自己"
);
```

这里：

- 第一个参数 = `systemPrompt`
- 第二个参数 = `userInput`

如果你还不熟 `systemPrompt`，可以简单理解为：

> 给模型的“角色设定”或“行为要求”

比如：

- 你是一个老师
- 你要用简洁语气回答
- 你要只用中文回答

---

## 7. 如果我想更精细地控制请求

这时就用 `TextRequest`。

```java
import com.textai.sdk.text.TextRequest;
import com.textai.sdk.text.TextResponse;

TextRequest request = TextRequest.builder()
        .systemPrompt("你是一个专业的 Java 助手。")
        .userInput("请解释一下什么是线程安全")
        .temperature(0)
        .maxTokens(512)
        .build();

TextResponse response = client.chat(request);
```

建议你这样理解：

- 简单场景：直接 `chat("...")`
- 需要更多控制：`TextRequest.builder()`

---

## 8. 流式请求怎么写

流式请求的特点是：

> 模型不是最后一次性返回，而是边生成边返回

最简单的写法是：

```java
import com.textai.sdk.text.TextStreamResponse;

TextStreamResponse response = client.stream(
        "请流式介绍一下你自己",
        delta -> {
            System.out.print(delta);
            System.out.flush();
        }
);

System.out.println();
System.out.println("完整回答: " + response.getFullText());
```

这里你要特别理解两件事。

### 8.1 `delta`

这是流式过程中一段一段返回的文本。

比如模型可能先返回：

- “你”
- “好”
- “，”
- “我”
- “是”

那么 `delta` 就会被多次回调。

---

### 8.2 `response.getFullText()`

这是 SDK 最后帮你拼好的完整回答。

所以：

- 想看“边生成边输出” -> 看 `delta`
- 想拿“完整最终结果” -> 看 `response.getFullText()`

---

## 9. 为什么流式也能拿到完整回答

因为 SDK 在流式场景下做了两件事：

1. 中间不断把片段文本通过 callback 给你
2. 结束后再把完整结果封装成 `TextStreamResponse`

所以这句代码是正确的：

```java
System.out.println("完整回答: " + response.getFullText());
```

---

## 10. 流式也能传 `systemPrompt` 吗

能。

很多人第一次看示例时会误以为流式不能传 `systemPrompt`，其实不是。

你可以这样写：

```java
TextStreamResponse response = client.stream(
        "你是一个专业、简洁的助手。",
        "请流式介绍一下你自己",
        delta -> {
            System.out.print(delta);
            System.out.flush();
        }
);
```

这里：

- 第一个参数 = `systemPrompt`
- 第二个参数 = `userInput`
- 第三个参数 = 流式回调

---

## 11. `streamWithListener(...)` 又是什么

如果你只想拿到文本片段，用 `DeltaCallback` 就够了。

但如果你还想知道：

- 流什么时候开始
- token 什么时候返回
- 流什么时候结束
- 出错时怎么处理

那就用：

```java
streamWithListener(...)
```

示例：

```java
import com.textai.sdk.text.TextStreamListener;
import com.textai.sdk.text.TextStreamResponse;
import com.textai.sdk.text.TextStreamUsage;

TextStreamResponse response = client.streamWithListener(
        "请流式介绍一下你自己",
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
                System.out.println("token总数: " + usage.totalTokens());
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

## 12. 异步请求怎么理解

异步不等于流式，这两个概念一定不要混。

### 异步非流式

```java
client.chatAsync("你好")
```

意思是：

- 后台发请求
- 最后一次性拿到完整结果

### 异步流式

```java
client.streamAsync("你好", delta -> {
    System.out.print(delta);
});
```

意思是：

- 后台发请求
- 中间不断收到文本片段

所以：

- `Async` = 是否异步
- `stream` = 是否流式

它们是两个不同维度。

---

## 13. 返回值里最常用的方法有哪些

### 非流式：`TextResponse`

最常用的是：

```java
response.isSuccess()
response.getText()
response.getTotalTokens()
response.getModel()
response.getResponseId()
response.getError()
```

---

### 流式：`TextStreamResponse`

最常用的是：

```java
response.isSuccess()
response.getFullText()
response.getTotalTokens()
response.getModel()
response.getResponseId()
response.getLastEventType()
response.getError()
```

---

## 14. 什么时候用 `forResponses`，什么时候用 `forChatCompletions`

如果你不确定，先记这个规则：

- 你的服务更适合 `/v1/responses` -> 用 `forResponses(...)`
- 你的服务更适合 `/v1/chat/completions` -> 用 `forChatCompletions(...)`

调用方式表面上很像：

```java
client.chat(...)
client.stream(...)
```

但底层到底走哪个协议，是由 client 的创建方式决定的。

也就是：

```java
TextAiClient.forResponses(...)
```

和：

```java
TextAiClient.forChatCompletions(...)
```

这两行，决定了你底层走哪种请求格式。

---

## 15. 初学者最容易犯的 6 个错误

### 错误 1：把“异步”和“流式”当成一回事

记住：

- `Async` 只是后台执行
- `stream` 才表示流式

---

### 错误 2：看不出当前走的是哪种协议

只看 client 的创建方式：

- `forResponses(...)`
- `forChatCompletions(...)`

---

### 错误 3：流式时只看 `delta`，忘了最终完整结果

记住：

```java
response.getFullText()
```

就是最终完整回答。

---

### 错误 4：以为流式不能传 `systemPrompt`

其实可以。

流式和非流式都能传 `systemPrompt`。

---

### 错误 5：失败时直接 `getText()` 或 `getFullText()`

更稳的写法应该先判断：

```java
if (response.isSuccess()) {
    ...
} else {
    ...
}
```

---

### 错误 6：一上来就用最复杂的 API

学习顺序建议是：

1. `chat("你好")`
2. `chat(systemPrompt, userInput)`
3. `TextRequest.builder()`
4. `stream(..., delta -> ...)`
5. `streamWithListener(...)`
6. `chatAsync(...)`

---

## 16. 推荐学习路径

如果你是第一次接触这个 SDK，我建议这样学：

### 第一步

先跑最简单的非流式：

```java
client.chat("你好")
```

目标：

- 跑通
- 看懂 `TextResponse`

---

### 第二步

再加 `systemPrompt`：

```java
client.chat("你是一个专业助手", "你好")
```

目标：

- 理解 `systemPrompt`
- 理解请求参数的基本结构

---

### 第三步

再试流式：

```java
client.stream("你好", delta -> ...)
```

目标：

- 理解 `delta`
- 理解 `getFullText()`

---

### 第四步

再试 `TextRequest.builder()`

目标：

- 学会控制 `temperature`
- 学会控制 `maxTokens`

---

### 第五步

最后再学异步和 listener。

---

## 17. 一份最值得直接复制的完整示例

```java
import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextResponse;

public class BeginnerDemo {
    public static void main(String[] args) {
        TextAiClient client = TextAiClient.forResponses(
                "http://127.0.0.1:48760",
                "你的API_KEY",
                "gpt-5.4"
        );

        TextResponse response = client.chat(
                "你是一个专业的 Java 助手。",
                "请用三句话介绍一下你自己。"
        );

        if (response.isSuccess()) {
            System.out.println("回答: " + response.getText());
            System.out.println("模型: " + response.getModel());
            System.out.println("token总数: " + response.getTotalTokens());
        } else {
            System.out.println("失败类型: " + response.getError().getType());
            System.out.println("失败信息: " + response.getError().getMessage());
        }
    }
}
```

---

## 18. 看完这篇后，你下一步应该做什么

如果你已经能看懂这篇文档，建议你下一步做这三件事：

1. 自己跑通一个非流式请求
2. 自己跑通一个流式请求
3. 把 `forResponses` 和 `forChatCompletions` 都试一遍

等你做到这一步，再去看下面这些文档会更轻松：

- [README.md](</D:/Codex-Manager-main (1)/Codex-Manager-main/standalone-text-ai-sdk/README.md>)
- [text-ai-sdk-guide.md](</D:/Codex-Manager-main (1)/Codex-Manager-main/standalone-text-ai-sdk/docs/text-ai-sdk-guide.md>)
- [text-ai-sdk-compatibility-examples.md](</D:/Codex-Manager-main (1)/Codex-Manager-main/standalone-text-ai-sdk/docs/text-ai-sdk-compatibility-examples.md>)

这几个文档更适合在你“已经能跑起来”之后再看。
