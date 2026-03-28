<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:58:24+00:00", "source_path": "custom-nodes.md", "source_sha256": "8861431b1168e3a84a61d42d31879f808dad72da248447e521cdeec67901c228", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 自定义节点实现 { #custom-node-implementation }

本页面提供了在 Koog 框架中实现自定义节点的详细说明。自定义节点允许您通过创建执行特定操作的可复用组件来扩展智能体工作流的功能。

要了解更多关于图节点是什么、其用法以及现有的默认节点，请参阅[图节点](nodes-and-components.md)。

## 节点架构概述 { #node-architecture-overview }

在深入实现细节之前，了解 Koog 框架中节点的架构非常重要。节点是智能体工作流的基本构建块，每个节点代表工作流中的一个特定操作或转换。您使用边连接节点，边定义了节点之间的执行流程。

每个节点都有一个 `execute` 方法，该方法接收输入并产生输出，然后传递给工作流中的下一个节点。

## 实现自定义节点 { #implementing-a-custom-node }

自定义节点的实现范围广泛，从对输入数据执行基本逻辑并返回输出的简单实现，到接受参数并在多次运行之间维护状态的更复杂节点实现。

### 基本节点实现 { #basic-node-implementation }

在图中实现自定义节点并定义自定义逻辑的最简单方法是使用以下模式：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    typealias Input = String
    typealias Output = Int
    val returnValue = 42
    val str = strategy<Input, Output>("my-strategy") {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    val myNode by node<Input, Output>("node_name") { input ->
        // Processing
        returnValue
    }
    ```
    <!--- KNIT example-custom-nodes-01.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    class exampleCustomNodesJava01 {
        static class Input {}
        static class Output {}
        static Output returnValue = new Output();
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    var myNode = AIAgentNode.builder("node_name")
        .withInput(Input.class)
        .withOutput(Output.class)
        .withAction((input, ctx) -> {
            // Processing
            return returnValue;
        })
        .build();
    ```
    <!--- KNIT exampleCustomNodesJava01.java -->

The code above represents a custom node `myNode` with predefined `Input` and `Output` types, with the optional name
string parameter (`node_name`). In an actual example, here is a simple node that takes a string input and returns
the input's length:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    val str = strategy<String, Int>("my-strategy") {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    val myNode by node<String, Int>("node_name") { input ->
        // Processing
        input.length
    }
    ```
    <!--- KNIT example-custom-nodes-02.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    class exampleCustomNodesJava02 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    var myNode = AIAgentNode.builder("node_name")
        .withInput(String.class)
        .withOutput(Integer.class)
        .withAction((input, ctx) -> {
            // Processing
            return input.length();
        })
        .build();
    ```
    <!--- KNIT exampleCustomNodesJava02.java -->

