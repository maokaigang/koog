<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T16:29:03+00:00", "source_path": "a2a-koog-integration.md", "source_sha256": "a5c28e05c7ba37416bfeb404ff6c4f5bfd6fb81b4962f4202812ce9756d5b36d", "source_tag": "0.7.3", "translation_status": "changed"} -->
# A2A 与 Koog 集成 { #a2a-and-koog-integration }

Koog 提供与 A2A 协议的无缝集成，允许您将 Koog 智能体暴露为 A2A 服务器，并将
Koog 智能体连接到其他符合 A2A 规范的智能体。

## 依赖项 { #dependencies }

A2A Koog 集成需要根据您的使用场景添加特定的功能模块：

### 用于将 Koog 智能体暴露为 A2A 服务器 { #for-exposing-koog-agents-as-a2a-servers }

将以下依赖项添加到您的 `build.gradle.kts`：

```kotlin
dependencies {
    // Koog A2A server integration feature
    implementation("ai.koog:agents-features-a2a-server:$koogVersion")

    // HTTP JSON-RPC transport
    implementation("ai.koog:a2a-transport-server-jsonrpc-http:$koogVersion")

    // Ktor server engine (choose one that fits your needs)
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
}
```

### 用于将 Koog 智能体连接到 A2A 智能体 { #for-connecting-koog-agents-to-a2a-agents }

将以下依赖项添加到您的 `build.gradle.kts`：

```kotlin
dependencies {
    // Koog A2A client integration feature
    implementation("ai.koog:agents-features-a2a-client:$koogVersion")

    // HTTP JSON-RPC transport
    implementation("ai.koog:a2a-transport-client-jsonrpc-http:$koogVersion")

    // Ktor client engine (choose one that fits your needs)
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
}
```

## 概述 { #overview }

该集成支持两种主要模式：

1. **将 Koog 智能体暴露为 A2A 服务器** - 使您的 Koog 智能体可通过 A2A 协议被发现和访问
2. **将 Koog 智能体连接到 A2A 智能体** - 让您的 Koog 智能体与其他符合 A2A 规范的智能体通信

## 将 Koog 智能体暴露为 A2A 服务器 { #exposing-koog-agents-as-a2a-servers }

### 定义具有 A2A 功能的 Koog 智能体 { #define-koog-agent-with-a2a-feature }

首先定义一个 Koog 智能体。智能体的逻辑可以各不相同，但这里是一个带有工具的基本单次运行智能体示例。
该智能体接收来自用户的消息，将其转发给大语言模型。
如果大语言模型的响应包含工具调用，智能体执行该工具并将结果转发给大语言模型。
如果大语言模型的响应包含助手消息，智能体将助手消息发送给用户并结束。

在输入接收时，智能体向 A2A 客户端发送带有输入消息的任务提交事件。
在每次工具调用时，智能体向 A2A 客户端发送带有工具调用和结果的任务进行中事件。
在助手消息时，智能体向 A2A 客户端发送带有助手消息的任务完成事件。

