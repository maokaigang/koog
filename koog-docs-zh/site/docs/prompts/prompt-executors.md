<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:12:25+00:00", "source_path": "prompts/prompt-executors.md", "source_sha256": "b7e65e777811281be076afb910954f0aa731b009d0737b213b05f0b52f1529fe", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 提示执行器 { #prompt-executors }

提示执行器提供了一个更高层次的抽象，让你能够管理一个或多个 LLM 客户端的生命周期。
你可以通过统一的接口与多个 LLM 提供商协作，抽象掉提供商特定的细节，
并支持动态切换和故障转移。

## 执行器类型 { #executor-types }

Koog 提供了三种主要类型的提示执行器，它们都实现了 [`PromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.model.PromptExecutor) 接口：

| 类型            | <div style="width:175px">类</div>                                                                                                                               | 描述                                                                                                                                                                                                                                                          |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 单提供商        | [`SingleLLMPromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.llms.SingleLLMPromptExecutor) | 包装单个 LLM 客户端，用于一个提供商。如果你的智能体只需要在单个 LLM 提供商内切换模型，请使用此执行器。                                                                                                                     |
| 多提供商        | [`MultiLLMPromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.llms.MultiLLMPromptExecutor)   | 包装多个 LLM 客户端，并根据 LLM 提供商路由调用。它可以选择性地使用配置的备用提供商和 LLM，当请求的客户端不可用时。如果你的智能体需要在不同提供商的 LLM 之间切换，请使用此执行器。 |
| 路由            | [`RoutingLLMPromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.llms.RoutingLLMPromptExecutor) | 使用路由策略将请求分发到给定 LLM 模型的多个客户端实例。使用此执行器可以避免速率限制、提高吞吐量，并通过负载均衡实现故障转移策略。                                               |

## 创建单提供商执行器 { #creating-a-single-provider-executor }

要为特定的 LLM 提供商创建提示执行器，请执行以下步骤：

1. 使用相应的 API 密钥为特定提供商配置一个 LLM 客户端。
2. 使用 [`MultiLLMPromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.llms.MultiLLMPromptExecutor) 创建提示执行器。

以下是一个示例：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
    -->

    ```kotlin
    val openAIClient = OpenAILLMClient(System.getenv("OPENAI_API_KEY"))
    val promptExecutor = MultiLLMPromptExecutor(openAIClient)
    ```
    <!--- KNIT example-prompt-executors-01.kt -->

=== "Java"<!--- INCLUDE
    /**
    -->
<!--- SUFFIX
    **/
    -->
```java
OpenAILLMClient openAIClient = new OpenAILLMClient(System.getenv("OPENAI_API_KEY"));
MultiLLMPromptExecutor promptExecutor = new MultiLLMPromptExecutor(openAIClient);
```
<!--- KNIT example-prompt-executors-java-01.java -->

## 创建多提供商执行器 { #creating-a-multi-provider-executor }

要创建一个支持多个 LLM 提供商的提示执行器，请按以下步骤操作：

1. 为所需的 LLM 提供商配置客户端，并提供相应的 API 密钥。
2. 将配置好的客户端传递给 [`MultiLLMPromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.llms.MultiLLMPromptExecutor) 类的构造函数，以创建一个支持多个 LLM 提供商的提示执行器。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.ollama.client.OllamaClient
    import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
    import ai.koog.prompt.llm.LLMProvider
    -->

    ```kotlin
    val openAIClient = OpenAILLMClient(System.getenv("OPENAI_API_KEY"))
    val ollamaClient = OllamaClient()

    val multiExecutor = MultiLLMPromptExecutor(
        LLMProvider.OpenAI to openAIClient,
        LLMProvider.Ollama to ollamaClient
    )
    ```
    <!--- KNIT example-prompt-executors-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    OpenAILLMClient openAIClient = new OpenAILLMClient(System.getenv("OPENAI_API_KEY"));
    OllamaClient ollamaClient = new OllamaClient();

    MultiLLMPromptExecutor promptExecutor = new MultiLLMPromptExecutor(openAIClient, ollamaClient);
    ```
    <!--- KNIT example-prompt-executors-java-02.java -->

## 创建路由执行器 { #creating-a-routing-executor }

!!! warning "实验性 API"
    路由功能目前处于实验阶段，未来版本中可能会发生变化。
    如需使用，请通过 `@OptIn(ExperimentalRoutingApi::class)` 选择启用。

要创建一个使用路由策略在多个 LLM 客户端实例之间分发请求的提示执行器，请按以下步骤操作：

1. 配置多个客户端实例（可以是相同或不同的 LLM 提供商），并提供相应的 API 密钥。
2. 使用路由策略（例如 [`RoundRobinRouter`](api:prompt-executor-model::ai.koog.prompt.executor.llms.RoundRobinRouter)）创建一个路由器。
3. 将路由器传递给 [`RoutingLLMPromptExecutor`](api:prompt-executor-model::ai.koog.prompt.executor.llms.RoutingLLMPromptExecutor) 类的构造函数。

这对于避免速率限制、提高吞吐量以及实现故障转移策略非常有用。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
    import ai.koog.prompt.executor.llms.RoundRobinRouter
    import ai.koog.prompt.executor.llms.RoutingLLMPromptExecutor
    -->
    ```kotlin
    // 创建多个客户端实例
    val openAI1 = OpenAILLMClient(apiKey = "openai-key-1")
    val openAI2 = OpenAILLMClient(apiKey = "openai-key-2")
    val anthropic = AnthropicLLMClient(apiKey = "anthropic-key")

    // 使用轮询策略创建路由器
    val router = RoundRobinRouter(openAI1, openAI2, anthropic)

    // 创建路由执行器
    val routingExecutor = RoutingLLMPromptExecutor(router)
    ```
    <!--- KNIT example-prompt-executors-03.kt -->

=== "Java"<!--- INCLUDE
    /**
    -->
<!--- SUFFIX
    **/
    -->
```java
// 创建多个客户端实例
OpenAILLMClient openAI1 = new OpenAILLMClient("openai-key-1");
OpenAILLMClient openAI2 = new OpenAILLMClient("openai-key-2");
AnthropicLLMClient anthropic = new AnthropicLLMClient("anthropic-key");

