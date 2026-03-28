<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:08:48+00:00", "source_path": "sessions.md", "source_sha256": "90d6115794b294bb91b575f5bc6ebcae4d9aee0f8b5039c33bf13fe8a1eb8235", "source_tag": "0.7.3", "translation_status": "changed"} -->
# LLM 会话与手动历史管理 { #llm-sessions-and-manual-history-management }

本页提供关于 LLM 会话的详细信息，包括如何使用读写会话、管理对话历史记录以及向语言模型发送请求。

## 简介 { #introduction }

LLM 会话是一个核心概念，它提供了一种与语言模型（LLMs）交互的结构化方式。会话负责管理对话历史记录、处理对 LLM 的请求，并为运行工具和处理响应提供一致的接口。

## 理解 LLM 会话 { #understanding-llm-sessions }

一个 LLM 会话代表与语言模型交互的上下文环境。它封装了：

- 对话历史记录（提示）
- 可用工具
- 向 LLM 发送请求的方法
- 更新对话历史记录的方法
- 运行工具的方法

会话由 `AIAgentLLMContext` 类管理，该类提供了创建读写会话的方法。

### 会话类型 { #session-types }

Koog 框架提供两种类型的会话：

1. **写会话**（`AIAgentLLMWriteSession`）：允许修改提示和工具、发送 LLM 请求以及运行工具。在写会话中所做的更改会持久化回 LLM 上下文。

2. **读会话**（`AIAgentLLMReadSession`）：提供对提示和工具的只读访问。它们适用于在不进行更改的情况下检查当前状态。

关键区别在于：写会话可以修改对话历史记录，而读会话不能。

### 会话生命周期 { #session-lifecycle }

会话具有明确定义的生命周期：

1. **创建**：使用 `llm.writeSession { ... }` 或 `llm.readSession { ... }` 创建会话。
2. **活动阶段**：会话在 lambda 块执行期间处于活动状态。
3. **终止**：当 lambda 块执行完成时，会话自动关闭。

会话实现了 `AutoCloseable` 接口，确保即使在发生异常时也能正确清理资源。

## 使用 LLM 会话 { #working-with-llm-sessions }

### 创建会话 { #creating-sessions }

会话是通过 `AIAgentLLMContext` 类的扩展函数创建的：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node


val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
// Creating a write session
llm.writeSession {
    // Session code here
}

// Creating a read session
llm.readSession {
    // Session code here
}
```
<!--- KNIT example-sessions-01.kt -->

这些函数接受一个在会话上下文中运行的 lambda 块。当块执行完成时，会话会自动关闭。

### 会话作用域与线程安全 { #session-scope-and-thread-safety }

会话使用读写锁来确保线程安全：

- 多个读会话可以同时处于活动状态。
- 一次只能有一个写会话处于活动状态。
- 写会话会阻塞所有其他会话（包括读和写）。

这确保了对话历史记录不会因并发修改而损坏。

### 访问会话属性 { #accessing-session-properties }

在会话内部，您可以访问提示和工具：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node


val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.readSession {
    val messageCount = prompt.messages.size
    val availableTools = tools.map { it.name }
}
```
<!--- KNIT example-sessions-02.kt -->

在写会话中，您还可以修改这些属性：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.tools.ToolDescriptor

val newTools = listOf<ToolDescriptor>()

val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.writeSession {
    // Modify the prompt
    appendPrompt {
        user("New user message")
    }

    // Modify the tools
    tools = newTools
}
```
<!--- KNIT example-sessions-03.kt -->

更多信息，请参阅详细的 API 参考文档：[AIAgentLLMReadSession](api:agents-core::ai.koog.agents.core.agent.session.AIAgentLLMReadSession) 和 [AIAgentLLMWriteSession](api:agents-core::ai.koog.agents.core.agent.session.AIAgentLLMWriteSession)。

## 发送 LLM 请求 { #making-llm-requests }

### 基本请求方法 { #basic-request-methods }

发送 LLM 请求最常用的方法是：1. `requestLLM()`：向 LLM 发送请求，包含当前提示词和工具，返回单个响应。

2. `requestLLMMultiple()`：向 LLM 发送请求，包含当前提示词和工具，返回多个响应。

3. `requestLLMWithoutTools()`：向 LLM 发送请求，包含当前提示词但不使用任何工具，返回单个响应。

4. `requestLLMForceOneTool`：向 LLM 发送请求，包含当前提示词和工具，强制使用一个工具。

5. `requestLLMOnlyCallingTools`：向 LLM 发送请求，该请求应仅通过工具处理。

示例：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node


val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.writeSession {
    // Make a request with tools enabled
    val response = requestLLM()

    // Make a request without tools
    val responseWithoutTools = requestLLMWithoutTools()

    // Make a request that returns multiple responses
    val responses = requestLLMMultiple()
}
```
<!--- KNIT example-sessions-04.kt -->

