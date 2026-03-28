<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T16:25:41+00:00", "source_path": "tools-overview.md", "source_sha256": "98d76c9ca65b44f4b7babc58ae65179fb4f2226555bc2b01b673374363ad5533", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 概述 { #overview }

代理使用工具来执行特定任务或访问外部系统。

## 工具工作流 { #tool-workflow }

Koog 框架为在 Kotlin 和 Java 中使用工具提供了以下工作流：

1. 创建自定义工具或使用内置工具之一。
2. 将工具添加到工具注册表。
3. 将工具注册表传递给代理。
4. 与代理一起使用该工具。

### 可用工具类型 { #available-tool-types }

Koog 框架中有三种类型的工具：

- 内置工具，提供代理-用户交互和对话管理的功能。详情请参阅[内置工具](built-in-tools.md)。
- 基于注解的自定义工具，允许您将函数作为工具暴露给 LLM。详情请参阅[基于注解的工具](annotation-based-tools.md)。
- 自定义工具，允许您控制工具参数、元数据、执行逻辑以及其注册和调用方式。详情请参阅[基于类的工具](class-based-tools.md)。

### 工具注册表 { #tool-registry }

在代理中使用工具之前，必须将其添加到工具注册表中。
工具注册表管理代理可用的所有工具。

工具注册表的主要特性：

- 组织工具。
- 支持合并多个工具注册表。
- 提供按名称或类型检索工具的方法。

要了解更多信息，请参阅 [ToolRegistry](api:agents-tools::ai.koog.agents.core.tools.ToolRegistry)。

以下是如何创建工具注册表并向其中添加工具的示例：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.tools.ToolRegistry
    import ai.koog.agents.core.tools.annotations.Tool
    import ai.koog.agents.core.tools.reflect.ToolSet
    class MyToolSet : ToolSet {
        @Tool
        fun myTool(): String {
            // Tool implementation
            return "Result"
        }
    }
    val myTool = MyToolSet()
    -->
    ```kotlin
    val toolRegistry = ToolRegistry {
        tools(myTool)
    }
    ```
    <!--- KNIT example-tools-overview-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // Create an instance of your ToolSet
    MyToolSet myTool = new MyToolSet();

    // Build the ToolRegistry and register tools from the ToolSet
    ToolRegistry toolRegistry = ToolRegistry.builder()
        .tools(myTool)
        .build();
    ```
    <!--- KNIT example-tools-overview-java-01.java -->

要合并多个工具注册表，请执行以下操作：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.tools.ToolRegistry
    import ai.koog.agents.core.tools.annotations.Tool
    import ai.koog.agents.core.tools.reflect.ToolSet
    class FirstToolSet : ToolSet {
        @Tool
        fun firstSampleTool(): String {
            // Tool implementation
            return "First result"
        }
    }
    class SecondToolSet : ToolSet {
        @Tool
        fun secondSampleTool(): String {
            // Tool implementation
            return "Second result"
        }
    }
    val firstSampleTool = FirstToolSet()
    val secondSampleTool = SecondToolSet()
    -->
    ```kotlin
    val firstToolRegistry = ToolRegistry {
        tools(firstSampleTool)
    }
    
    val secondToolRegistry = ToolRegistry {
        tools(secondSampleTool)
    }
    
    val newRegistry = firstToolRegistry + secondToolRegistry
    ```
    <!--- KNIT example-tools-overview-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // Create instances of your ToolSets
    FirstToolSet firstSampleTool = new FirstToolSet();
    SecondToolSet secondSampleTool = new SecondToolSet();

    // Build separate tool registries
    ToolRegistry firstToolRegistry = ToolRegistry.builder()
        .tools(firstSampleTool)
        .build();

    ToolRegistry secondToolRegistry = ToolRegistry.builder()
        .tools(secondSampleTool)
        .build();

    ToolRegistry newRegistry = firstToolRegistry.plus(secondToolRegistry);
    ```
    <!--- KNIT example-tools-overview-java-02.java -->

### 向代理传递工具 { #passing-tools-to-an-agent }

