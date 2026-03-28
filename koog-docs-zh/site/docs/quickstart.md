<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:55:27+00:00", "source_path": "quickstart.md", "source_sha256": "7e24c86b44e53e64377280c669ead17b897eb9b63e256a2d67bd36a8b02f2564", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 快速开始 { #quickstart }

本指南将帮助您开始在项目中使用 Koog。

## 前提条件 { #prerequisites }

--8<-- "quickstart-snippets.md:prerequisites"

## 安装 Koog { #install-koog }

--8<-- "quickstart-snippets.md:dependencies"

??? tip "夜间构建版本"

    开发分支的夜间构建版本会发布到 [JetBrains Grazie Maven](https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public) 仓库。
    
    要使用夜间构建版本，请在构建配置中添加以下仓库：
    `https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public`。
    
    然后将您的 Koog 依赖项更新到所需的夜间版本。夜间版本遵循以下模式：
    `[next-major-version]-develop-[date]-[time]`。
    
    您可以在此处浏览可用的夜间构建版本。

## 设置 API 密钥 { #set-up-an-api-key }

Koog 需要来自 [支持的 LLM 提供商](llm-providers.md) 的 API 密钥，或者本地运行的 LLM。

!!! warning
    避免在源代码中硬编码 API 密钥。
    请使用环境变量来存储 API 密钥。