// 使用轮询策略创建路由器
RoundRobinRouter router = new RoundRobinRouter(openAI1, openAI2, anthropic);

// 创建路由执行器
RoutingLLMPromptExecutor routingExecutor = new RoutingLLMPromptExecutor(router);
```
<!--- KNIT example-prompt-executors-java-03.java -->

当你使用此执行器执行提示时，对 OpenAI 模型的请求将按照轮询策略在 `openAI1` 和 `openAI2` 之间交替进行。
对 Anthropic 模型的请求始终会发送到单一的 `anthropic` 客户端，因为轮询策略会为每个提供商维护独立的计数器。

你也可以通过创建实现 [`LLMClientRouter`](api:prompt-executor-model::ai.koog.prompt.executor.llms.LLMClientRouter) 接口的类来实现自定义路由策略。

## 预定义的提示执行器 { #pre-defined-prompt-executors }

为了更快地设置，Koog 在 Kotlin 和 Java 中为常见提供商提供了即用型执行器实现。

下表列出了**预定义的单提供商执行器**，
它们返回配置了特定 LLM 客户端的 `SingleLLMPromptExecutor`。

<!--TODO: SingleLLMPromptExecutor is deprecated and is being replaced by PromptExecutor. Once it is implemented,
the predefined executors will return a PromptExecutor instance configured with a specific client.-->| LLM 提供商 | 提示词执行器                                                                                                                                                                             | 描述                                                                      |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| OpenAI         | [simpleOpenAIExecutor](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor)                                  | 封装了使用 OpenAI 模型运行提示词的 `OpenAILLMClient`。                    |
| OpenAI         | [simpleAzureOpenAIExecutor](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleAzureOpenAIExecutor)                       | 封装了配置为使用 Azure OpenAI Service 的 `OpenAILLMClient`。               |
| Anthropic      | [simpleAnthropicExecutor](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor)                              | 封装了使用 Anthropic 模型运行提示词的 `AnthropicLLMClient`。              |
| Google         | [simpleGoogleAIExecutor](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor)                              | 封装了使用 Google 模型运行提示词的 `GoogleLLMClient`。                    |
| OpenRouter     | [simpleOpenRouterExecutor](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleOpenRouterExecutor)                           | 封装了使用 OpenRouter 运行提示词的 `OpenRouterLLMClient`。                   |
| Amazon Bedrock | [simpleBedrockExecutor](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleBedrockExecutor)                                  | 封装了使用 AWS Bedrock 运行提示词的 `BedrockLLMClient`。                     |
| Amazon Bedrock | [simpleBedrockExecutorWithBearerToken](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleBedrockExecutorWithBearerToken) | 封装了 `BedrockLLMClient` 并使用提供的 Bedrock API 密钥发送请求。 |
| Mistral        | [simpleMistralAIExecutor](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleMistralAIExecutor)                            | 封装了使用 Mistral 模型运行提示词的 `MistralAILLMClient`。                |
| Ollama         | [simpleOllamaAIExecutor](api:prompt-executor-llms-all::ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor)                              | 封装了使用 Ollama 运行提示词的 `OllamaClient`。                              |

以下是创建预定义执行器的示例：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
    import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
    import ai.koog.prompt.executor.clients.google.GoogleLLMClient
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import kotlinx.coroutines.runBlocking
    -->

    ```kotlin
    // 创建 OpenAI 执行器
    val promptExecutor = simpleOpenAIExecutor("OPENAI_API_KEY")
    ```
    <!--- KNIT example-prompt-executors-04.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // 创建 OpenAI 执行器
    PromptExecutor openAIExecutor = simpleOpenAIExecutor("OPENAI_API_KEY");
    ```
    <!--- KNIT example-prompt-executors-java-04.java -->

