<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T16:49:25+00:00", "source_path": "prompts/prompt-executors.md", "source_sha256": "b7e65e777811281be076afb910954f0aa731b009d0737b213b05f0b52f1529fe", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 提示词执行器 { #prompt-executors }

提示词执行器提供了一个更高层次的抽象，让你能够管理一个或多个 LLM 客户端的生命周期。
你可以通过统一的接口与多个 LLM 服务商协作，无需关注特定服务商的细节，
并支持动态切换和故障转移。

## 执行器类型 { #executor-types }

Koog 提供了三种主要类型的提示词执行器，它们都实现了 [`PromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.model.PromptExecutor) 接口：| 类型            | <div style="width:175px">类</div>                                                                                                                               | 描述                                                                                                                                                                                                                                                          |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 单提供商        | [`SingleLLMPromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.llms.SingleLLMPromptExecutor) | 包装单个提供商的 LLM 客户端。如果您的智能体仅需在单个 LLM 提供商的不同模型间切换，请使用此执行器。                                                                                                                     |
| 多提供商        | [`MultiLLMPromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.llms.MultiLLMPromptExecutor)   | 包装多个 LLM 客户端，并根据 LLM 提供商路由调用。它可选择性地在请求的客户端不可用时使用配置的备用提供商和 LLM。如果您的智能体需要在不同提供商的 LLM 之间切换，请使用此执行器。 |
| 路由            | [`RoutingLLMPromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.llms.RoutingLLMPromptExecutor) | 使用路由策略将给定 LLM 模型的请求分发到多个客户端实例。使用此执行器可避免速率限制、提高吞吐量，并通过负载均衡实现故障转移策略。                                               |

## 创建单提供商执行器 { #creating-a-single-provider-executor }

要为特定 LLM 提供商创建提示执行器，请执行以下操作：1. 为特定提供商配置一个 LLM 客户端，并提供相应的 API 密钥。
2. 使用 [`MultiLLMPromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.llms.MultiLLMPromptExecutor) 创建一个提示执行器。

示例如下：

=== "Kotlin"

    ```kotlin
    val openAIClient = OpenAILLMClient(System.getenv("OPENAI_API_KEY"))
    val promptExecutor = MultiLLMPromptExecutor(openAIClient)
    ```

=== "Java"

    ```java
    OpenAILLMClient openAIClient = new OpenAILLMClient(System.getenv("OPENAI_API_KEY"));
    MultiLLMPromptExecutor promptExecutor = new MultiLLMPromptExecutor(openAIClient);
    ```

## Creating a multi-provider executor { #creating-a-multi-provider-executor }

要创建一个能与多个LLM提供者协同工作的提示执行器，请按以下步骤操作：

1. 为所需的LLM提供者配置客户端，并使用相应的API密钥。
2. 将配置好的客户端传递给 [`MultiLLMPromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.llms.MultiLLMPromptExecutor) 类的构造函数以创建提示执行器
   使用多个LLM提供程序。

=== "Kotlin"

    ```kotlin
    val openAIClient = OpenAILLMClient(System.getenv("OPENAI_API_KEY"))
    val ollamaClient = OllamaClient()

    val multiExecutor = MultiLLMPromptExecutor(
        LLMProvider.OpenAI to openAIClient,
        LLMProvider.Ollama to ollamaClient
    )
    ```

=== "Java"

    ```java
    OpenAILLMClient openAIClient = new OpenAILLMClient(System.getenv("OPENAI_API_KEY"));
    OllamaClient ollamaClient = new OllamaClient();

    MultiLLMPromptExecutor promptExecutor = new MultiLLMPromptExecutor(openAIClient, ollamaClient);
    ```

## Creating a routing executor { #creating-a-routing-executor }

!!! warning "Experimental API"
    路由功能目前处于实验阶段，未来版本中可能发生变化。如需使用，请通过`@OptIn(ExperimentalRoutingApi::class)`选择启用。

