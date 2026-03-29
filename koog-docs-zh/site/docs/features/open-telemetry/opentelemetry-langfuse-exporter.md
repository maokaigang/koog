<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:01:17+00:00", "source_path": "features/open-telemetry/opentelemetry-langfuse-exporter.md", "source_sha256": "5445375fc209b70527759d6534dfacaea07e3ec56c147ba130ed790c5d0b518f", "source_tag": "0.7.3", "translation_status": "changed"} -->
# Langfuse 导出器 { #langfuse-exporter }

Koog 内置支持将智能体追踪数据导出到 [Langfuse](https://langfuse.com/)，这是一个用于 AI 应用可观测性和分析的平台。
通过集成 Langfuse，您可以可视化、分析和调试您的 Koog 智能体如何与 LLM、API 及其他组件进行交互。

关于 Koog 对 OpenTelemetry 支持的背景信息，请参阅 [OpenTelemetry 支持](https://docs.koog.ai/opentelemetry-support/)。

---

## 设置说明 { #setup-instructions }

1.  创建一个 Langfuse 项目。按照 [在 Langfuse 中创建新项目](https://langfuse.com/docs/get-started#create-new-project-in-langfuse) 指南进行操作。
2.  获取 API 凭据。按照 [Langfuse API 密钥在哪里？](https://langfuse.com/faq/all/where-are-langfuse-api-keys) 中的说明，获取您的 Langfuse `public key` 和 `secret key`。
3.  将 Langfuse 主机地址、私钥和密钥传递给 Langfuse 导出器。
    可以通过向 `addLangfuseExporter()` 函数提供这些参数来完成，或者如下所示设置环境变量：

```bash
   export LANGFUSE_HOST="https://cloud.langfuse.com"
   export LANGFUSE_PUBLIC_KEY="<your-public-key>"
   export LANGFUSE_SECRET_KEY="<your-secret-key>"
```

## 配置 { #configuration }

要启用 Langfuse 导出，请安装 **OpenTelemetry 功能** 并添加 `LangfuseExporter`。 该导出器在底层使用 `OtlpHttpSpanExporter` 将追踪数据发送到 Langfuse 的 OpenTelemetry 端点。

### 示例：启用 Langfuse 追踪的智能体 { #example-agent-with-langfuse-tracing }

=== "Kotlin"

    ```kotlin
    fun main() = runBlocking {
        val agent = AIAgent(
            promptExecutor = promptExecutor,
            llmModel = OpenAIModels.Chat.GPT4oMini,
            systemPrompt = "You are a code assistant. Provide concise code examples."
        ) {
            install(OpenTelemetry) {
                addLangfuseExporter()
            }
        }

        println("Running agent with Langfuse tracing")

        val result = agent.run("Tell me a joke about programming")
        println("Result: $result\nSee traces on the Langfuse instance")
    }
    ```

=== "Java"

    ```java
    public static void main(String[] args) {
        var agent = AIAgent.builder()
            .promptExecutor(promptExecutor)
            .llmModel(OpenAIModels.Chat.GPT4oMini)
            .systemPrompt("You are a code assistant. Provide concise code examples.")
            .install(OpenTelemetry.Feature, config ->
                config.addLangfuseExporter()
            )
            .build();

        System.out.println("Running agent with Langfuse tracing");

        var result = agent.run("Tell me a joke about programming");
        System.out.println("Result: " + result + "\nSee traces on the Langfuse instance");
    }
    ```

## 追踪属性 { #trace-attributes }

Langfuse 利用追踪层级的属性来增强可观测性，提供会话、环境、标签及其他元数据等功能。`addLangfuseExporter` 函数支持一个 `traceAttributes` 参数，该参数接受一个 `CustomAttribute` 对象列表。

这些属性会被添加到每个追踪的根`InvokeAgentSpan` span中，并启用Langfuse的高级功能。您可以传递Langfuse支持的任何属性——详情请参阅[Langfuse 的 OpenTelemetry 文档完整列表

Langfuse 的 OpenTelemetry 文档提供了全面的指南和参考，帮助开发者集成 OpenTelemetry 以实现可观测性。以下是文档的完整列表：

1. **概述**
   - OpenTelemetry 简介
   - Langfuse 与 OpenTelemetry 的集成优势

2. **快速开始**
   - 环境准备
   - 安装与配置步骤
   - 示例项目

3. **配置指南**
   - 数据收集设置
   - 导出器配置
   - 采样策略
   - 上下文传播

4. **集成示例**
   - Python 应用集成
   - JavaScript/Node.js 应用集成
   - Java 应用集成
   - Go 应用集成
   - .NET 应用集成

5. **数据模型**
   - 跟踪（Traces）
   - 指标（Metrics）
   - 日志（Logs）
   - 属性与事件

6. **高级功能**
   - 自定义检测
   - 性能优化
   - 安全与合规性
   - 故障排除

7. **API 参考**
   - OpenTelemetry API 端点
   - 配置参数详解
   - 错误代码与处理

8. **最佳实践**
   - 生产环境部署建议
   - 监控与告警设置
   - 成本优化策略

9. **常见问题解答（FAQ）**
   - 安装与配置问题
   - 数据导出问题
   - 性能与扩展性问题

10. **更新日志**
    - 版本历史
    - 兼容性说明
    - 未来计划

如需查看最新文档，请访问 [Langfuse OpenTelemetry 官方文档](https://langfuse.com/docs/opentelemetry)。](https://langfuse.com/integrations/native/opentelemetry#trace-level-attributes)。

Common attributes:
- **会话** (`langfuse.session.id`): 将相关追踪分组，用于聚合指标、成本分析和评分
- **环境**：将生产环境追踪与开发和预发布环境隔离，以实现更清晰的分析
- **标签** (`langfuse.trace.tags`): 使用功能名称、实验ID或客户细分来标记追踪记录（字符串数组）

### 示例：会话与标签 { #example-with-session-and-tags }

=== "Kotlin"

    ```kotlin
    fun main() = runBlocking {
        val sessionId = UUID.randomUUID().toString()

        val agent = AIAgent(
            promptExecutor = promptExecutor,
            llmModel = OpenAIModels.Chat.GPT4oMini,
            systemPrompt = "You are a helpful assistant."
        ) {
            install(OpenTelemetry) {
                addLangfuseExporter(
                    traceAttributes = listOf(
                        CustomAttribute("langfuse.session.id", sessionId),
                        CustomAttribute("langfuse.trace.tags", listOf("chat", "kotlin", "production"))
                    )
                )
            }
        }

        println("Running agent with Langfuse tracing")

        // Multiple runs with the same session ID will be grouped in Langfuse
        agent.run("What is Kotlin?")
        agent.run("Show me a coroutine example")
    }
    ```

=== "Java"

    ```java
    public static void main(String[] args) {
        var sessionId = UUID.randomUUID().toString();

        var agent = AIAgent.builder()
            .promptExecutor(promptExecutor)
            .systemPrompt("You are a helpful assistant.")
            .llmModel(OpenAIModels.Chat.GPT4oMini)
            .install(OpenTelemetry.Feature, config ->
                config.addLangfuseExporter(
                    null, null, null, null,
                    List.of(
                        new CustomAttribute("langfuse.session.id", sessionId),
                        new CustomAttribute("langfuse.trace.tags", List.of("chat", "kotlin", "production"))
                    )
                ))
            .build();

        System.out.println("Running agent with Langfuse tracing");

        // Multiple runs with the same session ID will be grouped in Langfuse
        agent.run("How to setup Langfuse integration in Koog agent?");
        agent.run("Show me a Java API  example");
    }
    ```

## 什么会被追踪 { #what-gets-traced }

启用后，Langfuse导出器会捕获与Koog通用OpenTelemetry集成相同的跨度，包括：

- **智能体生命周期事件**：启动、停止、错误
- **模型交互**（LLM 交互）：提示、响应、令牌使用量、延迟
- **工具调用**：工具调用的执行轨迹
- **系统上下文**：元数据，如模型名称、环境、Koog 版本

Koog 还捕获了 Langfuse 显示 [智能体图](https://langfuse.com/docs/observability/features/agent-graphs) 所需的 span 属性。

出于安全考虑，OpenTelemetry 的部分追踪内容默认会被屏蔽。若要在 Langfuse 中查看这些内容，请在 OpenTelemetry 配置中使用 [设置详细模式](index.md#setverbose) 方法，并将其 `verbose` 参数设置为 `true`，具体操作如下：

=== "Kotlin"

    ```kotlin
    install(OpenTelemetry) {
        addLangfuseExporter()
        setVerbose(true)
    }
    ```

=== "Java"

    ```java
    install(OpenTelemetry.Feature, config -> {
        config.addLangfuseExporter();
        config.setVerbose(true);
    })
    ```

在 Langfuse 中可视化时，追踪记录显示如下：
![Langfuse traces](../../img/opentelemetry-langfuse-exporter-light.png#only-light)
![Langfuse traces](../../img/opentelemetry-langfuse-exporter-dark.png#only-dark)

有关 Langfuse OpenTelemetry 追踪的更多详情，请参阅：[Langfuse OpenTelemetry 文档](https://langfuse.com/integrations/native/opentelemetry#opentelemetry-endpoint)。

---

## 故障排查 { #troubleshooting }

### Langfuse 中没有出现追踪记录 { #no-traces-appear-in-langfuse }
- 请确认您的环境中已设置 `LANGFUSE_HOST`、`LANGFUSE_PUBLIC_KEY` 和 `LANGFUSE_SECRET_KEY`。
- 如果在自托管的 Langfuse 上运行，请确认您的应用环境能够访问 `LANGFUSE_HOST`。
- 验证公钥/私钥对是否属于正确的项目。
