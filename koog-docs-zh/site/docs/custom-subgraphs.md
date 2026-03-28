<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:55:52+00:00", "source_path": "custom-subgraphs.md", "source_sha256": "b833c3e32d36b8891d832b7bf3ed9a714fc3dc3b2f78489aaddda1766202e7ff", "source_tag": "0.7.3", "translation_status": "changed"} -->
## 创建和配置子图 { #creating-and-configuring-subgraphs }

以下部分提供了为智能体工作流创建子图时的代码模板和常见模式。

### 基础子图创建 { #basic-subgraph-creation }

自定义子图通常使用以下模式创建：

* 指定工具选择策略的子图：

=== "Kotlin"

    ```kotlin
    strategy<StrategyInput, StrategyOutput>("strategy-name") {
        val subgraphIdentifier by subgraph<Input, Output>(
            name = "subgraph-name",
            toolSelectionStrategy = ToolSelectionStrategy.ALL
        ) {
            // Define nodes and edges for this subgraph
        }
    
        nodeStart then subgraphIdentifier then nodeFinish
    }
    ```

=== "Java"

    ```java
    var strategyBuilder = AIAgentGraphStrategy.builder("strategy-name")
        .withInput(String.class)
        .withOutput(String.class);

    var subgraphIdentifier = AIAgentSubgraph.builder("subgraph-name")
        .withToolSelectionStrategy(ToolSelectionStrategy.ALL.INSTANCE)
        .withInput(String.class)
        .withOutput(String.class)
        .define(subgraph -> {
            // Define nodes and edges for this subgraph
        })
        .build();

    var strategy = strategyBuilder
        .edge(strategyBuilder.nodeStart, subgraphIdentifier)
        .edge(subgraphIdentifier, strategyBuilder.nodeFinish)
        .build();
    ```

* 具有指定工具列表的子图（来自已定义工具注册表的工具子集）：

=== "Kotlin"

    ```kotlin
    strategy<StrategyInput, StrategyOutput>("strategy-name") {
       val subgraphIdentifier by subgraph<Input, Output>(
           name = "subgraph-name",
           tools = listOf(firstTool, secondTool)
       ) {
            // Define nodes and edges for this subgraph
        }
    }
    ```

=== "Java"

    ```java
    var strategyBuilder = AIAgentGraphStrategy.builder("strategy-name")
        .withInput(String.class)
        .withOutput(String.class);

    var subgraphIdentifier = AIAgentSubgraph.builder("subgraph-name")
        .limitedTools(List.of(firstTool, secondTool))
        .withInput(String.class)
        .withOutput(String.class)
        .define(subgraph -> {
            // Define nodes and edges for this subgraph
        })
        .build();

    var strategy = strategyBuilder
        .edge(strategyBuilder.nodeStart, subgraphIdentifier)
        .edge(subgraphIdentifier, strategyBuilder.nodeFinish)
        .build();
    ```

有关参数和参数值的更多信息，请参阅`subgraph` [API 参考](api:agents-core::ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase.subgraph)。有关工具的更多信息，请参阅[工具](tools-overview.md)。

以下代码示例展示了一个自定义子图的实际实现：

=== "Kotlin"

    ```kotlin
    strategy<String, String>("my-strategy") {
       val mySubgraph by subgraph<String, String>(
          tools = listOf(firstTool, secondTool)
       ) {
            // Define nodes and edges for this subgraph
            val sendInput by nodeLLMRequest()
            val executeToolCall by nodeExecuteTool()
            val sendToolResult by nodeLLMSendToolResult()

            edge(nodeStart forwardTo sendInput)
            edge(sendInput forwardTo executeToolCall onToolCall { true })
            edge(executeToolCall forwardTo sendToolResult)
            edge(sendToolResult forwardTo nodeFinish onAssistantMessage { true })
        }
    }
    ```

=== "Java"

    ```java
    var strategyBuilder = AIAgentGraphStrategy.builder("my-strategy")
            .withInput(String.class)
            .withOutput(String.class);

    var sendInput = AIAgentNode.llmRequest();
    var executeToolCall = AIAgentNode.executeTool();
    var sendToolResult = AIAgentNode.llmSendToolResult();

    var mySubgraph = AIAgentSubgraph.builder()
        .limitedTools(List.of(firstTool, secondTool))
        .withInput(String.class)
        .withOutput(String.class)
        .define(subgraph -> {
            // Define nodes and edges for this subgraph
            subgraph
                .edge(subgraph.nodeStart, sendInput)
                .edge(AIAgentEdge.builder()
                    .from(sendInput)
                    .to(executeToolCall)
                    .onIsInstance(Message.Tool.Call.class)
                    .build()
                )
                .edge(executeToolCall, sendToolResult)
                .edge(AIAgentEdge.builder()
                    .from(sendToolResult)
                    .to(subgraph.nodeFinish)
                    .onIsInstance(Message.Assistant.class)
                    .transformed(Message.Assistant::getContent)
                    .build()
                )
                .build();

        })
        .build();

    var strategy = strategyBuilder
        .edge(strategyBuilder.nodeStart, mySubgraph)
        .edge(mySubgraph, strategyBuilder.nodeFinish)
        .build();
    ```

