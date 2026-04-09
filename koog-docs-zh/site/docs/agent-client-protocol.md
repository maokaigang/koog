<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T16:42:24+00:00", "source_path": "agent-client-protocol.md", "source_sha256": "a77a14280e4d4c5bad1e02f927f76e719a2253bfcaea321a0abdbcd558c531ce", "source_tag": "0.7.3", "translation_status": "changed"} -->
# Agent Client Protocol（ACP） { #agent-client-protocol }

Agent Client Protocol (ACP) 是一个开源的标准化协议，它使客户端应用程序能够通过一致的、双向的接口与 AI 智能体进行通信。通过在您的 Koog 智能体中实现 ACP，您可以确保它能够轻松集成到任何符合 ACP 的环境中，例如 IDE。

更多信息，请参阅 [Agent Client Protocol] 文档。

## 与 Koog 集成 { #integration-with-koog }

Koog 框架通过 [ACP Kotlin SDK] 并辅以额外的 API 扩展，实现了与 ACP 的集成。此集成提供以下功能：

* 为 Koog 智能体与符合 ACP 的客户端应用程序提供标准化通信
* 自动执行工具调用、智能体思考和完成状态的更新
* 在 Koog 的多模态消息格式与 ACP 的内容块之间实现无缝消息转换
* 将 Koog 智能体状态的生命周期映射到 ACP 会话事件

!!! note

    由于 [ACP Kotlin SDK] 是 JVM 特定的，因此 ACP 集成目前仅在 JVM 平台上可用。

### 添加依赖项 { #add-dependencies }

