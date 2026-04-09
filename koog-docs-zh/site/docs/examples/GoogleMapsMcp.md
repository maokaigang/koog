<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:56:57+00:00", "source_path": "examples/GoogleMapsMcp.md", "source_sha256": "cd76216fc578b8c505ac5a66704665613c2f42d0c9ccd193836dad099a3d732d", "source_tag": "0.7.3", "translation_status": "changed"} -->
# Google 地图 MCP 与 Koog：从零到海拔的 Kotlin 笔记本之旅 { #google-maps-mcp-with-koog-from-zero-to-elevation-in-a-kotlin-notebook }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/GoogleMapsMcp.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/GoogleMapsMcp.ipynb
){ .md-button }

在这篇简短的、博客风格的教程中，我们将把 Koog 连接到用于 Google 地图的模型上下文协议（MCP）服务器。我们将使用 Docker 启动服务器，发现可用的工具，并让一个 AI 智能体对地址进行地理编码并获取其海拔高度——所有这些都从一个 Kotlin 笔记本中完成。

到最后，你将拥有一个可重现的端到端示例，可以将其融入你的工作流或文档中。

```kotlin
%useLatestDescriptors
%use koog

```

## 先决条件 { #prerequisites }
在运行下面的单元格之前，请确保你具备：

- 已安装并正在运行的 Docker
- 一个有效的 Google 地图 API 密钥，已导出为环境变量：`GOOGLE_MAPS_API_KEY`
- 一个 OpenAI API 密钥，已导出为 `OPENAI_API_KEY`

你可以在你的 shell 中这样设置它们（macOS/Linux 示例）：

```bash
export GOOGLE_MAPS_API_KEY="<your-key>"
export OPENAI_API_KEY="<your-openai-key>"
```

```kotlin
// Get the API key from environment variables
val googleMapsApiKey = System.getenv("GOOGLE_MAPS_API_KEY") ?: error("GOOGLE_MAPS_API_KEY environment variable not set")
val openAIApiToken = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY environment variable not set")

```

## 启动 Google 地图 MCP 服务器（Docker） { #start-the-google-maps-mcp-server-docker }
我们将使用官方的 `mcp/google-maps` 镜像。该容器将通过 MCP 公开诸如 `maps_geocode` 和 `maps_elevation` 等工具。我们通过环境变量传递 API 密钥，并以附加模式启动它，以便笔记本可以通过标准输入/输出与其通信。

```kotlin
// Start the Docker container with the Google Maps MCP server
val process = ProcessBuilder(
    "docker",
    "run",
    "-i",
    "-e",
    "GOOGLE_MAPS_API_KEY=$googleMapsApiKey",
    "mcp/google-maps"
).start()

```

## 通过 McpToolRegistry 发现工具 { #discover-tools-via-mcptoolregistry }
Koog 可以通过标准输入/输出连接到 MCP 服务器。在这里，我们从正在运行的进程创建一个工具注册表，并打印出发现的工具及其描述符。

```kotlin
val toolRegistry = McpToolRegistryProvider.fromTransport(
    transport = McpToolRegistryProvider.defaultStdioTransport(process)
)
toolRegistry.tools.forEach {
    println(it.name)
    println(it.descriptor)
}

```

## 使用 OpenAI 构建 AI 智能体 { #build-an-ai-agent-with-openai }
接下来，我们组装一个由 OpenAI 执行器和模型支持的简单智能体。该智能体将能够通过我们刚刚创建的注册表调用 MCP 服务器公开的工具。

```kotlin
val agent = AIAgent(
    executor = simpleOpenAIExecutor(openAIApiToken),
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry,
)

```

## 请求海拔高度：先地理编码，再获取海拔 { #ask-for-elevation-geocode-first-then-elevation }
我们提示智能体查找 JetBrains 慕尼黑办公室的海拔高度。指令明确告诉智能体仅使用可用的工具，并优先使用哪些工具来完成此任务。

```kotlin
import kotlinx.coroutines.runBlocking

val request = "Get elevation of the Jetbrains Office in Munich, Germany?"
runBlocking {
    agent.run(
        request +
            "You can only call tools. Get it by calling maps_geocode and maps_elevation tools."
    )
}

```

## 清理 { #clean-up }
完成后，停止 Docker 进程，以免在后台留下任何运行中的内容。

```kotlin
process.destroy()

```

## 故障排除与后续步骤 { #troubleshooting-and-next-steps }
- 如果容器启动失败，请检查 Docker 是否正在运行以及你的 `GOOGLE_MAPS_API_KEY` 是否有效。
- 如果智能体无法调用工具，请重新运行发现单元格以确保工具注册表已填充。
- 尝试使用可用的 Google 地图工具进行其他提示，例如路线规划或地点搜索。

接下来，可以考虑组合多个 MCP 服务器（例如，用于 Web 自动化的 Playwright + Google 地图），并让 Koog 协调工具使用，以完成更丰富的任务。