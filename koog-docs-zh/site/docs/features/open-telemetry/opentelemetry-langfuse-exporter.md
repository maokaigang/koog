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
<!--- KNIT example-langfuse-exporter-01.txt -->

## 配置 { #configuration }

要启用 Langfuse 导出，请安装 **OpenTelemetry 功能** 并添加 `LangfuseExporter`。
该导出器在底层使用 `OtlpHttpSpanExporter` 将追踪数据发送到 Langfuse 的 OpenTelemetry 端点。

### 示例：启用 Langfuse 追踪的智能体 { #example-agent-with-langfuse-tracing }

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import kotlinx.coroutines.runBlocking
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    -->
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
    <!--- KNIT example-langfuse-exporter-01.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    public class exampleLangfuseExporterJava01 {
        static PromptExecutor promptExecutor = PromptExecutor.builder()
            .openAI("openai-api-key")
            .build();
    -->
    <!--- SUFFIX
    }
    -->
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
    <!--- KNIT exampleLangfuseExporterJava01.java -->

## 追踪属性 { #trace-attributes }

Langfuse 使用追踪级别的属性来增强可观测性，支持会话、环境、标签和其他元数据等功能。
`addLangfuseExporter` 函数支持一个 `traceAttributes` 参数，该参数接受一个 `CustomAttribute` 对象列表。

这些属性会被添加到每个追踪的根 `InvokeAgentSpan` 跨度中，并启用 Langfuse 的高级功能。您可以传递 Langfuse 支持的任何属性 - 请参阅 [Langfuse 的 OpenTelemetry 文档中的完整列表](https://langfuse.com/integrations/native/opentelemetry#trace-level-attributes)。通用属性：
- **会话** (`langfuse.session.id`)：将相关追踪分组，用于聚合指标、成本分析和评分
- **环境**：将生产环境追踪与开发和预发布环境隔离，以便进行更清晰的分析
- **标签** (`langfuse.trace.tags`)：使用功能名称、实验ID或客户细分（字符串数组）标记追踪

### 会话和标签示例 { #example-with-session-and-tags }

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.attribute.CustomAttribute
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import kotlinx.coroutines.runBlocking
    import java.util.UUID
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    -->
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

        // 使用相同会话ID的多次运行将在Langfuse中被分组
        agent.run("What is Kotlin?")
        agent.run("Show me a coroutine example")
    }
    ```
    <!--- KNIT example-langfuse-exporter-02.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.attribute.CustomAttribute;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import java.util.List;
    import java.util.UUID;
    public class exampleLangfuseExporterJava02 {
        static PromptExecutor promptExecutor = PromptExecutor.builder()
            .openAI("openai-api-key")
            .build();
    -->
    <!--- SUFFIX
    }
    -->
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

        // 使用相同会话ID的多次运行将在Langfuse中被分组
        agent.run("How to setup Langfuse integration in Koog agent?");
        agent.run("Show me a Java API  example");
    }
    ```
    <!--- KNIT exampleLangfuseExporterJava02.java -->

## 追踪内容 { #what-gets-traced }

启用后，Langfuse导出器会捕获与Koog通用OpenTelemetry集成相同的跨度，包括：

- **代理生命周期事件**：代理启动、停止、错误
- **LLM交互**：提示、响应、令牌使用量、延迟
- **工具调用**：工具调用的执行追踪
- **系统上下文**：元数据，如模型名称、环境、Koog版本

Koog还会捕获Langfuse显示[代理图谱](https://langfuse.com/docs/observability/features/agent-graphs)所需的跨度属性。

出于安全考虑，OpenTelemetry跨度的部分内容默认会被屏蔽。
若要在Langfuse中查看这些内容，请在OpenTelemetry配置中使用[setVerbose](opentelemetry-support.md#setverbose)方法，并将其`verbose`参数设置为`true`，如下所示：

=== "Kotlin"<!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant."
    ) {
    -->
<!--- SUFFIX
    }
    -->
```kotlin
install(OpenTelemetry) {
    addLangfuseExporter()
    setVerbose(true)
}
```
<!--- KNIT example-langfuse-exporter-03.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.attribute.CustomAttribute;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import java.util.List;
    import java.util.UUID;
    public class exampleLangfuseExporterJava03 {
        public static void main(String[] args) {
            var promptExecutor = PromptExecutor.builder()
                .openAI("openai-api-key")
                .build();
            var agent = AIAgent.builder()
                .promptExecutor(promptExecutor)
                .systemPrompt("You are a helpful assistant.")
                .llmModel(OpenAIModels.Chat.GPT4oMini)
                .
    -->
    <!--- SUFFIX
            .build();
        }
    }
    -->
    ```java
    install(OpenTelemetry.Feature, config -> {
        config.addLangfuseExporter();
        config.setVerbose(true);
    })
    ```
    <!--- KNIT exampleLangfuseExporterJava03.java -->

在 Langfuse 中可视化时，追踪信息显示如下：
![Langfuse traces](img/opentelemetry-langfuse-exporter-light.png#only-light)
![Langfuse traces](img/opentelemetry-langfuse-exporter-dark.png#only-dark)

有关 Langfuse OpenTelemetry 追踪的更多详情，请参阅：  
[Langfuse OpenTelemetry 文档](https://langfuse.com/integrations/native/opentelemetry#opentelemetry-endpoint)。

---

## 故障排除 { #troubleshooting }

### Langfuse 中未显示追踪信息 { #no-traces-appear-in-langfuse }
- 请仔细检查您的环境中是否已设置 `LANGFUSE_HOST`、`LANGFUSE_PUBLIC_KEY` 和 `LANGFUSE_SECRET_KEY`。
- 如果在自托管的 Langfuse 上运行，请确认您的应用环境能够访问 `LANGFUSE_HOST`。
- 验证公钥/私钥对是否属于正确的项目。