```kotlin
/**
 * Create a Koog agent with A2A feature
 */
@OptIn(ExperimentalUuidApi::class)
private fun createAgent(
    context: RequestContext<MessageSendParams>,
    eventProcessor: SessionEventProcessor,
) = AIAgent(
    promptExecutor = MultiLLMPromptExecutor(
        LLMProvider.Google to GoogleLLMClient("api-key")
    ),
    toolRegistry = ToolRegistry {
        // Declare tools here
    },
    strategy = strategy<A2AMessage, Unit>("test") {
        val nodeSetup by node<A2AMessage, Unit> { inputMessage ->
            // Convenience function to transform A2A message into Koog message
            val input = inputMessage.toKoogMessage()
            llm.writeSession {
                appendPrompt {
                    message(input)
                }
            }
            // Send update event to A2A client
            withA2AAgentServer {
                sendTaskUpdate("Request submitted: ${input.content}", TaskState.Submitted)
            }
        }

        // Calling llm
        val nodeLLMRequest by node<Unit, Message> {
            llm.writeSession {
                requestLLM()
            }
        }

        // Executing tool
        val nodeProcessTool by node<Message.Tool.Call, Unit> { toolCall ->
            withA2AAgentServer {
                sendTaskUpdate("Executing tool: ${toolCall.content}", TaskState.Working)
            }

            val toolResult = environment.executeTool(toolCall)

            llm.writeSession {
                appendPrompt {
                    tool {
                        result(toolResult)
                    }
                }
            }
            withA2AAgentServer {
                sendTaskUpdate("Tool result: ${toolResult.content}", TaskState.Working)
            }
        }

        // Sending assistant message
        val nodeProcessAssistant by node<String, Unit> { assistantMessage ->
            withA2AAgentServer {
                sendTaskUpdate(assistantMessage, TaskState.Completed)
            }
        }

        edge(nodeStart forwardTo nodeSetup)
        edge(nodeSetup forwardTo nodeLLMRequest)

        // If a tool call is returned from llm, forward to the tool processing node and then back to llm
        edge(nodeLLMRequest forwardTo nodeProcessTool onToolCall { true })
        edge(nodeProcessTool forwardTo nodeLLMRequest)

        // If an assistant message is returned from llm, forward to the assistant processing node and then to finish
        edge(nodeLLMRequest forwardTo nodeProcessAssistant onAssistantMessage { true })
        edge(nodeProcessAssistant forwardTo nodeFinish)
    },
    agentConfig = AIAgentConfig(
        prompt = prompt("agent") { system("You are a helpful assistant.") },
        model = GoogleModels.Gemini2_5Pro,
        maxAgentIterations = 10
    ),
) {
    install(A2AAgentServer) {
        this.context = context
        this.eventProcessor = eventProcessor
    }
}

/**
 * Convenience function to send task update event to A2A client
 * @param content The message content
 * @param state The task state
 */
@OptIn(ExperimentalUuidApi::class)
private suspend fun A2AAgentServer.sendTaskUpdate(
    content: String,
    state: TaskState,
) {
    val message = A2AMessage(
        messageId = Uuid.random().toString(),
        role = Role.Agent,
        parts = listOf(
            TextPart(content)
        ),
        contextId = context.contextId,
        taskId = context.taskId,
    )

    val task = Task(
        id = context.taskId,
        contextId = context.contextId,
        status = TaskStatus(
            state = state,
            message = message,
            timestamp = Clock.System.now(),
        )
    )
    eventProcessor.sendTaskEvent(task)
}
```

## A2AAgentServer 功能机制 { #a2aagentserver-feature-mechanism }

`A2AAgentServer` 是一个 Koog 智能体功能，可实现 Koog 智能体与 A2A 协议之间的无缝集成。
`A2AAgentServer` 功能提供对 `RequestContext` 和 `SessionEventProcessor` 实体的访问，这些实体用于
在 Koog 智能体内与 A2A 客户端通信。

要安装该功能，请在智能体上调用 `install` 函数，并传入 `A2AAgentServer` 功能以及 `RequestContext` 和 `SessionEventProcessor`：
```kotlin
// Install the feature
install(A2AAgentServer) {
    this.context = context
    this.eventProcessor = eventProcessor
}
```

要从 Koog 智能体策略访问这些实体，该功能提供了一个 `withA2AAgentServer` 函数，允许智能体节点在其执行上下文中访问 A2A 服务器能力。
它检索已安装的 `A2AAgentServer` 功能，并将其作为操作块的接收器提供。

```kotlin
// Usage within agent nodes
withA2AAgentServer {
    // 'this' is now A2AAgentServer instance
    eventProcessor.sendTaskUpdate("Processing your request...", TaskState.Working)
}
```

### 启动 A2A 服务器 { #start-a2a-server }
启动服务器后，Koog 智能体将通过 A2A 协议被发现和访问。

```kotlin
val agentCard = AgentCard(
    name = "Koog Agent",
    url = "http://localhost:9999/koog",
    description = "Simple universal agent powered by Koog",
    version = "1.0.0",
    protocolVersion = "0.3.0",
    preferredTransport = TransportProtocol.JSONRPC,
    capabilities = AgentCapabilities(streaming = true),
    defaultInputModes = listOf("text"),
    defaultOutputModes = listOf("text"),
    skills = listOf(
        AgentSkill(
            id = "koog",
            name = "Koog Agent",
            description = "Universal agent powered by Koog. Supports tool calling.",
            tags = listOf("chat", "tool"),
        )
    )
)
// Server setup
val server = A2AServer(agentExecutor = KoogAgentExecutor(), agentCard = agentCard)
val transport = HttpJSONRPCServerTransport(server)
transport.start(engineFactory = Netty, port = 8080, path = "/chat", wait = true)
```

