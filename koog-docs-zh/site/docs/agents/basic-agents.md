<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:51:28+00:00", "source_path": "agents/basic-agents.md", "source_sha256": "ad9fdc835cb8b3090d6e85ad4ad36c941f77568c08b6c3b546a7b97474569362", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 基础智能体 { #basic-agents }

基础智能体采用预定义策略和简单执行流程，适用于大多数常见场景。
它接收字符串输入（问题、请求或任务描述）并将其发送至配置的 LLM。
LLM 可决定是否调用提供的工具。
智能体将执行工具并将结果返回给 LLM。
此过程重复进行，直到 LLM 不再请求工具调用并返回字符串响应。
随后智能体输出该响应。

在[基于图的智能体](graph-based-agents.md)中，
您可了解如何重建基础智能体使用的预定义策略图。

??? note "前置条件"

    --8<-- "quickstart-snippets.md:prerequisites"

    --8<-- "quickstart-snippets.md:dependencies"

    --8<-- "quickstart-snippets.md:api-key"

    本页示例假设您已设置 `OPENAI_API_KEY` 环境变量。

## 创建最小化智能体 { #create-a-minimal-agent }

要创建最基础的智能体，请实例化 [`AIAgent`](https://api.koog.ai/agents/agents-core/ai.koog.agents.core.agent/-a-i-agent/index.html)
并提供包含[语言模型](../model-capabilities.md#creating-a-model-llmodel-configuration)的[提示执行器](../prompts/prompt-executors.md)：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import kotlinx.coroutines.runBlocking
    -->
    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
        llmModel = OpenAIModels.Chat.GPT4o
    )
    ```

    This agent will expect a string as input and return a string as output.
    To run the agent, use the `run()` function with some user input:

    ```kotlin
    fun main() = runBlocking {
        val result = agent.run("Hello! How can you help me?")
        println(result)
    }
    ```
    <!--- KNIT example-basic-01.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    
    import static ai.koog.prompt.executor.llms.all.SimplePromptExecutorsKt.simpleOpenAIExecutor;

    class exampleBasicJava01 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
        .llmModel(OpenAIModels.Chat.GPT4o)
        .build();
    ```

    This agent expects a string as input and returns a string as output.
    To run the agent, use the `run()` method with some user input:

    ```java
    String result = agent.run("Hello! How can you help me?");
    System.out.println(result);
    ```
    <!--- KNIT exampleBasicJava01.java -->

The agent will return a generic answer, such as:

```text
I can assist with a wide range of topics and tasks. Here are some examples:

1. **Answering questions**: I can provide information on various subjects, from science and history to entertainment and culture.
2. **Generating text**: I can help with writing tasks, such as suggesting alternative phrases, providing definitions, or even creating entire articles or stories.
3. **Translation**: I can translate text from one language to another, including popular languages such as Spanish, French, German, Chinese, and many more.
4. **Conversation**: I can engage in natural-sounding conversations, using context and understanding to respond to questions and statements.
5. **Brainstorming**: I can help generate ideas for creative projects, such as writing stories, composing music, or coming up with business ideas.
6. **Learning**: I can help with language learning, explaining grammar rules, vocabulary, and pronunciation.
7. **Calculations**: I can perform mathematical calculations, including basic arithmetic, algebra, and more advanced math concepts.

What's on your mind? Do you have a specific question, topic, or task you'd like to tackle?
```
<!--- KNIT example-basic-01.txt -->

## Add a system prompt

Provide a [system message](../prompts/prompt-creation/index.md#system-message) to define the agent's role
as well as the purpose, context, and instructions related to the task.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    -->
    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
        systemPrompt = "You are an expert in internet memes. Be helpful, friendly, and answer user questions concisely, showing your knowledge of memes.",
        llmModel = OpenAIModels.Chat.GPT4o
    )
    ```
    <!--- KNIT example-basic-02.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    
    import static ai.koog.prompt.executor.llms.all.SimplePromptExecutorsKt.simpleOpenAIExecutor;

    class exampleBasicJava02 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
        .systemPrompt("You are an expert in internet memes. Be helpful, friendly, and answer user questions concisely, showing your knowledge of memes.")
        .llmModel(OpenAIModels.Chat.GPT4o)
        .build();
    ```
    <!--- KNIT exampleBasicJava02.java -->

The instructions in the system prompt will guide the agent's response:

```text
I'm here to help you navigate the wild world of internet memes!

What's on your mind? Are you trying to understand a specific meme, need help finding a popular joke, or perhaps want some recommendations for trending memes? Let me know, and I'll do my best to provide you with some LOLs!
```
<!--- KNIT example-basic-02.txt -->

## Configure LLM output

You can provide some [LLM parameters](../llm-parameters.md#llm-parameter-reference) directly to the agent constructor 
(Kotlin) or via the builder methods (Java) to customize the behavior of the LLM.
For example, use the `temperature` parameter to adjust the randomness of the generated responses:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    -->
    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
        systemPrompt = "You are an expert in internet memes. Be helpful, friendly, and answer user questions concisely, showing your knowledge of memes.",
        llmModel = OpenAIModels.Chat.GPT4o,
        temperature = 0.7
    )
    ```
    <!--- KNIT example-basic-java-01.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    
    import static ai.koog.prompt.executor.llms.all.SimplePromptExecutorsKt.simpleOpenAIExecutor;

    class exampleBasicJava03 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
        .systemPrompt("You are an expert in internet memes. Be helpful, friendly, and answer user questions concisely, showing your knowledge of memes.")
        .llmModel(OpenAIModels.Chat.GPT4o)
        .temperature(0.7)
        .build();
    ```
    <!--- KNIT exampleBasicJava03.java -->

Here are some response examples with different temperature values:

=== "0.4"
    
    ```text
    I'm here to help you navigate the wild world of internet memes! Whether you're looking for explanations, examples, or just want to share a meme with someone, I'm your go-to expert. What's on your mind? Got a specific meme in mind that's got you curious? Or maybe you need some meme-related advice? Fire away!
    ```
    <!--- KNIT example-basic-03.txt -->

=== "0.7"

    ```text
    I'm here to help you navigate the wild world of internet memes!
    
    What's on your mind? Need help understanding a specific meme, finding a popular joke or trend, or maybe even creating your own meme? Let's get this meme party started!
    ```
    <!--- KNIT example-basic-04.txt -->

=== "1.0"

    ```text
    I'd be happy to help you navigate the wild world of internet memes!
    
    Whether you're looking for explanations of classic memes, suggestions for new ones to try out, or just want to discuss your favorite meme culture trends, I'm here to assist. What's on your mind?
    
    Do you have a specific question about memes (e.g., "What does this meme mean?"), or are you looking for some meme-related recommendations (e.g., "Can you recommend a funny meme to share with friends?"). Let me know how I can help!
    ```
    <!--- KNIT example-basic-05.txt -->

## Add tools

Agents can use [tools](../tools-overview.md) to perform specific tasks.

First, create a tool by annotating a function (Kotlin) or method (Java) with the [`@Tool`](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools.annotations/-tool/index.html) annotation:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.tools.ToolRegistry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import ai.koog.agents.core.tools.annotations.LLMDescription
    import ai.koog.agents.core.tools.annotations.Tool
    -->
    ```kotlin
    @Tool
    @LLMDescription("Ask the user a question by sending it to stdout and return the answer from stdin")
    fun askUser(
        @LLMDescription("Question from the agent")
        question: String
    ): String {
        println(question)
        return readln()
    }
    ```

    Then, use the [`ToolRegistry`](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool-registry/index.html) to make this tool available to the agent:

    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
        systemPrompt = "You are an expert in internet memes. Be helpful, friendly, and answer user questions concisely, showing your knowledge of memes.",
        llmModel = OpenAIModels.Chat.GPT4o,
        temperature = 0.7,
        toolRegistry = ToolRegistry {
            tool(::askUser)
        }
    )
    ```
    <!--- KNIT example-basic-03.kt -->

    In the example, `askUser` is a tool that helps the agent maintain a conversation with the user via printing and reading from the console.
    If the agent decides to ask the user a question,
    it can call this tool that writes to `stdout` via `println()` and reads from `stdin` via `readln()`.

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.core.tools.ToolRegistry;
    import ai.koog.agents.core.tools.annotations.LLMDescription;
    import ai.koog.agents.core.tools.annotations.Tool;
    import ai.koog.agents.core.tools.reflect.ToolSet;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    
    import java.util.Scanner;
    
    import static ai.koog.prompt.executor.llms.all.SimplePromptExecutorsKt.simpleOpenAIExecutor;

    class exampleBasicJava04 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    // Create a ToolSet class
    class UserConversationTools implements ToolSet {
        @Tool
        @LLMDescription("Ask the user a question by sending it to stdout and return the answer from stdin")
        public String askUser(
            @LLMDescription("Question from the agent")
            String question
        ) {
            System.out.println(question);
            Scanner scanner = new Scanner(System.in);
            return scanner.nextLine();
        }
    }
    ```
    
    Then, use the [`ToolRegistry`](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool-registry/index.html) to make this tool available to the agent:

    ```java
    UserConversationTools askUser = new UserConversationTools();

    ToolRegistry toolRegistry = ToolRegistry.builder()
            .tools(askUser)
            .build();

    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
        .systemPrompt("You are an expert in internet memes. Be helpful, friendly, and answer user questions concisely, showing your knowledge of memes.")
        .llmModel(OpenAIModels.Chat.GPT4o)
        .temperature(0.7)
        .toolRegistry(toolRegistry)
        .build();
    ```
    <!--- KNIT exampleBasicJava04.java -->

    In the example, `askUser` is a tool that helps the agent maintain a conversation with the user via printing and reading from the console.