要创建一个提示执行器，通过路由策略将请求分发到多个LLM客户端实例，请按以下步骤操作：

1. 配置多个客户端实例（它们可以用于相同或不同的LLM提供商）及其对应的API密钥。
2. 使用路由策略（例如[`RoundRobinRouter`](api:prompt-executor-model::ai.koog.prompt.executor.llms.RoundRobinRouter)）创建一个路由器。
3. 将路由器传递给 [`RoutingLLMPromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.llms.RoutingLLMPromptExecutor) 类的构造函数。

这有助于避免速率限制、提高吞吐量，并实现故障转移策略。

=== "Kotlin"

    ```kotlin
    // Create multiple client instances
    val openAI1 = OpenAILLMClient(apiKey = "openai-key-1")
    val openAI2 = OpenAILLMClient(apiKey = "openai-key-2")
    val anthropic = AnthropicLLMClient(apiKey = "anthropic-key")

    // Create router with round-robin strategy
    val router = RoundRobinRouter(openAI1, openAI2, anthropic)

    // Create routing executor
    val routingExecutor = RoutingLLMPromptExecutor(router)
    ```

=== "Java"

    ```java
    // Create multiple client instances
    OpenAILLMClient openAI1 = new OpenAILLMClient("openai-key-1");
    OpenAILLMClient openAI2 = new OpenAILLMClient("openai-key-2");
    AnthropicLLMClient anthropic = new AnthropicLLMClient("anthropic-key");

    // Create router with round-robin strategy
    RoundRobinRouter router = new RoundRobinRouter(openAI1, openAI2, anthropic);

    // Create routing executor
    RoutingLLMPromptExecutor routingExecutor = new RoutingLLMPromptExecutor(router);
    ```

当您通过此执行器运行提示时，对OpenAI模型的请求将采用轮询策略在`openAI1`和`openAI2`之间交替进行。而对Anthropic模型的请求始终会发送至单一的`anthropic`客户端，因为轮询机制会为每个服务提供商维护独立的计数器。

您也可以通过创建一个实现 [`LLMClientRouter`](api:prompt-executor-model::ai.koog.prompt.executor.llms.LLMClientRouter) 接口的类来实现自定义路由策略。

## Pre-defined prompt executors { #pre-defined-prompt-executors }

为加快设置速度，Koog 为常见服务商提供了开箱即用的执行器实现，同时支持 Kotlin 和 Java 两种环境。

下表列出了**预定义的单提供者执行器**，它们返回配置了特定LLM客户端的`SingleLLMPromptExecutor`。

<!--TODO: SingleLLMPromptExecutor is deprecated and is being replaced by PromptExecutor. Once it is implemented,
the predefined executors will return a PromptExecutor instance configured with a specific client.-->

| LLM 提供商 | 提示执行器 | 描述 |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| OpenAI | [简单OpenAI执行器](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor) | 封装了`OpenAILLMClient`，用于运行基于OpenAI模型的提示。 |
| OpenAI | [simpleAzureOpenAIExecutor](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleAzureOpenAIExecutor) | 包装`OpenAILLMClient`以适配Azure OpenAI Service的使用配置。 |
| Anthropic | [简单Anthropic执行器](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor) | 封装了`AnthropicLLMClient`，用于运行基于Anthropic模型的提示。 |
| Google | [simpleGoogleAIExecutor](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor) | 封装了`GoogleLLMClient`，用于运行基于Google模型的提示。 |
| OpenRouter | [简单OpenRouter执行器](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleOpenRouterExecutor) | 包装`OpenRouterLLMClient`，使其能够通过OpenRouter运行提示。 |
| Amazon Bedrock | [简单Bedrock执行器](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleBedrockExecutor) | 包装 `BedrockLLMClient`，使其通过 AWS Bedrock 运行提示。 |
| Amazon Bedrock | [简单BedrockExecutorWithBearerToken](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleBedrockExecutorWithBearerToken) | 包装 `BedrockLLMClient` 并使用提供的 Bedrock API 密钥发送请求。 |
| Mistral | [simpleMistralAIExecutor](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleMistralAIExecutor) | 包装`MistralAILLMClient`，使其能够使用Mistral模型运行提示。 |
| Ollama | [simpleOllamaAIExecutor](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor) | 封装了`OllamaClient`，使其能够通过Ollama运行提示。 |

以下是一个创建预定义执行器的示例：

=== "Kotlin"

    ```kotlin
    // Create an OpenAI executor
    val promptExecutor = simpleOpenAIExecutor("OPENAI_API_KEY")
    ```

=== "Java"

    ```java
    // Create an OpenAI executor
    PromptExecutor openAIExecutor = simpleOpenAIExecutor("OPENAI_API_KEY");
    ```

## Running a prompt { #running-a-prompt }

要使用提示执行器运行提示，请按以下步骤操作：

1. Create a prompt executor.
2. 使用 `execute()` 方法运行带有特定 LLM 的提示。

这是一个示例：

=== "Kotlin"

    ```kotlin
    // Create an OpenAI executor
    val promptExecutor = simpleOpenAIExecutor("OPENAI_API_KEY")

    // Execute a prompt
    val response = promptExecutor.execute(
        prompt = prompt("demo") { user("Summarize this.") },
        model = OpenAIModels.Chat.GPT4o
    )
    ```

=== "Java"

    ```java
    // Create an OpenAI executor
    PromptExecutor promptExecutor = simpleOpenAIExecutor("OPENAI_API_KEY");

    // Create a prompt
    Prompt prompt = Prompt.builder("demo")
        .user("Summarize this.")
        .build();

    // Run the prompt
    List<Message.Response> response = promptExecutor.execute(prompt, OpenAIModels.Chat.GPT4o);
    ```

这将使用`GPT4o`模型运行提示并返回响应。

!!! note
    提示执行器提供了多种方法来运行提示，包括流式处理、多选生成和内容审核等功能。由于提示执行器封装了LLM客户端，因此每个执行器都支持对应客户端的全部能力。具体细节请参阅[LLM 客户端](llm-clients.md)。

## Switching between providers { #switching-between-providers }

当您使用`MultiLLMPromptExecutor`与多个LLM提供商协作时，可以在它们之间进行切换。具体流程如下：

1. 为每个要使用的提供商创建一个LLM客户端实例。
2. 创建一个`MultiLLMPromptExecutor`，用于将LLM提供者映射到LLM客户端。
3. 使用作为参数传递给`execute()`方法的对应客户端模型运行提示。
   提示执行器将根据模型提供商使用相应的客户端来运行提示。

以下是切换提供商的示例：

=== "Kotlin"

    ```kotlin
    // Create LLM clients for OpenAI, Anthropic, and Google providers
    val openAIClient = OpenAILLMClient("OPENAI_API_KEY")
    val anthropicClient = AnthropicLLMClient("ANTHROPIC_API_KEY")
    val googleClient = GoogleLLMClient("GOOGLE_API_KEY")

    // Create a MultiLLMPromptExecutor that maps LLM providers to LLM clients
    val executor = MultiLLMPromptExecutor(
        LLMProvider.OpenAI to openAIClient,
        LLMProvider.Anthropic to anthropicClient,
        LLMProvider.Google to googleClient
    )

    // Create a prompt
    val p = prompt("demo") { user("Summarize this.") }

    // Run the prompt with an OpenAI model; the prompt executor automatically switches to the OpenAI client
    val openAIResult = executor.execute(p, OpenAIModels.Chat.GPT4o)

    // Run the prompt with an Anthropic model; the prompt executor automatically switches to the Anthropic client
    val anthropicResult = executor.execute(p, AnthropicModels.Sonnet_4_5)
    ```

=== "Java"

    ```java
    // Create LLM clients for OpenAI, Anthropic, and Google providers
    OpenAILLMClient openAIClient = new OpenAILLMClient("OPENAI_API_KEY");
    AnthropicLLMClient anthropicClient = new AnthropicLLMClient("ANTHROPIC_API_KEY");
    GoogleLLMClient googleClient = new GoogleLLMClient("GOOGLE_API_KEY");

    // Create a MultiLLMPromptExecutor that maps LLM providers to LLM clients
    MultiLLMPromptExecutor promptExecutor = new MultiLLMPromptExecutor(
        Map.of(
            LLMProvider.OpenAI, openAIClient,
            LLMProvider.Anthropic, anthropicClient,
            LLMProvider.Google, googleClient
        )
    );

    // Create a prompt
    Prompt prompt = Prompt.builder("demo")
        .user("Summarize this.")
        .build();

    // Run the prompt with an OpenAI model; the prompt executor automatically switches to the OpenAI client
    List<Message.Response> openAIResult = promptExecutor.execute(prompt, OpenAIModels.Chat.GPT4o);

    // Run the prompt with an Anthropic model; the prompt executor automatically switches to the Anthropic client
    List<Message.Response> anthropicResult = promptExecutor.execute(prompt, AnthropicModels.Sonnet_4_5);
    ```

您可以选择配置一个备用的 LLM 提供者和模型，以便在请求的客户端不可用时使用。具体详情请参阅 [配置回退机制](#configuring-fallbacks)。

## Configuring fallbacks { #configuring-fallbacks }

多提供商和路由提示执行器可以配置为在请求的LLM客户端不可用时，使用备用的LLM提供商和模型。

要配置回退机制，请在创建 `MultiLLMPromptExecutor` 或 `RoutingLLMPromptExecutor` 时传入回退设置：

=== "Kotlin"

    ```kotlin
    val openAIClient = OpenAILLMClient(System.getenv("OPENAI_API_KEY"))
    val ollamaClient = OllamaClient()

    val multiExecutor = MultiLLMPromptExecutor(
        LLMProvider.OpenAI to openAIClient,
        LLMProvider.Ollama to ollamaClient,
        fallback = MultiLLMPromptExecutor.FallbackPromptExecutorSettings(
            fallbackProvider = LLMProvider.Ollama,
            fallbackModel = OllamaModels.Meta.LLAMA_3_2
        )
    )
    ```

=== "Java"

    ```java
    OpenAILLMClient openAIClient = new OpenAILLMClient(System.getenv("OPENAI_API_KEY"));
    OllamaClient ollamaClient = new OllamaClient();

    MultiLLMPromptExecutor multiExecutor = new MultiLLMPromptExecutor(
        Map.of(
            LLMProvider.OpenAI, openAIClient,
            LLMProvider.Ollama, ollamaClient
        ),
        new MultiLLMPromptExecutor.FallbackPromptExecutorSettings(
            LLMProvider.Ollama,
            OllamaModels.Meta.LLAMA_3_2
        )
    );
    ```

如果您传递的模型来自LLM提供商，且该模型未包含在`MultiLLMPromptExecutor`中，提示执行器将使用备用模型：

=== "Kotlin"

    ```kotlin
    // Create a prompt
    val p = prompt("demo") { user("Summarize this") }
    // If you pass a Google model, the prompt executor will use the fallback model, as the Google client is not included
    val response = multiExecutor.execute(p, GoogleModels.Gemini2_5Pro)
    ```

=== "Java"

    ```java
    // Create a prompt
    Prompt p = Prompt.builder("demo")
        .user("Summarize this")
        .build();

    // If you pass a Google model, the prompt executor will use the fallback model, as the Google client is not included
    List<Message.Response> response = multiExecutor.execute(p, GoogleModels.Gemini2_5Pro);
    ```

!!! note
    仅针对`execute()`和`executeMultipleChoices()`方法提供回退机制。

