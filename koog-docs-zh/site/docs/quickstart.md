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

=== "Mistral"

    获取你的 [Mistral API 密钥](https://console.mistral.ai/api-keys) 并将其赋值给 `MISTRAL_API_KEY` 环境变量。

    === "Linux/macOS"

        ```shell
        export MISTRAL_API_KEY=your-api-key
        ```

    === "Windows"

        ```cmd
        setx MISTRAL_API_KEY "your-api-key"
        ```

=== "Ollama"

    按照 [Ollama 文档](https://docs.ollama.com/quickstart) 中的说明，在 Ollama 中运行一个本地 LLM。

## 创建你的第一个 Koog 智能体 { #create-your-first-koog-agent }

=== "OpenAI"

    以下示例使用 [`GPT-4o`](https://platform.openai.com/docs/models/gpt-4o) 模型，通过 OpenAI API 创建并运行一个简单的 Koog 智能体。

    === "Kotlin"

        ```kotlin
        fun main() = runBlocking {
            // Get the OpenAI API key from the OPENAI_API_KEY environment variable
            val apiKey = System.getenv("OPENAI_API_KEY")
                ?: error("The API key is not set.")

            // Create an agent
            val agent = AIAgent(
                promptExecutor = simpleOpenAIExecutor(apiKey),
                llmModel = OpenAIModels.Chat.GPT4o
            )

            // Run the agent
            val result = agent.run("Hello! How can you help me?")
            println(result)
        }
        ```

    === "Java"

        ```java
        // Get the OpenAI API key from the OPENAI_API_KEY environment variable
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("The API key is not set.");
        }

        // Create an agent
        AIAgent<String, String> agent = AIAgent.builder()
            .promptExecutor(simpleOpenAIExecutor(apiKey))
            .llmModel(OpenAIModels.Chat.GPT4o)
            .build();

        // Run the agent
        String result = agent.run("Hello! How can you help me?");
        System.out.println(result);
        ```

    示例可能会输出以下内容：

    ```
    Hello! I'm here to help you with whatever you need. Here are just a few things I can do:

    - Answer questions.
    - Explain concepts or topics you're curious about.
    - Provide step-by-step instructions for tasks.
    - Offer advice, notes, or ideas.
    - Help with research or summarize complex material.
    - Write or edit text, emails, or other documents.
    - Brainstorm creative projects or solutions.
    - Solve problems or calculations.

    Let me know what you need help with—I’m here for you!
    ```

=== "Anthropic"

    以下示例使用 [`Claude Opus 4.1`](https://www.anthropic.com/news/claude-opus-4-1) 模型，通过 Anthropic API 创建并运行一个简单的 Koog 智能体。

    === "Kotlin"

        ```kotlin
        fun main() = runBlocking {
            // Get the Anthropic API key from the ANTHROPIC_API_KEY environment variable
            val apiKey = System.getenv("ANTHROPIC_API_KEY")
                ?: error("The API key is not set.")

            // Create an agent
            val agent = AIAgent(
                promptExecutor = simpleAnthropicExecutor(apiKey),
                llmModel = AnthropicModels.Opus_4_1
            )

            // Run the agent
            val result = agent.run("Hello! How can you help me?")
            println(result)
        }
        ```

=== "Java"

        ```java
        // Get the Anthropic API key from the ANTHROPIC_API_KEY environment variable
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("The API key is not set.");
        }

        // Create an agent
        AIAgent<String, String> agent = AIAgent.builder()
            .promptExecutor(simpleAnthropicExecutor(apiKey))
            .llmModel(AnthropicModels.Opus_4_1)
            .build();

        // Run the agent
        String result = agent.run("Hello! How can you help me?");
        System.out.println(result);
        ```

    该示例可以生成以下输出：

    ```
    Hello! I can help you with:

    - **Answering questions** and explaining topics
    - **Writing** - drafting, editing, proofreading
    - **Learning** - homework, math, study help
    - **Problem-solving** and brainstorming
    - **Research** and information finding
    - **General tasks** - instructions, planning, recommendations

    What do you need help with today?
    ```

=== "Google"

    以下示例使用 [`Gemini 2.5 Pro`](https://cloud.google.com/vertex-ai/generative-ai/docs/models/gemini/2-5-pro) 模型，通过 Gemini API 创建并运行一个简单的 Koog 智能体。

    === "Kotlin"

        ```kotlin
        fun main() = runBlocking {
            // Get the Gemini API key from the GOOGLE_API_KEY environment variable
            val apiKey = System.getenv("GOOGLE_API_KEY")
                ?: error("The API key is not set.")

            // Create an agent
            val agent = AIAgent(
                promptExecutor = simpleGoogleAIExecutor(apiKey),
                llmModel = GoogleModels.Gemini2_5Pro
            )

            // Run the agent
            val result = agent.run("Hello! How can you help me?")
            println(result)
        }
        ```

    === "Java"

        ```java
        // Get the Gemini API key from the GOOGLE_API_KEY environment variable
        String apiKey = System.getenv("GOOGLE_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("The API key is not set.");
        }

        // Create an agent
        AIAgent<String, String> agent = AIAgent.builder()
            .promptExecutor(simpleGoogleAIExecutor(apiKey))
            .llmModel(GoogleModels.Gemini2_5Pro)
            .build();

        // Run the agent
        String result = agent.run("Hello! How can you help me?");
        System.out.println(result);
        ```

    该示例可以生成以下输出：

    ```
    I'm an AI that can help you with tasks involving language and information. You can ask me to:

    *   **Answer questions**
    *   **Write or edit text** (emails, stories, code, etc.)
    *   **Brainstorm ideas**
    *   **Summarize long documents**
    *   **Plan things** (like trips or projects)
    *   **Be a creative partner**

    Just tell me what you need
    ```

=== "DeepSeek"

    以下示例通过 DeepSeek API 使用 `deepseek-chat` 模型创建并运行一个简单的 Koog 智能体。

    === "Kotlin"

        ```kotlin
        fun main() = runBlocking {
            // Get the DeepSeek API key from the DEEPSEEK_API_KEY environment variable
            val apiKey = System.getenv("DEEPSEEK_API_KEY")
                ?: error("The API key is not set.")

            // Create an LLM client
            val deepSeekClient = DeepSeekLLMClient(apiKey)

            // Create an agent
            val agent = AIAgent(
                // Create a prompt executor using the LLM client
                promptExecutor = MultiLLMPromptExecutor(deepSeekClient),
                // Provide a model
                llmModel = DeepSeekModels.DeepSeekChat
            )

            // Run the agent
            val result = agent.run("Hello! How can you help me?")
            println(result)
        }
        ```

    === "Java"

        ```java
        // Get the DeepSeek API key from the DEEPSEEK_API_KEY environment variable
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("The API key is not set.");
        }

        // Create an LLM client
        DeepSeekLLMClient deepSeekClient = new DeepSeekLLMClient(apiKey);

        // Create an agent
        AIAgent<String, String> agent = AIAgent.builder()
            // Create a prompt executor using the LLM client
            .promptExecutor(new MultiLLMPromptExecutor(deepSeekClient))
            // Provide a model
            .llmModel(DeepSeekModels.DeepSeekChat)
            .build();

        // Run the agent
        String result = agent.run("Hello! How can you help me?");
        System.out.println(result);
        ```

    该示例可以生成以下输出：

    ```
    Hello! I'm here to assist you with a wide range of tasks, including answering questions, providing information, helping with problem-solving, offering creative ideas, and even just chatting. Whether you need help with research, writing, learning something new, or simply want to discuss a topic, feel free to ask—I’m happy to help! 😊
    ```

=== "OpenRouter"

    以下示例使用 [`GPT-4o`](https://openrouter.ai/openai/gpt-4o) 模型，通过 OpenRouter API 创建并运行一个简单的 Koog 代理。

    === "Kotlin"

        ```kotlin
        fun main() = runBlocking {
            // Get the OpenRouter API key from the OPENROUTER_API_KEY environment variable
            val apiKey = System.getenv("OPENROUTER_API_KEY")
                ?: error("The API key is not set.")

            // Create an agent
            val agent = AIAgent(
                promptExecutor = simpleOpenRouterExecutor(apiKey),
                llmModel = OpenRouterModels.GPT4o
            )

            // Run the agent
            val result = agent.run("Hello! How can you help me?")
            println(result)
        }
        ```

    === "Java"

        ```java
        // Get the OpenRouter API key from the OPENROUTER_API_KEY environment variable
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("The API key is not set.");
        }

        // Create an agent
        AIAgent<String, String> agent = AIAgent.builder()
            .promptExecutor(simpleOpenRouterExecutor(apiKey))
            .llmModel(OpenRouterModels.GPT4o)
            .build();

        // Run the agent
        String result = agent.run("Hello! How can you help me?");
        System.out.println(result);
        ```

    该示例可以生成以下输出：

    ```
    I can answer questions, help with writing, solve problems, organize tasks, and more—just let me know what you need!
    ```

=== "Bedrock"

    以下示例使用 [`Claude Sonnet 4.5`](https://www.anthropic.com/news/claude-sonnet-4-5) 模型，通过 Bedrock API 创建并运行一个简单的 Koog 代理。

    === "Kotlin"

        ```kotlin
        fun main() = runBlocking {
            // Get the Bedrock API key from the BEDROCK_API_KEY environment variable
            val apiKey = System.getenv("BEDROCK_API_KEY")
                ?: error("The API key is not set.")

            // Create an agent
            val agent = AIAgent(
                promptExecutor = simpleBedrockExecutorWithBearerToken(apiKey),
                llmModel = BedrockModels.AnthropicClaude4_5Sonnet
            )

            // Run the agent
            val result = agent.run("Hello! How can you help me?")
            println(result)
        }
        ```

    === "Java"

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

    该示例可以生成以下输出：

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

=== "Mistral"

    以下示例使用 [`Mistral Medium 3.1`](https://docs.mistral.ai/models/mistral-medium-3-1-25-08) 模型，通过 Mistral AI API 创建并运行一个简单的 Koog 智能体。

    === "Kotlin"

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

    === "Java"

        ```java
        // Get the Mistral AI API key from the MISTRAL_API_KEY environment variable
        String apiKey = System.getenv("MISTRAL_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("The API key is not set.");
        }

        // Create an agent
        AIAgent<String, String> agent = AIAgent.builder()
            .promptExecutor(simpleMistralAIExecutor(apiKey))
            .llmModel(MistralAIModels.Chat.MistralMedium31)
            .build();

        // Run the agent
        String result = agent.run("Hello! How can you help me?");
        System.out.println(result);
        ```

    该示例可以生成以下输出：

    ```
    I can assist you with a wide range of topics and tasks. Here are some examples:

    1. **Answering questions**: I can provide information on various subjects, including history, science, technology, literature, and more.
    2. **Providing definitions**: If you're unsure about the meaning of a word or phrase, I can help define it for you.
    3. **Generating text**: Whether it's writing an email, creating content for social media, or composing a story, I can help with text generation.
    4. **Translation**: I can translate text from one language to another.
    5. **Conversation**: We can have a chat about any topic that interests you, and I'll respond accordingly.
    6. **Language practice**: If you're learning a new language, I can help with pronunciation, grammar, and vocabulary practice.
    7. **Brainstorming**: If you're stuck on a problem or need ideas for a project, I can help brainstorm solutions.
    8. **Summarization**: If you have a long piece of text and want a summary, I can condense it for you.

    What's on your mind? Is there something specific you'd like help with?
    ```

=== "Ollama"

    以下示例使用通过 Ollama 本地运行的 [`llama3.2`](https://ollama.com/library/llama3.2) 模型创建并运行一个简单的 Koog 智能体。

    === "Kotlin"

        ```kotlin
        fun main() = runBlocking {
            // Create an agent
            val agent = AIAgent(
                promptExecutor = simpleOllamaAIExecutor(),
                llmModel = OllamaModels.Meta.LLAMA_3_2
            )

            // Run the agent
            val result = agent.run("Hello! How can you help me?")
            println(result)
        }
        ```

    === "Java"

        ```java
        // Create an agent
        AIAgent<String, String> agent = AIAgent.builder()
            .promptExecutor(simpleOllamaAIExecutor("http://localhost:11434"))
            .llmModel(OllamaModels.Meta.LLAMA_3_2)
            .build();

        // Run the agent
        String result = agent.run("Hello! How can you help me?");
        System.out.println(result);
        ```

    该示例可以生成以下输出：

    ```
    I can assist with various tasks such as answering questions, providing information, and even helping with language-related tasks like proofreading or writing suggestions. What's on your mind today?
    ```

## 下一步 { #next-steps }

- 了解更多关于[智能体类型](agents/index.md)的信息
