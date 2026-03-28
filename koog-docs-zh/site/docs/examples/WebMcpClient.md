<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:57:55+00:00", "source_path": "examples/WebMcpClient.md", "source_sha256": "c06b0dbd4076832b1ffd47228fd8d1a6fe7cfbbf8c106f14393e064eff5926f5", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 使用 Bright Data 的 The Web MCP 和 Koog 进行网络爬取 { #web-scraping-with-the-web-mcp-by-bright-data-and-koog }

[:material-github: 在 GitHub 上打开](https://github.com/JetBrains/koog/blob/develop/examples/bright-data-mcp/){ .md-button .md-button--primary }
[:material-download: 下载 .kt](https://raw.githubusercontent.com/JetBrains/koog/develop/examples/bright-data-mcp/Main.kt){ .md-button }

在本教程中，您将把一个 Koog 代理连接到 Bright Data 的 Web MCP 服务器，并让它执行网络爬取和数据收集任务。我们将演示如何通过 Model Context Protocol，利用 Bright Data 强大的网络爬取基础设施搜索关于 Koog.ai 的信息。

我们将保持简单和可复现性，专注于一个最小化但实用的代理 + 工具设置，您可以将其适配到自己的网络爬取需求中。

## 先决条件 { #prerequisites }

- 一个已导出为环境变量的 OpenAI API 密钥：`OPENAI_API_KEY`
- 一个已导出为环境变量的 Bright Data API 令牌：`BRIGHT_DATA_API_TOKEN`
- 您的 PATH 上已安装 Node.js 和 npx
- 具有 Koog 依赖项的 Kotlin 开发环境

**提示**：Bright Data MCP 服务器提供对企业级网络爬取工具的访问，这些工具可以处理复杂的网站、验证码和反机器人措施。

## 1) 设置您的 API 凭据 { #1-set-up-your-api-credentials }

我们从环境变量中读取两个 API 密钥，以确保密钥安全且不暴露在代码中。

```kotlin
// Get API keys from environment variables
val openAIApiKey = System.getenv("OPENAI_API_KEY")
    ?: error("OPENAI_API_KEY environment variable is not set")
val brightDataToken = System.getenv("BRIGHT_DATA_API_TOKEN")
    ?: error("BRIGHT_DATA_API_TOKEN environment variable is not set")
```

## 2) 启动 Bright Data 的 The Web MCP 服务器 { #2-start-the-web-mcp-server-by-bright-data }

我们将使用 `npx` 启动 Bright Data 的 MCP 服务器，并使用您的 API 令牌进行配置。该服务器将通过 Model Context Protocol 暴露网络爬取能力。

```kotlin
println("Starting Bright Data MCP server...")

// Start the Bright Data MCP server as a separate process
val processBuilder = ProcessBuilder("npx", "@brightdata/mcp")

// Set the API_TOKEN environment variable for the MCP server process
val environment = processBuilder.environment()
environment["API_TOKEN"] = brightDataToken

// Start the process
val process = processBuilder.start()

// Give the process a moment to start
Thread.sleep(2000)
```

## 3) 从 Koog 连接并创建代理 { #3-connect-from-koog-and-create-the-agent }

我们构建一个带有 OpenAI 执行器的 Koog `AIAgent`，并通过 STDIO 传输将其工具注册表连接到 Bright Data MCP 服务器。然后，我们将探索可用的工具并运行一个网络爬取任务。

```kotlin
println("Creating STDIO transport...")
try {
    // Create the STDIO transport
    val transport = McpToolRegistryProvider.defaultStdioTransport(process)
    
    println("Creating tool registry...")
    
    // Create a tool registry with tools from the Bright Data MCP server
    val toolRegistry = McpToolRegistryProvider.fromTransport(
        transport = transport,
        name = "bright-data-client",
        version = "1.0.0"
    )
    
    // Print available tools (optional - for debugging)
    println("Available tools from Bright Data MCP server:")
    toolRegistry.tools.forEach { tool ->
        println("- ${tool.name}")
    }
    
    // Create the agent with MCP tools
    val agent = AIAgent(
        executor = simpleOpenAIExecutor(openAIApiKey),
        systemPrompt = "You are a helpful assistant with access to web scraping and data collection tools from Bright Data. You can help users gather information from websites, analyze web data, and provide insights.",
        llmModel = OpenAIModels.Chat.GPT4o,
        temperature = 0.7,
        toolRegistry = toolRegistry,
        maxIterations = 100
    )
    
    val result = agent.run("Please search for Koog.ai and tell me what is it and who invented it")
    
    println("\nAgent response:")
    println(result)
    
} catch (e: Exception) {
    println("Error: ${e.message}")
    e.printStackTrace()
} finally {
    println("Shutting down MCP server...")
    process.destroyForcibly()
}
```

## 4) 完整代码示例 { #4-complete-code-example }

以下是演示使用 Bright Data 的 The Web MCP 进行网络爬取的完整工作示例：

```kotlin
package koog

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import kotlinx.coroutines.runBlocking

/**
 * The entry point of the program demonstrating AI-driven web scraping and data collection.
 *
 * This function initializes a Bright Data MCP server, sets up tool integration,
 * and defines an AI agent for interacting with web scraping tools. It demonstrates the
 * following key operations:
 *
 * 1. Starts the Bright Data MCP server using a subprocess with proper API token configuration.
 * 2. Configures a registry of tools from the MCP server via STDIO transport communication.
 * 3. Creates an AI agent leveraging OpenAI's GPT-4o model with web scraping capabilities.
 * 4. Runs the agent to perform a specified task (e.g., searching for and analyzing web content
 *    about Koog.ai).
 * 5. Cleans up by shutting down the MCP server process after execution.
 *
 * This function is intended for tutorial purposes, demonstrating how to integrate
 * MCP (Model Context Protocol) servers with AI agents for web data collection and analysis.
 * It requires OPENAI_API_KEY and BRIGHT_DATA_API_TOKEN environment variables to be set.
 */
fun main() = runBlocking {
    // Get API keys from environment variables
    val openAIApiKey = System.getenv("OPENAI_API_KEY")
        ?: error("OPENAI_API_KEY environment variable is not set")
    val brightDataToken = System.getenv("BRIGHT_DATA_API_TOKEN")
        ?: error("BRIGHT_DATA_API_TOKEN environment variable is not set")

    println("Starting Bright Data MCP server...")

    // Start the Bright Data MCP server as a separate process
    val processBuilder = ProcessBuilder("npx", "@brightdata/mcp")

    // Set the API_TOKEN environment variable for the MCP server process
    val environment = processBuilder.environment()
    environment["API_TOKEN"] = brightDataToken

    // Start the process
    val process = processBuilder.start()

    // Give the process a moment to start
    Thread.sleep(2000)

    println("Creating STDIO transport...")

    try {
        // Create the STDIO transport
        val transport = McpToolRegistryProvider.defaultStdioTransport(process)
        
        println("Creating tool registry...")
        
        // Create a tool registry with tools from the Bright Data MCP server
        val toolRegistry = McpToolRegistryProvider.fromTransport(
            transport = transport,
            name = "bright-data-client",
            version = "1.0.0"
        )
        
        // Print available tools (optional - for debugging)
        println("Available tools from Bright Data MCP server:")
        toolRegistry.tools.forEach { tool ->
            println("- ${tool.name}")
        }
        
        // Create the agent with MCP tools
        val agent = AIAgent(
            executor = simpleOpenAIExecutor(openAIApiKey),
            systemPrompt = "You are a helpful assistant with access to web scraping and data collection tools from Bright Data. You can help users gather information from websites, analyze web data, and provide insights.",
            llmModel = OpenAIModels.Chat.GPT4o,
            temperature = 0.7,
            toolRegistry = toolRegistry,
            maxIterations = 100
        )
        
        val result = agent.run("Please search for Koog.ai and tell me what is it and who invented it")
        
        println("\nAgent response:")
        println(result)
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        println("Shutting down MCP server...")
        process.destroyForcibly()
    }
}
```

## 故障排除 { #troubleshooting }

- **连接问题**：如果代理无法连接到 MCP 服务器，请确保已通过 `npx @brightdata/mcp` 正确安装了 Bright Data MCP 包。
- **API 令牌错误**：请仔细检查您的 `BRIGHT_DATA_API_TOKEN` 是否有效，并具有网络爬取所需的必要权限。
- **OpenAI 身份验证**：请验证您的 `OPENAI_API_KEY` 环境变量是否正确设置，并且 API 密钥有效。
- **进程超时**：如果服务器启动时间较长，请增加 `Thread.sleep(2000)` 时长。

## 后续步骤 { #next-steps }

- **探索不同的查询**：尝试爬取不同的网站或搜索各种主题。
- **自定义工具集成**：在 Bright Data 的网络爬取能力之外添加您自己的工具。
- **高级爬取**：利用 Bright Data 的高级功能，如住宅代理、CAPTCHA 解决和 JavaScript 渲染。
- **数据处理**：将爬取的数据与其他 Koog 代理结合进行分析和洞察。
- **生产部署**：将此模式集成到您的应用程序中，实现自动化的网络数据收集。## 所学内容

本教程演示了如何：
- 设置并配置 Bright Data 的 The Web MCP
- 通过 STDIO 传输将 Koog AI 代理连接到外部 MCP 服务器
- 使用自然语言指令执行 AI 驱动的网页抓取任务
- 正确处理资源清理和错误管理
- 为生产级网页抓取应用程序构建代码结构

将 Koog 的 AI 代理能力与 Bright Data 的企业级网页抓取基础设施相结合，为自动化数据收集和分析工作流提供了强大的基础。