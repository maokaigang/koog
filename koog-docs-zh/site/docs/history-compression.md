<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:07:50+00:00", "source_path": "history-compression.md", "source_sha256": "33f97fa74d2db0579b9a3c16f4613de83620dbabff7b4edc3844fe1bc41cd153", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 历史压缩 { #history-compression }

AI 代理维护着包含用户消息、助手回复、工具调用和工具响应的消息历史记录。
随着代理执行其策略，每次交互都会使历史记录不断增长。

对于长时间运行的对话，历史记录可能变得庞大并消耗大量令牌。
历史压缩通过将完整的消息列表总结为一条或几条仅包含代理后续操作所需重要信息的消息，来帮助减少这种消耗。

历史压缩解决了代理系统中的关键挑战：

- 优化上下文使用。聚焦且更小的上下文可提升 LLM 性能，并防止因超出令牌限制而导致失败。
- 提高性能。压缩历史记录减少了 LLM 处理的消息数量，从而实现更快的响应。
- 增强准确性。聚焦于相关信息有助于 LLM 保持专注，不受干扰地完成任务。
- 降低成本。减少无关消息可降低令牌使用量，从而减少 API 调用的总体成本。

## 何时压缩历史记录 { #when-to-compress-history }

历史压缩在代理工作流的特定步骤中执行：

- 在代理策略的逻辑步骤（子图）之间。
- 当上下文变得过长时。

## 历史压缩实现 { #history-compression-implementation }

在代理中实现历史压缩主要有两种方法：

- 在策略图中实现。
- 在自定义节点中实现。

### 在策略图中进行历史压缩 { #history-compression-in-a-strategy-graph }

要在策略图中压缩历史记录，需要使用 `nodeLLMCompressHistory` 节点。
根据您决定执行压缩的步骤，以下场景可供选择：

