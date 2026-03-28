<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:18:45+00:00", "source_path": "streaming-api.md", "source_sha256": "1da1c4a87902893e159bc59ecefa433b8a4928e6a86d0d7b5c01340c40422802", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 流式传输 API { #streaming-api }

Koog 的 **流式传输 API** 允许你将 **LLM 输出** 作为 `Flow<StreamFrame>` 增量式消费。你的代码无需等待完整响应，即可：

- 在助手文本到达时实时渲染，
- 实时检测 **工具调用** 并采取行动，
- 知晓流式传输 **何时结束** 及其原因。

流式传输携带 **类型化帧**，分为两类：

**增量帧**（增量/部分内容）：
- `StreamFrame.TextDelta(text: String, index: Int?)` — 增量助手文本
- `StreamFrame.ReasoningDelta(text: String?, summary: String?, index: Int?)` — 增量推理文本和摘要
- `StreamFrame.ToolCallDelta(id: String?, name: String?, content: String?, index: Int?)` — 部分工具调用

**完整帧**（完整内容）：
- `StreamFrame.TextComplete(text: String)` — 完整助手文本
- `StreamFrame.ReasoningComplete(text: List<String>, summary: List<String>?)` — 完整推理（含可选摘要）
- `StreamFrame.ToolCallComplete(id: String?, name: String, content: String)` — 完整工具调用

**结束标记**：
- `StreamFrame.End(finishReason: String?)` — 流式传输结束标记

提供辅助函数用于提取纯文本、将帧转换为 `Message.Response` 对象，并安全地 **合并分块工具调用**。

## API 概述 { #api-overview }

通过流式传输，你可以：

- 在数据到达时即时处理（提升 UI 响应性）
- 实时解析结构化信息（Markdown/JSON 等）
- 在对象完成时立即发出
- 实时触发工具调用
- 实时访问模型推理（适用于支持的模型）

你可以直接操作 **帧** 本身，也可以操作从帧派生的 **纯文本**。

### 增量帧与完整帧 { #delta-vs-complete-frames }

流式传输 API 区分两种类型的帧：

- **增量帧** (`DeltaFrame`) — 以分块形式到达的增量/部分内容。这些帧非常适合内容流入时的实时显示。例如：`TextDelta`、`ReasoningDelta`、`ToolCallDelta`。

- **完整帧** (`CompleteFrame`) — 在接收完该内容类型的所有增量后发出的完整内容。这些帧适用于最终处理及转换为 `Message.Response` 对象。例如：`TextComplete`、`ReasoningComplete`、`ToolCallComplete`。

通常，你会使用增量帧进行 UI 更新，使用完整帧提取最终的结构化数据。

---
## 使用方法 { #usage }

### 直接操作帧 { #working-with-frames-directly }

这是最通用的方法：响应每种帧类型。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.prompt.streaming.StreamFrame
    
    val strategy = strategy<String, String>("strategy_name") {
        val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
       }
    }
    -->
    ```kotlin
    llm.writeSession {
        appendPrompt { user("Tell me a joke, then call a tool with JSON args.") }
    
        val stream = requestLLMStreaming() // Flow<StreamFrame>
    
        stream.collect { frame ->
            when (frame) {
                is StreamFrame.TextDelta -> print(frame.text)
                is StreamFrame.ReasoningDelta -> print("[Reasoning] text=${frame.text} summary=${frame.summary}")
                is StreamFrame.ToolCallComplete -> {
                    println("\n🔧 Tool call: ${frame.name} args=${frame.content}")
                    // Optionally parse lazily:
                    // val json = frame.contentJson
                }
                is StreamFrame.End -> println("\n[END] reason=${frame.finishReason}")
                else -> {} // Handle other frame types (TextComplete, ToolCallDelta, etc.)
            }
        }
    }
    ```
    <!--- KNIT example-streaming-api-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-streaming-api-java-01.java -->


