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


It is important to note that you can parse the output by working directly with a raw string stream.
This approach gives you more flexibility and control over the parsing process.

Here is a raw string stream with the Markdown definition of the output structure:

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

### Working with reasoning frames

Models that support reasoning (such as Claude Sonnet 4.5 or GPT-o1) emit reasoning frames during streaming. You can access both the reasoning process and its summary:

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

### Working with a raw text stream (derived)

If you have existing streaming parsers that expect `Flow<String>`,
derive text chunks via `filterTextOnly()` or collect them with `collectText()`.

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

### Listening to stream events in event handlers

You can listen to stream events in [agent event handlers](features/agent-event-handlers.md).

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

You can transform a collected list of frames to standard message objects:
- `toAssistantMessageOrNull()` — extracts `Message.Assistant` from text frames
- `toReasoningMessageOrNull()` — extracts `Message.Reasoning` from reasoning frames
- `toToolCallMessages()` — extracts `Message.Tool.Call` from tool call frames
- `toMessageResponses()` — converts all complete frames to their corresponding `Message.Response` objects

## Examples

### Structured data while streaming (Markdown example)

Although it is possible to work with a raw string stream,
it is often more convenient to work with [structured data](structured-output.md).

The structured data approach includes the following key components:

1. **MarkdownStructureDefinition**: a class to help you define the schema and examples for structured data in
   Markdown format.
2. **markdownStreamingParser**: a function to create a parser that processes a stream of Markdown chunks and emits
   events.

The sections below provide step-by-step instructions and code samples related to processing a stream of structured data. 

#### 1. Define your data structure

First, define a data class to represent your structured data:

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

#### 2. Define the Markdown structure

Create a definition that specifies how your data should be structured in Markdown with the
`MarkdownStructureDefinition` class:

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

#### 3. Create a parser for your data structure

The `markdownStreamingParser` provides several handlers for different Markdown elements:

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

Using the defined handlers, you can implement a function that parses the Markdown stream and emits your data objects 
with the `markdownStreamingParser` function.

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

#### 4. Use the parser in your agent strategy

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

### Advanced usage: Streaming with tools

You can also use the Streaming API with tools to process data as it arrives. 
The following sections provide a brief step-by-step guide on how to define a tool and use it with streaming data.

### 1. Define a tool for your data structure

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

### 2. Use the tool with streaming data

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

### 3. Register the tool in your agent configuration

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

1. **Define clear structures**: create clear and unambiguous markdown structures for your data.

2. **Provide good examples**: include comprehensive examples in your `MarkdownStructureDefinition` to guide the LLM.

3. **Handle incomplete data**: always check for null or empty values when parsing data from the stream.

4. **Clean up resources**: use the `onFinishStream` handler to clean up resources and process any remaining data.

5. **Handle errors**: implement proper error handling for malformed Markdown or unexpected data.

6. **Testing**: test your parser with various input scenarios, including partial chunks and malformed input.

7. **Parallel processing**: for independent data items, consider using parallel tool calls for better performance.