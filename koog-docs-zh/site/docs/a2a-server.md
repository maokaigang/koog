<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T16:29:59+00:00", "source_path": "a2a-server.md", "source_sha256": "ed11ef756b1734357683b2fc89abb7ab68e233e54fbda630ef4e752501f3d2d7", "source_tag": "0.7.3", "translation_status": "changed"} -->
# A2A 服务器 { #a2a-server }

A2A 服务器使您能够通过标准化的 A2A（Agent-to-Agent）协议公开 AI 智能体。它完整实现了 [A2A 协议规范](https://a2a-protocol.org/latest/specification/)，处理客户端请求、执行智能体逻辑、管理复杂任务生命周期，并支持实时流式响应。

## 依赖项 { #dependencies }

要在您的项目中使用 A2A 服务器，请将以下依赖项添加到您的 `build.gradle.kts` 中：

```kotlin
dependencies {
    // Core A2A server library
    implementation("ai.koog:a2a-server:$koogVersion")

    // HTTP JSON-RPC transport (most common)
    implementation("ai.koog:a2a-transport-server-jsonrpc-http:$koogVersion")

    // Ktor server engine (choose one that fits your needs)
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
}
```

## 概述 { #overview }

A2A 服务器充当 A2A 协议传输层与您的自定义智能体逻辑之间的桥梁。
它编排整个请求生命周期，同时保持协议合规性并提供稳健的会话管理。

## 核心组件 { #core-components }

### A2AServer { #a2aserver }

实现完整 A2A 协议的主服务器类。它作为中央协调器，负责：

- **验证** 传入请求是否符合协议规范
- **管理** 并发会话和任务生命周期
- **编排** 传输层、存储层和业务逻辑层之间的通信
- **处理** 所有协议操作：消息发送、任务查询、取消、推送通知

`A2AServer` 接受两个必需参数：

* `AgentExecutor`：定义智能体的业务逻辑实现
* `AgentCard`：定义智能体的能力和元数据

以及多个可选参数，可用于自定义其存储和传输行为。

### AgentExecutor { #agentexecutor }

`AgentExecutor` 接口是您实现智能体核心业务逻辑的地方。
它充当 A2A 协议与您特定 AI 智能体能力之间的桥梁。
要启动智能体的执行，您必须实现 `execute` 方法，在其中定义您的智能体逻辑。
要取消智能体，您必须实现 `cancel` 方法。

```kotlin
class MyAgentExecutor : AgentExecutor {
    override suspend fun execute(
        context: RequestContext<MessageSendParams>,
        eventProcessor: SessionEventProcessor
    ) {
        // Agent logic here
    }

    override suspend fun cancel(
        context: RequestContext<TaskIdParams>,
        eventProcessor: SessionEventProcessor,
        agentJob: Deferred<Unit>?
    ) {
        // Cancel agent here, optional
    }
}
```

`RequestContext` 提供了有关当前请求的丰富信息，
包括当前会话的 `contextId` 和 `taskId`、发送的 `message` 以及请求的 `params`。

`SessionEventProcessor` 与客户端通信：

- **`sendMessage(message)`**：发送即时响应（聊天式交互）
- **`sendTaskEvent(event)`**：发送任务相关更新（长时间运行的操作）

```kotlin
// For immediate responses (like chatbots)
eventProcessor.sendMessage(
    Message(
        messageId = generateId(),
        role = Role.Agent,
        parts = listOf(TextPart("Here's your answer!")),
        contextId = context.contextId
    )
)

// For task-based operations
eventProcessor.sendTaskEvent(
    TaskStatusUpdateEvent(
        contextId = context.contextId,
        taskId = context.taskId,
        status = TaskStatus(
            state = TaskState.Working,
            message = Message(/* progress update */),
            timestamp = Clock.System.now()
        ),
        final = false  // More updates to come
    )
)
```

### AgentCard { #agentcard }

`AgentCard` 充当您智能体的自描述清单。它告知客户端您的智能体能做什么、如何与其通信以及有哪些安全要求。

```kotlin
val agentCard = AgentCard(
    // Basic Identity
    name = "Advanced Recipe Assistant",
    description = "AI agent specialized in cooking advice, recipe generation, and meal planning",
    version = "2.1.0",
    protocolVersion = "0.3.0",

    // Communication Settings
    url = "https://api.example.com/a2a",
    preferredTransport = TransportProtocol.JSONRPC,

    // Optional: Multiple transport support
    additionalInterfaces = listOf(
        AgentInterface("https://api.example.com/a2a", TransportProtocol.JSONRPC),
    ),

    // Capabilities Declaration
    capabilities = AgentCapabilities(
        streaming = true,              // Support real-time responses
        pushNotifications = true,      // Send async notifications
        stateTransitionHistory = true  // Maintain task history
    ),

    // Content Type Support
    defaultInputModes = listOf("text/plain", "text/markdown", "image/jpeg"),
    defaultOutputModes = listOf("text/plain", "text/markdown", "application/json"),

    // Define available security schemes
    securitySchemes = mapOf(
        "bearer" to HTTPAuthSecurityScheme(
            scheme = "Bearer",
            bearerFormat = "JWT",
            description = "JWT token authentication"
        ),
        "api-key" to APIKeySecurityScheme(
            `in` = In.Header,
            name = "X-API-Key",
            description = "API key for service authentication"
        )
    ),

    // Specify security requirements (logical OR of requirements)
    security = listOf(
        mapOf("bearer" to listOf("read", "write")),  // Option 1: JWT with read/write scopes
        mapOf("api-key" to emptyList())              // Option 2: API key
    ),

    // Enable extended card for authenticated users
    supportsAuthenticatedExtendedCard = true,
    
    // Skills/Capabilities
    skills = listOf(
        AgentSkill(
            id = "recipe-generation",
            name = "Recipe Generation",
            description = "Generate custom recipes based on ingredients, dietary restrictions, and preferences",
            tags = listOf("cooking", "recipes", "nutrition"),
            examples = listOf(
                "Create a vegan pasta recipe with mushrooms",
                "I have chicken, rice, and vegetables. What can I make?"
            )
        ),
        AgentSkill(
            id = "meal-planning",
            name = "Meal Planning",
            description = "Plan weekly meals and generate shopping lists",
            tags = listOf("meal-planning", "nutrition", "shopping")
        )
    ),

    // Optional: Branding
    iconUrl = "https://example.com/agent-icon.png",
    documentationUrl = "https://docs.example.com/recipe-agent",
    provider = AgentProvider(
        organization = "CookingAI Inc.",
        url = "https://cookingai.com"
    )
)
```

### 传输层 { #transport-layer }

A2A 本身支持多种传输协议与客户端通信。
目前，Koog 提供了基于 HTTP 的 JSON-RPC 服务器传输实现。

#### HTTP JSON-RPC 传输 { #http-json-rpc-transport }

```kotlin
val transport = HttpJSONRPCServerTransport(server)
transport.start(
    engineFactory = CIO,           // Ktor engine (CIO, Netty, Jetty)
    port = 8080,                   // Server port
    path = "/a2a",                 // API endpoint path
    wait = true                    // Block until server stops
)
```

### 存储 { #storage }

A2A 服务器采用可插拔的存储架构，分离不同类型的数据。
所有存储实现都是可选的，默认情况下使用内存版本用于开发。- **TaskStorage**：任务生命周期管理 - 存储并管理任务状态、历史记录与产物
- **MessageStorage**：对话历史管理 - 在会话上下文中管理消息历史
- **PushNotificationConfigStorage**：Webhook 管理 - 管理异步通知的 Webhook 配置

## 快速开始 { #quickstart }

### 1. 创建 AgentCard { #1-create-agentcard }
定义智能体的能力与元数据。
```kotlin
val agentCard = AgentCard(
    name = "IO Assistant",
    description = "AI agent specialized in input modification",
    version = "2.1.0",
    protocolVersion = "0.3.0",

    // Communication Settings
    url = "https://api.example.com/a2a",
    preferredTransport = TransportProtocol.JSONRPC,

    // Capabilities Declaration
    capabilities =
        AgentCapabilities(
            streaming = true,              // Support real-time responses
            pushNotifications = true,      // Send async notifications
            stateTransitionHistory = true  // Maintain task history
        ),

    // Content Type Support
    defaultInputModes = listOf("text/plain", "text/markdown", "image/jpeg"),
    defaultOutputModes = listOf("text/plain", "text/markdown", "application/json"),

    // Skills/Capabilities
    skills = listOf(
        AgentSkill(
            id = "echo",
            name = "echo",
            description = "Echoes back user messages",
            tags = listOf("io"),
        )
    )
)
```

### 2. 创建 AgentExecutor { #2-create-an-agentexecutor }
执行器负责实现智能体逻辑，处理传入请求并发送响应。

```kotlin
class EchoAgentExecutor : AgentExecutor {
    override suspend fun execute(
        context: RequestContext<MessageSendParams>,
        eventProcessor: SessionEventProcessor
    ) {
        val userMessage = context.params.message
        val userText = userMessage.parts
            .filterIsInstance<TextPart>()
            .joinToString(" ") { it.text }

        // Echo the user's message back
        val response = Message(
            messageId = UUID.randomUUID().toString(),
            role = Role.Agent,
            parts = listOf(TextPart("You said: $userText")),
            contextId = context.contextId,
            taskId = context.taskId
        )

        eventProcessor.sendMessage(response)
    }
}
```

### 2. 创建服务器 { #2-create-the-server }
将智能体执行器与智能体卡片传入服务器。

```kotlin
val server = A2AServer(
    agentExecutor = EchoAgentExecutor(),
    agentCard = agentCard
)
```

### 3. 添加传输层 { #3-add-transport-layer }
创建传输层并启动服务器。
```kotlin
// HTTP JSON-RPC transport
val transport = HttpJSONRPCServerTransport(server)
transport.start(
    engineFactory = CIO,
    port = 8080,
    path = "/agent",
    wait = true
)
```

## 智能体实现模式 { #agent-implementation-patterns }

### 简单响应型智能体 { #simple-response-agent }
若智能体仅需对单条消息进行响应，可将其实现为简单智能体。
该模式同样适用于执行逻辑不复杂且耗时较短的场景。

```kotlin
class SimpleAgentExecutor : AgentExecutor {
    override suspend fun execute(
        context: RequestContext<MessageSendParams>,
        eventProcessor: SessionEventProcessor
    ) {
        val response = Message(
            messageId = UUID.randomUUID().toString(),
            role = Role.Agent,
            parts = listOf(TextPart("Hello from agent!")),
            contextId = context.contextId,
            taskId = context.taskId
        )

        eventProcessor.sendMessage(response)
    }
}
```

### 任务型智能体 { #task-based-agent }
若智能体的执行逻辑复杂且需要多步骤处理，可将其实现为任务型智能体。
该模式同样适用于执行逻辑耗时较长且需要暂停的场景。

```kotlin
class TaskAgentExecutor : AgentExecutor {
    override suspend fun execute(
        context: RequestContext<MessageSendParams>,
        eventProcessor: SessionEventProcessor
    ) {
        // Send working status
        eventProcessor.sendTaskEvent(
            TaskStatusUpdateEvent(
                contextId = context.contextId,
                taskId = context.taskId,
                status = TaskStatus(
                    state = TaskState.Working,
                    timestamp = Clock.System.now()
                ),
                final = false
            )
        )

        // Do work...

        // Send completion
        eventProcessor.sendTaskEvent(
            TaskStatusUpdateEvent(
                contextId = context.contextId,
                taskId = context.taskId,
                status = TaskStatus(
                    state = TaskState.Completed,
                    timestamp = Clock.System.now()
                ),
                final = true
            )
        )
    }
}
```