<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:57:22+00:00", "source_path": "examples/PlaywrightMcp.md", "source_sha256": "c32b312cb9569abe59fc485380b32358dda95540b2c68bd6154724ff833a5327", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 使用 Playwright MCP 和 Koog 驱动浏览器 { #drive-the-browser-with-playwright-mcp-and-koog }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/PlaywrightMcp.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/PlaywrightMcp.ipynb
){ .md-button }

在本笔记本中，您将连接一个 Koog 智能体到 Playwright 的模型上下文协议 (MCP) 服务器，并让它驱动一个真实的浏览器来完成一项任务：打开 jetbrains.com，接受 Cookie，并点击工具栏中的 AI 部分。

我们将保持简单和可复现性，专注于一个最小化但实用的智能体 + 工具设置，您可以发布并重复使用。

```kotlin
%useLatestDescriptors
%use koog

```

## 先决条件 { #prerequisites }
- 一个 OpenAI API 密钥，已导出为环境变量：`OPENAI_API_KEY`
- 您的 PATH 上已安装 Node.js 和 npx
- Kotlin Jupyter 笔记本环境，可通过 `%use koog` 使用 Koog

提示：在可见模式下运行 Playwright MCP 服务器，以观察浏览器自动执行步骤。

## 1) 提供您的 OpenAI API 密钥 { #1-provide-your-openai-api-key }
我们从 `OPENAI_API_KEY` 环境变量中读取 API 密钥。这可以避免将密钥暴露在笔记本中。

```kotlin
// Get the API key from environment variables
val openAIApiToken = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY environment variable not set")

```

## 2) 启动 Playwright MCP 服务器 { #2-start-the-playwright-mcp-server }
我们将使用 `npx` 在本地启动 Playwright 的 MCP 服务器。默认情况下，它将暴露一个 SSE 端点，我们可以从 Koog 连接到此端点。

```kotlin
// Start the Playwright MCP server via npx
val process = ProcessBuilder(
    "npx",
    "@playwright/mcp@latest",
    "--port",
    "8931"
).start()

```

## 3) 从 Koog 连接并运行智能体 { #3-connect-from-koog-and-run-the-agent }
我们构建一个最小化的 Koog `AIAgent`，其中包含一个 OpenAI 执行器，并将其工具注册表指向通过 SSE 连接的 MCP 服务器。然后，我们要求它严格通过工具完成浏览器任务。

```kotlin
import kotlinx.coroutines.runBlocking

runBlocking {
    println("Connecting to Playwright MCP server...")
    val toolRegistry = McpToolRegistryProvider.fromTransport(
        transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931")
    )
    println("Successfully connected to Playwright MCP server")

    // Create the agent
    val agent = AIAgent(
        executor = simpleOpenAIExecutor(openAIApiToken),
        llmModel = OpenAIModels.Chat.GPT4o,
        toolRegistry = toolRegistry,
    )

    val request = "Open a browser, navigate to jetbrains.com, accept all cookies, click AI in toolbar"
    println("Sending request: $request")

    agent.run(
        request + ". " +
            "You can only call tools. Use the Playwright tools to complete this task."
    )
}

```

## 4) 关闭 MCP 进程 { #4-shut-down-the-mcp-process }
始终在运行结束时清理外部进程。

```kotlin
// Shutdown the Playwright MCP process
println("Closing connection to Playwright MCP server")
process.destroy()

```

## 故障排除 { #troubleshooting }
- 如果智能体无法连接，请确保 MCP 服务器正在 `http://localhost:8931` 上运行。
- 如果看不到浏览器，请确保 Playwright 已安装并且能够在您的系统上启动浏览器。
- 如果从 OpenAI 收到身份验证错误，请仔细检查 `OPENAI_API_KEY` 环境变量。

## 后续步骤 { #next-steps }
- 尝试不同的网站或流程。MCP 服务器暴露了丰富的 Playwright 工具集。
- 更换 LLM 模型，或向 Koog 智能体添加更多工具。
- 将此流程集成到您的应用程序中，或将笔记本发布为文档。