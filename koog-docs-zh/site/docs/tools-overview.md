<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:11:03+00:00", "source_path": "tools-overview.md", "source_sha256": "98d76c9ca65b44f4b7babc58ae65179fb4f2226555bc2b01b673374363ad5533", "source_tag": "0.7.3", "translation_status": "changed"} -->
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

To merge multiple tool registries, do the following:

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

### Passing tools to an agent

To enable an agent to use a tool, you need to provide a tool registry that contains this tool as an argument when creating the agent:

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

### Calling tools

There are several ways to call tools within your agent code. The recommended approach is to use the provided methods
in the agent context rather than calling tools directly, as this ensures proper handling of tool operation within the
agent environment.

!!! tip
    Ensure you have implemented proper [error handling](features/agent-event-handlers.md) in your tools to prevent agent failure.

The tools are called within a specific session context represented by `AIAgentLLMWriteSession`.
It provides several methods for calling tools so that you can:

- Call a tool with the given arguments.
- Call a tool by its name and the given arguments.
- Call a tool by the provided tool class and arguments.
- Call a tool of the specified type with the given arguments.
- Call a tool that returns a raw string result.

For more details, the API reference for [AIAgentLLMWriteSession](api:agents-core::ai.koog.agents.core.agent.session.AIAgentLLMWriteSession).

#### Parallel tool calls

You can also call tools in parallel using the `toParallelToolCallsRaw` extension. For example:

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

#### Calling tools from nodes

When building agent workflows with nodes, you can use special nodes to call tools:

* **nodeExecuteTool**: calls a single tool call and returns its result. For details, see [API reference](api:agents-core::ai.koog.agents.core.dsl.extension.nodeExecuteTool).

* **nodeExecuteSingleTool** that calls a specific tool with the provided arguments. For details, see [API reference](api:agents-core::ai.koog.agents.core.dsl.extension.nodeExecuteSingleTool).

* **nodeExecuteMultipleTools** that performs multiple tool calls and returns their results. For details, see [API reference](api:agents-core::ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools).

* **nodeLLMSendToolResult** that sends a tool result to the LLM and gets a response. For details, see [API reference](api:agents-core::ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult).

* **nodeLLMSendMultipleToolResults** that sends multiple tool results to the LLM. For details, see [API reference](api:agents-core::ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults).

## Using agents as tools

The framework provides the capability to convert any AI agent into a tool that can be used by other agents. 
This powerful feature enables you to create hierarchical agent architectures where specialized agents can be called as tools by higher-level orchestrating agents.

### Converting agents to tools

To convert an agent into a tool, use the `AIAgentService` and the `createAgentTool()` extension function:

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


### Using agent tools in other agents

Once converted to a tool, you can add the agent tool to another agent's tool registry:

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


### Agent tool execution

When an agent tool is called:

1. The arguments are deserialized according to the input descriptor.
2. The wrapped agent is executed with the deserialized input.
3. The agent's output is serialized and returned as the tool result.

### Benefits of agents as tools

- **Modularity**: Break complex workflows into specialized agents.
- **Reusability**: Use the same specialized agent across multiple coordinator agents.
- **Separation of concerns**: Each agent can focus on its specific domain.