# Text AI SDK 零基础教学文档

这份文档写给第一次接触 `text-ai-sdk` 的使用者。

目标不是一次讲完所有细节，而是先让你做到这三件事：

1. 知道这个 SDK 是干什么的
2. 能看懂最基础的调用方式
3. 能自己写出一个能运行的文本请求

如果你对下面这些词还不熟悉，也没有关系：

- OpenAI 接口协议
- `/v1/responses`
- `/v1/chat/completions`
- 流式请求
- 异步请求

这份文档会按“先理解，再上手”的顺序带你入门。

---

## 1. 一句话理解这个 SDK

`text-ai-sdk` 是一个 Java 纯文本调用 SDK。

它帮你做的事情是：

- 向模型发送文本问题
- 接收模型返回的文本回答
- 支持同步、异步、流式三种常见调用方式
- 支持两种 OpenAI 风格协议：
  - `/v1/responses`
  - `/v1/chat/completions`

你可以把它理解成：

> 一个专门负责“发文字请求、收文字回答”的 Java 工具包。

---

## 2. 先记住 5 个核心概念

### 2.1 `TextAiClient`

这是最核心的对象。

你可以把它理解成：

> 用来连接模型服务的客户端

后面几乎所有调用，都是从它开始。

例如：

```java
TextAiClient client = TextAiClient.forResponses(
        "http://127.0.0.1:48760",
        "你的 API_KEY",
        "gpt-5.4"
);
```

这段代码的意思是：

- 我要创建一个客户端
- 它连接到 `http://127.0.0.1:48760`
- 它使用指定的 `API_KEY`
- 默认模型是 `gpt-5.4`
- 它底层走的是 `responses` 协议

### 2.2 `TextRequest`

这是单次请求的参数对象。

如果你只想发一句简单问题，可以暂时不用它。

但如果你想控制更多参数，就会用到它，比如：

- `systemPrompt`
- `userInput`
- `temperature`
- `maxTokens`

例如：

```java
TextRequest request = TextRequest.builder()
        .systemPrompt("你是一个专业助手。")
        .userInput("请介绍一下你自己。")
        .temperature(0)
        .maxTokens(512)
        .build();
```

### 2.3 `TextResponse`

这是非流式请求的返回结果对象。

最常用的是：

```java
response.getText()
```

它表示最终完整回答。

### 2.4 `TextStreamResponse`

这是流式请求最终结束后的汇总结果对象。

虽然流式请求会边生成边输出，但整个流结束后，SDK 仍然会给你一个完整结果对象。

最常用的是：

```java
response.getFullText()
```

它表示拼接好的完整流式回答。

### 2.5 `DeltaCallback`

这是最简单的流式回调。

它的作用是：

> 每收到一小段文本，就立刻交给你处理。

例如：

```java
delta -> {
    System.out.print(delta);
    System.out.flush();
}
```

这段代码的含义是：

- 模型每生成一小段文本
- 就马上输出到控制台

---

## 3. 先分清两个维度

初学者最容易混淆的地方，就是“协议”和“是否流式”。

你要把 SDK 的调用拆成两个维度来看。

### 3.1 协议维度

由你创建客户端的方法决定：

#### 方式 A：`responses`

```java
TextAiClient client = TextAiClient.forResponses(...);
```

这表示底层走：

```text
/v1/responses
```

#### 方式 B：`chat/completions`

```java
TextAiClient client = TextAiClient.forChatCompletions(...);
```

这表示底层走：

```text
/v1/chat/completions
```

所以：

- `forResponses(...)` = `responses` 协议
- `forChatCompletions(...)` = `chat/completions` 协议

### 3.2 流式维度

由你调用的方法名决定：

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

所以：

- `chat` 开头 = 非流式
- `stream` 开头 = 流式

### 3.3 一句总规则

以后你看代码时，按下面判断就行：

- `forResponses / forChatCompletions`：看协议
- `chat / stream`：看是否流式
- `Async`：看是否异步

---

## 4. 第一个可运行示例

这是最推荐你先跑起来的代码：

