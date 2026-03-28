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
    // 定义历史记录过长的情况：消息数量超过 100 条
    private suspend fun AIAgentContext.historyIsTooLong(): Boolean = llm.readSession { prompt.messages.size > 100 }
    
    val strategy = strategy<String, String>("execute-with-history-compression") {
        val callLLM by nodeLLMRequest()
        val executeTool by nodeExecuteTool()
        val sendToolResult by nodeLLMSendToolResult()
    
        // 压缩 LLM 历史记录，并保留当前的 ReceivedToolResult 供下一个节点使用
        val compressHistory by nodeLLMCompressHistory<ReceivedToolResult>()
    
        edge(nodeStart forwardTo callLLM)
        edge(callLLM forwardTo nodeFinish onAssistantMessage { true })
        edge(callLLM forwardTo executeTool onToolCall { true })
    
        // 如果历史记录过长，则在执行任何工具后压缩历史记录
        edge(executeTool forwardTo compressHistory onCondition { historyIsTooLong() })
        edge(compressHistory forwardTo sendToolResult)
        // 否则，继续下一个 LLM 请求
        edge(executeTool forwardTo sendToolResult onCondition { !historyIsTooLong() })
    
        edge(sendToolResult forwardTo executeTool onToolCall { true })
        edge(sendToolResult forwardTo nodeFinish onAssistantMessage { true })
    }
    ```
    <!--- KNIT example-history-compression-01.kt -->

=== "Java"<!--- INCLUDE
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

// 压缩 LLM 历史记录，并保留当前的 ReceivedToolResult 供下一个节点使用
var compressHistory = AIAgentNode
    .llmCompressHistory("compressHistory")
    .withInput(ReceivedToolResult.class)
    .build();

// 从起始节点到 callLLM 的边
graph.edge(graph.nodeStart, callLLM);

// 当消息类型为 Assistant 时，从 callLLM 到结束节点的边
graph.edge(AIAgentEdge.builder()
    .from(callLLM)
    .to(graph.nodeFinish)
    .onIsInstance(Message.Assistant.class)
    .transformed(Message.Assistant::getContent)
    .build());

// 当消息类型为工具调用时，从 callLLM 到 executeTool 的边
graph.edge(AIAgentEdge.builder()
    .from(callLLM)
    .to(executeTool)
    .onIsInstance(Message.Tool.Call.class)
    .build());

// 如果历史记录过长，在执行任何工具后压缩历史记录
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

// 否则，继续执行下一个 LLM 请求
graph.edge(AIAgentEdge.builder()
    .from(executeTool)
    .to(sendToolResult)
    .onCondition((toolResult, ctx) ->
        ctx.getLlm().readSession(session ->
            session.getPrompt().getMessages().size() <= 100
        )
    )
    .build());

// 当消息类型为工具调用时，从 sendToolResult 到 executeTool 的边
graph.edge(AIAgentEdge.builder()
    .from(sendToolResult)
    .to(executeTool)
    .onIsInstance(Message.Tool.Call.class)
    .build());

// 当消息类型为 Assistant 时，从 sendToolResult 到结束节点的边
graph.edge(AIAgentEdge.builder()
    .from(sendToolResult)
    .to(graph.nodeFinish)
    .onIsInstance(Message.Assistant.class)
    .transformed(Message.Assistant::getContent)
    .build());
```
<!--- KNIT exampleHistoryCompressionJava01.java -->

在此示例中，策略会在每次工具调用后检查历史记录是否过长。
历史记录会在将工具结果发送回 LLM 之前进行压缩。这可以防止在长时间对话中上下文不断增长。

* 若要在策略的逻辑步骤（子图）之间压缩历史记录，您可以按如下方式实现您的策略：

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
            // 收集信息的一些步骤
        }
        val compressHistory by nodeLLMCompressHistory<String>()
        val makeTheDecision by subgraph<String, String> {
            // 基于当前压缩后的历史记录和收集到的信息做出决策的一些步骤
        }
        
```        nodeStart 然后 collectInformation 然后 compressHistory 然后 makeTheDecision
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

    // 用于收集信息的子图
    var collectInformation = AIAgentSubgraph.builder("collectInformation")
        .withInput(String.class)
        .withOutput(String.class)
        .limitedTools(Collections.emptyList())
        .withTask(input -> "Collect information based on: " + input)
        .build();

    // 收集信息后压缩历史
    var compressHistory = AIAgentNode
        .llmCompressHistory("compressHistory")
        .withInput(String.class)
        .build();

    // 基于压缩历史进行决策的子图
    var makeTheDecision = AIAgentSubgraph.builder("makeTheDecision")
        .withInput(String.class)
        .withOutput(String.class)
        .limitedTools(Collections.emptyList())
        .withTask(input -> "Make a decision based on the information")
        .build();

    // 构建流程：start -> collectInformation -> compressHistory -> makeTheDecision -> finish
    graph.edge(graph.nodeStart, collectInformation);
    graph.edge(collectInformation, compressHistory);
    graph.edge(compressHistory, makeTheDecision);
    graph.edge(makeTheDecision, graph.nodeFinish);
    ```
    <!--- KNIT exampleHistoryCompressionJava02.java -->

