# Text AI SDK 兼容性示例说明

这份文档汇总了 SDK 自带的兼容性示例，方便你快速判断：

> 哪个示例最适合我当前的 OpenAI-compatible 文本接入场景？

所有示例都位于：

- `src/main/java/com/textai/sdk/demo/examples`

它们有几个共同特点：

- 文件小
- 注释充分
- 每个示例只聚焦一个场景

---

## 1. 示例目录

### 1.1 `ResponsesProviderExample`

文件：

- `src/main/java/com/textai/sdk/demo/examples/ResponsesProviderExample.java`

适合场景：

- provider 暴露 `/v1/responses`
- 你想看最小非流式纯文本示例
- 你想了解 `responseId` 和 `model` 的基础读取方式

### 1.2 `ChatCompletionsProviderExample`

文件：

- `src/main/java/com/textai/sdk/demo/examples/ChatCompletionsProviderExample.java`

适合场景：

- provider 走 `/v1/chat/completions`
- 你想看 system prompt + user prompt 的文本示例
- 你想对比 `chat/completions` 和 `responses` 两种接入方式

### 1.3 `GatewayCustomizationExample`

文件：

- `src/main/java/com/textai/sdk/demo/examples/GatewayCustomizationExample.java`

适合场景：

- 你的网关需要自定义 path
- 你需要附加 query 参数，例如 `api-version`
- 你需要附加 provider 特定 header

这也是最适合以下场景复制的示例：

- 内部网关
- Azure 风格兼容层
- 带自定义路由规则的代理层

### 1.4 `ResponsesStreamListenerExample`

文件：

- `src/main/java/com/textai/sdk/demo/examples/ResponsesStreamListenerExample.java`

适合场景：

- 你需要 `responses` 风格流式输出
- 简单 `DeltaCallback` 已经不够用
- 你希望拿到如下生命周期事件：
  - 开始
  - 增量文本
  - usage
  - 完成
  - 错误

这也是最适合 GUI 和可观测流式接入的示例。

### 1.5 `RetryAndMetadataExample`

文件：

- `src/main/java/com/textai/sdk/demo/examples/RetryAndMetadataExample.java`

适合场景：

- 你希望在非流式请求里启用瞬时错误重试
- 你希望读取响应元数据
- 你正在把 SDK 接入一个偏生产环境的后端服务

---

## 2. 示例使用的环境变量

这些示例会读取：

- `AI_API_KEY`
- `AI_BASE_URL`
- `AI_MODEL`

默认值：

- `AI_BASE_URL` 默认是 `http://127.0.0.1:48760`
- `AI_MODEL` 默认是 `gpt-5.4`

其中：

- `AI_API_KEY` 是必须的

---

## 3. 建议阅读顺序

如果你是第一次接触这个 SDK，推荐顺序：

1. `ResponsesProviderExample`
2. `ChatCompletionsProviderExample`
3. `GatewayCustomizationExample`
4. `ResponsesStreamListenerExample`
5. `RetryAndMetadataExample`

这个顺序会从最小文本调用逐步过渡到更偏运维、兼容和生产化的场景。

---

## 4. 与主示例的关系

仓库里已有的 `TextAiClientDemo` 仍然保留，它更像一个综合演示。

而兼容性示例套件的定位不同：

- 每个文件只聚焦一个场景
- 每个文件都更适合直接复制进真实项目
- 每个文件都对应一个具体的兼容性问题

