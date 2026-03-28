<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:58:38+00:00", "source_path": "examples/OpenTelemetry.md", "source_sha256": "f87f983112b79ab18fa0d0ef4662f3776d7f8364c99153392afed561a6694357", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 使用 Koog 进行 OpenTelemetry：追踪你的 AI 智能体 { #opentelemetry-with-koog-tracing-your-ai-agent }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/OpenTelemetry.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/OpenTelemetry.ipynb
){ .md-button }

本笔记本演示如何为 Koog AI 智能体添加基于 OpenTelemetry 的追踪功能。我们将：
- 将跨度（spans）输出到控制台，便于本地快速调试。
- 将跨度导出到 OpenTelemetry Collector，并在 Jaeger 中查看。

前提条件：
- 已安装 Docker/Docker Compose
- 环境变量 `OPENAI_API_KEY` 中已设置可用的 OpenAI API 密钥

在运行笔记本之前，请先启动本地 OpenTelemetry 服务栈（Collector + Jaeger）：
```bash
./docker-compose up -d
```
智能体运行后，打开 Jaeger UI：
- http://localhost:16686

稍后停止服务：
```bash
docker-compose down
```

---

```kotlin
%useLatestDescriptors
// %use koog
```

```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter

```

## 配置 OpenTelemetry 导出器 { #configure-opentelemetry-exporters }

在下一个单元格中，我们将：
- 创建一个 Koog AIAgent
- 安装 OpenTelemetry 功能
- 添加两个跨度导出器：
  - LoggingSpanExporter（用于控制台日志）
  - OTLP gRPC 导出器，指向 http://localhost:4317（Collector）

这对应了示例描述：控制台日志用于本地调试，OTLP 用于在 Jaeger 中查看追踪。

```kotlin
val agent = AIAgent(
    executor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    llmModel = OpenAIModels.Chat.GPT4oMini,
    systemPrompt = "You are a code assistant. Provide concise code examples."
) {
    install(OpenTelemetry) {
        // Add a console logger for local debugging
        addSpanExporter(LoggingSpanExporter.create())

        // Send traces to OpenTelemetry collector
        addSpanExporter(
            OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:4317")
                .build()
        )
    }
}
```

## 运行智能体并在 Jaeger 中查看追踪 { #run-the-agent-and-view-traces-in-jaeger }

执行下一个单元格以触发一个简单的提示。你应该会看到：
- 来自 LoggingSpanExporter 的控制台跨度日志
- 追踪数据导出到本地 OpenTelemetry Collector，并可在 Jaeger 中查看，地址为 http://localhost:16686

提示：运行单元格后，使用 Jaeger 搜索功能查找最近的追踪。

```kotlin
import ai.koog.agents.utils.use
import kotlinx.coroutines.runBlocking

runBlocking {
    agent.use { agent ->
        println("Running agent with OpenTelemetry tracing...")

        val result = agent.run("Tell me a joke about programming")

        "Agent run completed with result: '$result'.\nCheck Jaeger UI at http://localhost:16686 to view traces"
    }
}
```

## 清理与故障排除 { #cleanup-and-troubleshooting }

完成后：

- 停止服务：
  ```bash
  docker-compose down
  ```

- 如果在 Jaeger 中看不到追踪数据：
  - 确保服务栈正在运行：`./docker-compose up -d`，并等待几秒让其完全启动。
  - 验证端口：
    - Collector（OTLP gRPC）：http://localhost:4317
    - Jaeger UI：http://localhost:16686
  - 检查容器日志：`docker-compose logs --tail=200`
  - 确认运行笔记本的环境已设置 `OPENAI_API_KEY`。
  - 确保导出器中的端点与 Collector 匹配：`http://localhost:4317`。

- 预期会看到的跨度：
  - Koog 智能体生命周期
  - LLM 请求/响应元数据
  - 任何工具执行跨度（如果添加了工具）

现在你可以迭代开发你的智能体，并在追踪管道中观察变化。