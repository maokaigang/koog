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

Alternatively, you can create an instance of [`AIAgentConfig`](https://api.koog.ai/agents/agents-core/ai.koog.agents.core.agent.config/-a-i-agent-config/index.html)
to define the agent's behavior and parameters more granularly, then pass it to the agent constructor.
This enables you to define complex prompts with multiple messages,
conversation history, LLM parameters, and additional execution parameters.

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

Here are the parameters of `AIAgentConfig`:

- `prompt` defines the initial [prompt](../prompts/prompt-creation/index.md) and [LLM parameters](../llm-parameters.md).

- `model` specifies the language model with which the agent interacts.
  You can use one of the predefined models or [create a custom model configuration](../model-capabilities.md#creating-a-model-llmodel-configuration).

- `maxAgentIterations` limits the maximum number of steps the agent can take before it terminates.
  Each step is a [node](../nodes-and-components.md) in the agent's workflow.

- `missingToolsConversionStrategy` defines a strategy for handling missing tools during agent execution.

[//]: # (TODO write about missing tools in the TOols section and link from here)

- `responseProcessor` can be used to define a custom response processor.
  For example, it can moderate and validate the response content, change the response format, or log the response.

[//]: # (TODO write about response processing somewhere?)