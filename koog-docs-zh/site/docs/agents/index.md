<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:54:17+00:00", "source_path": "agents/index.md", "source_sha256": "56cf6b6937eeafea8a50ace73d572e25481d59138192ca40d34727bb5d6d9954", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 智能体 { #agents }

AI 智能体是能够自主推理、做出决策、与环境交互并执行行动以实现特定目标的自治系统。在 Koog 中，AI 智能体不仅仅是围绕 LLM 的简单封装；它是一个为 JVM 生态系统设计的结构化、类型安全的状态机。

Koog 智能体围绕以下核心概念构建：

- [提示执行器](../prompts/prompt-executors.md) 负责管理和执行提示，使智能体能够与 LLM 交互以进行推理和决策。
- [策略](../nodes-and-components.md) 定义了智能体的工作流程。它可以是有向图、函数或规划器的形式。详见[智能体类型](#agent-types)。
- 智能体可以使用[工具](../tools-overview.md)与外部数据源和服务进行交互。
- 您可以使用[功能特性](../features/index.md)来扩展和增强 AI 智能体的功能。

!!! tip

    有关创建和运行一个最小化智能体的信息，请参阅[快速入门](../quickstart.md)。

## 智能体类型 { #agent-types }

根据您需要执行的任务，Koog 提供了多种智能体类型：

- [基础智能体](basic-agents.md) 适用于不需要任何自定义逻辑的简单任务。这些智能体实现了适用于大多数常见用例的预定义策略。
- [基于图的智能体](graph-based-agents.md) 提供了对智能体工作流程、状态管理和可视化的完全控制和灵活性。
- [函数式智能体](functional-agents.md) 使您能够快速将自定义逻辑原型化为一个函数，并访问智能体的上下文。
- [规划器智能体](planner-agents/index.md) 能够通过迭代循环自主规划和执行多步骤任务，直到达到期望的最终状态。

## 智能体配置 { #agent-configuration }

智能体配置定义了智能体的执行参数，包括初始提示、语言模型和迭代限制。

!!! tip

    有关创建和运行一个最小化智能体的信息，请参阅[快速入门](../quickstart.md)。

对于简单的智能体，除了必需的提示执行器和语言模型外，您可以直接在智能体构造函数中指定初始系统提示和其他一些参数：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    -->
    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant.",
        temperature = 0.7,
        maxIterations = 10
    )
    ```
    <!--- KNIT example-agent-config-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")))
        .llmModel(OpenAIModels.Chat.GPT4o)
        .systemPrompt("You are a helpful assistant.")
        .temperature(0.7)
        .maxIterations(10)
        .build();
    ```
    <!--- KNIT example-agent-config-java-01.java -->

或者，您可以创建一个 [`AIAgentConfig`](https://api.koog.ai/agents/agents-core/ai.koog.agents.core.agent.config/-a-i-agent-config/index.html) 实例来更细致地定义代理的行为和参数，然后将其传递给代理构造函数。这样您就可以定义包含多条消息、对话历史、LLM 参数以及其他执行参数的复杂提示。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.agent.config.AIAgentConfig
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import ai.koog.prompt.params.LLMParams
    -->
    ```kotlin
    val agentConfig = AIAgentConfig(
        prompt = prompt(
            id = "assistant",
            params = LLMParams(
                temperature = 0.7
            )
        ) {
            system("You are a helpful assistant.")
        },
        model = OpenAIModels.Chat.GPT4o,
        maxAgentIterations = 10
    )

    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
        agentConfig = agentConfig
    )
    ```
    <!--- KNIT example-agent-config-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    Prompt prompt = Prompt.builder("assistant")
        .system("You are a helpful assistant.")
        .build()
        .withParams(new LLMParams(
            0.7,         // temperature
            null,        // maxTokens
            1,           // numberOfChoices
            null,        // speculation
            null,        // schema
            LLMParams.ToolChoice.Auto.INSTANCE, // toolChoice
            null,        // user
            null         // additionalProperties
        ));

    AIAgentConfig agentConfig = AIAgentConfig.builder(OpenAIModels.Chat.GPT4o)
        .prompt(prompt)
        .maxAgentIterations(10)
        .build();

    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
        .agentConfig(agentConfig)
        .build();
    ```
    <!--- KNIT example-agent-config-java-02.java -->

以下是 `AIAgentConfig` 的参数：

- `prompt` 定义了初始的 [提示](../prompts/prompt-creation/index.md) 和 [LLM 参数](../llm-parameters.md)。

- `model` 指定了代理与之交互的语言模型。
  您可以使用预定义的模型之一或[创建自定义模型配置](../model-capabilities.md#creating-a-model-llmodel-configuration)。

- `maxAgentIterations` 限制了代理在终止前可以执行的最大步数。
  每个步骤都是智能体工作流中的一个[节点](../nodes-and-components.md)。

- `missingToolsConversionStrategy` 定义了一种在代理执行过程中处理缺失工具的策略。

[//]: # (TODO 在工具部分撰写关于缺失工具的内容，并从此处链接)

- `responseProcessor` 可用于定义自定义响应处理器。
  例如，它可以审核和验证响应内容、更改响应格式或记录响应。

[//]: # (TODO 是否需要在某处撰写关于响应处理的内容？)