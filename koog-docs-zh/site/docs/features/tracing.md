<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:02:48+00:00", "source_path": "features/tracing.md", "source_sha256": "b5da11ede3ba98affe05dee8bf7f5cf1bc29e54c1bf37fc48b11a4540a2f6563", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 追踪 { #tracing }

本页详细介绍了追踪功能，该功能为AI智能体提供全面的追踪能力。

## 功能概述 { #feature-overview }

追踪功能是一个强大的监控和调试工具，能够捕获智能体运行的详细信息，包括：

- 策略执行
- LLM 调用
- LLM 流式处理（开始、帧、完成、错误）
- 工具调用
- 智能体图中的节点执行

该功能通过拦截智能体管道中的关键事件，并将其转发到可配置的消息处理器来运行。这些处理器可以将追踪信息输出到各种目的地，例如文件系统中的日志文件或其他类型的文件，使开发人员能够深入了解智能体行为并有效排查问题。

### 事件流 { #event-flow }

1. 追踪功能拦截智能体管道中的事件。
2. 事件根据配置的消息过滤器进行筛选。
3. 筛选后的事件传递给已注册的消息处理器。
4. 消息处理器格式化事件并将其输出到各自的目的地。

## 配置与初始化 { #configuration-and-initialization }

### 基础设置 { #basic-setup }

要使用追踪功能，您需要：

1. 拥有一个或多个消息处理器（可以使用现有的或创建自己的）。
2. 在您的智能体中安装 `Tracing`。
3. 配置消息过滤器（可选）。
4. 将消息处理器添加到该功能中。

```kotlin
// Defining a logger/file that will be used as a destination of trace messages
val logger = KotlinLogging.logger { }
val outputPath = Path("/path/to/trace.log")

// Creating an agent
val agent = AIAgent(
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2,
) {
    install(Tracing) {

        // Configure message processors to handle trace events
        addMessageProcessor(TraceFeatureMessageLogWriter(logger))
        addMessageProcessor(TraceFeatureMessageFileWriter(
            outputPath,
            { path: Path -> SystemFileSystem.sink(path).buffered() }
        ))
    }
}
```

### 消息过滤 { #message-filtering }

您可以处理所有现有事件，或根据特定条件选择其中一部分。
消息过滤器允许您控制处理哪些事件。这对于专注于智能体运行的特定方面非常有用：

```kotlin

val fileWriter = TraceFeatureMessageFileWriter(
    outputPath,
    { path: Path -> SystemFileSystem.sink(path).buffered() }
)

addMessageProcessor(fileWriter)

// Filter for LLM-related events only
fileWriter.setMessageFilter { message ->
    message is LLMCallStartingEvent || message is LLMCallCompletedEvent
}

// Filter for tool-related events only
fileWriter.setMessageFilter { message ->
    message is ToolCallStartingEvent ||
           message is ToolCallCompletedEvent ||
           message is ToolValidationFailedEvent ||
           message is ToolCallFailedEvent
}

// Filter for node execution events only
fileWriter.setMessageFilter { message ->
    message is NodeExecutionStartingEvent || message is NodeExecutionCompletedEvent
}
```

### 大量追踪数据 { #large-trace-volumes }

对于具有复杂策略或长时间运行的智能体，追踪事件的数量可能非常庞大。请考虑使用以下方法来管理事件量：

- 使用特定的消息过滤器来减少事件数量。
- 实现具有缓冲或采样功能的自定义消息处理器。
- 对日志文件使用文件轮转，以防止文件过大。

### 依赖关系图 { #dependency-graph }

追踪功能具有以下依赖项：

```
Tracing
├── AIAgentPipeline (for intercepting events)
├── TraceFeatureConfig
│   └── FeatureConfig
├── Message Processors
│   ├── TraceFeatureMessageLogWriter
│   │   └── FeatureMessageLogWriter
│   ├── TraceFeatureMessageFileWriter
│   │   └── FeatureMessageFileWriter
│   └── TraceFeatureMessageRemoteWriter
│       └── FeatureMessageRemoteWriter
└── Event Types (from ai.koog.agents.core.feature.model)
    ├── AgentStartingEvent
    ├── AgentCompletedEvent
    ├── AgentExecutionFailedEvent
    ├── AgentClosingEvent
    ├── GraphStrategyStartingEvent
    ├── FunctionalStrategyStartingEvent
    ├── StrategyCompletedEvent
    ├── NodeExecutionStartingEvent
    ├── NodeExecutionCompletedEvent
    ├── NodeExecutionFailedEvent
    ├── SubgraphExecutionStartingEvent
    ├── SubgraphExecutionCompletedEvent
    ├── SubgraphExecutionFailedEvent
    ├── LLMCallStartingEvent
    ├── LLMCallCompletedEvent
    ├── LLMStreamingStartingEvent
    ├── LLMStreamingFrameReceivedEvent
    ├── LLMStreamingFailedEvent
    ├── LLMStreamingCompletedEvent
    ├── ToolCallStartingEvent
    ├── ToolValidationFailedEvent
    ├── ToolCallFailedEvent
    └── ToolCallCompletedEvent
```

