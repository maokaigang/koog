<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:59:00+00:00", "source_path": "examples/Weave.md", "source_sha256": "013d3ffa83562aceef1327497fd2651d0111085bf88c4b2ed6deb4eeaaf07a77", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 为 Koog 代理启用 Weave 追踪 { #weave-tracing-for-koog-agents }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/Weave.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/Weave.ipynb
){ .md-button }

本笔记本演示了如何使用 OpenTelemetry (OTLP) 将 Koog 代理的追踪数据发送到 W&B Weave。
您将创建一个简单的 Koog `AIAgent`，启用 Weave 导出器，运行一个提示，并在 Weave UI 中查看丰富的追踪信息。

有关背景信息，请参阅 Weave OpenTelemetry 文档：https://weave-docs.wandb.ai/guides/tracking/otel/


## 先决条件 { #prerequisites }

在运行示例之前，请确保您已具备：

- 一个 Weave/W&B 账户：https://wandb.ai
- 您的 API 密钥（来自 https://wandb.ai/authorize）已作为环境变量暴露：`WEAVE_API_KEY`
- 您的 Weave 实体（团队或用户）名称已作为 `WEAVE_ENTITY` 暴露
  - 在您的 W&B 仪表板上查找：https://wandb.ai/home（左侧边栏的 "Teams"）
- 一个项目名称已作为 `WEAVE_PROJECT_NAME` 暴露（如果未设置，本示例将使用 `koog-tracing`）
- 一个 OpenAI API 密钥已作为 `OPENAI_API_KEY` 暴露，用于运行 Koog 代理

示例（macOS/Linux）：
```bash
export WEAVE_API_KEY=...  # required by Weave
export WEAVE_ENTITY=your-team-or-username
export WEAVE_PROJECT_NAME=koog-tracing
export OPENAI_API_KEY=...
```


## 笔记本设置 { #notebook-setup }

我们使用最新的 Kotlin Jupyter 描述符。如果您已将 Koog 预配置为 `%use` 插件，可以取消注释以下行。



```kotlin
%useLatestDescriptors
//%use koog

```

## 创建代理并启用 Weave 追踪 { #create-an-agent-and-enable-weave-tracing }

我们构建一个最小的 `AIAgent`，并使用 Weave 导出器安装 `OpenTelemetry` 功能。
该导出器会根据您的环境配置将 OTLP 跨度发送到 Weave：
- `WEAVE_API_KEY` — 对 Weave 的身份验证
- `WEAVE_ENTITY` — 拥有追踪的团队/用户
- `WEAVE_PROJECT_NAME` — 用于存储追踪的 Weave 项目



```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor

val entity = System.getenv()["WEAVE_ENTITY"] ?: throw IllegalArgumentException("WEAVE_ENTITY is not set")
val projectName = System.getenv()["WEAVE_PROJECT_NAME"] ?: "koog-tracing"

val agent = AIAgent(
    executor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    llmModel = OpenAIModels.Chat.GPT4oMini,
    systemPrompt = "You are a code assistant. Provide concise code examples."
) {
    install(OpenTelemetry) {
        addWeaveExporter(
            weaveEntity = entity,
            weaveProjectName = projectName
        )
    }
}

```

## 运行代理并在 Weave 中查看追踪 { #run-the-agent-and-view-traces-in-weave }

执行一个简单的提示。完成后，打开打印的链接以在 Weave 中查看追踪。
您应该能看到代理运行、模型调用和其他已检测操作的跨度。



```kotlin
import kotlinx.coroutines.runBlocking

println("Running agent with Weave tracing")

runBlocking {
    val result = agent.run("Tell me a joke about programming")
    "Result: $result\nSee traces on https://wandb.ai/$entity/$projectName/weave/traces"
}

```

## 故障排除 { #troubleshooting }

- 如果看不到追踪，请验证 `WEAVE_API_KEY`、`WEAVE_ENTITY` 和 `WEAVE_PROJECT_NAME` 是否已在您的环境中设置。
- 确保您的网络允许出站 HTTPS 连接到 Weave 的 OTLP 端点。
- 确认您的 OpenAI 密钥有效，并且所选模型可以从您的账户访问。