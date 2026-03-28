<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:22:14+00:00", "source_path": "features/open-telemetry/opentelemetry-weave-exporter.md", "source_sha256": "847fe6af6c9d4e32164083952056850d93a8bd2a2c18c5a9068dc92e8ec7abe8", "source_tag": "0.7.3", "translation_status": "changed"} -->
# W&B Weave 导出器 { #w-b-weave-exporter }

Koog 内置支持将智能体追踪数据导出到 [W&B Weave](https://wandb.ai/site/weave/)，
这是 Weights & Biases 提供的一款用于 AI 应用可观测性和分析的开发者工具。  
通过 Weave 集成，您可以捕获提示词、补全结果、系统上下文和执行追踪，
并直接在您的 W&B 工作区中可视化这些数据。

关于 Koog 的 OpenTelemetry 支持背景，请参阅 [OpenTelemetry 支持](https://docs.koog.ai/opentelemetry-support/)。

---

## 设置说明 { #setup-instructions }

1.  在 [https://wandb.ai](https://wandb.ai) 注册一个 W&B 账户。
2.  从 [https://wandb.ai/authorize](https://wandb.ai/authorize) 获取您的 API 密钥。
3.  访问您的 W&B 仪表板 [https://wandb.ai/home](https://wandb.ai/home) 以查找您的实体名称。
    您的实体通常是您的用户名（如果是个人账户）或您的团队/组织名称。
4.  为您的项目定义一个名称。您无需预先创建项目，当发送第一条追踪数据时，项目会自动创建。
5.  将 Weave 实体、项目名称和 API 密钥传递给 Weave 导出器。
    这可以通过将它们作为参数提供给 `addWeaveExporter()` 函数来实现，
    或者通过设置环境变量来完成，如下所示：

```bash
export WEAVE_API_KEY="<your-api-key>"
export WEAVE_ENTITY="<your-entity>"
export WEAVE_PROJECT_NAME="koog-tracing"
```
<!--- KNIT example-weave-exporter-01.txt -->

## 配置 { #configuration }

要启用 Weave 导出，请安装 **OpenTelemetry 功能** 并添加 `WeaveExporter`。  
导出器通过 `OtlpHttpSpanExporter` 使用 Weave 的 OpenTelemetry 端点。

### 示例：启用 Weave 追踪的智能体 { #example-agent-with-weave-tracing }

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
        val entity = System.getenv()["WEAVE_ENTITY"] 
            ?: throw IllegalArgumentException("WEAVE_ENTITY is not set")
        
        val projectName = System.getenv()["WEAVE_PROJECT_NAME"] 
            ?: "koog-tracing"
        
        val agent = AIAgent(
            promptExecutor = promptExecutor,
            llmModel = OpenAIModels.Chat.GPT4oMini,
            systemPrompt = "You are a code assistant. Provide concise code examples."
        ) {
            install(OpenTelemetry) {
                addWeaveExporter()
            }
        }
    
        println("Running agent with Weave tracing")
    
        val result = agent.run("Tell me a joke about programming")
        println("Result: $result\nSee traces on https://wandb.ai/$entity/$projectName/weave/traces")
    }
    ```
    <!--- KNIT example-weave-exporter-01.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import java.util.Optional;
    public class exampleWeaveExporterJava01 {
        static PromptExecutor promptExecutor = PromptExecutor.builder()
            .openAI("openai-api-key")
            .build();
    -->
    <!--- SUFFIX
    }
    -->
    ```java
    public static void main(String[] args) {
        var entity = Optional.ofNullable(System.getenv("WEAVE_ENTITY"))
            .filter(env -> !env.isBlank())
            .orElseThrow(() -> new IllegalArgumentException("WEAVE_ENTITY is not set"));

        var projectName = Optional.ofNullable(System.getenv("WEAVE_PROJECT_NAME"))
            .filter(env -> !env.isBlank())
            .orElse("koog-tracing");

        var agent = AIAgent.builder()
            .promptExecutor(promptExecutor)
            .llmModel(OpenAIModels.Chat.GPT4oMini)
            .systemPrompt("You are a helpful assistant.")
            .install(OpenTelemetry.Feature, config ->
                config.addWeaveExporter(null, entity, projectName)
            )
            .build();

        System.out.println("Running agent with Weave tracing");

        var result = agent.run("Tell me a joke about programming");
        System.out.println("Result: " + result + "\nSee traces on https://wandb.ai/" + entity + "/" + projectName + "/weave/traces");
    }
    ```
    <!--- KNIT exampleWeaveExporterJava01.java -->

## What gets traced

When enabled, the Weave exporter captures the same spans as Koog’s general OpenTelemetry integration, including:

- **Agent lifecycle events**: agent start, stop, errors
- **LLM interactions**: prompts, completions, latency
- **Tool calls**: execution traces for tool invocations
- **System context**: metadata such as model name, environment, Koog version

For security reasons, some content of OpenTelemetry spans is masked by default.
To make the content available in Weave, use the [setVerbose](opentelemetry-support.md#setverbose) method in the OpenTelemetry configuration and set its `verbose` argument to `true` as follows:

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
        addWeaveExporter()
        setVerbose(true)
    }
    ```
    <!--- KNIT example-weave-exporter-02.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.attribute.CustomAttribute;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import java.util.List;
    import java.util.UUID;
    public class exampleWeaveExporterJava02 {
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
        config.addWeaveExporter();
        config.setVerbose(true);
    })
    ```
    <!--- KNIT exampleWeaveExporterJava02.java -->

When visualized in W&B Weave, the trace appears as follows:
![W&B Weave traces](img/opentelemetry-weave-exporter-light.png#only-light)
![W&B Weave traces](img/opentelemetry-weave-exporter-dark.png#only-dark)

For more details, see the official [Weave OpenTelemetry Docs](https://weave-docs.wandb.ai/guides/tracking/otel/).

---

## Troubleshooting

### No traces appear in Weave
- Confirm that `WEAVE_API_KEY`, `WEAVE_ENTITY`, and `WEAVE_PROJECT_NAME` are set in your environment.
- Ensure that your W&B account has access to the specified entity and project.

### Authentication errors
- Check that your `WEAVE_API_KEY` is valid.
- API key must have permission to write traces for the selected entity.

### Connection issues
- Make sure your environment has network access to W&B’s OpenTelemetry ingestion endpoints.