### 在子图中配置工具 { #configuring-tools-in-a-subgraph }

子图可以通过多种方式配置工具：

* 直接在子图定义中：

=== "Kotlin"

    ```kotlin
    val mySubgraph by subgraph<String, String>(
       tools = listOf(AskUser)
     ) {
        // Subgraph definition
     }
    ```

=== "Java"

    ```java
    var mySubgraph = AIAgentSubgraph.builder()
        .limitedTools(List.of(AskUser.INSTANCE))
        .withInput(String.class)
        .withOutput(String.class)
        .define(subgraph -> {
            // Subgraph definition
        })
        .build();
    ```

* 来自工具注册表：

=== "Kotlin"

    ```kotlin
    val mySubgraph by subgraph<String, String>(
        tools = listOf(toolRegistry.getTool("AskUser"))
    ) {
        // Subgraph definition
    }
    ```

=== "Java"

    ```java
    var mySubgraph = AIAgentSubgraph.builder()
        .limitedTools(List.of(toolRegistry.getTool("AskUser")))
        .withInput(String.class)
        .withOutput(String.class)
        .define(subgraph -> {
            // Subgraph definition
        })
        .build();
    ```

* 动态执行期间：

=== "Kotlin"

    ```kotlin
    // Make a set of tools
    this.llm.writeSession {
        tools = tools.filter { it.name in listOf("first_tool_name", "second_tool_name") }
    }
    ```

=== "Java"

    ```java
    var node = AIAgentNode.builder("node_name")
        .withInput(String.class)
        .withOutput(String.class)
        .withAction((input, ctx) -> {
            // Make a set of tools
            ctx.getLlm().writeSession(session -> {
                session.setTools(session.getTools().stream()
                    .filter(t -> List.of("first_tool_name", "second_tool_name").contains(t.getName()))
                    .collect(Collectors.toList()));
                return null;
            });
            return input;
        })
        .build();
    ```

## 高级子图技术 { #advanced-subgraph-techniques }

### 多部分策略 { #multi-part-strategies }

复杂的工作流程可以分解为多个子图，每个子图负责处理流程中的特定部分：

=== "Kotlin"

    ```kotlin
    strategy("complex-workflow") {
       val inputProcessing by subgraph<String, A>(
       ) {
          // Process the initial input
       }

       val reasoning by subgraph<A, B>(
       ) {
          // Perform reasoning based on the processed input
       }

       val toolRun by subgraph<B, C>(
          // Optional subset of tools from the tool registry
          tools = listOf(firstTool, secondTool)
       ) {
          // Run tools based on the reasoning
       }

       val responseGeneration by subgraph<C, String>(
       ) {
          // Generate a response based on the tool results
       }

       nodeStart then inputProcessing then reasoning then toolRun then responseGeneration then nodeFinish

    }
    ```

=== "Java"

    ```java
    var strategyBuilder = AIAgentGraphStrategy.builder("complex-workflow")
            .withInput(String.class)
            .withOutput(String.class);

    var inputProcessing = AIAgentSubgraph.builder()
        .withInput(String.class)
        .withOutput(String.class)
        .define(subgraph -> {
            // Process the initial input
        })
        .build();

    var reasoning = AIAgentSubgraph.builder()
        .withInput(String.class)
        .withOutput(String.class)
        .define(subgraph -> {
            // Perform reasoning based on the processed input
        })
        .build();

    var toolRun = AIAgentSubgraph.builder()
        // Optional subset of tools from the tool registry
        .limitedTools(List.of(firstTool, secondTool))
        .withInput(String.class)
        .withOutput(String.class)
        .define(subgraph -> {
            // Run tools based on the reasoning
        })
        .build();

    var responseGeneration = AIAgentSubgraph.builder()
        .withInput(String.class)
        .withOutput(String.class)
        .define(subgraph -> {
            // Generate a response based on the tool results
        })
        .build();

    var strategy = strategyBuilder
        .edge(strategyBuilder.nodeStart, inputProcessing)
        .edge(inputProcessing, reasoning)
        .edge(reasoning, toolRun)
        .edge(toolRun, responseGeneration)
        .edge(responseGeneration, strategyBuilder.nodeFinish)
        .build();
    ```

