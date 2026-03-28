<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:06:20+00:00", "source_path": "prompts/index.md", "source_sha256": "0a9eb7870b9b31fa43b0997aaf37a5022900804e3f9a38ddae877759719e0c1c", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 提示词 { #prompts }

提示词是指导大型语言模型（LLM）生成回复的指令。
它们定义了您与LLM交互的内容和结构。
本节介绍如何使用Koog创建和运行提示词。

## 创建提示词 { #creating-prompts }

在Koog中，提示词是[**Prompt**](api:prompt-model::ai.koog.prompt.dsl.Prompt)数据类的实例，具有以下属性：

- `id`：提示词的唯一标识符。
- `messages`：表示与LLM对话的消息列表。
- `params`：可选的[LLM配置参数](prompt-creation/index.md#prompt-parameters)（例如温度、工具选择等）。

虽然您可以直接实例化`Prompt`类，
但推荐的创建提示词方式是使用[Kotlin DSL](prompt-creation/index.md)或Java构建器API，
它们提供了定义对话的结构化方式。

!!! note
    本页的Kotlin示例使用Kotlin DSL。Java示例使用`Prompt.builder("id")`构建器，并酌情使用`system(...)`、`user(...)`、`assistant(...)`、`toolCall(...)`、`toolResult(...)`和`withOutput(Foo.class)`等显式方法。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    -->
    ```kotlin
    val myPrompt = prompt("hello-koog") {
        system("You are a helpful assistant.")
        user("What is Koog?")
    }
    ```
    <!--- KNIT example-prompts-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    var myPrompt = Prompt.builder("hello-koog")
        .system("You are a helpful assistant.")
        .user("What is Koog?")
        .build();
    ```
    <!--- KNIT example-prompts-java-01.java -->

!!! note
    AI agents can take a simple text prompt as input.
    They automatically convert the text prompt to the Prompt object and send it to the LLM for execution.
    This is useful for a [basic agent](../agents/basic-agents.md)
    that only needs to run a single request and does not require complex conversation logic.

## Running prompts

Koog provides two levels of abstraction for running prompts against LLMs: LLM clients and prompt executors.
Both accept Prompt objects and can be used for direct prompt execution, without an AI agent.
The execution flow is the same for both clients and executors:

```mermaid
flowchart TB
    A([Prompt built with Kotlin DSL or Java builder])
    B{LLM client or prompt executor}
    C[LLM provider]
    D([Response to your application])

    A -->|"passed to"| B
    B -->|"sends request"| C
    C -->|"returns response"| B
    B -->|"returns result"| D
```
<!--- KNIT example-prompts-01.txt -->

<div class="grid cards" markdown>

-   :material-arrow-right-bold:{ .lg .middle } [**LLM clients**](llm-clients.md)

    ---

    Low‑level interfaces for direct interaction with specific LLM providers.
    Use them when you work with a single provider and do not need advanced lifecycle management.

-   :material-swap-horizontal:{ .lg .middle } [**Prompt executors**](prompt-executors.md)

    ---

    High-level abstractions that manage the lifecycles of one or multiple LLM clients.
    Use them when you need a unified API for running prompts across multiple providers,
    with dynamic switching between them and fallbacks.

</div>

## Optimizing performance and handling failures

Koog allows you to optimize performance and handle failures when running prompts.

<div class="grid cards" markdown>

-   :material-cached:{ .lg .middle } [**LLM response caching**](llm-response-caching.md)

    ---

    Cache LLM responses to optimize performance and reduce costs for repeated requests.

-   :material-shield-check:{ .lg .middle } [**Handling failures**](handling-failures.md)

    ---

    Use built-in retries, timeouts, and other error handling mechanisms in your application.

</div>

## Prompts in AI agents

In Koog, AI agents maintain and manage prompts during their lifecycle.
While LLM clients or executors are used to run prompts, agents handle the flow of prompt updates, ensuring the
conversation history remains relevant and consistent.

The prompt lifecycle in an agent usually includes several stages:

1. Initial prompt setup.
2. Automatic prompt updates.
3. Context window management.
4. Manual prompt management.

### Initial prompt setup

When you [initialize an agent](../quickstart.md#create-your-first-koog-agent),
you can define a [system message](prompt-creation/index.md#system-message) that sets the agent's behavior.
Then, when you call the agent's `run()` method,
you typically provide an initial [user message](prompt-creation/index.md#user-messages) as input.
Together, these messages form the agent's initial prompt. For example: 

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import kotlinx.coroutines.runBlocking
    val apiKey = System.getenv("OPENAI_API_KEY")
    fun main() = runBlocking {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    // Create an agent
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(apiKey),
        systemPrompt = "You are a helpful assistant.",
        llmModel = OpenAIModels.Chat.GPT4o
    )
    
    // Run the agent
    val result = agent.run("What is Koog?")
    ```
    <!--- KNIT example-prompts-02.kt -->

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
        .systemPrompt("You are a helpful assistant. Answer user questions concisely.")
        .llmModel(OpenAIModels.Chat.GPT4o)
        .build();

    var result = agent.run("What is Koog?");
    ```
    <!--- KNIT example-prompts-java-02.java -->

In the example, the agent automatically converts the text prompt to the Prompt object and sends it to the prompt executor:

```mermaid
flowchart TB
    A([Your application])
    B{{Configured AI agent}}
    C["Text prompt"]
    D["Prompt object"]
    E{{Prompt executor}}
    F[LLM provider]

    A -->|"run() with text"| B
    B -->|"takes"| C
    C -->|"converted to"| D
    D -->|"sent via"| E
    E -->|"calls"| F
    F -->|"responds to"| E
    E -->|"result to"| B
    B -->|"result to"| A
```
<!--- KNIT example-prompts-02.txt -->

For more advanced configurations, you can also use [AIAgentConfig](api:agents-core::ai.koog.agents.core.agent.config.AIAgentConfig)
to define the agent's initial prompt.

### Automatic prompt updates

As the agent runs its strategy, [predefined nodes](../nodes-and-components.md) automatically update the prompt.
For example:

- [`nodeLLMRequest`](../nodes-and-components.md#nodellmrequest): Appends a user message to the prompt and captures the LLM response.
- [`nodeLLMSendToolResult`](../nodes-and-components.md#nodellmsendtoolresult): Appends tool execution results to the conversation.
- [`nodeAppendPrompt`](../nodes-and-components.md#nodeappendprompt): Inserts specific messages into the prompt at any point in the workflow.

### Context window management

To avoid exceeding the LLM context window in long-running interactions, agents can use the
[history compression](../history-compression.md) feature.

### Manual prompt management

For complex workflows, you can manage the prompt manually using [LLM sessions](../sessions.md).
In an agent strategy or custom node, you can use `llm.writeSession` to access and change the `Prompt` object.
This lets you add, remove, or reorder messages as needed.