=== "OpenAI"

    获取您的 [OpenAI API 密钥](https://platform.openai.com/api-keys) 并将其分配给 `OPENAI_API_KEY` 环境变量。
    
    === "Linux/macOS"

        ```shell
        export OPENAI_API_KEY=your-api-key
        ```

    === "Windows"

        ```cmd
        setx OPENAI_API_KEY "your-api-key"
        ```

=== "Anthropic"

    获取您的 [Anthropic API 密钥](https://console.anthropic.com/settings/keys) 并将其分配给 `ANTHROPIC_API_KEY` 环境变量。

    === "Linux/macOS"

        ```shell
        export ANTHROPIC_API_KEY=your-api-key
        ```

    === "Windows"

        ```cmd
        setx ANTHROPIC_API_KEY "your-api-key"
        ```

=== "Google"

    获取您的 [Gemini API 密钥](https://aistudio.google.com/app/api-keys) 并将其分配给 `GOOGLE_API_KEY` 环境变量。

    === "Linux/macOS"

        ```shell
        export GOOGLE_API_KEY=your-api-key
        ```

    === "Windows"

        ```cmd
        setx GOOGLE_API_KEY "your-api-key"
        ```  

=== "DeepSeek"

    获取您的 [DeepSeek API 密钥](https://platform.deepseek.com/api_keys) 并将其分配给 `DEEPSEEK_API_KEY` 环境变量。

    === "Linux/macOS"

        ```shell
        export DEEPSEEK_API_KEY=your-api-key
        ```

    === "Windows"

        ```cmd
        setx DEEPSEEK_API_KEY "your-api-key"
        ``` 

=== "OpenRouter"

    获取您的 [OpenRouter API 密钥](https://openrouter.ai/keys) 并将其分配给 `OPENROUTER_API_KEY` 环境变量。

    === "Linux/macOS"

        ```shell
        export OPENROUTER_API_KEY=your-api-key
        ```

    === "Windows"

        ```cmd
        setx OPENROUTER_API_KEY "your-api-key"
        ```  

=== "Bedrock"

    [生成 Amazon Bedrock API 密钥](https://docs.aws.amazon.com/bedrock/latest/userguide/api-keys.html) 并将其分配给 `BEDROCK_API_KEY` 环境变量。

    === "Linux/macOS"

        ```shell
        export BEDROCK_API_KEY=your-api-key
        ``` 

    === "Windows"

        ```cmd
        setx BEDROCK_API_KEY "your-api-key"
        ```  

=== "Mistral"获取你的 [Mistral API 密钥](https://console.mistral.ai/api-keys) 并将其赋值给 `MISTRAL_API_KEY` 环境变量。

=== "Linux/macOS"

    ```shell
    export MISTRAL_API_KEY=your-api-key
    ```

=== "Windows"

    ```cmd
    setx MISTRAL_API_KEY "your-api-key"
    ```
    <!--- KNIT example-getting-started-01.txt -->

=== "Ollama"

    按照 [Ollama 文档](https://docs.ollama.com/quickstart) 中的说明，在 Ollama 中运行一个本地 LLM。

## 创建你的第一个 Koog 智能体 { #create-your-first-koog-agent }

=== "OpenAI"

    以下示例使用 [`GPT-4o`](https://platform.openai.com/docs/models/gpt-4o) 模型，通过 OpenAI API 创建并运行一个简单的 Koog 智能体。

    === "Kotlin"

        <!--- INCLUDE
        import ai.koog.agents.core.agent.AIAgent
        import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
        import ai.koog.prompt.executor.clients.openai.OpenAIModels
        import kotlinx.coroutines.runBlocking
        -->
        ```kotlin
        fun main() = runBlocking {
            // 从 OPENAI_API_KEY 环境变量中获取 OpenAI API 密钥
            val apiKey = System.getenv("OPENAI_API_KEY")
                ?: error("未设置 API 密钥。")
            
            // 创建智能体
            val agent = AIAgent(
                promptExecutor = simpleOpenAIExecutor(apiKey),
                llmModel = OpenAIModels.Chat.GPT4o
            )
        
            // 运行智能体
            val result = agent.run("你好！你能如何帮助我？")
            println(result)
        }
        ```
        <!--- KNIT example-getting-started-01.kt -->

    === "Java"

        <!--- INCLUDE
        /**
        -->
        <!--- SUFFIX
        **/
        -->
        ```java
        // 从 OPENAI_API_KEY 环境变量中获取 OpenAI API 密钥
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("未设置 API 密钥。");
        }

        // 创建智能体
        AIAgent<String, String> agent = AIAgent.builder()
            .promptExecutor(simpleOpenAIExecutor(apiKey))
            .llmModel(OpenAIModels.Chat.GPT4o)
            .build();

        // 运行智能体
        String result = agent.run("你好！你能如何帮助我？");
        System.out.println(result);
        ```
        <!--- KNIT example-getting-started-java-01.java -->

    示例可能产生以下输出：
    
    ```
    你好！我在这里为你提供所需的任何帮助。以下是我能做的一些事情：

    - 回答问题。
    - 解释你好奇的概念或主题。
    - 为任务提供分步指导。
    - 提供建议、笔记或想法。
    - 协助研究或总结复杂材料。
    - 撰写或编辑文本、电子邮件或其他文档。
    - 为创意项目或解决方案进行头脑风暴。
    - 解决问题或进行计算。

    告诉我你需要什么帮助——我随时为你服务！
    ```
    <!--- KNIT example-getting-started-02.txt -->

=== "Anthropic"

    以下示例使用 [`Claude Opus 4.1`](https://www.anthropic.com/news/claude-opus-4-1) 模型，通过 Anthropic API 创建并运行一个简单的 Koog 智能体。

    === "Kotlin"<!--- INCLUDE
        import ai.koog.agents.core.agent.AIAgent
        import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
        import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
        import kotlinx.coroutines.runBlocking
        -->
```kotlin
fun main() = runBlocking {
    // 从 ANTHROPIC_API_KEY 环境变量获取 Anthropic API 密钥
    val apiKey = System.getenv("ANTHROPIC_API_KEY")
        ?: error("未设置 API 密钥。")
    
    // 创建智能体
    val agent = AIAgent(
        promptExecutor = simpleAnthropicExecutor(apiKey),
        llmModel = AnthropicModels.Opus_4_1
    )

    // 运行智能体
    val result = agent.run("你好！你能如何帮助我？")
    println(result)
}
```
<!--- KNIT example-getting-started-02.kt -->

=== "Java"

    <!--- INCLUDE
        /**
        -->
    <!--- SUFFIX
        **/
        -->
    ```java
    // 从 ANTHROPIC_API_KEY 环境变量获取 Anthropic API 密钥
    String apiKey = System.getenv("ANTHROPIC_API_KEY");
    if (apiKey == null) {
        throw new RuntimeException("未设置 API 密钥。");
    }

    // 创建智能体
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleAnthropicExecutor(apiKey))
        .llmModel(AnthropicModels.Opus_4_1)
        .build();

    // 运行智能体
    String result = agent.run("你好！你能如何帮助我？");
    System.out.println(result);
    ```
    <!--- KNIT example-getting-started-java-02.java -->

    示例可能产生以下输出：

    ```
    你好！我可以帮助你：

    - **回答问题** 并解释主题
    - **写作** - 起草、编辑、校对
    - **学习** - 作业、数学、学习辅导
    - **解决问题** 和头脑风暴
    - **研究** 和信息查找
    - **一般任务** - 指导、规划、建议
    
    你今天需要什么帮助？
    ```
    <!--- KNIT example-getting-started-03.txt -->

=== "Google"

    以下示例使用 [`Gemini 2.5 Pro`](https://cloud.google.com/vertex-ai/generative-ai/docs/models/gemini/2-5-pro) 模型，通过 Gemini API 创建并运行一个简单的 Koog 智能体。

    === "Kotlin"

        <!--- INCLUDE
        import ai.koog.agents.core.agent.AIAgent
        import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
        import ai.koog.prompt.executor.clients.google.GoogleModels
        import kotlinx.coroutines.runBlocking
        -->
        ```kotlin
        fun main() = runBlocking {
            // 从 GOOGLE_API_KEY 环境变量获取 Gemini API 密钥
            val apiKey = System.getenv("GOOGLE_API_KEY")
                ?: error("未设置 API 密钥。")
            
            // 创建智能体
            val agent = AIAgent(
                promptExecutor = simpleGoogleAIExecutor(apiKey),
                llmModel = GoogleModels.Gemini2_5Pro
            )
        
            // 运行智能体
            val result = agent.run("你好！你能如何帮助我？")
            println(result)
        }
        ```
        <!--- KNIT example-getting-started-03.kt -->

    === "Java"

        <!--- INCLUDE
        /**
        -->
        <!--- SUFFIX
        **/
        -->
        ```java
        // 从 GOOGLE_API_KEY 环境变量获取 Gemini API 密钥
        String apiKey = System.getenv("GOOGLE_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("未设置 API 密钥。");
        }// 创建智能体
        AIAgent<String, String> agent = AIAgent.builder()
            .promptExecutor(simpleGoogleAIExecutor(apiKey))
            .llmModel(GoogleModels.Gemini2_5Pro)
            .build();

        // 运行智能体
        String result = agent.run("你好！你能如何帮助我？");
        System.out.println(result);
        ```
        <!--- KNIT example-getting-started-java-03.java -->

    该示例可能产生以下输出：

    ```
    我是一个能够协助处理语言和信息任务的AI。你可以让我：

    *   **回答问题**
    *   **撰写或编辑文本**（邮件、故事、代码等）
    *   **头脑风暴想法**
    *   **总结长文档**
    *   **规划事务**（如旅行或项目）
    *   **作为创意伙伴**

    只需告诉我你的需求
    ```
    <!--- KNIT example-getting-started-04.txt -->

=== "DeepSeek"

    以下示例通过 DeepSeek API 使用 `deepseek-chat` 模型创建并运行一个简单的 Koog 智能体。

    === "Kotlin"

        <!--- INCLUDE
        import ai.koog.agents.core.agent.AIAgent
        import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
        import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
        import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
        import kotlinx.coroutines.runBlocking
        -->
        ```kotlin
        fun main() = runBlocking {
            // 从 DEEPSEEK_API_KEY 环境变量获取 DeepSeek API 密钥
            val apiKey = System.getenv("DEEPSEEK_API_KEY")
                ?: error("未设置 API 密钥。")
            
            // 创建 LLM 客户端
            val deepSeekClient = DeepSeekLLMClient(apiKey)
        
            // 创建智能体
            val agent = AIAgent(
                // 使用 LLM 客户端创建提示词执行器
                promptExecutor = MultiLLMPromptExecutor(deepSeekClient),
                // 提供模型
                llmModel = DeepSeekModels.DeepSeekChat
            )
        
            // 运行智能体
            val result = agent.run("你好！你能如何帮助我？")
            println(result)
        }
        ```
        <!--- KNIT example-getting-started-04.kt -->

    === "Java"

        <!--- INCLUDE
        /**
        -->
        <!--- SUFFIX
        **/
        -->
        ```java
        // 从 DEEPSEEK_API_KEY 环境变量获取 DeepSeek API 密钥
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("未设置 API 密钥。");
        }

        // 创建 LLM 客户端
        DeepSeekLLMClient deepSeekClient = new DeepSeekLLMClient(apiKey);

        // 创建智能体
        AIAgent<String, String> agent = AIAgent.builder()
            // 使用 LLM 客户端创建提示词执行器
            .promptExecutor(new MultiLLMPromptExecutor(deepSeekClient))
            // 提供模型
            .llmModel(DeepSeekModels.DeepSeekChat)
            .build();

        // 运行智能体
        String result = agent.run("你好！你能如何帮助我？");
        System.out.println(result);
        ```
        <!--- KNIT example-getting-started-java-04.java -->

    该示例可能产生以下输出：```
你好！我在这里协助你处理各种任务，包括回答问题、提供信息、帮助解决问题、提供创意想法，甚至只是聊天。无论你需要研究、写作、学习新知识方面的帮助，还是只想讨论某个话题，随时都可以问我——我很乐意帮忙！😊
```
<!--- KNIT example-getting-started-05.txt -->

=== "OpenRouter"

    以下示例使用 [`GPT-4o`](https://openrouter.ai/openai/gpt-4o) 模型，通过 OpenRouter API 创建并运行一个简单的 Koog 代理。

    === "Kotlin"

        <!--- INCLUDE
        import ai.koog.agents.core.agent.AIAgent
        import ai.koog.prompt.executor.llms.all.simpleOpenRouterExecutor
        import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
        import kotlinx.coroutines.runBlocking
        -->
        ```kotlin
        fun main() = runBlocking {
            // 从 OPENROUTER_API_KEY 环境变量获取 OpenRouter API 密钥
            val apiKey = System.getenv("OPENROUTER_API_KEY")
                ?: error("The API key is not set.")
            
            // 创建代理
            val agent = AIAgent(
                promptExecutor = simpleOpenRouterExecutor(apiKey),
                llmModel = OpenRouterModels.GPT4o
            )
        
            // 运行代理
            val result = agent.run("Hello! How can you help me?")
            println(result)
        }
        ```
        <!--- KNIT example-getting-started-05.kt -->

    === "Java"

        <!--- INCLUDE
        /**
        -->
        <!--- SUFFIX
        **/
        -->
        ```java
        // 从 OPENROUTER_API_KEY 环境变量获取 OpenRouter API 密钥
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("The API key is not set.");
        }

        // 创建代理
        AIAgent<String, String> agent = AIAgent.builder()
            .promptExecutor(simpleOpenRouterExecutor(apiKey))
            .llmModel(OpenRouterModels.GPT4o)
            .build();

        // 运行代理
        String result = agent.run("Hello! How can you help me?");
        System.out.println(result);
        ```
        <!--- KNIT example-getting-started-java-05.java -->

    该示例可能产生以下输出：

    ```
    我可以回答问题、协助写作、解决问题、组织任务等等——只需告诉我你需要什么！
    ```
    <!--- KNIT example-getting-started-06.txt -->

=== "Bedrock"

    以下示例使用 [`Claude Sonnet 4.5`](https://www.anthropic.com/news/claude-sonnet-4-5) 模型，通过 Bedrock API 创建并运行一个简单的 Koog 代理。
    
    === "Kotlin"

        <!--- INCLUDE
        import ai.koog.agents.core.agent.AIAgent
        import ai.koog.prompt.executor.llms.all.simpleBedrockExecutorWithBearerToken
        import ai.koog.prompt.executor.clients.bedrock.BedrockModels
        import kotlinx.coroutines.runBlocking
        -->
        ```kotlin
        fun main() = runBlocking {
            // 从 BEDROCK_API_KEY 环境变量获取 Bedrock API 密钥
            val apiKey = System.getenv("BEDROCK_API_KEY")
                ?: error("The API key is not set.")
            
            // 创建代理
            val agent = AIAgent(
                promptExecutor = simpleBedrockExecutorWithBearerToken(apiKey),
                llmModel = BedrockModels.AnthropicClaude4_5Sonnet
            )
        
            // 运行代理
            val result = agent.run("Hello! How can you help me?")
            println(result)
        }
        ```
        <!--- KNIT example-getting-started-06.kt -->

    === "Java"<!--- INCLUDE
        /**
        -->
        <!--- SUFFIX
        **/
        -->
        ```java
        // Get the Bedrock API key from the BEDROCK_API_KEY environment variable
        String apiKey = System.getenv("BEDROCK_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("The API key is not set.");
        }

        // Create an agent
        AIAgent<String, String> agent = AIAgent.builder()
            .promptExecutor(simpleBedrockExecutorWithBearerToken(apiKey, new BedrockClientSettings()))
            .llmModel(BedrockModels.INSTANCE.getAnthropicClaude4_5Sonnet())
            .build();

        // Run the agent
        String result = agent.run("Hello! How can you help me?");
        System.out.println(result);
        ```
        <!--- KNIT example-getting-started-java-06.java -->

    该示例可能产生以下输出：

    ```
    Hello! I'm a helpful assistant and I can assist you in many ways, including:

    - **Answering questions** on a wide range of topics (science, history, technology, etc.)
    - **Writing help** - drafting emails, essays, creative content, or editing text
    - **Problem-solving** - working through math problems, logic puzzles, or troubleshooting issues
    - **Learning support** - explaining concepts, providing study notes, or tutoring
    - **Planning & organizing** - helping with projects, schedules, or breaking down tasks
    - **Coding assistance** - explaining programming concepts or helping debug code
    - **Creative brainstorming** - generating ideas for projects, stories, or solutions
    - **General conversation** - discussing topics or just chatting
    
     What would you like help with today?
    ```
    <!--- KNIT example-getting-started-07.txt -->

=== "Mistral"

    以下示例使用 [`Mistral Medium 3.1`](https://docs.mistral.ai/models/mistral-medium-3-1-25-08) 模型，通过 Mistral AI API 创建并运行一个简单的 Koog 智能体。

    === "Kotlin"

        <!--- INCLUDE
        import ai.koog.agents.core.agent.AIAgent
        import ai.koog.prompt.executor.llms.all.simpleMistralAIExecutor
        import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
        import kotlinx.coroutines.runBlocking
        -->
        ```kotlin
        fun main() = runBlocking {
            // Get the Mistral AI API key from the MISTRAL_API_KEY environment variable
            val apiKey = System.getenv("MISTRAL_API_KEY")
                ?: error("The API key is not set.")
            
            // Create an agent
            val agent = AIAgent(
                promptExecutor = simpleMistralAIExecutor(apiKey),
                llmModel = MistralAIModels.Chat.MistralMedium31
            )
        
            // Run the agent
            val result = agent.run("Hello! How can you help me?")
            println(result)
        }
        ```
        <!--- KNIT example-getting-started-07.kt -->
    
    === "Java"

        <!--- INCLUDE
        /**
        -->
        <!--- SUFFIX
        **/
        -->   
        ```java
        // Get the Mistral AI API key from the MISTRAL_API_KEY environment variable
        String apiKey = System.getenv("MISTRAL_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("The API key is not set.");
        }        // 创建智能体
        AIAgent<String, String> agent = AIAgent.builder()
            .promptExecutor(simpleMistralAIExecutor(apiKey))
            .llmModel(MistralAIModels.Chat.MistralMedium31)
            .build();

        // 运行智能体
        String result = agent.run("你好！你能如何帮助我？");
        System.out.println(result);
        ```
        <!--- KNIT example-getting-started-java-07.java -->

    示例可能产生以下输出：

    ```
    我可以协助您处理广泛的主题和任务。以下是一些示例：

    1. **回答问题**：我可以提供关于历史、科学、技术、文学等各种主题的信息。
    2. **提供定义**：如果您不确定某个单词或短语的含义，我可以帮助您定义它。
    3. **生成文本**：无论是撰写电子邮件、创建社交媒体内容还是编写故事，我都可以协助文本生成。
    4. **翻译**：我可以将文本从一种语言翻译成另一种语言。
    5. **对话**：我们可以就任何您感兴趣的话题进行聊天，我会相应回应。
    6. **语言练习**：如果您正在学习一门新语言，我可以帮助发音、语法和词汇练习。
    7. **头脑风暴**：如果您在某个问题上遇到困难或需要项目创意，我可以帮助构思解决方案。
    8. **摘要**：如果您有一篇长文本并希望获得摘要，我可以为您浓缩内容。
    
    您在想什么？有什么具体需要帮助的吗？
    ```
    <!--- KNIT example-getting-started-08.txt -->

=== "Ollama"

    以下示例使用通过 Ollama 本地运行的 [`llama3.2`](https://ollama.com/library/llama3.2) 模型创建并运行一个简单的 Koog 智能体。

    === "Kotlin"

        <!--- INCLUDE
        import ai.koog.agents.core.agent.AIAgent
        import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
        import ai.koog.prompt.executor.ollama.client.OllamaModels
        import kotlinx.coroutines.runBlocking
        -->
        ```kotlin
        fun main() = runBlocking {
            // 创建智能体
            val agent = AIAgent(
                promptExecutor = simpleOllamaAIExecutor(),
                llmModel = OllamaModels.Meta.LLAMA_3_2
            )

            // 运行智能体
            val result = agent.run("你好！你能如何帮助我？")
            println(result)
        }
        ```
        <!--- KNIT example-getting-started-08.kt -->

    === "Java"

        <!--- INCLUDE
        /**
        -->
        <!--- SUFFIX
        **/
        -->  
        ```java
        // 创建智能体
        AIAgent<String, String> agent = AIAgent.builder()
            .promptExecutor(simpleOllamaAIExecutor("http://localhost:11434"))
            .llmModel(OllamaModels.Meta.LLAMA_3_2)
            .build();

        // 运行智能体
        String result = agent.run("你好！你能如何帮助我？");
        System.out.println(result);
        ```
        <!--- KNIT example-getting-started-java-08.java -->

    示例可能产生以下输出：

    ```
    我可以协助处理各种任务，例如回答问题、提供信息，甚至帮助语言相关任务，如校对或写作建议。您今天有什么想法？
    ```
    <!--- KNIT example-getting-started-09.txt -->

## 后续步骤 { #next-steps }

- 了解更多关于[智能体类型](agents/index.md)的信息