要让智能体能够使用工具，你需要在创建智能体时提供一个包含该工具的工具注册表作为参数：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.example.exampleToolsOverview01.toolRegistry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    -->
    ```kotlin
    // Agent initialization
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
        systemPrompt = "You are a helpful assistant with strong mathematical skills.",
        llmModel = OpenAIModels.Chat.GPT4o,
        // Pass your tool registry to the agent
        toolRegistry = toolRegistry
    )
    ```
    <!--- KNIT example-tools-overview-03.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
        .systemPrompt("You are a helpful assistant with strong mathematical skills.")
        .llmModel(OpenAIModels.Chat.GPT4o)
        .toolRegistry(ToolRegistry.builder()
            .tools(secondSampleTool)
            .build()
        )
        .build();
    ```
    <!--- KNIT example-tools-overview-java-03.java -->

### 调用工具 { #calling-tools }

在您的代理代码中调用工具有多种方式。推荐的方法是使用代理上下文中提供的方法，而非直接调用工具，这能确保在代理环境中正确处理工具操作。

!!! tip
    确保您的工具中已正确实现[错误处理](features/agent-event-handlers.md)，以防止代理失败。

工具在特定的会话上下文中被调用，该上下文由`AIAgentLLMWriteSession`表示。它提供了多种调用工具的方法，以便您能够：

- 使用给定参数调用工具。
- 调用指定名称的工具及其参数。
- 调用指定工具类及参数的工具。
- 调用指定类型的工具并传入给定参数。
- 调用一个返回原始字符串结果的工具。

更多详情，请参阅API中关于[AIAgentLLM写入会话](api:agents-core::ai.koog.agents.core.agent.session.AIAgentLLMWriteSession)的参考。

#### 并行工具调用 { #parallel-tool-calls }

你也可以使用`toParallelToolCallsRaw`扩展并行调用工具。例如：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.tools.SimpleTool
    import kotlinx.coroutines.flow.collect
    import kotlinx.coroutines.flow.flow
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
    
    class BookTool() : SimpleTool<Book>(
        argsType = typeToken<Book>(),
        name = NAME,
        description = "A tool to parse book information from Markdown"
    ) {
        companion object {
            const val NAME = "book"
        }
    
        override suspend fun execute(args: Book): String {
            println("${args.title} by ${args.author}:\n ${args.description}")
            return "Done"
        }
    }
    
    val strategy = strategy<Unit, Unit>("strategy-name") {
    
        /*...*/
    
        val myNode by node<Unit, Unit> { _ ->
            llm.writeSession {
                flow {
                    emit(Book("Book 1", "Author 1", "Description 1"))
                }.toParallelToolCallsRaw(BookTool::class).collect()
            }
        }
    }
    
    ```
    <!--- KNIT example-tools-overview-04.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-tools-overview-java-04.java -->

#### 从节点调用工具 { #calling-tools-from-nodes }

在构建基于节点的智能体工作流时，您可以使用特殊节点来调用工具：

* **nodeExecuteTool**：调用单个工具调用并返回其结果。详情请参阅[API 参考](api:agents-core::ai.koog.agents.core.dsl.extension.nodeExecuteTool)。

* **nodeExecuteSingleTool**，用于调用指定工具并传入相应参数。详情请参阅[API 参考](api:agents-core::ai.koog.agents.core.dsl.extension.nodeExecuteSingleTool)。

* **nodeExecuteMultipleTools** 用于执行多个工具调用并返回其结果。详情请参阅 [API 参考](api:agents-core::ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools)。

* **nodeLLMSendToolResult** 用于向 LLM 发送工具执行结果并获取响应。详细信息请参阅 [API 参考](api:agents-core::ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult)。

* **nodeLLMSendMultipleToolResults**，用于向LLM发送多个工具结果。详情请参阅[API 参考](api:agents-core::ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults)。

## 将智能体用作工具 { #using-agents-as-tools }

该框架提供了将任何AI智能体转换为可供其他智能体使用的工具的能力。这一强大功能使您能够创建分层智能体架构，其中专门的智能体可作为工具被更高层级的协调智能体调用。

### 将代理转换为工具 { #converting-agents-to-tools }

要将代理转换为工具，请使用 `AIAgentService` 和 `createAgentTool()` 扩展函数：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.agent.AIAgentService
    import ai.koog.agents.core.agent.createAgentTool
    import ai.koog.agents.core.tools.ToolParameterDescriptor
    import ai.koog.agents.core.tools.ToolParameterType
    import ai.koog.agents.core.tools.ToolRegistry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import ai.koog.serialization.typeToken
    const val apiKey = ""
    val analysisToolRegistry = ToolRegistry {}
    -->
    ```kotlin
    // Create a specialized agent service, responsible for creating financial analysis agents.
    val analysisAgentService = AIAgentService(
        promptExecutor = simpleOpenAIExecutor(apiKey),
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a financial analysis specialist.",
        toolRegistry = analysisToolRegistry
    )
    
    // Create a tool that would run financial analysis agent once called.
    val analysisAgentTool = analysisAgentService.createAgentTool(
        agentName = "analyzeTransactions",
        agentDescription = "Performs financial transaction analysis",
        inputDescription = "Transaction analysis request",
        inputType = typeToken<String>(),
    )
    ```
    <!--- KNIT example-tools-overview-05.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-tools-overview-java-05.java -->


### 在其他智能体中使用智能体工具 { #using-agent-tools-in-other-agents }

转换为工具后，你可以将该代理工具添加到另一个代理的工具注册表中：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.tools.ToolRegistry
    import ai.koog.agents.example.exampleToolsOverview05.analysisAgentTool
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    const val apiKey = ""
    -->
    ```kotlin
    // Create a coordinator agent that can use specialized agents as tools
    val coordinatorAgent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(apiKey),
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You coordinate different specialized services.",
        toolRegistry = ToolRegistry {
            tool(analysisAgentTool)
            // Add other tools as needed
        }
    )
    ```
    <!--- KNIT example-tools-overview-06.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-tools-overview-java-06.java -->


### 代理工具执行 { #agent-tool-execution }

当调用代理工具时：

1. 参数根据输入描述符进行反序列化。
2. 包装后的代理使用反序列化后的输入执行。
3. 代理的输出经过序列化后作为工具结果返回。

### 作为工具的智能体优势 { #benefits-of-agents-as-tools }

- **模块化**：将复杂工作流拆分为专门的智能体。
- **可复用性**：在多个协调器代理中使用相同的专用代理。
- **关注点分离**：每个代理可以专注于其特定领域。