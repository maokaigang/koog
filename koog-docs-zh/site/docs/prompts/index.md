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
    AI 代理可以接受简单的文本提示词作为输入。
    它们会自动将文本提示词转换为 Prompt 对象，并将其发送给LLM执行。
    这对于[基础代理](../agents/basic-agents.md)非常有用，
    该代理仅需运行单个请求，不需要复杂的对话逻辑。

## 运行提示词 { #running-prompts }

Koog提供了两个抽象层级来针对LLM运行提示词：LLM客户端和提示词执行器。
两者都接受 Prompt 对象，并可用于直接执行提示词，无需 AI 代理。
客户端和执行器的执行流程相同：

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

-   :material-arrow-right-bold:{ .lg .middle } [**LLM 客户端**](llm-clients.md)

    ---

    用于与特定LLM提供商直接交互的低级接口。
    当您与单一提供商合作且不需要高级生命周期管理时使用。

-   :material-swap-horizontal:{ .lg .middle } [**提示词执行器**](prompt-executors.md)

    ---

    管理一个或多个LLM客户端生命周期的高级抽象。
    当您需要统一的API来跨多个提供商运行提示词，
    并支持动态切换和故障转移时使用。

</div>

## 优化性能和处理故障 { #optimizing-performance-and-handling-failures }

Koog允许您在运行提示词时优化性能和处理故障。

<div class="grid cards" markdown>-   :material-cached:{ .lg .middle } [**LLM 响应缓存**](llm-response-caching.md)

    ---

    缓存 LLM 响应，以优化性能并降低重复请求的成本。

-   :material-shield-check:{ .lg .middle } [**处理失败情况**](handling-failures.md)

    ---

    在应用程序中使用内置的重试、超时和其他错误处理机制。

</div>

## AI 代理中的提示词 { #prompts-in-ai-agents }

在 Koog 中，AI 代理在其生命周期内维护和管理提示词。
虽然使用 LLM 客户端或执行器来运行提示词，但代理负责处理提示词的更新流程，确保对话历史保持相关性和一致性。

代理中的提示词生命周期通常包括以下几个阶段：

1.  初始提示词设置。
2.  自动提示词更新。
3.  上下文窗口管理。
4.  手动提示词管理。

### 初始提示词设置 { #initial-prompt-setup }

当你[初始化一个代理](../quickstart.md#create-your-first-koog-agent)时，可以定义一个[系统消息](prompt-creation/index.md#system-message)来设定代理的行为。
然后，当你调用代理的 `run()` 方法时，通常会提供一个初始的[用户消息](prompt-creation/index.md#user-messages)作为输入。
这些消息共同构成了代理的初始提示词。例如：

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
    // 创建代理
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(apiKey),
        systemPrompt = "You are a helpful assistant.",
        llmModel = OpenAIModels.Chat.GPT4o
    )
    
    // 运行代理
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

在此示例中，代理自动将文本提示词转换为 Prompt 对象并发送给提示词执行器：

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

对于更高级的配置，你也可以使用 [AIAgentConfig](api:agents-core::ai.koog.agents.core.agent.config.AIAgentConfig) 来定义代理的初始提示词。

### 自动提示词更新 { #automatic-prompt-updates }

当代理运行其策略时，[预定义节点](../nodes-and-components.md)会自动更新提示词。例如：

-   [`nodeLLMRequest`](../nodes-and-components.md#nodellmrequest)：将用户消息附加到提示词中，并捕获 LLM 响应。
-   [`nodeLLMSendToolResult`](../nodes-and-components.md#nodellmsendtoolresult)：将工具执行结果附加到对话中。
-   [`nodeAppendPrompt`](../nodes-and-components.md#nodeappendprompt)：在工作流的任意时间点将特定消息插入到提示词中。

### 上下文窗口管理 { #context-window-management }

为了避免在长时间运行的交互中超出 LLM 上下文窗口，代理可以使用[历史压缩](../history-compression.md)功能。

### 手动提示词管理 { #manual-prompt-management }

对于复杂的工作流，你可以使用 [LLM 会话](../sessions.md)手动管理提示词。
在代理策略或自定义节点中，可以使用 `llm.writeSession` 来访问和更改 `Prompt` 对象。
这允许你根据需要添加、删除或重新排序消息。