需要注意的是，你可以通过直接处理原始字符串流来解析输出。这种方法能让你在解析过程中获得更大的灵活性和控制力。

以下是原始字符串流，包含输出结构的Markdown定义：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.prompt.structure.markdown.MarkdownStructureDefinition
    val strategy = strategy<String, String>("strategy_name") {
        val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
       }
    }
    -->
    ```kotlin
    fun markdownBookDefinition(): MarkdownStructureDefinition {
        return MarkdownStructureDefinition("name", schema = { /*...*/ })
    }

    val mdDefinition = markdownBookDefinition()

    llm.writeSession {
        val stream = requestLLMStreaming(mdDefinition)
        // Access the raw string chunks directly
        stream.collect { chunk ->
            // Process each chunk of text as it arrives
            println("Received chunk: $chunk") // The chunks together will be structured as a text following the mdDefinition schema
        }
    }
    ```
    <!--- KNIT example-streaming-api-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-streaming-api-java-02.java -->

### 使用推理框架 { #working-with-reasoning-frames }

支持推理的模型（如Claude Sonnet 4.5或GPT-o1）在流式传输过程中会输出推理帧。您可以同时访问推理过程及其总结：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.prompt.streaming.StreamFrame

val strategy = strategy<String, String>("strategy_name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
   }
}
-->
```kotlin
llm.writeSession {
    appendPrompt { user("Solve this complex problem: ...") }

    val stream = requestLLMStreaming()
    val reasoningSteps = mutableListOf<String>()
    val summarySteps = mutableListOf<String>()

    stream.collect { frame ->
        when (frame) {
            is StreamFrame.ReasoningDelta -> {
                frame.text?.let { 
                    reasoningSteps.add(it)
                    print(frame.text) // Display reasoning as it arrives
                }
                frame.summary?.let {
                    summarySteps.add(it)
                    print(frame.summary) // Display reasoning summary as it arrives
                }
            }
            is StreamFrame.ReasoningComplete -> {
                // Access complete reasoning
                println("\nComplete reasoning: ${frame.text.joinToString("")}")
                println("Summary: ${frame.summary?.joinToString("") ?: "N/A"}")
            }
            is StreamFrame.TextDelta -> print(frame.text)
            is StreamFrame.End -> println("\n[END]")
            else -> {}
        }
    }
}
```
<!--- KNIT example-streaming-api-reasoning-01.kt -->

### 处理原始文本流（派生） { #working-with-a-raw-text-stream-derived }

如果您已有期望使用`Flow<String>`的流式解析器，可通过`filterTextOnly()`派生文本块，或使用`collectText()`收集它们。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.prompt.streaming.filterTextOnly
    import ai.koog.prompt.streaming.collectText
    val strategy = strategy<String, String>("strategy_name") {
        val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
       }
    }
    -->
    ```kotlin
    llm.writeSession {
        val frames = requestLLMStreaming()

        // Stream text chunks as they come:
        frames.filterTextOnly().collect { chunk -> print(chunk) }

        // Or, gather all text into one String after End:
        val fullText = frames.collectText()
        println("\n---\n$fullText")
    }
    ```
    <!--- KNIT example-streaming-api-03.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-streaming-api-java-03.java -->

### 在事件处理器中监听流事件 { #listening-to-stream-events-in-event-handlers }

