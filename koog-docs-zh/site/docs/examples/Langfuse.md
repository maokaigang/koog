<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:00:34+00:00", "source_path": "examples/Langfuse.md", "source_sha256": "2341e410672564c8788b08c505d58b5cbb9879f8eec42763cd0bdb0d882b4c51", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 通过 OpenTelemetry 将 Koog 智能体追踪至 Langfuse { #tracing-koog-agents-to-langfuse-with-opentelemetry }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/Langfuse.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/Langfuse.ipynb
){ .md-button }

本笔记本展示了如何使用 OpenTelemetry 将 Koog 智能体追踪数据导出到您的 Langfuse 实例。您将设置环境变量、运行一个简单的智能体，然后在 Langfuse 中检查跨度和追踪。

## 您将学习到 { #what-you-ll-learn }

- Koog 如何与 OpenTelemetry 集成以发出追踪数据
- 如何通过环境变量配置 Langfuse 导出器
- 如何运行智能体并在 Langfuse 中查看其追踪

## 前提条件 { #prerequisites }

- 一个 Langfuse 项目（托管 URL、公钥、私钥）
- 用于 LLM 执行器的 OpenAI API 密钥
- 在您的 shell 中设置的环境变量：

```bash
export OPENAI_API_KEY=sk-...
export LANGFUSE_HOST=https://cloud.langfuse.com # or your self-hosted URL
export LANGFUSE_PUBLIC_KEY=pk_...
export LANGFUSE_SECRET_KEY=sk_...
```



```kotlin
%useLatestDescriptors
//%use koog
```


```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor

/**
 * Example of Koog agents tracing to [Langfuse](https://langfuse.com/)
 *
 * Agent traces are exported to:
 * - Langfuse OTLP endpoint instance using [OtlpHttpSpanExporter]
 *
 * To run this example:
 *  1. Set up a Langfuse project and credentials as described [here](https://langfuse.com/docs/get-started#create-new-project-in-langfuse)
 *  2. Get Langfuse credentials as described [here](https://langfuse.com/faq/all/where-are-langfuse-api-keys)
 *  3. Set `LANGFUSE_HOST`, `LANGFUSE_PUBLIC_KEY`, and `LANGFUSE_SECRET_KEY` environment variables
 *
 * @see <a href="https://langfuse.com/docs/opentelemetry/get-started#opentelemetry-endpoint">Langfuse OpenTelemetry Docs</a>
 */
val agent = AIAgent(
    executor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    llmModel = OpenAIModels.Chat.GPT4oMini,
    systemPrompt = "You are a code assistant. Provide concise code examples."
) {
    install(OpenTelemetry) {
        addLangfuseExporter()
    }
}
```

## 配置智能体和 Langfuse 导出器 { #configure-the-agent-and-langfuse-exporter }

在下一个单元格中，我们将：

- 创建一个使用 OpenAI 作为 LLM 执行器的 AIAgent
- 安装 OpenTelemetry 功能并添加 Langfuse 导出器
- 依赖环境变量进行 Langfuse 配置

在底层，Koog 会为智能体生命周期、LLM 调用和工具执行（如果有）发出跨度。Langfuse 导出器通过 OpenTelemetry 端点将这些跨度发送到您的 Langfuse 实例。



```kotlin
import kotlinx.coroutines.runBlocking

println("Running agent with Langfuse tracing")

runBlocking {
    val result = agent.run("Tell me a joke about programming")
    "Result: $result\nSee traces on the Langfuse instance"
}

```

## 运行智能体并查看追踪 { #run-the-agent-and-view-traces }

执行下一个单元格以触发一个简单的提示。这将生成导出到您的 Langfuse 项目的跨度。

### 在 Langfuse 中查看的位置 { #where-to-look-in-langfuse }

1. 打开您的 Langfuse 仪表板并选择您的项目
2. 导航到追踪/跨度视图
3. 查找您运行此单元格时间附近的最新条目
4. 深入查看跨度以了解：
   - 智能体生命周期事件
   - LLM 请求/响应元数据
   - 错误（如果有）

### 故障排除 { #troubleshooting }

- 没有显示追踪？
  - 仔细检查 LANGFUSE_HOST、LANGFUSE_PUBLIC_KEY、LANGFUSE_SECRET_KEY
  - 确保您的网络允许出站 HTTPS 连接到 Langfuse 端点
  - 确认您的 Langfuse 项目处于活动状态且密钥属于正确的项目
- 身份验证错误
  - 在 Langfuse 中重新生成密钥并更新环境变量
- OpenAI 问题
  - 确认 OPENAI_API_KEY 已设置且有效