Another way to create a custom node in Kotlin is to define an extension function on `AIAgentSubgraphBuilderBase` that
calls the `node` function. In Java, you achieve the same reusability by extracting the node builder call into a helper method:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
    import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    typealias Input = String
    typealias Output = String
    val strategy = strategy<String, String>("strategy_name") {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    fun AIAgentSubgraphBuilderBase<*, *>.myCustomNode(
        name: String? = null
    ): AIAgentNodeDelegate<Input, Output> = node(name) { input ->
        // Custom logic
        input // Return the input as output (pass-through)
    }

    val myCustomNode by myCustomNode("node_name")
    ```
    <!--- KNIT example-custom-nodes-03.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    class exampleCustomNodesJava03 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    var myCustomNode = AIAgentNode.builder("node_name")
        .withInput(String.class)
        .withOutput(String.class)
        .withAction((input, ctx) -> {
            // Custom logic
            return input; // Return the input as output (pass-through)
        })
        .build();
    ```
    <!--- KNIT exampleCustomNodesJava03.java -->

This creates a pass-through node that performs some custom logic but returns the input as the output without modification.

### Nodes with additional arguments

You can create nodes that accept arguments to customize their behavior:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
    import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    typealias Input = String
    typealias Output = String
    val strategy = strategy<String, String>("strategy_name") {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
        fun AIAgentSubgraphBuilderBase<*, *>.myNodeWithArguments(
        name: String? = null,
        arg1: String,
        arg2: Int
    ): AIAgentNodeDelegate<Input, Output> = node(name) { input ->
        // Use arg1 and arg2 in your custom logic
        input // Return the input as the output
    }

    val myCustomNode by myNodeWithArguments("node_name", arg1 = "value1", arg2 = 42)
    ```
    <!--- KNIT example-custom-nodes-04.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    class exampleCustomNodesJava04 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    String arg1 = "value1";
    int arg2 = 42;

    var myCustomNode = AIAgentNode.builder("node_name")
        .withInput(String.class)
        .withOutput(String.class)
        .withAction((input, ctx) -> {
            // Use arg1 and arg2 in your custom logic
            return input; // Return the input as the output
        })
        .build();
    ```
    <!--- KNIT exampleCustomNodesJava04.java -->


### Parameterized nodes

You can define nodes with input and output parameters:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
    import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    -->
    ```kotlin
    inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.myParameterizedNode(
        name: String? = null,
    ): AIAgentNodeDelegate<T, T> = node(name) { input ->
        // Do some additional actions
        // Return the input as the output
        input
    }

    val strategy = strategy<String, String>("strategy_name") {
        val myCustomNode by myParameterizedNode<String>("node_name")
    }
    ```
    <!--- KNIT example-custom-nodes-05.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    class exampleCustomNodesJava05 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    // In Java, specify the types explicitly when building the node
    var myCustomNode = AIAgentNode.builder("node_name")
        .withInput(String.class)
        .withOutput(String.class)
        .withAction((input, ctx) -> {
            // Do some additional actions
            // Return the input as the output
            return input;
        })
        .build();
    ```
    <!--- KNIT exampleCustomNodesJava05.java -->

### Stateful nodes

If your node needs to maintain state between runs, you can use closure variables:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
    import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
    import ai.koog.agents.core.dsl.builder.node
    typealias Input = Unit
    typealias Output = Unit
    -->
    ```kotlin
    fun AIAgentSubgraphBuilderBase<*, *>.myStatefulNode(
        name: String? = null
    ): AIAgentNodeDelegate<Input, Output> {
        var counter = 0

        return node(name) { input ->
            counter++
            println("Node executed $counter times")
            input
        }
    }
    ```
    <!--- KNIT example-custom-nodes-06.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    import java.util.concurrent.atomic.AtomicInteger;
    class exampleCustomNodesJava06 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    // In Java, use AtomicInteger (or similar) since the lambda captures must be effectively final
    AtomicInteger counter = new AtomicInteger(0);

    var myStatefulNode = AIAgentNode.builder("node_name")
        .withInput(String.class)
        .withOutput(String.class)
        .withAction((input, ctx) -> {
            int count = counter.incrementAndGet();
            System.out.println("Node executed " + count + " times");
            return input;
        })
        .build();
    ```
    <!--- KNIT exampleCustomNodesJava06.java -->

## Node input and output types

Nodes can have different input and output types, which are specified as generic parameters:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    val strategy = strategy<String, String>("strategy_name") {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    val stringToIntNode by node<String, Int>("node_name") { input: String ->
        // Processing
        input.toInt() // Convert string to integer
    }
    ```
    <!--- KNIT example-custom-nodes-07.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    class exampleCustomNodesJava07 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    var stringToIntNode = AIAgentNode.builder("node_name")
        .withInput(String.class)
        .withOutput(Integer.class)
        .withAction((input, ctx) -> {
            // Processing
            return Integer.parseInt(input); // Convert string to integer
        })
        .build();
    ```
    <!--- KNIT exampleCustomNodesJava07.java -->

!!! note
    The input and output types determine how the node can be connected to other nodes in the workflow. Nodes can only be connected if the output type of the source node is compatible with the input type of the target node.

## Best practices

When implementing custom nodes, follow these best practices:

1. **Keep nodes focused**: each node should perform a single, well-defined operation.
2. **Use descriptive names**: node names should clearly indicate their purpose.
3. **Document parameters**: provide clear documentation for all parameters.
4. **Handle errors gracefully**: implement proper error handling to prevent workflow failures.
5. **Make nodes reusable**: design nodes to be reusable across different workflows.
6. **Use type parameters**: use generic type parameters when appropriate to make nodes more flexible.
7. **Provide default values**: when possible, provide sensible default values for parameters.

## Common patterns

The following sections provide some common patterns for implementing custom nodes.

### Pass-through nodes

Nodes that perform an operation but return the input as the output.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    val strategy = strategy<String, String>("strategy_name") {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    val loggingNode by node<String, String>("node_name") { input ->
        println("Processing input: $input")
        input // Return the input as the output
    }
    ```
    <!--- KNIT example-custom-nodes-08.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    class exampleCustomNodesJava08 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    var loggingNode = AIAgentNode.builder("node_name")
        .withInput(String.class)
        .withOutput(String.class)
        .withAction((input, ctx) -> {
            System.out.println("Processing input: " + input);
            return input; // Return the input as the output
        })
        .build();
    ```
    <!--- KNIT exampleCustomNodesJava08.java -->

### Transformation nodes

Nodes that transform the input into a different output.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    val strategy = strategy<String, String>("strategy_name") {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    val upperCaseNode by node<String, String>("node_name") { input ->
        println("Processing input: $input")
        input.uppercase() // Transform the input to uppercase
    }
    ```
    <!--- KNIT example-custom-nodes-09.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    class exampleCustomNodesJava09 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    var upperCaseNode = AIAgentNode.builder("node_name")
        .withInput(String.class)
        .withOutput(String.class)
        .withAction((input, ctx) -> {
            System.out.println("Processing input: " + input);
            return input.toUpperCase(); // Transform the input to uppercase
        })
        .build();
    ```
    <!--- KNIT exampleCustomNodesJava09.java -->

### LLM interaction nodes

Nodes that interact with the LLM.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    val strategy = strategy<String, String>("strategy_name") {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    val summarizeTextNode by node<String, String>("node_name") { input ->
        llm.writeSession {
            appendPrompt {
                user("Please summarize the following text: $input")
            }

            val response = requestLLMWithoutTools()
            response.content
        }
    }
    ```
    <!--- KNIT example-custom-nodes-10.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    import ai.koog.prompt.message.Message;
    class exampleCustomNodesJava10 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    // In Java, LLM interaction is handled using pre-built factory nodes.
    // AIAgentNode.llmRequest() creates a node that sends the input string as a user
    // message to the LLM and returns the response. The prompt text is provided as
    // the node's input when it is executed in the graph.
    var summarizeTextNode = AIAgentNode.llmRequest(true, "node_name");

    // To extract the text content from the LLM response, chain a separate node:
    var extractContent = AIAgentNode.builder("extract-content")
        .withInput(Message.Response.class)
        .withOutput(String.class)
        .withAction((response, ctx) -> response.getContent())
        .build();
    ```
    <!--- KNIT exampleCustomNodesJava10.java -->

!!! note
    The Kotlin example above shows fine-grained control over the LLM session (custom prompt construction, explicit `requestLLMWithoutTools` call). The Java API provides higher-level factory methods like `AIAgentNode.llmRequest()` that handle prompt construction automatically — the input string becomes the user message. For most use cases this is sufficient; for advanced prompt customization, compose multiple nodes or use a custom subgraph.

### Tool run node

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.prompt.message.Message
    import ai.koog.prompt.message.ResponseMetaInfo
    import kotlin.time.Clock
    import kotlinx.serialization.Serializable
    import kotlinx.serialization.json.Json
    import java.util.*
    val toolName = "my-custom-tool"
    @Serializable
    data class ToolArgs(val arg1: String, val arg2: Int)
    val strategy = strategy<String, String>("strategy_name") {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    val nodeExecuteCustomTool by node<String, String>("node_name") { input ->
        val toolCall = Message.Tool.Call(
            id = UUID.randomUUID().toString(),
            tool = toolName,
            metaInfo = ResponseMetaInfo.create(Clock.System),
            content = Json.encodeToString(ToolArgs(arg1 = input, arg2 = 42)) // Use the input as tool arguments
        )

        val result = environment.executeTool(toolCall)
        result.content
    }
    ```
    <!--- KNIT example-custom-nodes-11.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentSubgraph;
    class exampleCustomNodesJava11 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    // In Java, direct tool execution (as shown in the Kotlin example) is not available
    // through the Java builder API. Instead, use a subgraph that delegates tool calls
    // to the LLM, which decides when and how to invoke the tools:
    var toolSubgraph = AIAgentSubgraph.builder("tool-subgraph")
        .withInput(String.class)
        .withOutput(String.class)
        .withTask(input -> "Use my_tool with input: " + input)
        .build();
    ```
    <!--- KNIT exampleCustomNodesJava11.java -->

!!! note
    The Kotlin example demonstrates low-level tool execution by manually constructing a `Message.Tool.Call` and calling `environment.executeTool()`. The Java API encourages a higher-level approach using subgraphs with `withTask()`, where the LLM orchestrates tool calls automatically. To restrict which tools are available, chain `.limitedTools(List.of(myTool))` before `.withInput()`.