您可以在[代理事件处理器](features/agent-event-handlers.md)中监听流事件。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.agent.GraphAIAgent
    import ai.koog.agents.features.eventHandler.feature.handleEvents
    import ai.koog.prompt.streaming.StreamFrame
    import ai.koog.prompt.structure.markdown.MarkdownStructureDefinition
    
    fun GraphAIAgent.FeatureContext.installStreamingApi() {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    handleEvents {
        onToolCallStarting { context ->
            println("\n🔧 Using ${context.toolName} with ${context.toolArgs}... ")
        }
        onLLMStreamingFrameReceived { context ->
            when (val frame = context.streamFrame) {
                is StreamFrame.TextDelta -> print(frame.text)
                is StreamFrame.ReasoningDelta -> print("[Reasoning] text=${frame.text} summary=${frame.summary}")
                else -> {} // Handle other frame types if needed
            }
        }
        onLLMStreamingFailed { context ->
            println("❌ Error: ${context.error}")
        }
        onLLMStreamingCompleted {
            println("🏁 Done")
        }
    }
    ```
    <!--- KNIT example-streaming-api-04.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-streaming-api-java-04.java -->

### Converting frames to `Message.Response`

您可以将收集到的帧列表转换为标准消息对象：
- `toAssistantMessageOrNull()` — 从文本帧中提取 `Message.Assistant`
- `toReasoningMessageOrNull()` — 从推理框架中提取 `Message.Reasoning`
- `toToolCallMessages()` — 从工具调用帧中提取 `Message.Tool.Call`
- `toMessageResponses()` — 将所有完整帧转换为对应的`Message.Response`对象

## Examples

### 流式传输中的结构化数据（Markdown示例） { #structured-data-while-streaming-markdown-example }

虽然可以直接处理原始字符串流，但通常使用[结构化数据](structured-output.md)会更加方便。

结构化数据方法包含以下关键组成部分：

1. **MarkdownStructureDefinition**：一个用于帮助您定义结构化数据模式和示例的类
   Markdown format.
2. **markdownStreamingParser**：一个用于创建解析器的函数，该解析器处理 Markdown 数据块流并输出
   events.

以下部分提供了处理结构化数据流的分步说明和代码示例。

#### 1. 定义你的数据结构 { #1-define-your-data-structure }

首先，定义一个数据类来表示您的结构化数据：

=== "Kotlin"

    <!--- INCLUDE
    import kotlinx.serialization.Serializable
    -->
    ```kotlin
    @Serializable
    data class Book(
        val title: String,
        val author: String,
        val description: String
    )
    ```
    <!--- KNIT example-streaming-api-05.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // A simple Java POJO equivalent to the Kotlin @Serializable data class.
    public class Book {
        public final String title;
        public final String author;
        public final String description;

        public Book(String title, String author, String description) {
            this.title = title;
            this.author = author;
            this.description = description;
        }
    }
    ```
    <!--- KNIT exampleStreamingApiJava01.java -->

#### 2. 定义 Markdown 结构 { #2-define-the-markdown-structure }

创建一个定义，使用 `MarkdownStructureDefinition` 类来指定您的数据在 Markdown 中应如何结构化：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.markdown.markdown
    import ai.koog.prompt.structure.markdown.MarkdownStructureDefinition
    -->
    ```kotlin
    fun markdownBookDefinition(): MarkdownStructureDefinition {
        return MarkdownStructureDefinition("bookList", schema = {
            markdown {
                header(1, "title")
                bulleted {
                    item("author")
                    item("description")
                }
            }
        }, examples = {
            markdown {
                header(1, "The Great Gatsby")
                bulleted {
                    item("F. Scott Fitzgerald")
                    item("A novel set in the Jazz Age that tells the story of Jay Gatsby's unrequited love for Daisy Buchanan.")
                }
            }
        })
    }
    ```
    <!--- KNIT example-streaming-api-06.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-streaming-api-java-05.java -->

#### 3. 为你的数据结构创建一个解析器 { #3-create-a-parser-for-your-data-structure }

`markdownStreamingParser` 为不同的 Markdown 元素提供了多个处理器：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.example.exampleStreamingApi05.Book
    import ai.koog.prompt.structure.markdown.markdownStreamingParser
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.flow
    fun parseMarkdownStreamToBooks(markdownStream: Flow<String>): Flow<Book> {
        return flow {
    -->
    <!--- SUFFIX
       }
    }
    -->
    ```kotlin
    markdownStreamingParser {
        // Handle level 1 headings (level ranges from 1 to 6)
        onHeader(1) { headerText -> }
        // Handle bullet points
        onBullet { bulletText -> }
        // Handle code blocks
        onCodeBlock { codeBlockContent -> }
        // Handle lines matching a regex pattern
        onLineMatching(Regex("pattern")) { line -> }
        // Handle the end of the stream
        onFinishStream { remainingText -> }
    }
    ```
    <!--- KNIT example-streaming-api-07.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-streaming-api-java-06.java -->

利用已定义的处理程序，您可以实现一个解析Markdown流并利用`markdownStreamingParser`函数输出数据对象的函数。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.example.exampleStreamingApi05.Book
    import ai.koog.prompt.structure.markdown.markdownStreamingParser
    import ai.koog.prompt.streaming.StreamFrame
    import ai.koog.prompt.streaming.filterTextOnly
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.flow
    -->
    ```kotlin
    fun parseMarkdownStreamToBooks(markdownStream: Flow<StreamFrame>): Flow<Book> {
       return flow {
          markdownStreamingParser {
             var currentBookTitle = ""
             val bulletPoints = mutableListOf<String>()

             // Handle the event of receiving the Markdown header in the response stream
             onHeader(1) { headerText ->
                // If there was a previous book, emit it
                if (currentBookTitle.isNotEmpty() && bulletPoints.isNotEmpty()) {
                   val author = bulletPoints.getOrNull(0) ?: ""
                   val description = bulletPoints.getOrNull(1) ?: ""
                   emit(Book(currentBookTitle, author, description))
                }

                currentBookTitle = headerText
                bulletPoints.clear()
             }

             // Handle the event of receiving the Markdown bullets list in the response stream
             onBullet { bulletText ->
                bulletPoints.add(bulletText)
             }

             // Handle the end of the response stream
             onFinishStream {
                // Emit the last book, if present
                if (currentBookTitle.isNotEmpty() && bulletPoints.isNotEmpty()) {
                   val author = bulletPoints.getOrNull(0) ?: ""
                   val description = bulletPoints.getOrNull(1) ?: ""
                   emit(Book(currentBookTitle, author, description))
                }
             }
          }.parseStream(markdownStream.filterTextOnly())
       }
    }
    ```
    <!--- KNIT example-streaming-api-08.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-streaming-api-java-07.java -->

#### 4. 在您的智能体策略中应用解析器 { #4-use-the-parser-in-your-agent-strategy }

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.forwardTo
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.example.exampleStreamingApi05.Book
    import ai.koog.agents.example.exampleStreamingApi06.markdownBookDefinition
    import ai.koog.agents.example.exampleStreamingApi08.parseMarkdownStreamToBooks
    -->
    ```kotlin
    val agentStrategy = strategy<String, List<Book>>("library-assistant") {
       // Describe the node containing the output stream parsing
       val getMdOutput by node<String, List<Book>> { booksDescription ->
          val books = mutableListOf<Book>()
          val mdDefinition = markdownBookDefinition()

          llm.writeSession {
             appendPrompt { user(booksDescription) }
             // Initiate the response stream in the form of the definition `mdDefinition`
             val markdownStream = requestLLMStreaming(mdDefinition)
             // Call the parser with the result of the response stream and perform actions with the result
             parseMarkdownStreamToBooks(markdownStream).collect { book ->
                books.add(book)
                println("Parsed Book: ${book.title} by ${book.author}")
             }
          }

          books
       }
       // Describe the agent's graph making sure the node is accessible
       edge(nodeStart forwardTo getMdOutput)
       edge(getMdOutput forwardTo nodeFinish)
    }
    ```
    <!--- KNIT example-streaming-api-09.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-streaming-api-java-08.java -->

### 高级用法：工具流式调用 { #advanced-usage-streaming-with-tools }

您也可以使用流式处理API配合工具来处理实时到达的数据。以下部分简要介绍了如何定义工具并将其用于流式数据的逐步指南。

### 1. 为你的数据结构定义一个工具 { #1-define-a-tool-for-your-data-structure }

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.tools.SimpleTool
    import ai.koog.agents.core.tools.ToolDescriptor
    import ai.koog.agents.example.exampleStreamingApi05.Book
    import ai.koog.serialization.typeToken
    import kotlinx.serialization.Serializable
    -->
    ```kotlin
    @Serializable
    data class Book(
       val title: String,
       val author: String,
       val description: String
    )
    
    class BookTool(): SimpleTool<Book>(
        argsType = typeToken<Book>(),
        name = NAME,
        description = "A tool to parse book information from Markdown"
    ) {
    
        companion object { const val NAME = "book" }
    
        override suspend fun execute(args: Book): String {
            println("${args.title} by ${args.author}:\n ${args.description}")
            return "Done"
        }
    }
    ```
    <!--- KNIT example-streaming-api-10.kt -->

=== "Java"

    <!--- INCLUDE
    -->
    ```java
    ```
    <!--- KNIT example-streaming-api-java-09.java -->

### 2. 使用工具处理流式数据 { #2-use-the-tool-with-streaming-data }

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.forwardTo
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.example.exampleStreamingApi06.markdownBookDefinition
    import ai.koog.agents.example.exampleStreamingApi08.parseMarkdownStreamToBooks
    import ai.koog.agents.example.exampleStreamingApi10.BookTool
    import ai.koog.agents.core.agent.session.callToolRaw
    -->
    ```kotlin
    val agentStrategy = strategy<String, Unit>("library-assistant") {
       val getMdOutput by node<String, Unit> { input ->
          val mdDefinition = markdownBookDefinition()

          llm.writeSession {
             appendPrompt { user(input) }
             val markdownStream = requestLLMStreaming(mdDefinition)

             parseMarkdownStreamToBooks(markdownStream).collect { book ->
                callToolRaw(BookTool.NAME, book)
                /* Other possible options:
                    callTool(BookTool::class, book)
                    callTool<BookTool>(book)
                    findTool(BookTool::class).execute(book)
                */
             }

             // We can make parallel tool calls
             parseMarkdownStreamToBooks(markdownStream).toParallelToolCallsRaw(toolClass=BookTool::class).collect {
                println("Tool call result: $it")
             }
          }
       }

       edge(nodeStart forwardTo getMdOutput)
       edge(getMdOutput forwardTo nodeFinish)
     }
    ```
    <!--- KNIT example-streaming-api-11.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-streaming-api-java-10.java -->

### 3. 在您的智能体配置中注册该工具 { #3-register-the-tool-in-your-agent-configuration }

<!--- INCLUDE
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.example.exampleStreamingApi10.BookTool
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor

-->
```kotlin
val toolRegistry = ToolRegistry {
    tool(BookTool())
}

val runner = AIAgent(
    promptExecutor = simpleOpenAIExecutor("OPENAI_API_KEY"),
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry
)
```
<!--- KNIT example-streaming-api-12.kt -->

## Best practices

1. **定义清晰的结构**：为你的数据创建清晰且无歧义的Markdown结构。

2. **提供优质示例**：在您的`MarkdownStructureDefinition`中包含全面的示例，以指导LLM。

3. **处理不完整数据**：在解析流中的数据时，始终检查是否存在空值或空值。

4. **清理资源**：使用 `onFinishStream` 处理器来清理资源并处理任何剩余数据。

5. **错误处理**：针对格式错误的Markdown或意外数据，实施适当的错误处理机制。

6. **测试**：使用各种输入场景测试你的解析器，包括部分数据块和格式错误的输入。

7. **并行处理**：对于独立的数据项，可考虑使用并行工具调用来提升性能。