```java
import com.textai.sdk.text.TextAiClient;
import com.textai.sdk.text.TextResponse;

public class FirstDemo {
    public static void main(String[] args) {
        TextAiClient client = TextAiClient.forResponses(
                "http://127.0.0.1:48760",
                "你的 API_KEY",
                "gpt-5.4"
        );

        TextResponse response = client.chat("你好，请介绍一下你自己。");

        if (response.isSuccess()) {
            System.out.println("回答: " + response.getText());
        } else {
            System.out.println("失败类型: " + response.getError().getType());
            System.out.println("失败信息: " + response.getError().getMessage());
        }
    }
}
```

建议你先不用纠结所有类，只要记住这三步：

1. 创建 `TextAiClient`
2. 调用 `chat(...)`
3. 用 `response.getText()` 读取回答

---

## 5. 怎么传 system prompt

### 5.1 非流式传 system prompt

```java
TextResponse response = client.chat(
        "你是一个专业、简洁的助手。",
        "请介绍一下你自己。"
);
```

第一个参数是 `systemPrompt`，第二个参数是 `userInput`。

### 5.2 流式也能传 system prompt

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

所以：

> 不是流式没有 `systemPrompt`，而是很多最短示例没有显式写出来。

---

## 6. 为什么流式还能拿到完整回答

很多人第一次看到下面这种代码会疑惑：

```java
TextStreamResponse response = client.stream(
        "请流式介绍一下你自己。",
        delta -> System.out.print(delta)
);

System.out.println(response.getFullText());
```

为什么它既是流式，又能拿完整结果？

原因是流式调用做了两件事：

1. 在生成过程中，通过 callback 一段一段把文本推给你
2. 在整个流结束后，再返回一个 `TextStreamResponse`

所以：

- 想边生成边看：用 `delta`
- 想在结束后拿完整结果：用 `response.getFullText()`

---

## 7. 异步不等于流式

这是第二个常见误区。

### 7.1 异步非流式

```java
client.chatAsync("你好")
```

含义是：

- 请求在后台执行
- 结果还是一次性回来

### 7.2 异步流式

```java
client.streamAsync("你好", delta -> System.out.print(delta))
```

含义是：

- 请求在后台执行
- 中间会不断回调增量文本

所以：

- `Async` = 是否异步
- `stream` = 是否流式

这两个不是一回事。

---

## 8. 如果你需要更完整的流式事件

如果你除了想接收文本，还想知道：

- 流什么时候开始
- token 用量
- 流什么时候结束
- 如果流失败了怎么办

那就不要只用 `DeltaCallback`，而是用 `TextStreamListener`。

例如：

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
            public void onComplete(TextStreamResponse response) {
                System.out.println();
                System.out.println("流式结束");
            }
        }
);
```

---

## 9. 初学者最常见的错误

### 错误 1：把 `forResponses` 和 `forChatCompletions` 当成流式开关

不是。

它们决定的是协议，不决定是不是流式。

### 错误 2：把 `chatAsync` 当成流式

不是。

`chatAsync` 是异步非流式。

### 错误 3：以为流式拿不到完整结果

不对。

你可以通过：

```java
response.getFullText()
```

拿到完整结果。

### 错误 4：以为流式不能传 `systemPrompt`

不对。

流式和非流式都能传。

### 错误 5：一上来就使用 `TextRequest`

不是不行，但初学者建议先从简单重载开始：

- `chat("你好")`
- `chat("系统提示", "用户输入")`
- `stream("你好", delta -> ...)`

先跑通，再上复杂参数。

---

## 10. 推荐学习顺序

建议你按下面顺序熟悉这个 SDK：

1. `forResponses(...) + chat("你好")`
2. `chat(systemPrompt, userInput)`
3. `stream(..., delta -> ...)`
4. `TextRequest.builder()`
5. `chatAsync(...)`
6. `streamWithListener(...)`

这样最容易理解，也最不容易混淆。

---

## 11. 下一步看什么

如果你已经能跑通最基础示例，接下来建议看：

- [详细使用指南](text-ai-sdk-guide.md)
- [兼容性示例说明](text-ai-sdk-compatibility-examples.md)
- [中文通用文档](text-ai-sdk-general-guide-zh.md)