## 最佳实践 { #best-practices }

在使用子图时，请遵循以下最佳实践：

1. **将复杂工作流拆分为子图**：每个子图应具备清晰、专注的职责范围。

2. **仅传递必要的上下文**：仅传递后续子图正常运行所需的信息。

3. **记录子图依赖关系**：清晰记录每个子图对前序子图的依赖需求，以及它为后续子图提供的输出内容。

4. **独立测试子图**：在将每个子图集成到策略之前，确保其能够正确处理各种输入。

5. **考虑令牌使用**：注意令牌使用情况，尤其是在子图之间传递大量历史记录时。

## Troubleshooting

### 工具不可用 { #tools-not-available }

如果子图中没有可用的工具：

- 检查工具是否已在工具注册表中正确注册。

### 子图未按定义和预期顺序运行 { #subgraphs-not-running-in-the-defined-and-expected-order }

如果子图未按定义的顺序执行：

- 检查策略定义，确保子图按正确顺序排列。
- 验证每个子图是否正确将其输出传递给下一个子图。
- 确保你的子图与其余子图相连，并且可以从起点（和终点）访问。注意条件边，确保它们覆盖所有可能的继续条件，以免在子图或节点中被阻塞。

## Examples

以下示例展示了在实际场景中如何使用子图来构建智能体策略。代码示例包含三个已定义的子图：`researchSubgraph`、`planSubgraph` 和 `executeSubgraph`，每个子图在助手流程中都具有明确且独立的功能定位。

=== "Kotlin"

    ```kotlin
    // Define the agent strategy
    val strategy = strategy<String, String>("assistant") {

        // A subgraph that includes a tool call
        val researchSubgraph by subgraph<String, String>(
            "research_subgraph",
            tools = listOf(WebSearchTool())
        ) {
            val nodeCallLLM by nodeLLMRequest("call_llm")
            val nodeExecuteTool by nodeExecuteTool()
            val nodeSendToolResult by nodeLLMSendToolResult()

            edge(nodeStart forwardTo nodeCallLLM)
            edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
            edge(nodeExecuteTool forwardTo nodeSendToolResult)
            edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })
            edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
        }

        val planSubgraph by subgraph(
            "plan_subgraph",
            tools = listOf()
        ) {
            val nodeUpdatePrompt by node<String, Unit> { research ->
                llm.writeSession {
                    rewritePrompt {
                        prompt("research_prompt") {
                            system(
                                "You are given a problem and some research on how it can be solved." +
                                        "Make step by step a plan on how to solve given task."
                            )
                            user("Research: $research")
                        }
                    }
                }
            }
            val nodeCallLLM by nodeLLMRequest("call_llm")

            edge(nodeStart forwardTo nodeUpdatePrompt)
            edge(nodeUpdatePrompt forwardTo nodeCallLLM transformed { "Task: $agentInput" })
            edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
        }

        val executeSubgraph by subgraph<String, String>(
            "execute_subgraph",
            tools = listOf(DoAction(), DoAnotherAction()),
        ) {
            val nodeUpdatePrompt by node<String, Unit> { plan ->
                llm.writeSession {
                    rewritePrompt {
                        prompt("execute_prompt") {
                            system(
                                "You are given a task and detailed plan how to execute it." +
                                        "Perform execution by calling relevant tools."
                            )
                            user("Execute: $plan")
                            user("Plan: $plan")
                        }
                    }
                }
            }
            val nodeCallLLM by nodeLLMRequest("call_llm")
            val nodeExecuteTool by nodeExecuteTool()
            val nodeSendToolResult by nodeLLMSendToolResult()

            edge(nodeStart forwardTo nodeUpdatePrompt)
            edge(nodeUpdatePrompt forwardTo nodeCallLLM transformed { "Task: $agentInput" })
            edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
            edge(nodeExecuteTool forwardTo nodeSendToolResult)
            edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })
            edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
        }

        nodeStart then researchSubgraph then planSubgraph then executeSubgraph then nodeFinish
    }
    ```

