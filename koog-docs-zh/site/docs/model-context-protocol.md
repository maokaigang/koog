<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:02:36+00:00", "source_path": "model-context-protocol.md", "source_sha256": "c93d00387ee5fd6d4c8110b72c33dcc134de5aaeb74c77f5cc27cc88ab5aaeaa", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 模型上下文协议 { #model-context-protocol }

模型上下文协议（MCP）是一种标准化协议，允许AI智能体通过一致的接口与外部工具和服务进行交互。

MCP将工具和提示作为API端点暴露给AI智能体调用。每个工具具有特定的名称，并使用JSON架构格式描述其输入和输出的输入模式。

Koog框架提供与MCP服务器的集成，使您能够将MCP工具纳入您的Koog智能体中。

要了解更多关于该协议的信息，请参阅[模型上下文协议](https://modelcontextprotocol.io)文档。

## MCP服务器 { #mcp-servers }

MCP服务器实现了模型上下文协议，为AI智能体与工具和服务的交互提供了标准化方式。

您可以在[MCP市场](https://mcp.so/)或[MCP DockerHub](https://hub.docker.com/u/mcp)中找到现成的MCP服务器。

MCP服务器支持以下传输协议与智能体通信：

* 标准输入/输出（stdio）传输协议，用于与作为独立进程运行的MCP服务器通信。例如，Docker容器或CLI工具。
* 服务器发送事件（SSE）传输协议（可选），用于通过HTTP与MCP服务器通信。

## 与Koog的集成 { #integration-with-koog }

Koog框架通过[MCP SDK](https://github.com/modelcontextprotocol/kotlin-sdk)与MCP集成，并包含`agent-mcp`模块中提供的额外API扩展。

此集成使Koog智能体能够执行以下操作：

* 通过各种传输机制（stdio、SSE）连接到MCP服务器。
* 从MCP服务器检索可用工具。
* 将MCP工具转换为Koog工具接口。
* 将转换后的工具注册到工具注册表中。
* 使用LLM提供的参数调用MCP工具。

### 关键组件 { #key-components }

以下是Koog中MCP集成的主要组件：

| 组件                                                                                                                                                           | 描述                                                                                                |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| [`McpTool`](api:agents-mcp::ai.koog.agents.mcp.McpTool)                                                                          | 作为 Koog 工具接口与 MCP SDK 之间的桥梁。                  |                                                                              |
| [`McpToolDescriptorParser`](api:agents-mcp::ai.koog.agents.mcp.McpToolDescriptorParser)                                        | 将 MCP 工具定义解析为 Koog 工具描述符格式。                                          |
| [`McpToolRegistryProvider`](api:agents-mcp::ai.koog.agents.mcp.McpToolRegistryProvider) | 创建 MCP 工具注册表，通过多种传输机制（stdio、SSE）连接到 MCP 服务器。 |

## 快速开始 { #getting-started }

### 1. 设置 MCP 连接 { #1-set-up-an-mcp-connection }

要在 Koog 中使用 MCP，您需要建立连接：

1. 启动一个 MCP 服务器（可以作为进程、Docker 容器或 Web 服务运行）。
2. 创建与服务器通信的传输机制。

MCP 服务器支持 stdio 和 SSE 传输机制与智能体通信，因此您可以使用其中一种进行连接。

#### 使用 stdio 连接 { #connect-with-stdio }

当 MCP 服务器作为独立进程运行时，使用此协议。以下是通过 stdio 传输设置 MCP 连接的示例：

```kotlin
// Start an MCP server (for example, as a process)
val process = ProcessBuilder("path/to/mcp/server").start()

// Create the stdio transport
val transport = McpToolRegistryProvider.defaultStdioTransport(process)
```

#### 使用 SSE 连接 { #connect-with-sse }

当 MCP 服务器作为 Web 服务运行时，使用此协议。以下是通过 SSE 传输设置 MCP 连接的示例：

```kotlin
// Create the SSE transport
val transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931")
```

### 2. 创建工具注册表 { #2-create-a-tool-registry }

建立 MCP 连接后，您可以通过以下方式之一创建包含 MCP 服务器中工具的注册表：

* 使用提供的传输机制进行通信。例如：

```kotlin
// Create a tool registry with tools from the MCP server
val toolRegistry = McpToolRegistryProvider.fromTransport(
    transport = transport,
    serverInfo = McpServerInfo(url = "http://localhost:8931", command = "path/to/mcp/server"),
    name = "my-client",
    version = "1.0.0"
)
```

* 使用连接到 MCP 服务器的 MCP 客户端。例如：
```kotlin
// Create a tool registry from an existing MCP client
val toolRegistry = McpToolRegistryProvider.fromClient(
    mcpClient = existingMcpClient,
    serverInfo = McpServerInfo(url = "http://localhost:8931")
)
```

### 3. 与您的智能体集成 { #3-integrate-with-your-agent }

要在您的 Koog 智能体中使用 MCP 工具，您需要将工具注册表注册到智能体：
```kotlin
// Create an agent with the tools
val agent = AIAgent(
    promptExecutor = executor,
    strategy = strategy,
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry
)

// Run the agent with a task that uses an MCP tool
val result = agent.run("Use the MCP tool to perform a task")
```

[//]: # (## 直接使用 MCP 工具)

[//]: # ()
[//]: # (除了通过智能体运行工具外，您也可以直接运行它们：)

[//]: # ()
[//]: # (1. 从工具注册表中检索特定工具。)

[//]: # (2. 使用标准的 Koog 机制，通过特定参数运行该工具。)[//]: # ()
[//]: # (以下是一个示例：)

[//]: # ()

[//]: # (```kotlin)

[//]: # (// 获取一个工具)

[//]: # (val tool = toolRegistry.getTool&#40;"tool-name"&#41; as McpTool)

[//]: # ()
[//]: # (// 为工具创建参数)

[//]: # (val args = McpTool.Args&#40;buildJsonObject { )

[//]: # (    put&#40;"parameter1", JsonPrimitive&#40;"value1"&#41;&#41;)

[//]: # (    put&#40;"parameter2", JsonPrimitive&#40;"value2"&#41;&#41;)

[//]: # (}&#41;)

[//]: # ()
[//]: # (// 使用给定参数运行工具)

[//]: # (val toolResult = tool.execute&#40;args&#41;)

[//]: # ()
[//]: # (// 打印结果)

[//]: # (println&#40;toolResult&#41;)

[//]: # (```)

[//]: # ()

[//]: # ()
[//]: # (你也可以从注册表中检索所有可用的 MCP 工具：)

[//]: # ()
[//]: # ()

[//]: # ()

[//]: # (```kotlin)

[//]: # (// 获取所有工具)

[//]: # (val tools = toolRegistry.tools)

[//]: # (```)

[//]: # ()

## 使用示例 { #usage-examples }

### Google 地图 MCP 集成 { #google-maps-mcp-integration }

此示例演示了如何使用 MCP 连接到 [Google 地图](https://mcp.so/server/google-maps/modelcontextprotocol) 服务器以获取地理数据：

```kotlin
// Start the Docker container with the Google Maps MCP server
val process = ProcessBuilder(
    "docker", "run", "-i",
    "-e", "GOOGLE_MAPS_API_KEY=$googleMapsApiKey",
    "mcp/google-maps"
).start()

// Create the ToolRegistry with tools from the MCP server
val toolRegistry = McpToolRegistryProvider.fromProcess(process = process)

// Create and run the agent
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(openAIApiToken),
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry,
)
agent.run("Get elevation of the Jetbrains Office in Munich, Germany?")
```

### Playwright MCP 集成 { #playwright-mcp-integration }

此示例演示了如何使用 MCP 连接到 [Playwright](https://mcp.so/server/playwright-mcp/microsoft) 服务器以进行网页自动化：

```kotlin
// Start the Playwright MCP server
val process = ProcessBuilder(
    "npx", "@playwright/mcp@latest", "--port", "8931"
).start()

// Create the ToolRegistry with tools from the MCP server
val toolRegistry = McpToolRegistryProvider.fromSseUrl("http://localhost:8931")

// Create and run the agent
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(openAIApiToken),
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry,
)
agent.run("Open a browser, navigate to jetbrains.com, accept all cookies, click AI in toolbar")
```
