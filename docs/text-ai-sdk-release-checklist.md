# Text AI SDK 发布检查清单

在将 `text-ai-sdk` 对外发布之前，请先完成这份检查清单。

---

## 1. 发布前校验

- 确认 `README.md` 仍然与公开 API 一致
- 确认兼容性示例套件仍可编译，且反映当前 SDK 行为
- 确认 `CHANGELOG.md` 已包含当前快照或本次发布记录
- 确认源码和文档中不存在示例 key 或本地敏感信息

---

## 2. 测试与构建验证

在 `standalone-text-ai-sdk` 目录下执行：

```bash
mvn -q test
mvn -q package
```

预期结果：

- 测试全部通过
- 生成主 jar
- 生成 `-sources.jar`
- 生成 `-javadoc.jar`

---

## 3. 公开发布前需要维护者补齐的元数据

以下内容不要随意填写占位值，公开发布前请替换为真实信息：

- 最终发布版本号，而不是 `1.0.0-SNAPSHOT`
- license 及 license 元数据
- SCM URL 与 connection 信息
- developer 或 organization 元数据
- 如果发布目标需要，还应补齐项目主页 URL

---

## 4. 制品检查

在 `target` 目录中确认：

- `text-ai-sdk-<version>.jar`
- `text-ai-sdk-<version>-sources.jar`
- `text-ai-sdk-<version>-javadoc.jar`

同时抽查：

- README 链接都能打开
- docs 没有引用已经删除的 API
- `pom.xml` 中的 `groupId`、`artifactId`、Java 版本仍然符合预期

---

## 5. 发布决策前确认

发布前请再次确认：

- 当前包的边界仍然是“纯文本”
- 重试行为仍然只作用于非流式请求
- 兼容性示例仍然反映你愿意公开支持的 provider / gateway 场景
- tool calling、多模态、结构化输出等后续能力没有在当前模块中被误宣传

---

## 6. 可选的发布后续动作

在本地打包成功后，可以继续考虑：

- 如果要发到 Maven Central，补充签名产物
- 增加 CI 构建与打包流程
- 基于 `CHANGELOG.md` 生成 release notes
- 为后续能力包定义统一的语义化版本策略

