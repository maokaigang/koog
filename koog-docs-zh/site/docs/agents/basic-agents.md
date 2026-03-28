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

    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
        llmModel = OpenAIModels.Chat.GPT4o
    )
    ```

    该代理期望接收字符串作为输入并返回字符串作为输出。要运行该代理，请使用 `run()` 函数并传入用户输入：

    ```kotlin
    fun main() = runBlocking {
        val result = agent.run("Hello! How can you help me?")
        println(result)
    }
    ```

=== "Java"

    ```java
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
        .llmModel(OpenAIModels.Chat.GPT4o)
        .build();
    ```

    该代理期望接收字符串作为输入并返回字符串作为输出。要运行该代理，请使用 `run()` 方法并传入用户输入：

    ```java
    String result = agent.run("Hello! How can you help me?");
    System.out.println(result);
    ```

代理将返回一个通用答案，例如：

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

## Add a system prompt

提供一个[系统消息](../prompts/prompt-creation/index.md#system-message)来定义代理的角色，以及任务相关的目的、背景和说明。

=== "Kotlin"

    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
        systemPrompt = "You are an expert in internet memes. Be helpful, friendly, and answer user questions concisely, showing your knowledge of memes.",
        llmModel = OpenAIModels.Chat.GPT4o
    )
    ```

=== "Java"

    ```java
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
        .systemPrompt("You are an expert in internet memes. Be helpful, friendly, and answer user questions concisely, showing your knowledge of memes.")
        .llmModel(OpenAIModels.Chat.GPT4o)
        .build();
    ```

系统提示中的指令将指导代理的响应：

```text
I'm here to help you navigate the wild world of internet memes!

What's on your mind? Are you trying to understand a specific meme, need help finding a popular joke, or perhaps want some recommendations for trending memes? Let me know, and I'll do my best to provide you with some LOLs!
```

## Configure LLM output

您可以直接向代理构造函数（Kotlin）提供一些[LLM 参数](../llm-parameters.md#llm-parameter-reference)，或通过构建器方法（Java）来自定义LLM的行为。例如，使用`temperature`参数来调整生成响应的随机性：

=== "Kotlin"

    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
        systemPrompt = "You are an expert in internet memes. Be helpful, friendly, and answer user questions concisely, showing your knowledge of memes.",
        llmModel = OpenAIModels.Chat.GPT4o,
        temperature = 0.7
    )
    ```

=== "Java"

    ```java
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
        .systemPrompt("You are an expert in internet memes. Be helpful, friendly, and answer user questions concisely, showing your knowledge of memes.")
        .llmModel(OpenAIModels.Chat.GPT4o)
        .temperature(0.7)
        .build();
    ```

以下是不同温度值下的响应示例：

=== "0.4"
    
    ```text
    I'm here to help you navigate the wild world of internet memes! Whether you're looking for explanations, examples, or just want to share a meme with someone, I'm your go-to expert. What's on your mind? Got a specific meme in mind that's got you curious? Or maybe you need some meme-related advice? Fire away!
    ```

=== "0.7"

    ```text
    I'm here to help you navigate the wild world of internet memes!
    
    What's on your mind? Need help understanding a specific meme, finding a popular joke or trend, or maybe even creating your own meme? Let's get this meme party started!
    ```

=== "1.0"

    ```text
    I'd be happy to help you navigate the wild world of internet memes!
    
    Whether you're looking for explanations of classic memes, suggestions for new ones to try out, or just want to discuss your favorite meme culture trends, I'm here to assist. What's on your mind?
    
    Do you have a specific question about memes (e.g., "What does this meme mean?"), or are you looking for some meme-related recommendations (e.g., "Can you recommend a funny meme to share with friends?"). Let me know how I can help!
    ```

## Add tools

代理可以使用[工具](../tools-overview.md)来执行特定任务。

首先，通过使用 [`@Tool`](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools.annotations/-tool/index.html) 注解标注函数（Kotlin）或方法（Java）来创建一个工具：

=== "Kotlin"

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

    然后，使用[`ToolRegistry`](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool-registry/index.html)使该工具对代理可用：

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

    在示例中，`askUser` 是一个工具，它通过控制台的打印和读取来帮助代理与用户保持对话。如果代理决定向用户提问，它可以调用这个工具，该工具通过 `println()` 写入 `stdout`，并通过 `readln()` 从 `stdin` 读取。

=== "Java"

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
    
    然后，使用[`ToolRegistry`](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool-registry/index.html)使该工具对代理可用：

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

    在示例中，`askUser` 是一个工具，它通过控制台的打印和读取来帮助代理与用户保持对话。

以下是与该智能体交互的一个示例：

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

## Adjust agent iterations

为避免无限循环，Koog 允许任何代理执行有限数量的步骤（默认为50步）。您可以通过 `maxIterations` 参数来调整此限制：若预期代理需要更多步骤（例如工具调用和 LLM 请求），可增加该值；对于仅需少量步骤的代理，则可减少限制。例如，此处描述的简单代理很可能不需要超过10个步骤：

=== "Kotlin"

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

=== "Java"

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

!!! tip

    除了直接将模型、温度、最大迭代次数等参数传递给 Kotlin 构造函数或 Java 构建器，您也可以将它们定义为一个独立的配置对象进行传递。更多信息请参阅 [代理配置](index.md#agent-configuration)。

## 处理代理运行期间的事件 { #handle-events-during-agent-runtime }

为便于测试和调试，以及为链式智能体交互创建钩子，Koog 提供了 [EventHandler](https://api.koog.ai/agents/agents-features/agents-features-event-handler/ai.koog.agents.features.eventHandler.feature/-event-handler/index.html) 功能。

=== "Kotlin"

    在智能体构造函数的 lambda 表达式内调用 `handleEvents()` 函数来安装该功能并注册事件处理器：

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

=== "Java"
    使用智能体构建器上的 `.install()` 方法，通过 `EventHandler.Feature` 注册事件处理器：<!--- INCLUDE import ai.koog.agents.core.agent.AIAgent; import ai.koog.agents.core.tools.ToolRegistry; import ai.koog.agents.core.tools.annotations.LLMDescription; import ai.koog.agents.core.tools.annotations.Tool; import ai.koog.agents.core.tools.reflect.ToolSet; import ai.koog.agents.features.eventHandler.feature.EventHandler; import ai.koog.prompt.executor.clients.openai.OpenAIModels;

    
    import java.util.Scanner;
    
    import static ai.koog.prompt.executor.llms.all.SimplePromptExecutorsKt.simpleOpenAIExecutor;

    class exampleBasicJava06 { public static void main(String[] args) { -->
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

当智能体调用 `askUser` 工具时，现在将输出类似以下内容：

```text
Tool called: askUser with args {"question":"Which meme would you like me to explain?"}
```

有关 Koog 智能体功能的更多信息，请参阅[功能](../features/index.md)。

## 后续步骤 { #next-steps }

- 了解更多关于构建[基于图的智能体](graph-based-agents.md)和[函数式智能体](functional-agents.md)的信息