Here is an example interaction with the agent:

```text
Agent: Which meme would you like me to explain? Please choose from: Grumpy Cat, Success Kid, or Doge.

User: Explain Doge

Agent:

**Doge**

Doge is a popular internet meme that originated on the imageboard website 4chan in 2013. The meme features a picture of a Shiba Inu dog with a caption written in a playful, exaggerated style.

The typical format of a Doge meme includes:

* A photo of a Shiba Inu dog
* A caption written in all capital letters, using a intentionally simplistic and childlike tone
* Exaggerated or made-up words or phrases, often used to convey a humorous or nonsensical idea

Examples of Doge memes might include:

* "Such wow. Such happy."
* "I had fun today!"
* "Wow, I am good at napping."

The meme is known for its lighthearted and playful tone, and is often used to express excitement, happiness, or silliness. The meme has since become a cultural phenomenon, with countless variations and parodies emerging online.
```
<!--- KNIT example-basic-06.txt -->

## Adjust agent iterations

To avoid infinite loops, Koog allows any agent to take a limited number of steps (50 by default).
Use the `maxIterations` parameter to either increase this limit if you expect the agent to require more steps
(such as tool calls and LLM requests) or decrease it for agents that require only a few steps.
For example, a simple agent described here is not likely to require more than 10 steps:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.tools.ToolRegistry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import ai.koog.agents.core.tools.annotations.LLMDescription
    import ai.koog.agents.core.tools.annotations.Tool
    @Tool
    @LLMDescription("Asks the user a question by sending it to stdout and returns the answer from stdin")
    fun askUser(
        @LLMDescription("Question from the agent")
        question: String
    ): String {
        println(question)
        return readln()
    }
    -->
    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
        systemPrompt = "You are an expert in internet memes. Be helpful, friendly, and answer user questions concisely, showing your knowledge of memes.",
        llmModel = OpenAIModels.Chat.GPT4o,
        temperature = 0.7,
        toolRegistry = ToolRegistry {
            tool(::askUser)
        },
        maxIterations = 10
    )
    ```
    <!--- KNIT example-basic-04.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.core.tools.ToolRegistry;
    import ai.koog.agents.core.tools.annotations.LLMDescription;
    import ai.koog.agents.core.tools.annotations.Tool;
    import ai.koog.agents.core.tools.reflect.ToolSet;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    
    import java.util.Scanner;
    
    import static ai.koog.prompt.executor.llms.all.SimplePromptExecutorsKt.simpleOpenAIExecutor;

    class exampleBasicJava05 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    // Create a ToolSet class
    class UserConversationTools implements ToolSet {
        @Tool
        @LLMDescription("Ask the user a question by sending it to stdout and return the answer from stdin")
        public String askUser(
            @LLMDescription("Question from the agent")
            String question
        ) {
            System.out.println(question);
            Scanner scanner = new Scanner(System.in);
            return scanner.nextLine();
        }
    }

    // In main method:
    UserConversationTools askUser = new UserConversationTools();

    ToolRegistry toolRegistry = ToolRegistry.builder()
            .tools(askUser)
            .build();

    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
        .systemPrompt("You are an expert in internet memes. Be helpful, friendly, and answer user questions concisely, showing your knowledge of memes.")
        .llmModel(OpenAIModels.Chat.GPT4o)
        .temperature(0.7)
        .toolRegistry(toolRegistry)
        .maxIterations(10)
        .build();
    ```
    <!--- KNIT exampleBasicJava05.java -->