## 将 Koog 智能体连接到 A2A 智能体 { #connecting-koog-agents-to-a2a-agents }

### 创建 A2A 客户端并连接到 A2A 服务器 { #create-a2a-client-and-connect-to-the-a2a-server }

```kotlin
val transport = HttpJSONRPCClientTransport(url = "http://localhost:9999/koog")
val agentCardResolver =
    UrlAgentCardResolver(baseUrl = "http://localhost:9999", path = "/koog")
val client = A2AClient(transport = transport, agentCardResolver = agentCardResolver)

val agentId = "koog"
client.connect()
```### 创建 Koog 代理并将 A2A 客户端添加到 A2AAgentClient 功能
要从您的 Koog 代理连接到 A2A 代理，您可以使用 A2AAgentClient 功能，该功能提供了一个客户端 API 用于连接到 A2A 代理。
该客户端的原理与服务器相同：您安装该功能，并传递 `A2AAgentClient` 功能以及 `RequestContext` 和 `SessionEventProcessor`。

```kotlin
val agent = AIAgent(
    promptExecutor = MultiLLMPromptExecutor(
        LLMProvider.Google to GoogleLLMClient("api-key")
    ),
    toolRegistry = ToolRegistry {
        // declare tools here
    },
    strategy = strategy<String, Unit>("test") {

        val nodeCheckStreaming by nodeA2AClientGetAgentCard().transform { it.capabilities.streaming }

        val nodeA2ASendMessageStreaming by nodeA2AClientSendMessageStreaming()
        val nodeA2ASendMessage by nodeA2AClientSendMessage()

        val nodeProcessStreaming by node<Flow<Response<Event>>, Unit> {
            it.collect { response ->
                when (response.data) {
                    is Task -> {
                        // Process task
                    }

                    is A2AMessage -> {
                        // Process message
                    }

                    is TaskStatusUpdateEvent -> {
                        // Process task status update
                    }

                    is TaskArtifactUpdateEvent -> {
                        // Process task artifact update
                    }
                }
            }
        }

        val nodeProcessEvent by node<CommunicationEvent, Unit> { event ->
            when (event) {
                is Task -> {
                    // Process task
                }

                is A2AMessage -> {
                    // Process message
                }
            }
        }

        // If streaming is supported, send a message, process response and finish
        edge(nodeStart forwardTo nodeCheckStreaming transformed { agentId })
        edge(
            nodeCheckStreaming forwardTo nodeA2ASendMessageStreaming
                onCondition { it == true } transformed { buildA2ARequest(agentId) }
        )
        edge(nodeA2ASendMessageStreaming forwardTo nodeProcessStreaming)
        edge(nodeProcessStreaming forwardTo nodeFinish)

        // If streaming is not supported, send a message, process response and finish
        edge(
            nodeCheckStreaming forwardTo nodeA2ASendMessage
                onCondition { it == false } transformed { buildA2ARequest(agentId) }
        )
        edge(nodeA2ASendMessage forwardTo nodeProcessEvent)
        edge(nodeProcessEvent forwardTo nodeFinish)

        // If streaming is not supported, send a message, process response and finish
        edge(nodeCheckStreaming forwardTo nodeFinish onCondition { it == null }
            transformed { println("Failed to get agents card") }
        )

    },
    agentConfig = AIAgentConfig(
        prompt = prompt("agent") { system("You are a helpful assistant.") },
        model = GoogleModels.Gemini2_5Pro,
        maxAgentIterations = 10
    ),
) {
    install(A2AAgentClient) {
        this.a2aClients = mapOf(agentId to client)
    }
}


@OptIn(ExperimentalUuidApi::class)
private fun AIAgentGraphContextBase.buildA2ARequest(agentId: String): A2AClientRequest<MessageSendParams> =
    A2AClientRequest(
        agentId = agentId,
        callContext = ClientCallContext.Default,
        params = MessageSendParams(
            message = A2AMessage(
                messageId = Uuid.random().toString(),
                role = Role.User,
                parts = listOf(
                    TextPart(agentInput as String)
                )
            )
        )
    )
```