在此示例中，历史记录在完成信息收集阶段之后、进入决策阶段之前被压缩。

### 在自定义节点中进行历史压缩 { #history-compression-in-a-custom-node }

如果您正在实现自定义节点，可以按如下方式使用 `replaceHistoryWithTLDR()` 函数压缩历史记录：

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

这种方法为您提供了更大的灵活性，可以根据您的特定需求在自定义节点逻辑中的任意点实现压缩。

要了解更多关于自定义节点的信息，请参阅 [自定义节点](custom-nodes.md)。

## 历史压缩策略 { #history-compression-strategies }

您可以通过向 `nodeLLMCompressHistory(strategy=...)` 或 `replaceHistoryWithTLDR(strategy=...)` 传递可选的 `strategy` 参数来自定义压缩过程。
框架提供了几种内置策略。

### WholeHistory（默认） { #wholehistory-default }

默认策略，将整个历史记录压缩为一条 TLDR 消息，总结迄今为止已实现的内容。
此策略适用于大多数通用用例，您希望在减少令牌使用量的同时保持对整个对话上下文的感知。

您可以按如下方式使用它：

* 在策略图中：

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

=== "Java"<!--- INCLUDE
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
// 在压缩节点中使用 WholeHistory 策略
var compressHistory = AIAgentNode
    .llmCompressHistory("compressHistory")
    .withInput(String.class)
    .compressionStrategy(HistoryCompressionStrategy.WholeHistory)
    .build();

// 注意：此示例仅展示节点创建。
// 您需要添加边和其他节点以完成图。
```
<!--- KNIT exampleHistoryCompressionJava03.java -->

* 在自定义节点中：

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

### FromLastNMessages { #fromlastnmessages }

该策略仅将最后 `n` 条消息压缩为一条 TLDR 消息，并完全丢弃更早的消息。
这在只有代理的最新成果（或最新发现的事实、最新上下文）对解决问题相关时非常有用。

您可以按如下方式使用：

* 在策略图中：

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
    // 使用 FromLastNMessages 策略仅压缩最后 5 条消息
    var compressHistory = AIAgentNode
        .llmCompressHistory("compressHistory")
        .withInput(String.class)
        .compressionStrategy(HistoryCompressionStrategy.FromLastNMessages(5))
        .build();

    // 注意：此示例仅展示节点创建。
    // 您需要添加边和其他节点以完成图。
    ```
    <!--- KNIT exampleHistoryCompressionJava04.java -->

* 在自定义节点中：

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

### Chunked { #chunked }

该策略将整个消息历史按固定大小分块，并将每个块独立压缩为一条 TLDR 消息。
这在您不仅需要简洁的 TLDR 来总结已完成的工作，还想跟踪整体进度，且某些较早信息可能仍然重要时非常有用。

您可以按如下方式使用：

* 在策略图中：

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

=== "Java"<!--- INCLUDE
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
// 使用分块策略，以每10条消息为单位压缩历史记录
var compressHistory = AIAgentNode
    .llmCompressHistory("compressHistory")
    .withInput(String.class)
    .compressionStrategy(HistoryCompressionStrategy.Chunked(10))
    .build();

// 注意：此示例仅展示节点创建。
// 您需要添加边和其他节点以完成图结构。
```
<!--- KNIT exampleHistoryCompressionJava05.java -->

* 在自定义节点中：

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

### 从历史记录中检索事实 { #retrievefactsfromhistory }

该策略会在历史记录中搜索与提供的概念列表相关的特定事实，并将其提取出来。
它将整个历史记录替换为这些事实，并将其保留为未来 LLM 请求的上下文。
当您明确知道哪些具体事实有助于 LLM 更好地执行任务时，此策略非常有用。

使用方法如下：

* 在策略图中：

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
                // 给 LLM 的描述——具体要搜索什么
                description = "用户对推荐系统的偏好，包括偏好的对话风格、应用程序主题等。",
                // LLM 将搜索与此概念相关的多个事实：
                factType = FactType.MULTIPLE
            ),
            Concept(
                keyword = "product_details",
                // 给 LLM 的描述——具体要搜索什么
                description = "用户已查看目录中产品的简要详情",
                // LLM 将搜索与此概念相关的多个事实：
                factType = FactType.MULTIPLE
            ),
            Concept(
                keyword = "issue_solved",
                // 给 LLM 的描述——具体要搜索什么
                description = "用户的初始问题是否已解决？",
                // LLM 将搜索该问题的单一答案：
                factType = FactType.SINGLE
            )
        )
    )
    ```
    <!--- KNIT example-history-compression-10.kt -->