!!! tip

    Instead of passing the model, temperature, max iterations, and other parameters directly to the Kotlin constructor 
    or Java builder, you can also define and pass them as a separate configuration object.
    For more information, see [Agent configuration](index.md#agent-configuration).

## Handle events during agent runtime

To assist with testing and debugging, as well as making hooks for chained agent interactions,
Koog provides the [EventHandler](https://api.koog.ai/agents/agents-features/agents-features-event-handler/ai.koog.agents.features.eventHandler.feature/-event-handler/index.html) feature.

=== "Kotlin"

    在智能体构造函数的 lambda 表达式内调用 `handleEvents()` 函数来安装该功能并注册事件处理器：

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.tools.ToolRegistry
    import ai.koog.agents.features.eventHandler.feature.handleEvents
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import ai.koog.agents.core.tools.annotations.LLMDescription
    import ai.koog.agents.core.tools.annotations.Tool
    @Tool
    @LLMDescription("Asks the user a question by sending it to stdout and returns the answer from stdin")
    fun askUser(
        @LLMDescription("Question from the agent")
        question: String
    ): String {
        println(question)
        return readln()
    }
    -->
    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
        systemPrompt = "你是一位网络迷因专家。请保持乐于助人、友好亲切的态度，简洁地回答用户问题，并展现你对迷因的了解。",
        llmModel = OpenAIModels.Chat.GPT4o,
        temperature = 0.7,
        toolRegistry = ToolRegistry {
            tool(::askUser)
        },
        maxIterations = 10
    ){
        handleEvents {
            // 处理工具调用
            onToolCallStarting { eventContext ->
                println("工具调用：${eventContext.toolName}，参数 ${eventContext.toolArgs}")
            }
        }
    }
    ```
    <!--- KNIT example-basic-05.kt -->

=== "Java"
    使用智能体构建器上的 `.install()` 方法，通过 `EventHandler.Feature` 注册事件处理器：<!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.core.tools.ToolRegistry;
    import ai.koog.agents.core.tools.annotations.LLMDescription;
    import ai.koog.agents.core.tools.annotations.Tool;
    import ai.koog.agents.core.tools.reflect.ToolSet;
    import ai.koog.agents.features.eventHandler.feature.EventHandler;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;

    
    import java.util.Scanner;
    
    import static ai.koog.prompt.executor.llms.all.SimplePromptExecutorsKt.simpleOpenAIExecutor;

    class exampleBasicJava06 {
        public static void main(String[] args) {
    -->
<!--- SUFFIX
        }
    }
    -->
```java
// 创建一个 ToolSet 类
class UserConversationTools implements ToolSet {
    @Tool
    @LLMDescription("通过向标准输出发送问题来询问用户，并从标准输入返回答案")
    public String askUser(
        @LLMDescription("来自智能体的问题")
        String question
    ) {
        System.out.println(question);
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }
}

// 在 main 方法中：
UserConversationTools askUser = new UserConversationTools();

ToolRegistry toolRegistry = ToolRegistry.builder()
        .tools(askUser)
        .build();

AIAgent<String, String> agent = AIAgent.builder()
    .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
    .systemPrompt("你是一位网络迷因专家。请保持乐于助人、态度友好，并简洁地回答用户问题，展现你对迷因的了解。")
    .llmModel(OpenAIModels.Chat.GPT4o)
    .temperature(0.7)
    .toolRegistry(toolRegistry)
    .maxIterations(10)
    .install(EventHandler.Feature, config -> {
        config.onToolCallStarting(eventContext -> {
            System.out.println("工具调用开始：" + eventContext.getToolName() +
                "，参数为 " + eventContext.getToolArgs());
        });
    })
    .build();
```
<!--- KNIT exampleBasicJava06.java -->

当智能体调用 `askUser` 工具时，现在将输出类似以下内容：

```text
Tool called: askUser with args {"question":"Which meme would you like me to explain?"}
```
<!--- KNIT example-basic-07.txt -->

有关 Koog 智能体功能的更多信息，请参阅[功能](../features/index.md)。

## 后续步骤 { #next-steps }

- 了解更多关于构建[基于图的智能体](graph-based-agents.md)和[函数式智能体](functional-agents.md)的信息