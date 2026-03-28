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
                    // 可选延迟解析：
                    // val json = frame.contentJson
                }
                is StreamFrame.End -> println("\n[END] reason=${frame.finishReason}")
                else -> {} // 处理其他帧类型（TextComplete、ToolCallDelta 等）
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
    <!--- KNIT example-streaming-api-java-01.java -->需要注意的是，你可以通过直接处理原始字符串流来解析输出。
这种方法让你对解析过程拥有更高的灵活性和控制力。

以下是一个包含输出结构 Markdown 定义的原始字符串流：

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
        // 直接访问原始字符串块
        stream.collect { chunk ->
            // 处理每个到达的文本块
            println("Received chunk: $chunk") // 这些块将共同构成遵循 mdDefinition 模式的文本
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

### 处理推理帧 { #working-with-reasoning-frames }

支持推理的模型（例如 Claude Sonnet 4.5 或 GPT-o1）在流式传输过程中会发出推理帧。你可以同时访问推理过程及其摘要：

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

如果你已有的流式解析器期望 `Flow<String>`，
可以通过 `filterTextOnly()` 派生文本块，或使用 `collectText()` 收集它们。

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

        // 流式传输到达的文本块：
        frames.filterTextOnly().collect { chunk -> print(chunk) }

        // 或者，在 End 事件后收集所有文本到一个字符串中：
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

你可以在 [代理事件处理器](features/agent-event-handlers.md) 中监听流事件。

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
                else -> {} // 根据需要处理其他帧类型
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

### 将帧转换为 `Message.Response`您可以将收集到的帧列表转换为标准消息对象： { #converting-frames-to-message-response }
- `toAssistantMessageOrNull()` — 从文本帧中提取 `Message.Assistant`
- `toReasoningMessageOrNull()` — 从推理帧中提取 `Message.Reasoning`
- `toToolCallMessages()` — 从工具调用帧中提取 `Message.Tool.Call`
- `toMessageResponses()` — 将所有完整帧转换为其对应的 `Message.Response` 对象

## 示例 { #examples }

### 流式处理中的结构化数据（Markdown 示例） { #structured-data-while-streaming-markdown-example }

虽然可以处理原始字符串流，但通常更方便处理[结构化数据](structured-output.md)。

结构化数据方法包含以下关键组件：

1. **MarkdownStructureDefinition**：一个帮助您定义 Markdown 格式结构化数据模式和示例的类。
2. **markdownStreamingParser**：一个用于创建处理 Markdown 块流并发出事件的解析器的函数。

以下部分提供了处理结构化数据流的分步说明和代码示例。

#### 1. 定义您的数据结构 { #1-define-your-data-structure }

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
    // 一个简单的 Java POJO，相当于 Kotlin 的 @Serializable 数据类。
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

使用 `MarkdownStructureDefinition` 类创建一个定义，指定您的数据在 Markdown 中应如何结构化：

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

#### 3. 为您的数据结构创建解析器 { #3-create-a-parser-for-your-data-structure }

`markdownStreamingParser` 为不同的 Markdown 元素提供了多个处理程序：

=== "Kotlin"<!--- INCLUDE
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
    // 处理一级标题（标题级别范围为1到6）
    onHeader(1) { headerText -> }
    // 处理项目符号列表项
    onBullet { bulletText -> }
    // 处理代码块
    onCodeBlock { codeBlockContent -> }
    // 处理匹配正则表达式的行
    onLineMatching(Regex("pattern")) { line -> }
    // 处理流结束事件
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

通过已定义的处理程序，您可以实现一个解析Markdown流并利用`markdownStreamingParser`函数输出数据对象的函数。

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

             // 处理响应流中接收到Markdown标题的事件
             onHeader(1) { headerText ->
                // 若存在前一本图书，则输出
                if (currentBookTitle.isNotEmpty() && bulletPoints.isNotEmpty()) {
                   val author = bulletPoints.getOrNull(0) ?: ""
                   val description = bulletPoints.getOrNull(1) ?: ""
                   emit(Book(currentBookTitle, author, description))
                }

                currentBookTitle = headerText
                bulletPoints.clear()
             }

             // 处理响应流中接收到Markdown项目符号列表的事件
             onBullet { bulletText ->
                bulletPoints.add(bulletText)
             }

             // 处理响应流结束事件
             onFinishStream {
                // 若存在最后一本图书，则输出
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

#### 4. 在您的智能体策略中使用解析器 { #4-use-the-parser-in-your-agent-strategy }

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
       // 描述包含输出流解析的节点
       val getMdOutput by node<String, List<Book>> { booksDescription ->
          val books = mutableListOf<Book>()
          val mdDefinition = markdownBookDefinition()

``````kotlin
llm.writeSession {
    appendPrompt { user(booksDescription) }
    // 以定义 `mdDefinition` 的形式发起响应流
    val markdownStream = requestLLMStreaming(mdDefinition)
    // 使用响应流的结果调用解析器，并对结果执行操作
    parseMarkdownStreamToBooks(markdownStream).collect { book ->
        books.add(book)
        println("解析的书籍: ${book.title} 作者: ${book.author}")
    }
}

books
```
// 描述智能体的图结构，确保节点可访问
edge(nodeStart forwardTo getMdOutput)
edge(getMdOutput forwardTo nodeFinish)
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

### 高级用法：结合工具进行流式处理 { #advanced-usage-streaming-with-tools }

您也可以将流式 API 与工具结合使用，以便在数据到达时即时处理。
以下部分简要介绍了如何定义工具并将其与流式数据结合使用的分步指南。

### 1. 为您的数据结构定义工具 { #1-define-a-tool-for-your-data-structure }

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
        description = "用于从 Markdown 解析书籍信息的工具"
    ) {
    
        companion object { const val NAME = "book" }
    
        override suspend fun execute(args: Book): String {
            println("${args.title} 作者 ${args.author}:\n ${args.description}")
            return "完成"
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

### 2. 将工具与流式数据结合使用 { #2-use-the-tool-with-streaming-data }

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
                    /* 其他可选方式：
                        callTool(BookTool::class, book)
                        callTool<BookTool>(book)
                        findTool(BookTool::class).execute(book)
                    */
                }

                // 我们可以并行调用工具
                parseMarkdownStreamToBooks(markdownStream).toParallelToolCallsRaw(toolClass=BookTool::class).collect {
                    println("工具调用结果: $it")
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

### 3. 在智能体配置中注册工具 { #3-register-the-tool-in-your-agent-configuration }

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

## 最佳实践1. **定义清晰的结构**：为你的数据创建清晰且无歧义的 Markdown 结构。 { #best-practices }

2. **提供优质示例**：在 `MarkdownStructureDefinition` 中包含全面的示例，以指导 LLM。

3. **处理不完整数据**：解析流中的数据时，始终检查空值或缺失值。

4. **清理资源**：使用 `onFinishStream` 处理器来清理资源并处理剩余数据。

5. **处理错误**：针对格式错误的 Markdown 或意外数据，实施适当的错误处理机制。

6. **测试**：使用多种输入场景测试你的解析器，包括部分数据块和格式错误的输入。

7. **并行处理**：对于独立的数据项，考虑使用并行工具调用以提升性能。