### 请求的工作原理 { #how-requests-work }

LLM 请求仅在显式调用请求方法时发起。需要理解的关键点包括：

1. **显式调用**：仅当调用 `requestLLM()`、`requestLLMWithoutTools()` 等方法时才会发起请求。
2. **立即执行**：调用请求方法时，请求会立即发起，方法将阻塞直至收到响应。
3. **自动更新历史记录**：在写入会话中，响应会自动添加到对话历史记录中。
4. **无隐式请求**：系统不会发起隐式请求，必须显式调用请求方法。

### 使用工具的请求方法 { #request-methods-with-tools }

启用工具发起请求时，LLM 可能返回工具调用而非文本响应。请求方法会透明处理这种情况：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.prompt.message.Message

val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.writeSession {
    val response = requestLLM()

    // The response might be a tool call or a text response
    if (response is Message.Tool.Call) {
        // Handle tool call
    } else {
        // Handle text response
    }
}
```
<!--- KNIT example-sessions-05.kt -->

实践中通常无需手动检查响应类型，因为智能体图会自动处理路由逻辑。

### 结构化与流式请求 { #structured-and-streaming-requests }

针对更高级的用例，平台提供了结构化请求和流式请求方法：

1. `requestLLMStructured()`：要求 LLM 以特定结构化格式提供响应。

2. `requestLLMStructuredOneShot()`：类似 `requestLLMStructured()`，但不进行重试或修正。

3. `requestLLMStreaming()`：向 LLM 发起流式请求，返回响应数据流。

示例：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.example.exampleParallelNodeExecution07.JokeRating


val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.writeSession {
    // Make a structured request
    val structuredResponse = requestLLMStructured<JokeRating>()

    // Make a streaming request
    val responseStream = requestLLMStreaming()
    responseStream.collect { chunk ->
        // Process each chunk as it arrives
    }
}
```
<!--- KNIT example-sessions-06.kt -->

## 管理对话历史记录 { #managing-conversation-history }

### 更新提示词 { #updating-the-prompt }

在写入会话中，可通过 `appendPrompt` 方法向提示词（对话历史记录）添加消息：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import kotlin.time.Clock

val myToolResult = Message.Tool.Result(
    id = "",
    tool = "",
    content = "",
    metaInfo = RequestMetaInfo(Clock.System.now())
)

val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.writeSession {
    appendPrompt {
        // Add a system message
        system("You are a helpful assistant.")

        // Add a user message
        user("Hello, can you help me with a coding question?")

        // Add an assistant message
        assistant("Of course! What's your question?")

        // Add a tool result
        tool {
            result(myToolResult)
        }
    }
}
```
<!--- KNIT example-sessions-07.kt -->

也可使用 `rewritePrompt` 方法完全重写提示词：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.prompt.message.Message

val filteredMessages = emptyList<Message>()

val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.writeSession {
    rewritePrompt { oldPrompt ->
        // Create a new prompt based on the old one
        oldPrompt.copy(messages = filteredMessages)
    }
}
```
<!--- KNIT example-sessions-08.kt -->

### 响应时自动更新历史记录 { #automatic-history-update-on-response }

在写入会话中发起 LLM 请求时，响应会自动添加到对话历史记录：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node


val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.writeSession {
    // Add a user message
    appendPrompt {
        user("What's the capital of France?")
    }

    // Make a request – the response is automatically added to the history
    val response = requestLLM()

    // The prompt now includes both the user message and the model's response
}
```
<!--- KNIT example-sessions-09.kt -->

这种自动历史记录更新是写入会话的核心特性，确保对话能够自然流畅地进行。

### 历史记录压缩 { #history-compression }

长时间运行的对话可能导致历史记录过大并消耗大量令牌。平台提供了历史记录压缩方法：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR

val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.writeSession {
    // Compress the history using a TLDR approach
    replaceHistoryWithTLDR(HistoryCompressionStrategy.WholeHistory, preserveMemory = true)
}
```
<!--- KNIT example-sessions-10.kt -->您也可以在策略图中使用 `nodeLLMCompressHistory` 节点，在特定位置压缩历史记录。