ACP 支持是一个可选的 [功能](features/index.md)，在 Koog 中默认不可用。要为您的 Koog 智能体实现 ACP，请添加对 [ai.koog:agents-features-acp](https://mvnrepository.com/artifact/ai.koog/agents-features-acp) 的依赖，该依赖本身依赖于 [com.agentclientprotocol:acp](https://mvnrepository.com/artifact/com.agentclientprotocol/acp)。

例如，在使用 `build.gradle.kts` 的情况下：

```kotlin
dependencies {
    implementation("ai.koog:agents-features-acp:$koogVersion")
}
```

### 为 Koog 智能体启用 ACP { #enable-acp-for-a-koog-agent }

为了将 Koog 智能体的内部[事件系统](agent-events.md)与 ACP 协议桥接起来，请安装 `ai.koog.agents.features.acp.AcpAgent` 功能。安装后，它会监听生命周期事件（例如工具调用或 LLM 响应），并将其发送到 ACP 客户端。

```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    llmModel = OpenAIModels.Chat.GPT4o
) {
    install(AcpAgent) {
        this.sessionId = sessionId
        this.protocol = protocol
        this.eventsProducer = eventsProducer
        this.setDefaultNotifications = true
    }
}
```

关键配置选项：

*   **`sessionId`**：标识当前对话会话的唯一字符串。
*   **`protocol`**：用于底层通信的 [`com.agentclientprotocol.protocol.Protocol`](https://github.com/agentclientprotocol/kotlin-sdk/blob/master/acp/src/commonMain/kotlin/com/agentclientprotocol/protocol/Protocol.kt) 实例。
*   **`eventsProducer`**：用于发送 ACP 事件的 `kotlinx.coroutines.channels.ProducerScope<Event>`。
    更多信息，请参阅[事件流](#event-streaming)。
*   **`setDefaultNotifications`**：是否为智能体生命周期事件注册默认的通知处理器。
    更多信息，请参阅[处理智能体通知](#handling-agent-notifications)。

此智能体必须在下一章所述的 ACP 会话范围内运行。

### 实现支持 ACP 的智能体 { #implement-an-acp-enabled-agent }

要将您的 Koog 智能体连接到 ACP 客户端，
请实现来自 [ACP Kotlin SDK](https://github.com/agentclientprotocol/kotlin-sdk) 的两个核心接口：

- [`AgentSupport`](https://github.com/agentclientprotocol/kotlin-sdk/blob/master/acp/src/commonMain/kotlin/com/agentclientprotocol/agent/AgentSupport.kt)：
  管理智能体的身份、能力和会话生命周期（创建或加载会话）。
- [`AgentSession`](https://github.com/agentclientprotocol/kotlin-sdk/blob/master/acp/src/commonMain/kotlin/com/agentclientprotocol/agent/AgentSession.kt)：
  管理单个对话会话，处理 `prompt` 执行，并管理取消操作。

在 `AgentSession` 的 `prompt()` 方法内部，您应该初始化和运行支持 ACP 的 Koog 智能体。
以下是一个示例：

=== "AgentSession"

    ```kotlin
    class MyAgentSession(
        override val sessionId: SessionId,
        private val promptExecutor: PromptExecutor,
        private val protocol: Protocol,
        private val clock: Clock
    ) : AgentSession {

        private var agentJob: Deferred<Unit>? = null
        private val agentMutex = Mutex()

        override suspend fun prompt(
            content: List<ContentBlock>,
            _meta: JsonElement?
        ): Flow<Event> = channelFlow {
            val agentConfig = AIAgentConfig(
                prompt = prompt("acp") {
                    system("You are a helpful assistant.")
                }.appendPrompt(content),
                model = OpenAIModels.Chat.GPT4o,
                maxAgentIterations = 1000
            )

            // Ensure only one agent session runs at a time
            agentMutex.withLock {
                val agent = AIAgent(
                    promptExecutor = promptExecutor,
                    agentConfig = agentConfig
                ) {
                    install(AcpAgent) {
                        this.sessionId = this@MyAgentSession.sessionId.value
                        this.protocol = this@MyAgentSession.protocol
                        this.eventsProducer = this@channelFlow
                        this.setDefaultNotifications = true
                    }
                }

                agentJob = async { agent.run("Hello. How can you help me?") }
                agentJob?.await()
            }
        }

        private fun Prompt.appendPrompt(content: List<ContentBlock>): Prompt {
            return withMessages { messages ->
                messages + listOf(content.toKoogMessage(clock))
            }
        }

        override suspend fun cancel() {
            agentJob?.cancel()
        }
    }
    ```

=== "AgentSupport"

    ```kotlin
    class MyAgentSupport(
        private val promptExecutor: PromptExecutor,
        private val clock: Clock,
        private val protocol: Protocol,
    ) : AgentSupport {

        override suspend fun initialize(clientInfo: ClientInfo): AgentInfo {
            return AgentInfo(
                protocolVersion = LATEST_PROTOCOL_VERSION,
                capabilities = AgentCapabilities(
                    loadSession = false, // Set to true if you implement session persistence
                    promptCapabilities = PromptCapabilities(
                        audio = false,
                        image = false,
                        embeddedContext = true
                    )
                )
            )
        }

        @OptIn(ExperimentalUuidApi::class)
        override suspend fun createSession(sessionParameters: SessionCreationParameters): AgentSession {
            val sessionId = SessionId(Uuid.random().toString())
            return MyAgentSession(sessionId, promptExecutor, protocol, clock)
        }

        override suspend fun loadSession(sessionId: SessionId, sessionParameters: SessionCreationParameters): AgentSession {
            throw UnsupportedOperationException("Session loading not implemented")
        }
    }
    ```

## Event streaming { #event-streaming }

示例中的`AgentSession`定义了一个返回事件`channelFlow`的`prompt()`函数。随后通过`this@channelFlow`将`AcpAgent`功能安装为`eventsProducer`。这使得可以从不同的协程发送事件。

## Execution synchronization { #execution-synchronization }

示例中的`AgentSession`使用互斥锁来同步对智能体实例的访问，因为ACP不应在前一个智能体执行完成前触发新的执行。为此，智能体的创建和运行都在`withLock`所定义的互斥锁作用域内进行。

您还在`channelFlow`作用域内以异步方式运行智能体，将其作为延迟作业`agentJob`，以确保智能体不会过早被取消。

## 处理 ACP 客户端输入 { #handling-acp-client-input }

ACP 客户端将用户输入作为 [`ContentBlock`](https://agentclientprotocol.com/protocol/schema#contentblock) 对象列表发送。要在 Koog 中处理这些输入，请使用 `List<ContentBlock>.toKoogMessage()` 扩展函数将 ACP 内容块转换为 `Message.User` 并附加到您的 [智能体提示](prompts/index.md) 中。

示例中的`AgentSession`定义了一个私有函数，用于在ACP会话中扩展初始智能体提示。

```kotlin
private fun Prompt.appendPrompt(content: List<ContentBlock>): Prompt {
    return withMessages { messages ->
        messages + listOf(content.toKoogMessage(clock))
    }
}
```

!!! note

    需要 `Clock` 实例来为消息添加时间戳。

更多信息，请参见[转换消息](#converting-messages)。

## Converting messages { #converting-messages }

`agents-features-acp` 模块提供扩展函数，可在Koog的内部消息类型与[ACP 内容块](https://agentclientprotocol.com/protocol/content)之间实现无缝转换。

当接收到来自ACP客户端的输入时，请使用以下函数：

- `List<ContentBlock>.toKoogMessage()` 将一系列 ACP 内容块转换为 `Message.User`
- `ContentBlock.toKoogContentPart()` 将单个 ACP 内容块转换为 [`ContentPart`](api:prompt-model::ai.koog.prompt.message.ContentPart)

使用以下函数从Koog消息中构建ACP事件或内容块：

- `Message.Response.toAcpEvents()` 将 [`Message.Response`](api:prompt-model::ai.koog.prompt.message.Message.Response) 转换为 ACP 会话更新事件列表
- `ContentPart.toAcpContentBlock()` 将 [`ContentPart`](api:prompt-model::ai.koog.prompt.message.ContentPart) 转换为单个 ACP 内容块

## Handling agent notifications { #handling-agent-notifications }

默认情况下，`setDefaultNotifications` 被设置为 `true`，且启用 ACP 的智能体会自动处理以下通知：

- **Agent completion**

    当智能体成功完成时，发送 `PromptResponseEvent` 与 `StopReason.END_TURN`

- **Agent execution failures**

    发送 `PromptResponseEvent` 并附带适当的停止原因：

    - `StopReason.MAX_TURN_REQUESTS` 当智能体超过最大迭代次数时
    - `StopReason.REFUSAL` 针对其他执行失败的情况

- **LLM responses**

    将LLM响应转换为ACP事件（文本、工具调用、推理）并发送

- **Tool call lifecycle**

    报告工具调用状态变更：

    - `ToolCallStatus.IN_PROGRESS` 当工具调用开始时
    - `ToolCallStatus.COMPLETED` 当工具调用成功时
    - `ToolCallStatus.FAILED` 当工具调用失败时

如需自定义通知处理，请设置 `setDefaultNotifications = false` 并根据规范处理智能体事件。

## Sending custom events { #sending-custom-events }

除了自动通知外，您还可以在智能体执行过程中的任意时刻，通过`withAcpAgent`代码块内的`sendEvent`向ACP客户端发送自定义事件。这种方式适用于进度更新、自定义状态消息或计划调整等场景。

你可以在一个`AIAgentContext`内部完成此操作，例如，在一个节点中：

```kotlin
val plan: Plan = TODO()

val strategy = strategy<Unit, Unit>("my-strategy") {
    val node by node<Unit, Unit> {
        withAcpAgent {
            sendEvent(
                Event.SessionUpdateEvent(
                    SessionUpdate.PlanUpdate(plan.entries)
                )
            )
        }
    }
}
```

你也可以访问底层的 `protocol` 来向客户端发送自定义请求，例如身份验证请求：

```kotlin
val strategy = strategy<Unit, Unit>("my-strategy") {
    val node by node<Unit, Unit> {
        withAcpAgent {
            protocol.sendRequest(
                AcpMethod.AgentMethods.Authenticate,
                AuthenticateRequest(methodId = AuthMethodId("Google"))
            )
        }
    }
}
```

## Examples { #examples }

你可以在 Koog 仓库的 [/示例](https://github.com/JetBrains/koog/tree/develop/examples/) 目录下找到 Koog 智能体的工作示例。

### 运行基于控制台的ACP客户端 { #running-a-console-based-acp-client }

此示例运行一个基于控制台的ACP客户端，该客户端与一个简单的Koog智能体进行交互。

1. Open [/examples/simple-examples](https://github.com/JetBrains/koog/blob/develop/examples/simple-examples/).
2. See the [README](https://github.com/JetBrains/koog/blob/develop/examples/simple-examples/README.md)
   有关为 LLM 提供商配置 API 密钥的信息。
3. 运行 `runExampleAcpApp` Gradle 任务。
4. 当 ACP 客户端在控制台启动时，输入对智能体的请求，例如：
    ```text
    List files in the current directory and create a new file named 'acp-test.txt' with the content 'Hello from ACP!'.
    ```
5. 观察控制台中的事件轨迹，
   其中展示了如何将Koog事件转换为ACP事件并发送至客户端。

### 将启用ACP的Koog智能体连接到JetBrains IDE { #connecting-an-acp-enabled-koog-agent-to-a-jetbrains-ide }

此示例展示了如何创建一个支持ACP的智能体，并连接到IntelliJ IDEA。

1. Open [/examples/acp-agent](https://github.com/JetBrains/koog/tree/develop/examples/acp-agent)
2. 运行 `installDist` Gradle 任务。
3. 这将创建智能体可执行文件：`build/install/acp-agent/bin/acp-agent`
   (`acp-agent.bat` for Windows).
4. 打开 IntelliJ IDEA（或其他 JetBrains IDE）。
5. 前往 **AI 聊天** > **选项** > **添加自定义智能体**。
6. 在打开的 `acp.json` 文件中，粘贴以下内容：

    ```json
    {
        "agent_servers": {
            "Koog Agent": {
                "command": "/absolute/path/to/acp-agent/build/install/acp-agent/bin/acp-agent",
                "args": [],
                "env": {
                    "OPENAI_API_KEY": "paste-your-api-key-here"
                }
            }
        }
    }
    ```

    Configuration parameters:

    - `agent_servers`: 包含一个或多个智能体配置的对象
    - `Koog Agent`: 在IDE的智能体选择器中显示的展示名称
    - `command`: 智能体可执行文件的绝对路径
    - `args`: 命令行参数（此智能体为空）
    - `env`: 传递给智能体进程的环境变量（本例中的OpenAI API键）

7. 该智能体将在 **AI 聊天** 工具窗口中变为可用。

有关向您的IDE添加自定义智能体的更多信息，请参阅[AI 助手文档](https://www.jetbrains.com/help/ai-assistant/acp.html#add-custom-agent)和[这篇博客文章](https://blog.jetbrains.com/ai/2026/02/koog-x-acp-connect-an-agent-to-your-ide-and-more/)。

[Agent Client Protocol]: https://agentclientprotocol.com [ACP Kotlin SDK]: https://github.com/agentclientprotocol/kotlin-sdk
