<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T16:28:27+00:00", "source_path": "a2a-client.md", "source_sha256": "112f6f65b4085d73ca4903aaeb5a66732d632f1c43b9520c02adc119aa5607d0", "source_tag": "0.7.3", "translation_status": "changed"} -->
# A2A 客户端 { #a2a-client }

A2A 客户端使您能够通过网络与符合 A2A 规范的智能体进行通信。
它完整实现了
[A2A 协议规范](https://a2a-protocol.org/latest/specification/)，处理智能体发现、消息
交换、任务管理以及实时流式响应。

## 依赖项 { #dependencies }

要在您的项目中使用 A2A 客户端，请将以下依赖项添加到您的 `build.gradle.kts` 中：

```kotlin
dependencies {
    // Core A2A client library
    implementation("ai.koog:a2a-client:$koogVersion")

    // HTTP JSON-RPC transport (most common)
    implementation("ai.koog:a2a-transport-client-jsonrpc-http:$koogVersion")

    // Ktor client engine (choose one that fits your needs)
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
}
```

## 概述 { #overview }

A2A 客户端充当您的应用程序与符合 A2A 规范的智能体之间的桥梁。
它编排整个通信生命周期，同时保持协议合规性并提供稳健的会话
管理。

## 核心组件 { #core-components }

### A2AClient { #a2aclient }

实现完整 A2A 协议的主要客户端类。它作为中央协调器，负责：

- **管理** 连接和通过可插拔解析器进行智能体发现
- **编排** 消息交换和任务操作，确保自动协议合规
- **处理** 流式响应和实时通信（当智能体支持时）
- **提供** 全面的错误处理和回退机制，以构建稳健的应用程序

`A2AClient` 接受两个必需参数：

* `ClientTransport` - 处理网络通信层
* `AgentCardResolver` - 处理智能体发现和元数据检索

`A2AClient` 接口提供了几个关键方法，用于与 A2A 智能体交互：

* `connect` 方法 - 连接到智能体并检索其能力，发现智能体能做什么并
  缓存 AgentCard
* `sendMessage` 方法 - 向智能体发送消息并接收单个响应，适用于简单的请求-响应
  模式
* `sendMessageStreaming` 方法 - 发送支持流式传输的消息以获取实时响应，返回一个包含
  部分消息和任务更新的事件流
* `getTask` 方法 - 查询特定任务的状态和详细信息
* `cancelTask` 方法 - 取消正在运行的任务（如果智能体支持取消操作）
* `cachedAgentCard` 方法 - 获取缓存的智能体卡片而无需发起网络请求，如果
  尚未调用 connect 方法则返回 null

### ClientTransport { #clienttransport }

`ClientTransport` 接口处理底层网络通信，而 A2A 客户端管理协议
逻辑。
它抽象了传输特定的细节，允许您无缝使用不同的协议。

#### HTTP JSON-RPC 传输 { #http-json-rpc-transport }

用于 A2A 智能体最常见的传输方式：

```kotlin
val transport = HttpJSONRPCClientTransport(
    url = "https://agent.example.com/a2a",        // Agent endpoint URL
    httpClient = HttpClient(CIO) {                // Optional: custom HTTP client
        install(ContentNegotiation) {
            json()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
        }
    }
)
```

### AgentCardResolver { #agentcardresolver }

`AgentCardResolver` 接口用于检索智能体元数据和能力。
它支持从各种来源发现智能体，并支持缓存策略以实现最佳性能。

#### URL 智能体卡片解析器 { #url-agent-card-resolver }

从遵循 A2A 约定的 HTTP 端点获取智能体卡片：

```kotlin
val agentCardResolver = UrlAgentCardResolver(
    baseUrl = "https://agent.example.com",           // Base URL of the agent service
    path = "/.well-known/agent-card.json",           // Standard agent card location
    httpClient = HttpClient(CIO),                    // Optional: custom HTTP client
)
```

## 快速开始 { #quickstart }

### 1. 创建客户端 { #1-create-the-client }

定义传输层和智能体卡片解析器，并创建客户端。

```kotlin
// HTTP JSON-RPC transport
val transport = HttpJSONRPCClientTransport(
    url = "https://agent.example.com/a2a"
)

// Agent card resolver
val agentCardResolver = UrlAgentCardResolver(
    baseUrl = "https://agent.example.com",
    path = "/.well-known/agent-card.json"
)

// Create client
val client = A2AClient(transport, agentCardResolver)
```

### 2. 连接与发现 { #2-connect-and-discover }

连接到智能体并获取其名片。拥有智能体名片后，您可以查询其能力并执行其他操作，例如检查是否支持流式传输。

```kotlin
// Connect and retrieve agent capabilities
client.connect()
val agentCard = client.cachedAgentCard()

println("Connected to: ${agentCard.name}")
println("Supports streaming: ${agentCard.capabilities.streaming}")
```

### 3. 发送消息 { #3-send-messages }

向智能体发送消息并接收单次响应。
响应可以是智能体直接回复的消息，也可以是智能体执行任务时产生的任务事件。

```kotlin
val message = Message(
    messageId = UUID.randomUUID().toString(),
    role = Role.User,
    parts = listOf(TextPart("Hello, agent!")),
    contextId = "conversation-1"
)

val request = Request(data = MessageSendParams(message))
val response = client.sendMessage(request)

// Handle response
when (val event = response.data) {
    is Message -> {
        val text = event.parts
            .filterIsInstance<TextPart>()
            .joinToString { it.text }
        print(text) // Stream partial responses
    }
    is TaskEvent -> {
        if (event.final) {
            println("\nTask completed")
        }
    }
}
```

### 4. 发送流式消息 { #4-send-messages-streaming }

A2A 客户端支持流式响应以实现实时通信。
它不会返回单次响应，而是返回包含消息和任务更新的 `Flow` 事件流。

```kotlin
// Check if agent supports streaming
if (client.cachedAgentCard()?.capabilities?.streaming == true) {
    client.sendMessageStreaming(request).collect { response ->
        when (val event = response.data) {
            is Message -> {
                val text = event.parts
                    .filterIsInstance<TextPart>()
                    .joinToString { it.text }
                print(text) // Stream partial responses
            }
            is TaskStatusUpdateEvent -> {
                if (event.final) {
                    println("\nTask completed")
                }
            }
        }
    }
} else {
    // Fallback to non-streaming
    val response = client.sendMessage(request)
    // Handle single response
}
```

### 5. 管理任务 { #5-manage-tasks }

A2A 客户端提供控制服务器任务的方法，包括查询任务状态和取消任务。

```kotlin
// Query task status
val taskRequest = Request(data = TaskQueryParams(taskId = "task-123"))
val taskResponse = client.getTask(taskRequest)
val task = taskResponse.data

println("Task state: ${task.status.state}")

// Cancel running task
if (task.status.state == TaskState.Working) {
    val cancelRequest = Request(data = TaskIdParams(taskId = "task-123"))
    val cancelledTask = client.cancelTask(cancelRequest).data
    println("Task cancelled: ${cancelledTask.status.state}")
}
```