有关历史记录压缩和压缩策略的更多信息，请参阅[历史记录压缩](history-compression.md)。

## 在会话中运行工具 { #running-tools-in-sessions }

### 调用工具 { #calling-tools }

写入会话提供了多种调用工具的方法：

1. `callTool(tool, args)`：通过引用调用工具。
2. `callTool(toolName, args)`：通过名称调用工具。
3. `callTool(toolClass, args)`：通过类调用工具。
4. `callToolRaw(toolName, args)`：通过名称调用工具并返回原始字符串结果。

示例：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.ext.tool.AskUser
import ai.koog.agents.core.agent.session.callTool
import ai.koog.agents.core.agent.session.callToolRaw

val myTool = AskUser
val myArgs = AskUser.Args("this is a string")

typealias MyTool = AskUser


val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.writeSession {
    // Call a tool by reference
    val result = callTool(myTool, myArgs)

    // Call a tool by name
    val result2 = callTool("myToolName", myArgs)

    // Call a tool by class
    val result3 = callTool(MyTool::class, myArgs)

    // Call a tool and get the raw result
    val rawResult = callToolRaw("myToolName", myArgs)
}
```
<!--- KNIT example-sessions-11.kt -->

### 并行工具运行 { #parallel-tool-runs }

要并行运行多个工具，写入会话提供了针对 `Flow` 的扩展函数：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.ext.tool.AskUser
import kotlinx.coroutines.flow.flow

typealias MyTool = AskUser

val data = ""
fun parseDataToArgs(data: String) = flow { emit(AskUser.Args(data)) }

val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.writeSession {
    // Run tools in parallel
    parseDataToArgs(data).toParallelToolCalls(MyTool::class).collect { result ->
        // Process each result
    }

    // Run tools in parallel and get raw results
    parseDataToArgs(data).toParallelToolCallsRaw(MyTool::class).collect { rawResult ->
        // Process each raw result
    }
}
```
<!--- KNIT example-sessions-12.kt -->

这对于高效处理大量数据非常有用。

## 最佳实践 { #best-practices }

在使用 LLM 会话时，请遵循以下最佳实践：

1. **使用正确的会话类型**：当需要修改对话历史记录时使用写入会话，仅需读取时使用读取会话。
2. **保持会话简短**：会话应专注于特定任务，并在完成后尽快关闭以释放资源。
3. **处理异常**：确保在会话内处理异常，以防止资源泄漏。
4. **管理历史记录大小**：对于长时间运行的对话，使用历史记录压缩以减少令牌使用量。
5. **优先使用高级抽象**：尽可能使用基于节点的 API。例如，使用 `nodeLLMRequest` 而不是直接操作会话。
6. **注意线程安全**：请记住，写入会话会阻塞其他会话，因此应尽可能缩短写入操作的时间。
7. **对复杂数据使用结构化请求**：当需要 LLM 返回结构化数据时，使用 `requestLLMStructured` 而不是解析自由格式文本。
8. **对长响应使用流式处理**：对于长响应，使用 `requestLLMStreaming` 来在响应到达时进行处理。

## 故障排除 { #troubleshooting }

### 会话已关闭 { #session-already-closed }

如果看到类似 `Cannot use session after it was closed` 的错误，表示您尝试在会话的 lambda 块完成后使用该会话。请确保所有会话操作都在会话块内执行。

### 历史记录过大 { #history-too-large }

如果历史记录变得过大并消耗过多令牌，请使用历史记录压缩技术：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR

val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.writeSession {
    replaceHistoryWithTLDR(HistoryCompressionStrategy.FromLastNMessages(10), preserveMemory = true)
}
```
<!--- KNIT example-sessions-13.kt -->

更多信息请参阅[历史记录压缩](history-compression.md)

### 工具未找到 { #tool-not-found }

如果看到工具未找到的错误，请检查：

- 工具是否已在工具注册表中正确注册。
- 您使用的工具名称或类是否正确。

## API 文档 { #api-documentation }

更多信息，请参阅完整的 [AIAgentLLMSession](api:agents-core::ai.koog.agents.core.agent.session.AIAgentLLMSession) 和 [AIAgentLLMContext](api:agents-core::ai.koog.agents.core.agent.context.AIAgentLLMContext) 参考文档。