* 若要在历史记录过长时进行压缩，您可以定义一个辅助函数，并将 `nodeLLMCompressHistory` 节点添加到策略图中，逻辑如下：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.context.AIAgentContext
    import ai.koog.agents.core.dsl.builder.forwardTo
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.nodeExecuteTool
    import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
    import ai.koog.agents.core.dsl.extension.nodeLLMRequest
    import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
    import ai.koog.agents.core.dsl.extension.onAssistantMessage
    import ai.koog.agents.core.dsl.extension.onToolCall
    import ai.koog.agents.core.environment.ReceivedToolResult
    -->
    ```kotlin
    // Define that the history is too long if there are more than 100 messages
    private suspend fun AIAgentContext.historyIsTooLong(): Boolean = llm.readSession { prompt.messages.size > 100 }
    
    val strategy = strategy<String, String>("execute-with-history-compression") {
        val callLLM by nodeLLMRequest()
        val executeTool by nodeExecuteTool()
        val sendToolResult by nodeLLMSendToolResult()
    
        // Compress the LLM history and keep the current ReceivedToolResult for the next node
        val compressHistory by nodeLLMCompressHistory<ReceivedToolResult>()
    
        edge(nodeStart forwardTo callLLM)
        edge(callLLM forwardTo nodeFinish onAssistantMessage { true })
        edge(callLLM forwardTo executeTool onToolCall { true })
    
        // Compress history after executing any tool if the history is too long 
        edge(executeTool forwardTo compressHistory onCondition { historyIsTooLong() })
        edge(compressHistory forwardTo sendToolResult)
        // Otherwise, proceed to the next LLM request
        edge(executeTool forwardTo sendToolResult onCondition { !historyIsTooLong() })
    
        edge(sendToolResult forwardTo executeTool onToolCall { true })
        edge(sendToolResult forwardTo nodeFinish onAssistantMessage { true })
    }
    ```
    <!--- KNIT example-history-compression-01.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentEdge;
    import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy;
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    import ai.koog.agents.core.environment.ReceivedToolResult;
    import ai.koog.prompt.message.Message;
    class exampleHistoryCompressionJava01 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    var graph = AIAgentGraphStrategy.builder("execute-with-history-compression")
        .withInput(String.class)
        .withOutput(String.class);

    var callLLM = AIAgentNode.llmRequest();
    var executeTool = AIAgentNode.executeTool();
    var sendToolResult = AIAgentNode.llmSendToolResult();

    // Compress the LLM history and keep the current ReceivedToolResult for the next node
    var compressHistory = AIAgentNode
        .llmCompressHistory("compressHistory")
        .withInput(ReceivedToolResult.class)
        .build();

    // Edge from start to callLLM
    graph.edge(graph.nodeStart, callLLM);

    // Edge from callLLM to finish on assistant message
    graph.edge(AIAgentEdge.builder()
        .from(callLLM)
        .to(graph.nodeFinish)
        .onIsInstance(Message.Assistant.class)
        .transformed(Message.Assistant::getContent)
        .build());

    // Edge from callLLM to executeTool on tool call
    graph.edge(AIAgentEdge.builder()
        .from(callLLM)
        .to(executeTool)
        .onIsInstance(Message.Tool.Call.class)
        .build());

    // Compress history after executing any tool if the history is too long
    graph.edge(AIAgentEdge.builder()
        .from(executeTool)
        .to(compressHistory)
        .onCondition((toolResult, ctx) ->
            ctx.getLlm().readSession(session ->
                session.getPrompt().getMessages().size() > 100
            )
        )
        .build());

    graph.edge(compressHistory, sendToolResult);

    // Otherwise, proceed to the next LLM request
    graph.edge(AIAgentEdge.builder()
        .from(executeTool)
        .to(sendToolResult)
        .onCondition((toolResult, ctx) ->
            ctx.getLlm().readSession(session ->
                session.getPrompt().getMessages().size() <= 100
            )
        )
        .build());

    // Edge from sendToolResult to executeTool on tool call
    graph.edge(AIAgentEdge.builder()
        .from(sendToolResult)
        .to(executeTool)
        .onIsInstance(Message.Tool.Call.class)
        .build());

    // Edge from sendToolResult to finish on assistant message
    graph.edge(AIAgentEdge.builder()
        .from(sendToolResult)
        .to(graph.nodeFinish)
        .onIsInstance(Message.Assistant.class)
        .transformed(Message.Assistant::getContent)
        .build());
    ```
    <!--- KNIT exampleHistoryCompressionJava01.java -->

In this example, the strategy checks if the history is too long after each tool call.
The history is compressed before sending the tool result back to the LLM. This prevents the context from growing during long conversations.

* To compress the history between the logical steps (subgraphs) of your strategy, you can implement your strateg as follows:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
    -->
    ```kotlin
    val strategy = strategy<String, String>("execute-with-history-compression") {
        val collectInformation by subgraph<String, String> {
            // Some steps to collect the information
        }
        val compressHistory by nodeLLMCompressHistory<String>()
        val makeTheDecision by subgraph<String, String> {
            // Some steps to make the decision based on the current compressed history and collected information
        }
        
        nodeStart then collectInformation then compressHistory then makeTheDecision
    }
    ```
    <!--- KNIT example-history-compression-02.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy;
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    import ai.koog.agents.core.agent.entity.AIAgentSubgraph;
    import java.util.Collections;
    class exampleHistoryCompressionJava02 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    var graph = AIAgentGraphStrategy.builder("execute-with-history-compression")
        .withInput(String.class)
        .withOutput(String.class);

    // Subgraph to collect information
    var collectInformation = AIAgentSubgraph.builder("collectInformation")
        .withInput(String.class)
        .withOutput(String.class)
        .limitedTools(Collections.emptyList())
        .withTask(input -> "Collect information based on: " + input)
        .build();

    // Compress history after collecting information
    var compressHistory = AIAgentNode
        .llmCompressHistory("compressHistory")
        .withInput(String.class)
        .build();

    // Subgraph to make decision based on compressed history
    var makeTheDecision = AIAgentSubgraph.builder("makeTheDecision")
        .withInput(String.class)
        .withOutput(String.class)
        .limitedTools(Collections.emptyList())
        .withTask(input -> "Make a decision based on the information")
        .build();

    // Build the flow: start -> collectInformation -> compressHistory -> makeTheDecision -> finish
    graph.edge(graph.nodeStart, collectInformation);
    graph.edge(collectInformation, compressHistory);
    graph.edge(compressHistory, makeTheDecision);
    graph.edge(makeTheDecision, graph.nodeFinish);
    ```
    <!--- KNIT exampleHistoryCompressionJava02.java -->