## 运行提示词 { #running-a-prompt }

要使用提示词执行器运行提示词，请执行以下操作：

1. 创建提示词执行器。
2. 使用 `execute()` 方法运行具有特定 LLM 的提示词。

示例如下：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import kotlinx.coroutines.runBlocking
    fun main() {
        runBlocking {
    -->
    <!--- SUFFIX
        }
    }
    -->```kotlin
// 创建 OpenAI 执行器
val promptExecutor = simpleOpenAIExecutor("OPENAI_API_KEY")

// 执行提示词
val response = promptExecutor.execute(
    prompt = prompt("demo") { user("Summarize this.") },
    model = OpenAIModels.Chat.GPT4o
)
```
<!--- KNIT example-prompt-executors-05.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // 创建 OpenAI 执行器
    PromptExecutor promptExecutor = simpleOpenAIExecutor("OPENAI_API_KEY");

    // 创建提示词
    Prompt prompt = Prompt.builder("demo")
        .user("Summarize this.")
        .build();

    // 运行提示词
    List<Message.Response> response = promptExecutor.execute(prompt, OpenAIModels.Chat.GPT4o);
    ```
    <!--- KNIT example-prompt-executors-java-05.java -->

这将使用 `GPT4o` 模型运行提示词并返回响应。

!!! note
    提示词执行器提供了多种功能来运行提示词，
    例如流式传输、多选生成和内容审核。
    由于提示词执行器封装了 LLM 客户端，每个执行器都支持相应客户端的功能。
    详情请参阅 [LLM 客户端](llm-clients.md)。

## 在提供商之间切换 { #switching-between-providers }

当您使用 `MultiLLMPromptExecutor` 与多个 LLM 提供商合作时，可以在它们之间切换。
流程如下：

1. 为每个要使用的提供商创建一个 LLM 客户端实例。
2. 创建一个 `MultiLLMPromptExecutor`，将 LLM 提供商映射到 LLM 客户端。
3. 运行提示词时，将相应客户端的模型作为参数传递给 `execute()` 方法。
   提示词执行器将根据模型提供商使用对应的客户端来运行提示词。

以下是在提供商之间切换的示例：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
    import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
    import ai.koog.prompt.executor.clients.google.GoogleLLMClient
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
    import ai.koog.prompt.llm.LLMProvider
    import ai.koog.prompt.dsl.prompt
    import kotlinx.coroutines.runBlocking
    fun main() = runBlocking {
    -->
    <!--- SUFFIX
    }
    -->

    ```kotlin
    // 为 OpenAI、Anthropic 和 Google 提供商创建 LLM 客户端
    val openAIClient = OpenAILLMClient("OPENAI_API_KEY")
    val anthropicClient = AnthropicLLMClient("ANTHROPIC_API_KEY")
    val googleClient = GoogleLLMClient("GOOGLE_API_KEY")

    // 创建 MultiLLMPromptExecutor，将 LLM 提供商映射到 LLM 客户端
    val executor = MultiLLMPromptExecutor(
        LLMProvider.OpenAI to openAIClient,
        LLMProvider.Anthropic to anthropicClient,
        LLMProvider.Google to googleClient
    )

    // 创建提示词
    val p = prompt("demo") { user("Summarize this.") }

    // 使用 OpenAI 模型运行提示词；提示词执行器自动切换到 OpenAI 客户端
    val openAIResult = executor.execute(p, OpenAIModels.Chat.GPT4o)

    // 使用 Anthropic 模型运行提示词；提示词执行器自动切换到 Anthropic 客户端
    val anthropicResult = executor.execute(p, AnthropicModels.Sonnet_4_5)
    ```
    <!--- KNIT example-prompt-executors-06.kt -->

=== "Java"<!--- INCLUDE
    /**
    -->
<!--- SUFFIX
    **/
    -->
```java
// 为 OpenAI、Anthropic 和 Google 提供商创建 LLM 客户端
OpenAILLMClient openAIClient = new OpenAILLMClient("OPENAI_API_KEY");
AnthropicLLMClient anthropicClient = new AnthropicLLMClient("ANTHROPIC_API_KEY");
GoogleLLMClient googleClient = new GoogleLLMClient("GOOGLE_API_KEY");