=== "Java"<!--- INCLUDE
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
// 使用 RetrieveFactsFromHistory 策略提取特定事实
var compressHistory = AIAgentNode
    .llmCompressHistory("compressHistory")
    .withInput(ReceivedToolResult.class)
    .compressionStrategy(new RetrieveFactsFromHistory(
        new Concept(
            "user_preferences",
            "用户对推荐系统的偏好，包括偏好的对话风格、应用主题等。",
            FactType.MULTIPLE
        ),
        new Concept(
            "product_details",
            "用户已查看目录中产品的简要详情",
            FactType.MULTIPLE
        ),
        new Concept(
            "issue_solved",
            "用户的初始问题是否已解决？",
            FactType.SINGLE
        )
    ))
    .build();

    // 注意：此示例仅展示节点创建。
    // 您需要添加边和其他节点以完成图。
```
<!--- KNIT exampleHistoryCompressionJava06.java -->

* 在自定义节点中：

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
                    // 描述 LLM —— 具体要搜索什么
                    description = "用户对推荐系统的偏好，包括偏好的对话风格、应用主题等。",
                    // LLM 将搜索与此概念相关的多个事实：
                    factType = FactType.MULTIPLE
                ),
                Concept(
                    keyword = "product_details",
                    // 描述 LLM —— 具体要搜索什么
                    description = "用户已查看目录中产品的简要详情",
                    // LLM 将搜索与此概念相关的多个事实：
                    factType = FactType.MULTIPLE
                ),
                Concept(
                    keyword = "issue_solved",
                    // 描述 LLM —— 具体要搜索什么
                    description = "用户的初始问题是否已解决？",
                    // LLM 将搜索问题的单一答案：
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

## 自定义历史压缩策略实现 { #custom-history-compression-strategy-implementation }

您可以通过扩展 `HistoryCompressionStrategy` 抽象类并实现 `compress` 方法来创建自己的历史压缩策略。

示例如下：

=== "Kotlin"<!--- INCLUDE
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
        // 1. 处理 llmSession.prompt.messages 中的当前历史记录
        // 2. 创建新的压缩后消息
        // 3. 使用压缩后的消息更新提示

        // 保存原始消息以保留它们
        val originalMessages = llmSession.prompt.messages
        
        // 示例实现：
        val importantMessages = llmSession.prompt.messages.filter {
            // 您的自定义过滤逻辑
            it.content.contains("important")
        }.filterIsInstance<Message.Response>()
        
        // 注意：您也可以使用 LLM 通过 `llmSession` 发出请求，并让 LLM 为您执行某些工作，例如使用 `llmSession.requestLLMWithoutTools()`
        // 或者您可以更改当前模型：`llmSession.model = AnthropicModels.Opus_4_6` 并询问其他 LLM 模型——但完成后别忘了改回来

        // 使用过滤后的消息组合提示
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

在此示例中，自定义策略会过滤包含单词“important”的消息，并仅将这些消息保留在压缩后的历史记录中。

然后您可以按如下方式使用它：

* 在策略图中：

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

* 在自定义节点中：

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

##  压缩期间的内存保留 { #memory-preservation-during-compression }

所有历史压缩方法都有一个 `preserveMemory` 参数，用于确定在压缩期间是否应保留与内存相关的消息。
这些消息包含从内存检索到的事实，或指示内存功能未启用。

您可以按如下方式使用 `preserveMemory` 参数：

* 在策略图中：

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

=== "Java"<!--- INCLUDE
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
// 使用 WholeHistory 策略并设置 preserveMemory=true
var compressHistory = AIAgentNode
        .llmCompressHistory("compressHistory")
        .withInput(String.class)
        .compressionStrategy(HistoryCompressionStrategy.WholeHistory)
        .preserveMemory(true)
        .build();

// 注意：此示例仅展示节点创建。
// 您需要添加边和其他节点以完成图结构。
```
<!--- KNIT exampleHistoryCompressionJava07.java -->

* 在自定义节点中：

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