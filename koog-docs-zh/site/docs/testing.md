<!-- koog-zh-meta: {"last_synced_at": "2026-03-29T03:04:14+00:00", "source_path": "testing.md", "source_sha256": "47bd4d8f87c7b023126a50377aaac7a61a65fe2722a132d2b8888b8afb9a2ebe", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 测试 { #testing }

## 概述 { #overview }

测试功能为 Koog 框架中的 AI 智能体流水线、子图以及工具交互提供了一个全面的测试框架。它使开发者能够创建受控的测试环境，包含模拟的 LLM（大语言模型）执行器、工具注册表和智能体环境。

### 目的 { #purpose }

此功能的主要目的是通过以下方式促进基于智能体的 AI 功能测试：

- 模拟对特定提示的 LLM 响应
- 模拟工具调用及其结果
- 测试智能体流水线子图及其结构
- 验证数据在智能体节点间的正确流转
- 为预期行为提供断言

## 配置与初始化 { #configuration-and-initialization }

### 设置测试依赖项 { #setting-up-test-dependencies }

在设置测试环境之前，请确保已添加以下依赖项：

```kotlin
// build.gradle.kts
dependencies {
   testImplementation("ai.koog:agents-test:LATEST_VERSION")
   testImplementation(kotlin("test"))
}
```

### 模拟 LLM 响应 { #mocking-llm-responses }

测试的基本形式涉及模拟 LLM 响应以确保确定性行为。您可以使用 `MockLLMBuilder` 及相关工具来实现。

=== "Kotlin"

    ```kotlin
    // 创建一个模拟的 LLM 执行器
    val mockLLMApi = getMockExecutor {
      // 模拟一个简单的文本响应
      mockLLMAnswer("Hello!") onRequestContains "Hello"

      // 模拟一个默认响应
      mockLLMAnswer("I don't know how to answer that.").asDefaultResponse
    }
    ```

=== "Java"

    ```java
    import ai.koog.agents.core.tools.ToolRegistry;
    import ai.koog.agents.testing.tools.MockExecutor;
    import ai.koog.prompt.executor.model.PromptExecutor;

    // 创建一个工具注册表（空）
    ToolRegistry toolRegistry = ToolRegistry.builder().build();    // 创建模拟 LLM 执行器
    PromptExecutor mockLLMApi = MockExecutor.builder()
        .toolRegistry(toolRegistry)
        .mockLLMAnswer("Hello!").onRequestContains("Hello")
        .mockLLMAnswer("I don't know how to answer that.").asDefaultResponse()
        .build();
    ```

### 模拟工具调用 { #mocking-tool-calls }

你可以根据输入模式模拟 LLM 调用特定工具：

=== "Kotlin"

    ```kotlin
    // 模拟工具调用响应
    mockLLMToolCall(CreateTool, CreateTool.Args("solve")) onRequestEquals "Solve task"

    // 模拟工具行为 - 最简单的形式（无需 lambda）
    mockTool(PositiveToneTool) alwaysReturns "The text has a positive tone."

    // 当需要执行额外操作时使用 lambda
    mockTool(NegativeToneTool) alwaysTells {
      // 执行一些额外操作
      println("Negative tone tool called")

      // 返回结果
      "The text has a negative tone."
    }

    // 基于特定参数模拟工具行为
    mockTool(AnalyzeTool) returns "Detailed analysis" onArguments AnalyzeTool.Args("analyze deeply")

    // 基于条件参数匹配模拟工具行为
    mockTool(SearchTool) returns "Found results" onArgumentsMatching { args ->
      args.query.contains("important")
    }
    ```

=== "Java"

    ```java
    ```

以上示例展示了从简单到复杂的不同模拟工具方法：

1. `alwaysReturns`：最简单形式，无需 lambda 直接返回值。
2. `alwaysTells`：需要执行额外操作时使用 lambda。
3. `returns...onArguments`：为精确参数匹配返回特定结果。
4. `returns...onArgumentsMatching`：基于自定义参数条件返回结果。

### 启用测试模式 { #enabling-testing-mode }

要在智能体上启用测试模式，请在 AIAgent 构造函数块中使用 `withTesting()` 函数：

=== "Kotlin"

    ```kotlin
    // 创建启用测试的智能体
    AIAgent(
    promptExecutor = mockLLMApi,
    toolRegistry = toolRegistry,
    llmModel = llmModel
    ) {
    // 启用测试模式
    withTesting()
    }
    ```

=== "Java"

    ```java
    ```

## 高级测试 { #advanced-testing }

### 测试图结构 { #testing-the-graph-structure }

在测试详细的节点行为和边连接之前，验证智能体图的整体结构非常重要。这包括检查所有必需的节点是否存在，以及是否在预期的子图中正确连接。

测试功能提供了一种全面的方式来测试智能体的图结构。这种方法对于具有多个子图和互连节点的复杂智能体尤其有价值。

#### 基础结构测试 { #basic-structure-testing }

首先验证智能体图的基本结构：

=== "Kotlin"

    ```kotlin
    AIAgent(
        // 构造函数参数
        promptExecutor = mockLLMApi,
        toolRegistry = toolRegistry,
        llmModel = llmModel
    ) {
        testGraph<String, String>("test") {
            val firstSubgraph = assertSubgraphByName<String, String>("first")
            val secondSubgraph = assertSubgraphByName<String, String>("second")

            // 断言子图连接
            assertEdges {
                startNode() alwaysGoesTo firstSubgraph
                firstSubgraph alwaysGoesTo secondSubgraph
                secondSubgraph alwaysGoesTo finishNode()
            }

            // 验证第一个子图
            verifySubgraph(firstSubgraph) {
                val start = startNode()
                val finish = finishNode()

                // 按名称断言节点
                val askLLM = assertNodeByName<String, Message.Response>("callLLM")
                val callTool = assertNodeByName<Message.Tool.Call, ReceivedToolResult>("executeTool")
// 断言节点可达性
                assertReachable(start, askLLM)
                assertReachable(askLLM, callTool)
            }
        }
    }
    ```

=== "Java"

    ```java
    ```

### 测试节点行为 { #testing-node-behavior }

节点行为测试允许您验证智能体图中节点在给定输入下是否产生预期输出。这对于确保智能体逻辑在不同场景下正确运行至关重要。

#### 基础节点测试 { #basic-node-testing }

从针对单个节点的简单输入输出验证开始：

=== "Kotlin"

    ```kotlin
    assertNodes {

        // 测试基础文本响应
        askLLM withInput "Hello" outputs assistantMessage("Hello!")

        // 测试工具调用响应
        askLLM withInput "Solve task" outputs toolCallMessage(CreateTool, CreateTool.Args("solve"))
    }
    ```

=== "Java"

    ```java
    ```

以上示例展示了如何测试以下行为：
1. 当 LLM 节点收到输入 `Hello` 时，会返回简单文本消息。
2. 当收到输入 `Solve task` 时，会返回工具调用。

#### 测试工具运行节点 { #testing-tool-run-nodes }

您还可以测试运行工具的节点：

=== "Kotlin"

    ```kotlin
    assertNodes {
        // 测试带特定参数的工具运行
        callTool withInput toolCallMessage(
            SolveTool,
            SolveTool.Args("solve")
        ) outputs toolResult(SolveTool, SolveTool.Args("solve"), "solved")
    }
    ```

=== "Java"

    ```java
    ```

这验证了当工具执行节点收到特定工具调用签名时，是否会产生预期的工具结果。

## 高级节点测试

对于更复杂的场景，您可以测试具有结构化输入和输出的节点：

=== "Kotlin"

    ```kotlin
    assertNodes {
        // 使用不同输入测试同一节点
        askLLM withInput "Simple query" outputs assistantMessage("Simple response")

        // 测试复杂参数
        askLLM withInput "Complex query with parameters" outputs toolCallMessage(
            AnalyzeTool,
            AnalyzeTool.Args(query = "parameters", depth = 3)
        )
    }
    ```

=== "Java"

    ```java
    ```

您还可以测试具有详细结果结构的复杂工具调用场景：

=== "Kotlin"

    ```kotlin
    assertNodes {
        // 测试具有结构化结果的复杂工具调用
        callTool withInput toolCallMessage(
            AnalyzeTool,
            AnalyzeTool.Args(query = "complex", depth = 5)
        ) outputs toolResult(AnalyzeTool, AnalyzeTool.Args(query = "complex", depth = 5), AnalyzeTool.Result(
            analysis = "Detailed analysis",
            confidence = 0.95,
            metadata = mapOf("source" to "database", "timestamp" to "2023-06-15")
        ))
    }
    ```

=== "Java"

    ```java
    ```

这些高级测试有助于确保您的节点正确处理复杂数据结构，这对于实现复杂的智能体行为至关重要。

### 测试边连接

边连接测试允许您验证智能体图是否正确地将一个节点的输出路由到适当的下一节点。这确保了您的智能体能够根据不同的输出遵循预期的工作流路径。

#### 基础边测试

从简单的边连接测试开始：

=== "Kotlin"

    ```kotlin
    assertEdges {
    // 测试文本消息路由
    askLLM withOutput assistantMessage("Hello!") goesTo giveFeedback

    // 测试工具调用路由
    askLLM withOutput toolCallMessage(CreateTool, CreateTool.Args("solve")) goesTo callTool
    }
    ```

=== "Java"

    ```java
    ```

此示例验证以下行为：
1. 当 LLM 节点输出简单文本消息时，流程被导向 `giveFeedback` 节点。
2. 当它输出工具调用时，流程被导向 `callTool` 节点。

#### 测试条件路由 { #basic-edge-testing }

您可以基于输出内容测试更复杂的路由逻辑：

=== "Kotlin"

    ```kotlin
    assertEdges {
        // 不同的文本响应可以路由到不同的节点
        askLLM withOutput assistantMessage("Need more information") goesTo askForInfo
        askLLM withOutput assistantMessage("Ready to proceed") goesTo processRequest
    }
    ```

=== "Java"

    ```java
    ```

#### 高级边测试 { #testing-conditional-routing }

对于复杂的智能体，您可以基于工具结果中的结构化数据测试条件路由：

=== "Kotlin"

    ```kotlin
    assertEdges {
        // 基于工具结果内容测试路由
        callTool withOutput toolResult(
            AnalyzeTool,
            AnalyzeTool.Args(query = "parameters", depth = 3),
            AnalyzeTool.Result(analysis = "Needs more processing", confidence = 0.5)
        ) goesTo processResult
    }
    ```

=== "Java"

    ```java
    ```

您还可以基于不同的结果属性测试复杂的决策路径：=== "Kotlin"

    ```kotlin
    assertEdges {
        // 根据置信度路由到不同节点
        callTool withOutput toolResult(
            AnalyzeTool,
            AnalyzeTool.Args(query = "parameters", depth = 3),
            AnalyzeTool.Result(analysis = "Complete", confidence = 0.9)
        ) goesTo finish

        callTool withOutput toolResult(
            AnalyzeTool,
            AnalyzeTool.Args(query = "parameters", depth = 3),
            AnalyzeTool.Result(analysis = "Uncertain", confidence = 0.3)
        ) goesTo verifyResult
    }
    ```

=== "Java"

    ```java
    ```

这些高级边测试有助于确保您的智能体能够根据节点输出的内容和结构做出正确决策，这对于创建智能、上下文感知的工作流至关重要。

## 完整测试示例

以下是一个用户故事，展示了完整的测试场景：

您正在开发一个语气分析智能体，用于分析文本的语气并提供反馈。该智能体使用检测积极、消极和中性语气的工具。

以下是测试该智能体的方法：

=== "Kotlin"

    ```kotlin
    @Test
    fun testToneAgent() = runTest {
        // 创建列表以跟踪工具调用
        var toolCalls = mutableListOf<String>()
        var result: String? = null

        // 创建工具注册表
        val toolRegistry = ToolRegistry {
            // 特殊工具，此类智能体必需
            tool(SayToUser)

            with(ToneTools) {
                tools()
            }
        }

        // 创建事件处理器
        val eventHandler = EventHandler {
            onToolCallStarting { tool, args ->
                println("[DEBUG_LOG] 工具调用：工具 ${tool.name}, 参数 $args")
                toolCalls.add(tool.name)
            }            handleError {
                println("[DEBUG_LOG] 发生错误: ${it.message}\n${it.stackTraceToString()}")
                true
            }

            handleResult {
                println("[DEBUG_LOG] 结果: $it")
                result = it
            }
        }

        val positiveText = "I love this product!"
        val negativeText = "Awful service, hate the app."
        val defaultText = "I don't know how to answer this question."

        val positiveResponse = "The text has a positive tone."
        val negativeResponse = "The text has a negative tone."
        val neutralResponse = "The text has a neutral tone."

        val mockLLMApi = getMockExecutor(toolRegistry, eventHandler) {
            // 为不同输入文本设置 LLM 响应
            mockLLMToolCall(NeutralToneTool, ToneTool.Args(defaultText)) onRequestEquals defaultText
            mockLLMToolCall(PositiveToneTool, ToneTool.Args(positiveText)) onRequestEquals positiveText
            mockLLMToolCall(NegativeToneTool, ToneTool.Args(negativeText)) onRequestEquals negativeText

            // 模拟当工具返回结果时，LLM 仅返回工具响应的行为
            mockLLMAnswer(positiveResponse) onRequestContains positiveResponse
            mockLLMAnswer(negativeResponse) onRequestContains negativeResponse
            mockLLMAnswer(neutralResponse) onRequestContains neutralResponse

            mockLLMAnswer(defaultText).asDefaultResponse

            // 工具模拟
            mockTool(PositiveToneTool) alwaysTells {
                toolCalls += "Positive tone tool called"
                positiveResponse
            }
            mockTool(NegativeToneTool) alwaysTells {
                toolCalls += "Negative tone tool called"
                negativeResponse
            }
            mockTool(NeutralToneTool) alwaysTells {
                toolCalls += "Neutral tone tool called"
                neutralResponse
            }
        }
    ```

=== "Java"

    ```java
    ```

这验证了当工具执行节点收到特定工具调用签名时，是否会产生预期的工具结果。

#### 高级节点测试

对于更复杂的场景，您可以测试具有结构化输入和输出的节点：

=== "Kotlin"

    ```kotlin
    assertNodes {
        // Test with different inputs to the same node
        askLLM withInput "Simple query" outputs assistantMessage("Simple response")

        // Test with complex parameters
        askLLM withInput "Complex query with parameters" outputs toolCallMessage(
            AnalyzeTool,
            AnalyzeTool.Args(query = "parameters", depth = 3)
        )
    }
    ```

=== "Java"

    ```java
    ```

您还可以测试带有详细结果结构的复杂工具调用场景：

=== "Kotlin"

    ```kotlin
    assertNodes {
        callTool withInput toolCallMessage(
            AnalyzeTool,
            AnalyzeTool.Args("deep analysis", 5)
        ) outputs toolResult(
            AnalyzeTool,
            AnalyzeTool.Result(
                analysis = "Detailed analysis complete",
                confidence = 0.95,
                metadata = mapOf("source" to "mock", "timestamp" to "2023-06-15")
            )
        )
    }
    ```

=== "Java"

    ```java
    ```

对于包含多个子图的更复杂智能体，您也可以测试图结构：

=== "Kotlin"

    ```kotlin
    @Test
    fun testMultiSubgraphAgentStructure() = runTest {
        val strategy = strategy("test") {
            val firstSubgraph by subgraph(
                "first",
                tools = listOf(DummyTool, CreateTool, SolveTool)
            ) {
                val callLLM by nodeLLMRequest(allowToolCalls = false)
                val executeTool by nodeExecuteTool()
                val sendToolResult by nodeLLMSendToolResult()
                val giveFeedback by node<String, String> { input ->
                    llm.writeSession {
                        appendPrompt {
                            user("Call tools! Don't chat!")
                        }
                    }
                    input
                }

                edge(nodeStart forwardTo callLLM)
                edge(callLLM forwardTo executeTool onToolCall { true })
                edge(callLLM forwardTo giveFeedback onAssistantMessage { true })
                edge(giveFeedback forwardTo giveFeedback onAssistantMessage { true })
                edge(giveFeedback forwardTo executeTool onToolCall { true })
                edge(executeTool forwardTo nodeFinish transformed { it.content })
            }

            val secondSubgraph by subgraph<String, String>("second") {
                edge(nodeStart forwardTo nodeFinish)
            }

            edge(nodeStart forwardTo firstSubgraph)
            edge(firstSubgraph forwardTo secondSubgraph)
            edge(secondSubgraph forwardTo nodeFinish)
        }

        val toolRegistry = ToolRegistry {
            tool(DummyTool)
            tool(CreateTool)
            tool(SolveTool)
        }

        val mockLLMApi = getMockExecutor(toolRegistry) {
            mockLLMAnswer("Hello!") onRequestContains "Hello"
            mockLLMToolCall(CreateTool, CreateTool.Args("solve")) onRequestEquals "Solve task"
        }

        val basePrompt = prompt("test") {}

        AIAgent(
            toolRegistry = toolRegistry,
            strategy = strategy,
            eventHandler = EventHandler {},
            agentConfig = AIAgentConfig(prompt = basePrompt, model = OpenAIModels.Chat.GPT4o, maxAgentIterations = 100),
            promptExecutor = mockLLMApi,
        ) {
            testGraph("test") {
                val firstSubgraph = assertSubgraphByName<String, String>("first")
                val secondSubgraph = assertSubgraphByName<String, String>("second")

                assertEdges {
                    startNode() alwaysGoesTo firstSubgraph
                    firstSubgraph alwaysGoesTo secondSubgraph
                    secondSubgraph alwaysGoesTo finishNode()
                }

                verifySubgraph(firstSubgraph) {
                    val start = startNode()
                    val finish = finishNode()

                    val askLLM = assertNodeByName<String, Message.Response>("callLLM")
                    val callTool = assertNodeByName<Message.Tool.Call, ReceivedToolResult>("executeTool")
                    val giveFeedback = assertNodeByName<Any?, Any?>("giveFeedback")

                    assertReachable(start, askLLM)
                    assertReachable(askLLM, callTool)

                    assertNodes {
                        askLLM withInput "Hello" outputs Message.Assistant("Hello!")
                        askLLM withInput "Solve task" outputs toolCallMessage(CreateTool, CreateTool.Args("solve"))

                        callTool withInput toolCallSignature(
                            SolveTool,
                            SolveTool.Args("solve")
                        ) outputs toolResult(SolveTool, "solved")

                        callTool withInput toolCallSignature(
                            CreateTool,
                            CreateTool.Args("solve")
                        ) outputs toolResult(CreateTool, "created")
                    }

                    assertEdges {
                        askLLM withOutput Message.Assistant("Hello!") goesTo giveFeedback
                        askLLM withOutput toolCallMessage(CreateTool, CreateTool.Args("solve")) goesTo callTool
                    }
                }
            }
        }
    }
    ```

=== "Java"

    ```java
    ```

## API 参考 { #complete-testing-example }

关于 Testing 功能的完整 API 参考，请参阅 [agents-test](api:agents-test::) 模块文档。

## FAQ 与故障排除 { #api-reference }

#### 如何模拟特定的工具响应？ { #how-do-i-mock-a-specific-tool-response }

在 `MockLLMBuilder` 中使用 `mockTool` 方法：

=== "Kotlin"

    ```kotlin
    val mockExecutor = getMockExecutor {
        mockTool(myTool) alwaysReturns myResult

        // Or with conditions
        mockTool(myTool) returns myResult onArguments myArgs
    }
    ```

=== "Java"

    ```java
    ```

#### 如何测试复杂的图结构？ { #how-can-i-test-complex-graph-structures }

使用子图断言、`verifySubgraph` 和节点引用：

=== "Kotlin"

    ```kotlin
    testGraph<Unit, String>("test") {
        val mySubgraph = assertSubgraphByName<Unit, String>("mySubgraph")

        verifySubgraph(mySubgraph) {
            // Get references to nodes
            val nodeA = assertNodeByName<Unit, String>("nodeA")
            val nodeB = assertNodeByName<String, String>("nodeB")

            // Assert reachability
            assertReachable(nodeA, nodeB)

            // Assert edge connections
            assertEdges {
                nodeA.withOutput("result") goesTo nodeB
            }
        }
    }
    ```

=== "Java"

    ```java
    ```

#### 如何根据输入模拟不同的 LLM 响应？ { #how-do-i-simulate-different-llm-responses-based-on-input }

使用模式匹配方法：

=== "Kotlin"

    ```kotlin
    getMockExecutor {
        mockLLMAnswer("Response A") onRequestContains "topic A"
        mockLLMAnswer("Response B") onRequestContains "topic B"
        mockLLMAnswer("Exact response") onRequestEquals "exact question"
        mockLLMAnswer("Conditional response") onCondition { it.contains("keyword") && it.length > 10 }
    }
    ```

=== "Java"

    ```java
    import ai.koog.agents.testing.tools.MockExecutor;
    import ai.koog.prompt.executor.model.PromptExecutor;

    PromptExecutor promptExecutor = MockExecutor.builder()
        .mockLLMAnswer("Response A").onRequestContains("topic A")
        .mockLLMAnswer("Response B").onRequestContains("topic B")
        .mockLLMAnswer("Exact response").onRequestEquals("exact question")
        .mockLLMAnswer("Conditional response").onCondition(s -> s.contains("keyword") && s.length() > 10)
        .build();
    ```

### 故障排除

#### 模拟执行器始终返回默认响应 { #mock-executor-always-returns-the-default-response }

检查您的模式匹配是否正确。模式区分大小写，且必须完全按照指定方式匹配。

#### 工具调用未被拦截 { #tool-calls-are-not-being-intercepted }

请确保：

1. 工具注册表已正确设置。
2. 工具名称完全匹配。
3. 工具操作已正确配置。

#### 图断言失败 { #graph-assertions-are-failing }

1. 验证节点名称是否正确。
2. 检查图结构是否符合预期。
3. 使用 `startNode()` 和 `finishNode()` 方法获取正确的入口点和出口点。
