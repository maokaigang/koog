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

上述代码定义了一个自定义节点 `myNode`，其包含预定义的 `Input` 和 `Output` 类型，并可选地接受字符串名称参数（`node_name`）。在实际示例中，这里有一个简单的节点，它接收字符串输入并返回输入的长度：

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

在 Kotlin 中创建自定义节点的另一种方式是在 `AIAgentSubgraphBuilderBase` 上定义一个扩展函数，该函数调用 `node` 函数。在 Java 中，你可以通过将节点构建器调用提取到辅助方法中来实现相同的可重用性：

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

这会创建一个直通节点，它执行一些自定义逻辑，但将输入原样返回作为输出，不做任何修改。

### 具有额外参数的节点 { #nodes-with-additional-arguments }

您可以创建接受参数以自定义其行为的节点：

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


### 参数化节点 { #parameterized-nodes }

您可以定义具有输入和输出参数的节点：

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

### 有状态节点 { #stateful-nodes }

如果你的节点需要在多次运行之间保持状态，可以使用闭包变量：

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

## 节点输入与输出类型 { #node-input-and-output-types }

节点可以拥有不同的输入和输出类型，这些类型通过泛型参数来指定：

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

## 最佳实践 { #best-practices }

在实现自定义节点时，请遵循以下最佳实践：

1. **保持节点专注**：每个节点应执行单一、明确定义的操作。
2. **使用描述性名称**：节点名称应清晰表明其用途。
3. **文档参数**：为所有参数提供清晰的文档说明。
4. **优雅地处理错误**：实施适当的错误处理机制，防止工作流中断。
5. **使节点可复用**：设计节点以便在不同工作流中重复使用。
6. **使用类型参数**：在适当的情况下使用泛型类型参数，使节点更具灵活性。
7. **提供默认值**：在可能的情况下，为参数提供合理的默认值。

## 常见模式 { #common-patterns }

以下部分提供了一些实现自定义节点的常见模式。

### 直通节点 { #pass-through-nodes }

执行操作但将输入作为输出返回的节点。

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

### 转换节点 { #transformation-nodes }

将输入转换为不同输出的节点。

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

### LLM 交互节点 { #llm-interaction-nodes }

与LLM交互的节点。

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
    上面的Kotlin示例展示了对LLM会话的细粒度控制（自定义提示词构建、显式调用`requestLLMWithoutTools`）。而Java API提供了更高级的工厂方法，例如`AIAgentNode.llmRequest()`，它能自动处理提示词构建——输入字符串直接作为用户消息。对于大多数使用场景这已经足够；若需高级提示词定制，可通过组合多个节点或使用自定义子图来实现。

### 工具运行节点 { #tool-run-node }

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
    Kotlin示例展示了通过手动构建`Message.Tool.Call`并调用`environment.executeTool()`来实现底层工具执行。而Java API则提倡采用更高级的方法，即使用带有`withTask()`的子图，由LLM自动编排工具调用。若要限制可用工具的范围，可在`.withInput()`前链入`.limitedTools(List.of(myTool))`。