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

## Trace attributes

Langfuse uses trace-level attributes to enhance observability with features like sessions, environments, tags and other metadata.
The `addLangfuseExporter` function supports a `traceAttributes` parameter that accepts a list of `CustomAttribute` objects.

These attributes are added to the root `InvokeAgentSpan` span of each trace and enable Langfuse's advanced features. You can pass
any attributes supported by Langfuse - see the [complete list in Langfuse's OpenTelemetry documentation](https://langfuse.com/integrations/native/opentelemetry#trace-level-attributes).

Common attributes:
- **Sessions** (`langfuse.session.id`): Group related traces for aggregated metrics, cost analysis, and scoring
- **Environments**: Isolate production traces from development and staging for cleaner analysis
- **Tags** (`langfuse.trace.tags`): Label traces with feature names, experiment IDs, or customer segments (array of strings)

### Example with session and tags

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

        // Multiple runs with the same session ID will be grouped in Langfuse
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

        // Multiple runs with the same session ID will be grouped in Langfuse
        agent.run("How to setup Langfuse integration in Koog agent?");
        agent.run("Show me a Java API  example");
    }
    ```
    <!--- KNIT exampleLangfuseExporterJava02.java -->

## What gets traced

When enabled, the Langfuse exporter captures the same spans as Koog’s general OpenTelemetry integration, including:

- **Agent lifecycle events**: agent start, stop, errors
- **LLM interactions**: prompts, responses, token usage, latency
- **Tool calls**: execution traces for tool invocations
- **System context**: metadata such as model name, environment, Koog version

Koog also captures span attributes required by Langfuse to show [Agent Graphs](https://langfuse.com/docs/observability/features/agent-graphs).

For security reasons, some content of OpenTelemetry spans is masked by default.
To make the content available in Langfuse, use the [setVerbose](opentelemetry-support.md#setverbose) method in the OpenTelemetry configuration and set its `verbose` argument to `true` as follows:

=== "Kotlin"

    <!--- INCLUDE
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

When visualized in Langfuse, the trace appears as follows:
![Langfuse traces](img/opentelemetry-langfuse-exporter-light.png#only-light)
![Langfuse traces](img/opentelemetry-langfuse-exporter-dark.png#only-dark)

For more details on Langfuse OpenTelemetry tracing, see:  
[Langfuse OpenTelemetry Docs](https://langfuse.com/integrations/native/opentelemetry#opentelemetry-endpoint).

---

## Troubleshooting

### No traces appear in Langfuse
- Double-check that `LANGFUSE_HOST`, `LANGFUSE_PUBLIC_KEY`, and `LANGFUSE_SECRET_KEY` are set in your environment.
- If running on self-hosted Langfuse, confirm that the `LANGFUSE_HOST` is reachable from your application environment.
- Verify that the public/secret key pair belongs to the correct project.