=== "Java"

    ```java
    // Define the agent strategy
    var strategyBuilder = AIAgentGraphStrategy.builder("assistant")
        .withInput(String.class)
        .withOutput(String.class);

    // A subgraph that includes a tool call
    var nodeCallLLM = AIAgentNode.llmRequest();
    var nodeExecuteTool = AIAgentNode.executeTool();
    var nodeSendToolResult = AIAgentNode.llmSendToolResult();

    var researchSubgraph = AIAgentSubgraph.builder("research_subgraph")
        .limitedTools(new WebSearchToolSet())
        .withInput(String.class)
        .withOutput(String.class)
        .define(subgraph -> {
            subgraph
                .edge(subgraph.nodeStart, nodeCallLLM)
                .edge(AIAgentEdge.builder()
                    .from(nodeCallLLM)
                    .to(nodeExecuteTool)
                    .onIsInstance(Message.Tool.Call.class)
                    .build()
                )
                .edge(nodeExecuteTool, nodeSendToolResult)
                .edge(AIAgentEdge.builder()
                    .from(nodeSendToolResult)
                    .to(nodeExecuteTool)
                    .onIsInstance(Message.Tool.Call.class)
                    .build()
                )
                .edge(AIAgentEdge.builder()
                    .from(nodeCallLLM)
                    .to(subgraph.nodeFinish)
                    .onIsInstance(Message.Assistant.class)
                    .transformed(Message.Assistant::getContent)
                    .build()
                )
                .build();
        })
        .build();

    var nodeUpdatePrompt = AIAgentNode.builder()
        .withInput(String.class)
        .withOutput(String.class)
        .withAction((research, ctx) -> {
            ctx.getLlm().writeSession(session -> {
                session.setPrompt(Prompt.builder("research_prompt")
                    .system(
                        "You are given a problem and some research on how it can be solved." +
                        "Make step by step a plan on how to solve given task."
                    )
                    .user("Research: " + research)
                    .build());
                return null;
            });
            return "Task: " + ctx.getAgentInput();
        })
        .build();
    var nodeCallLLMPlan = AIAgentNode.llmRequest();

    var planSubgraph = AIAgentSubgraph.builder("plan_subgraph")
        .limitedTools(Collections.emptyList())
        .withInput(String.class)
        .withOutput(String.class)
        .define(subgraph -> {
            subgraph
                .edge(subgraph.nodeStart, nodeUpdatePrompt)
                .edge(nodeUpdatePrompt, nodeCallLLMPlan)
                .edge(AIAgentEdge.builder()
                    .from(nodeCallLLMPlan)
                    .to(subgraph.nodeFinish)
                    .onIsInstance(Message.Assistant.class)
                    .transformed(Message.Assistant::getContent)
                    .build()
                )
                .build();
        })
        .build();

    var nodeUpdatePromptExecute = AIAgentNode.builder()
        .withInput(String.class)
        .withOutput(String.class)
        .withAction((plan, ctx) -> {
            ctx.getLlm().writeSession(session -> {
                session.setPrompt(Prompt.builder("execute_prompt")
                    .system(
                        "You are given a task and detailed plan how to execute it." +
                        "Perform execution by calling relevant tools."
                    )
                    .user("Execute: " + plan)
                    .user("Plan: " + plan)
                    .build());
                return null;
            });
            return "Task: " + ctx.getAgentInput();
        })
        .build();

    var nodeCallLLMExecute = AIAgentNode.llmRequest();
    var nodeExecuteToolExecute = AIAgentNode.executeTool();
    var nodeSendToolResultExecute = AIAgentNode.llmSendToolResult();

    var executeSubgraph = AIAgentSubgraph.builder("execute_subgraph")
        .limitedTools(new ActionToolSet())
        .withInput(String.class)
        .withOutput(String.class)
        .define(subgraph -> {
            subgraph
                .edge(subgraph.nodeStart, nodeUpdatePromptExecute)
                .edge(nodeUpdatePromptExecute, nodeCallLLMExecute)
                .edge(AIAgentEdge.builder()
                    .from(nodeCallLLMExecute)
                    .to(nodeExecuteToolExecute)
                    .onIsInstance(Message.Tool.Call.class)
                    .build()
                )
                .edge(nodeExecuteToolExecute, nodeSendToolResultExecute)
                .edge(AIAgentEdge.builder()
                    .from(nodeSendToolResultExecute)
                    .to(nodeExecuteToolExecute)
                    .onIsInstance(Message.Tool.Call.class)
                    .build()
                )
                .edge(AIAgentEdge.builder()
                    .from(nodeCallLLMExecute)
                    .to(subgraph.nodeFinish)
                    .onIsInstance(Message.Assistant.class)
                    .transformed(Message.Assistant::getContent)
                    .build()
                )
                .build();
        })
        .build();

    var strategy = strategyBuilder
        .edge(strategyBuilder.nodeStart, researchSubgraph)
        .edge(researchSubgraph, planSubgraph)
        .edge(planSubgraph, executeSubgraph)
        .edge(executeSubgraph, strategyBuilder.nodeFinish)
        .build();
    ```