// 创建一个 MultiLLMPromptExecutor，将 LLM 提供商映射到 LLM 客户端
MultiLLMPromptExecutor promptExecutor = new MultiLLMPromptExecutor(
    Map.of(
        LLMProvider.OpenAI, openAIClient,
        LLMProvider.Anthropic, anthropicClient,
        LLMProvider.Google, googleClient
    )
);

// 创建一个提示
Prompt prompt = Prompt.builder("demo")
    .user("Summarize this.")
    .build();

// 使用 OpenAI 模型运行提示；提示执行器会自动切换到 OpenAI 客户端
List<Message.Response> openAIResult = promptExecutor.execute(prompt, OpenAIModels.Chat.GPT4o);

// 使用 Anthropic 模型运行提示；提示执行器会自动切换到 Anthropic 客户端
List<Message.Response> anthropicResult = promptExecutor.execute(prompt, AnthropicModels.Sonnet_4_5);
```
<!--- KNIT example-prompt-executors-java-06.java -->

您可以选择配置一个备用的 LLM 提供商和模型，以便在请求的客户端不可用时使用。
有关详细信息，请参阅[配置回退机制](#configuring-fallbacks)。

## 配置回退机制 { #configuring-fallbacks }

多提供商和路由提示执行器可以配置为在请求的 LLM 客户端不可用时，使用备用的 LLM 提供商和模型。

要配置回退机制，请在创建 `MultiLLMPromptExecutor` 或 `RoutingLLMPromptExecutor` 时传递回退设置：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.ollama.client.OllamaClient
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import ai.koog.prompt.llm.LLMProvider
    -->

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
    <!--- KNIT example-prompt-executors-07.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    OpenAILLMClient openAIClient = new OpenAILLMClient(System.getenv("OPENAI_API_KEY"));
    OllamaClient ollamaClient = new OllamaClient();
```    MultiLLMPromptExecutor multiExecutor = new MultiLLMPromptExecutor(
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
    <!--- KNIT example-prompt-executors-java-07.java -->

如果你传入一个未包含在`MultiLLMPromptExecutor`中的LLM提供商的模型，
提示执行器将使用回退模型：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.ollama.client.OllamaClient
    import ai.koog.prompt.executor.clients.google.GoogleModels
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import ai.koog.prompt.llm.LLMProvider
    import kotlinx.coroutines.runBlocking
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
    fun main() = runBlocking {
    -->
    <!--- SUFFIX
    }
    -->

    ```kotlin
    // 创建提示
    val p = prompt("demo") { user("Summarize this") }
    // 如果传入Google模型，提示执行器将使用回退模型，因为Google客户端未包含在内
    val response = multiExecutor.execute(p, GoogleModels.Gemini2_5Pro)
    ```
    <!--- KNIT example-prompt-executors-08.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // 创建提示
    Prompt p = Prompt.builder("demo")
        .user("Summarize this")
        .build();

    // 如果传入Google模型，提示执行器将使用回退模型，因为Google客户端未包含在内
    List<Message.Response> response = multiExecutor.execute(p, GoogleModels.Gemini2_5Pro);
    ```
    <!--- KNIT example-prompt-executors-java-08.java -->

!!! note
    回退功能仅适用于`execute()`和`executeMultipleChoices()`方法。