In this example, the history is compressed after completing the information collection phase, but before proceeding to the decision-making phase.

### History compression in a custom node

If you are implementing a custom node, you can compress history using the `replaceHistoryWithTLDR()` function as follows:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR
    val strategy = strategy<String, String>("strategy_name") {
        val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    llm.writeSession {
        replaceHistoryWithTLDR()
    }
    ```
    <!--- KNIT example-history-compression-03.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-history-compression-java-01.java -->

This approach gives you more flexibility to implement compression at any point in your custom node logic, based on your specific requirements.

To learn more about custom nodes, see [Custom nodes](custom-nodes.md).

## History compression strategies

You can customize the compression process by passing an optional `strategy` parameter to `nodeLLMCompressHistory(strategy=...)` or to `replaceHistoryWithTLDR(strategy=...)`.
The framework provides several built-in strategies.

### WholeHistory (Default)

The default strategy that compresses the entire history into one TLDR message that summarizes what has been achieved so far.
This strategy works well for most general use cases where you want to maintain awareness of the entire conversation context while reducing token usage.

You can use it as follows: 

* In a strategy graph:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
    import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
    typealias ProcessedInput = String
    val strategy = strategy<String, String>("strategy_name") {
        val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    val compressHistory by nodeLLMCompressHistory<ProcessedInput>(
        strategy = HistoryCompressionStrategy.WholeHistory
    )
    ```
    <!--- KNIT example-history-compression-04.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy;
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    import ai.koog.agents.core.agent.entity.AIAgentSubgraph;
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy;
    class exampleHistoryCompressionJava03 {
        public static void main(String[] args) {
            var graph = AIAgentGraphStrategy.builder("execute-with-history-compression")
                .withInput(String.class)
                .withOutput(String.class);
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    // Using WholeHistory strategy in a compression node
    var compressHistory = AIAgentNode
        .llmCompressHistory("compressHistory")
        .withInput(String.class)
        .compressionStrategy(HistoryCompressionStrategy.WholeHistory)
        .build();

    // Note: This example only shows the node creation.
    // You would need to add edges and other nodes to complete the graph.
    ```
    <!--- KNIT exampleHistoryCompressionJava03.java -->

* In a custom node:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
    import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR
    val strategy = strategy<String, String>("strategy_name") {
        val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    llm.writeSession {
        replaceHistoryWithTLDR(strategy = HistoryCompressionStrategy.WholeHistory)
    }
    ```
    <!--- KNIT example-history-compression-05.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-history-compression-java-02.java -->

### FromLastNMessages

The strategy compresses only the last `n` messages into a TLDR message and completely discards earlier messages.
This is useful when only the latest achievements of the agent (or the latest discovered facts, the latest context) are relevant for solving the problem.

You can use it as follows:

* In a strategy graph:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
    import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
    typealias ProcessedInput = String
    val strategy = strategy<String, String>("strategy_name") {
    val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    val compressHistory by nodeLLMCompressHistory<ProcessedInput>(
        strategy = HistoryCompressionStrategy.FromLastNMessages(5)
    )
    ```
    <!--- KNIT example-history-compression-06.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy;
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    import ai.koog.agents.core.agent.entity.AIAgentSubgraph;
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy;
    class exampleHistoryCompressionJava04 {
        public static void main(String[] args) {
            var graph = AIAgentGraphStrategy.builder("execute-with-history-compression")
                .withInput(String.class)
                .withOutput(String.class);
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    // Using FromLastNMessages strategy to compress only the last 5 messages
    var compressHistory = AIAgentNode
        .llmCompressHistory("compressHistory")
        .withInput(String.class)
        .compressionStrategy(HistoryCompressionStrategy.FromLastNMessages(5))
        .build();

    // Note: This example only shows the node creation.
    // You would need to add edges and other nodes to complete the graph.
    ```
    <!--- KNIT exampleHistoryCompressionJava04.java -->

* In a custom node:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
    import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR
    typealias ProcessedInput = String
    val strategy = strategy<String, String>("strategy_name") {
    val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    llm.writeSession {
        replaceHistoryWithTLDR(strategy = HistoryCompressionStrategy.FromLastNMessages(5))
    }
    ```
    <!--- KNIT example-history-compression-07.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-history-compression-java-03.java -->

### Chunked

The strategy splits the whole message history into chunks of a fixed size and compresses each chunk independently into a TLDR message.
This is useful when you need not only the concise TLDR of what has been done so far but also want to keep track of the overall progress, and some older information might also be important.

You can use it as follows:

* In a strategy graph:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
    import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
    typealias ProcessedInput = String
    val strategy = strategy<String, String>("strategy_name") {
    val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    val compressHistory by nodeLLMCompressHistory<ProcessedInput>(
        strategy = HistoryCompressionStrategy.Chunked(10)
    )
    ```
    <!--- KNIT example-history-compression-08.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy;
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    import ai.koog.agents.core.agent.entity.AIAgentSubgraph;
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy;
    class exampleHistoryCompressionJava05 {
    public static void main(String[] args) {
        var graph = AIAgentGraphStrategy.builder("execute-with-history-compression")
            .withInput(String.class)
            .withOutput(String.class);
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    // Using Chunked strategy to compress history in chunks of 10 messages
    var compressHistory = AIAgentNode
        .llmCompressHistory("compressHistory")
        .withInput(String.class)
        .compressionStrategy(HistoryCompressionStrategy.Chunked(10))
        .build();

    // Note: This example only shows the node creation.
    // You would need to add edges and other nodes to complete the graph.
    ```
    <!--- KNIT exampleHistoryCompressionJava05.java -->

* In a custom node:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
    import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR
    typealias ProcessedInput = String
    val strategy = strategy<String, String>("strategy_name") {
    val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    llm.writeSession {
        replaceHistoryWithTLDR(strategy = HistoryCompressionStrategy.Chunked(10))
    }
    ```
    <!--- KNIT example-history-compression-09.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-history-compression-java-04.java -->

### RetrieveFactsFromHistory

The strategy searches for specific facts relevant to the provided list of concepts in the history and retrieves them.
It changes the whole history to just these facts and leaves them as context for future LLM requests.
This is useful when you have an idea of what exact facts will be relevant for the LLM to perform better on the task.

You can use it as follows:

* In a strategy graph:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
    import ai.koog.agents.memory.feature.history.RetrieveFactsFromHistory
    import ai.koog.agents.memory.model.Concept
    import ai.koog.agents.memory.model.FactType
    typealias ProcessedInput = String
    val strategy = strategy<String, String>("strategy_name") {
    val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    val compressHistory by nodeLLMCompressHistory<ProcessedInput>(
        strategy = RetrieveFactsFromHistory(
            Concept(
                keyword = "user_preferences",
                // Description to the LLM -- what specifically to search for
                description = "User's preferences for the recommendation system, including the preferred conversation style, theme in the application, etc.",
                // LLM would search for multiple relevant facts related to this concept:
                factType = FactType.MULTIPLE
            ),
            Concept(
                keyword = "product_details",
                // Description to the LLM -- what specifically to search for
                description = "Brief details about products in the catalog the user has been checking",
                // LLM would search for multiple relevant facts related to this concept:
                factType = FactType.MULTIPLE
            ),
            Concept(
                keyword = "issue_solved",
                // Description to the LLM -- what specifically to search for
                description = "Was the initial user's issue resolved?",
                // LLM would search for a single answer to the question:
                factType = FactType.SINGLE
            )
        )
    )
    ```
    <!--- KNIT example-history-compression-10.kt -->

=== "Java"
    
    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy;
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    import ai.koog.agents.core.environment.ReceivedToolResult;
    import ai.koog.agents.memory.feature.history.RetrieveFactsFromHistory;
    import ai.koog.agents.memory.model.Concept;
    import ai.koog.agents.memory.model.FactType;
    class exampleHistoryCompressionJava06 {
    public static void main(String[] args) {
        var graph = AIAgentGraphStrategy.builder("execute-with-history-compression")
            .withInput(String.class)
            .withOutput(String.class);
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    // Using RetrieveFactsFromHistory strategy to extract specific facts
    var compressHistory = AIAgentNode
        .llmCompressHistory("compressHistory")
        .withInput(ReceivedToolResult.class)
        .compressionStrategy(new RetrieveFactsFromHistory(
            new Concept(
                "user_preferences",
                "User's preferences for the recommendation system, including the preferred conversation style, theme in the application, etc.",
                FactType.MULTIPLE
            ),
            new Concept(
                "product_details",
                "Brief details about products in the catalog the user has been checking",
                FactType.MULTIPLE
            ),
            new Concept(
                "issue_solved",
                "Was the initial user's issue resolved?",
                FactType.SINGLE
            )
        ))
        .build();

        // Note: This example only shows the node creation.
        // You would need to add edges and other nodes to complete the graph.
    ```
    <!--- KNIT exampleHistoryCompressionJava06.java -->

* In a custom node:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR
    import ai.koog.agents.memory.feature.history.RetrieveFactsFromHistory
    import ai.koog.agents.memory.model.Concept
    import ai.koog.agents.memory.model.FactType
    typealias ProcessedInput = String
    val strategy = strategy<String, String>("strategy_name") {
    val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    llm.writeSession {
        replaceHistoryWithTLDR(
            strategy = RetrieveFactsFromHistory(
                Concept(
                    keyword = "user_preferences", 
                    // Description to the LLM -- what specifically to search for
                    description = "User's preferences for the recommendation system, including the preferred conversation style, theme in the application, etc.",
                    // LLM would search for multiple relevant facts related to this concept:
                    factType = FactType.MULTIPLE
                ),
                Concept(
                    keyword = "product_details",
                    // Description to the LLM -- what specifically to search for
                    description = "Brief details about products in the catalog the user has been checking",
                    // LLM would search for multiple relevant facts related to this concept:
                    factType = FactType.MULTIPLE
                ),
                Concept(
                    keyword = "issue_solved",
                    // Description to the LLM -- what specifically to search for
                    description = "Was the initial user's issue resolved?",
                    // LLM would search for a single answer to the question:
                    factType = FactType.SINGLE
                )
            )
        )
    }
    ```
    <!--- KNIT example-history-compression-11.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-history-compression-java-05.java -->

## Custom history compression strategy implementation

You can create your own history compression strategy by extending the `HistoryCompressionStrategy` abstract class and implementing the `compress` method.

Here is an example:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.session.AIAgentLLMWriteSession
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
    import ai.koog.prompt.message.Message
    -->
    ```kotlin
    class MyCustomCompressionStrategy : HistoryCompressionStrategy() {
        override suspend fun compress(
            llmSession: AIAgentLLMWriteSession,
            memoryMessages: List<Message>
        ) {
            // 1. Process the current history in llmSession.prompt.messages
            // 2. Create new compressed messages
            // 3. Update the prompt with the compressed messages
    
            // Save original messages to preserve them
            val originalMessages = llmSession.prompt.messages
            
            // Example implementation:
            val importantMessages = llmSession.prompt.messages.filter {
                // Your custom filtering logic
                it.content.contains("important")
            }.filterIsInstance<Message.Response>()
            
            // Note: you can also make LLM requests using the `llmSession` and ask the LLM to do some job for you using, for example, `llmSession.requestLLMWithoutTools()`
            // Or you can change the current model: `llmSession.model = AnthropicModels.Opus_4_6` and ask some other LLM model -- but don't forget to change it back after
    
            // Compose the prompt with the filtered messages
            val compressedMessages = composeMessageHistory(
                originalMessages,
                importantMessages,
                memoryMessages
            )
        }
    }
    ```
    <!--- KNIT example-history-compression-12.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-history-compression-java-06.java -->

In this example, the custom strategy filters messages that contain the word "important" and keeps only those in the compressed history.

Then you can use it as follows:

* In a strategy graph:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
    import ai.koog.agents.example.exampleHistoryCompression12.MyCustomCompressionStrategy
    typealias ProcessedInput = String
    val strategy = strategy<String, String>("strategy_name") {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    val compressHistory by nodeLLMCompressHistory<ProcessedInput>(
        strategy = MyCustomCompressionStrategy()
    )
    ```
    <!--- KNIT example-history-compression-13.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-history-compression-java-07.java -->

* In a custom node:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR
    import ai.koog.agents.example.exampleHistoryCompression12.MyCustomCompressionStrategy
    typealias ProcessedInput = String
    val strategy = strategy<String, String>("strategy_name") {
    val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    llm.writeSession {
        replaceHistoryWithTLDR(strategy = MyCustomCompressionStrategy())
    }
    ```
    <!--- KNIT example-history-compression-14.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-history-compression-java-08.java -->

##  Memory preservation during compression

All history compression methods have the `preserveMemory` parameter that determines whether memory-related messages should be preserved during compression.
These are messages that contain facts retrieved from memory or indicate that the memory feature is not enabled.

You can use the `preserveMemory` parameter as follows:

* In a strategy graph:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
    import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
    typealias ProcessedInput = String
    val strategy = strategy<String, String>("strategy_name") {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    val compressHistory by nodeLLMCompressHistory<ProcessedInput>(
        strategy = HistoryCompressionStrategy.WholeHistory,
        preserveMemory = true
    )
    ```
    <!--- KNIT example-history-compression-15.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy;
    import ai.koog.agents.core.agent.entity.AIAgentNode;
    import ai.koog.agents.core.agent.entity.AIAgentSubgraph;
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy;
    class exampleHistoryCompressionJava07 {
    public static void main(String[] args) {
        var graph = AIAgentGraphStrategy.builder("execute-with-history-compression")
            .withInput(String.class)
            .withOutput(String.class);
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    // Using WholeHistory strategy with preserveMemory=true
    var compressHistory = AIAgentNode
        .llmCompressHistory("compressHistory")
        .withInput(String.class)
        .compressionStrategy(HistoryCompressionStrategy.WholeHistory)
        .preserveMemory(true)
        .build();

    // Note: This example only shows the node creation.
    // You would need to add edges and other nodes to complete the graph.
    ```
    <!--- KNIT exampleHistoryCompressionJava07.java -->

* In a custom node:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.dsl.builder.strategy
    import ai.koog.agents.core.dsl.builder.node
    import ai.koog.agents.core.dsl.builder.subgraph
    import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
    import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR
    typealias ProcessedInput = String
    val strategy = strategy<String, String>("strategy_name") {
    val node by node<Unit, Unit> {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    llm.writeSession {
        replaceHistoryWithTLDR(
            strategy = HistoryCompressionStrategy.WholeHistory,
            preserveMemory = true
        )
    }
    ```
    <!--- KNIT example-history-compression-16.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-history-compression-java-09.java -->