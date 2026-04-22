# 更新记录

`text-ai-sdk` 的重要变更记录保存在此文件中。

## 1.0.0-SNAPSHOT

### 新增

- 面向 `/v1/chat/completions` 和 `/v1/responses` 的纯文本 `TextAiClient` 门面
- 严格的纯文本解析与类型化错误模型
- 同步、异步、流式、异步流式调用能力
- `TextStreamListener` 生命周期回调，支持开始、增量文本、usage、完成、错误
- 通过 endpoint path override、query 参数和额外请求头实现的传输层定制能力
- 通过 `responseId`、`model` 和原始响应头暴露的响应元数据
- 可选的非流式瞬时错误重试策略
- 面向 provider 和 gateway 场景的兼容性示例套件

### 调整

- 公开源码结构按 `text`、`error`、`core`、`openai` 四层重组
- 基于 listener 的流式 API 使用明确的方法名，避免 `null` 重载二义性
- demo 现已覆盖异步 listener 流式示例

### 已知边界

- 当前模块有意限制在纯文本请求与纯文本响应
- 尚未实现流式重试
- tool calling、多模态、结构化输出协议当前明确不在此模块范围内
- 若要发布到公共 Maven 仓库，仍需维护者补齐 license、SCM 等公开元数据