## 示例与快速入门 { #examples-and-quickstarts }

### 基础追踪到日志记录器 { #basic-tracing-to-logger }

```kotlin
// Create a logger
val logger = KotlinLogging.logger { }

fun main() {
    runBlocking {
       // Create an agent with tracing
       val agent = AIAgent(
          promptExecutor = simpleOllamaAIExecutor(),
          llmModel = OllamaModels.Meta.LLAMA_3_2,
       ) {
          install(Tracing) {
             addMessageProcessor(TraceFeatureMessageLogWriter(logger))
          }
       }

       // Run the agent
       agent.run("Hello, agent!")
    }
}
```

## 错误处理与边界情况 { #error-handling-and-edge-cases }

### 无消息处理器 { #no-message-processors }

如果未向追踪功能添加任何消息处理器，将记录一条警告：

```
Tracing Feature. No feature out stream providers are defined. Trace streaming has no target.
```

该功能仍会拦截事件，但不会对其进行处理或输出到任何地方。

### 资源管理 { #resource-management }

消息处理器可能持有需要正确释放的资源（如文件句柄）。使用 `use` 扩展函数确保正确清理：

```kotlin
// Creating an agent
val agent = AIAgent(
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2,
) {
    val writer = TraceFeatureMessageFileWriter(
        outputPath,
        { path: Path -> SystemFileSystem.sink(path).buffered() }
    )

    install(Tracing) {
        addMessageProcessor(writer)
    }
}
// Run the agent
agent.run(input)
// Writer will be automatically closed when the block exits
```

### 追踪特定事件到文件 { #tracing-specific-events-to-file }

```kotlin
install(Tracing) {

    val fileWriter = TraceFeatureMessageFileWriter(
        outputPath,
        { path: Path -> SystemFileSystem.sink(path).buffered() }
    )
    addMessageProcessor(fileWriter)

    // Only trace LLM calls
    fileWriter.setMessageFilter { message ->
        message is LLMCallStartingEvent || message is LLMCallCompletedEvent
    }
}
```

### 追踪特定事件到远程端点 { #tracing-specific-events-to-remote-endpoint }

当需要通过网络发送事件数据时，您可以使用追踪到远程端点。一旦启动，追踪到远程端点会在指定端口号启动一个轻量级服务器，并通过 Kotlin 服务器发送事件（SSE）。
```kotlin
// Creating an agent
val agent = AIAgent(
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2,
) {
    val connectionConfig = DefaultServerConnectionConfig(host = host, port = port)
    val writer = TraceFeatureMessageRemoteWriter(
        connectionConfig = connectionConfig
    )

    install(Tracing) {
        addMessageProcessor(writer)
    }
}
// Run the agent
agent.run(input)
// Writer will be automatically closed when the block exits
```

在客户端，您可以使用 `FeatureMessageRemoteClient` 来接收事件并对其进行反序列化。

```kotlin
val clientConfig = DefaultClientConnectionConfig(host = host, port = port, protocol = URLProtocol.HTTP)
val agentEvents = mutableListOf<DefinedFeatureEvent>()

val clientJob = launch {
    FeatureMessageRemoteClient(connectionConfig = clientConfig, scope = this).use { client ->
        val collectEventsJob = launch {
            client.receivedMessages.consumeAsFlow().collect { event ->
                // Collect events from server
                agentEvents.add(event as DefinedFeatureEvent)

                // Stop collecting events on agent finished
                if (event is AgentCompletedEvent) {
                    cancel()
                }
            }
        }
        client.connect()
        collectEventsJob.join()
        client.healthCheck()
    }
}

listOf(clientJob).joinAll()
```

## API 文档 { #api-documentation }

追踪功能采用模块化架构，包含以下关键组件：

1. [Tracing](api:agents-features-trace::ai.koog.agents.features.tracing.feature.Tracing)：主功能类，用于在智能体管道中拦截事件。
2. [TraceFeatureConfig](api:agents-features-trace::ai.koog.agents.features.tracing.feature.TraceFeatureConfig)：配置类，用于自定义功能行为。
3. 消息处理器：处理和输出追踪事件的组件：
    - [TraceFeatureMessageLogWriter](api:agents-features-trace::ai.koog.agents.features.tracing.writer.TraceFeatureMessageLogWriter)：将追踪事件写入日志。
    - [TraceFeatureMessageFileWriter](api:agents-features-trace::ai.koog.agents.features.tracing.writer.TraceFeatureMessageFileWriter)：将追踪事件写入文件。
    - [TraceFeatureMessageRemoteWriter](api:agents-features-trace::ai.koog.agents.features.tracing.writer.TraceFeatureMessageRemoteWriter)：将追踪事件发送到远程服务器。