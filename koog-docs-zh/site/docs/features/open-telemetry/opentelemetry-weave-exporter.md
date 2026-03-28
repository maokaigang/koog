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

## 配置 { #configuration }

要启用 Weave 导出，请安装 **OpenTelemetry 功能** 并添加 `WeaveExporter`。 导出器通过 `OtlpHttpSpanExporter` 使用 Weave 的 OpenTelemetry 端点。

### 示例：启用 Weave 追踪的智能体 { #example-agent-with-weave-tracing }

=== "Kotlin"

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

=== "Java"

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

## 什么会被追踪 { #what-gets-traced }

启用后，Weave导出器会捕获与Koog通用OpenTelemetry集成相同的跨度，包括：

- **智能体生命周期事件**：启动、停止、错误
- **LLM 交互**：提示、完成、延迟
- **工具调用**：工具调用的执行轨迹
- **系统上下文**：元数据，例如模型名称、环境、Koog 版本

出于安全考虑，OpenTelemetry 的部分追踪内容默认会被屏蔽。若要在 Weave 中查看这些内容，请在 OpenTelemetry 配置中使用 [设置详细模式](opentelemetry-support.md#setverbose) 方法，并将其 `verbose` 参数设置为 `true`，具体操作如下：

=== "Kotlin"

    ```kotlin
    install(OpenTelemetry) {
        addWeaveExporter()
        setVerbose(true)
    }
    ```

=== "Java"

    ```java
    install(OpenTelemetry.Feature, config -> {
        config.addWeaveExporter();
        config.setVerbose(true);
    })
    ```

在 W&B Weave 中可视化时，追踪记录会呈现如下所示：
![W&B Weave traces](img/opentelemetry-weave-exporter-light.png#only-light)
![W&B Weave traces](img/opentelemetry-weave-exporter-dark.png#only-dark)

更多详情，请参阅官方[Weave OpenTelemetry 文档](https://weave-docs.wandb.ai/guides/tracking/otel/)。

---

## Troubleshooting

### Weave中没有出现任何追踪记录 { #no-traces-appear-in-weave }
- 确认您的环境中已设置 `WEAVE_API_KEY`、`WEAVE_ENTITY` 和 `WEAVE_PROJECT_NAME`。
- 确保您的 W&B 账户有权访问指定的实体和项目。

### 认证错误 { #authentication-errors }
- 检查你的 `WEAVE_API_KEY` 是否有效。
- API 密钥必须拥有为所选实体写入追踪的权限。

### 连接问题 { #connection-issues }
- 确保您的环境能够访问 W&B 的 OpenTelemetry 数据接收端点。