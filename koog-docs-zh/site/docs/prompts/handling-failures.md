<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T16:55:03+00:00", "source_path": "prompts/handling-failures.md", "source_sha256": "5f6f1c637ffca1bb4e830c200a97215a966ebf5dca6a9fe52a2a99927ed2ab1d", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 处理故障 { #handling-failures }

本页介绍如何使用内置的重试和超时机制处理 LLM 客户端和提示执行器的故障。

## 重试功能 { #retry-functionality }

在使用 LLM 提供商时，可能会遇到速率限制或临时服务不可用等瞬时错误。
`RetryingLLMClient` 装饰器为 Kotlin 和 Java 中的任何 LLM 客户端添加了自动重试逻辑。

### 基本用法 { #basic-usage }

用重试功能包装任何现有客户端：

=== "Kotlin"

    ```kotlin
    // Wrap any client with the retry capability
    val client = OpenAILLMClient(apiKey)
    val resilientClient = RetryingLLMClient(client)

    // Now all operations will automatically retry on transient errors
    val response = resilientClient.execute(prompt, OpenAIModels.Chat.GPT4o)
    ```

=== "Java"

    ```java
    OpenAILLMClient client = new OpenAILLMClient(apiKey);
    RetryingLLMClient resilientClient = new RetryingLLMClient(client);

    // Now all operations will automatically retry on transient errors
    List<Message.Response> response = resilientClient.execute(prompt, OpenAIModels.Chat.GPT4o);
    ```

### Configuring retry behavior { #configuring-retry-behavior }

默认情况下，`RetryingLLMClient` 配置 LLM 客户端时，最多重试 3 次，初始延迟为 1 秒，最大延迟为 30 秒。您可以通过向 `RetryingLLMClient` 传递 `RetryConfig` 来指定不同的重试配置。例如：

=== "Kotlin"

    ```kotlin
    // Use the predefined configuration
    val conservativeClient = RetryingLLMClient(
        delegate = client,
        config = RetryConfig.CONSERVATIVE
    )
    ```

=== "Java"

    ```java
    OpenAILLMClient client = new OpenAILLMClient(apiKey);
    // Use the predefined configuration
    RetryingLLMClient conservativeClient = new RetryingLLMClient(
        client,
        RetryConfig.Companion.getCONSERVATIVE()
    );
    ```

Koog 提供了多种预定义的重试配置，可通过 Kotlin 中的 `RetryConfig` 以及 Java 中的 `RetryConfig.Companion` 来使用：

| 配置 (Kotlin) | 最大尝试次数 | 初始延迟 | 最大延迟 | 用例 |
|----------------------------|--------------|---------------|-----------|----------------------------------------------------------------------------------------------------------|
| `RetryConfig.DISABLED` | 1（无重试） | - | - | 开发、测试与调试。 |
| `RetryConfig.CONSERVATIVE` | 3 | 2秒 | 30秒 | 后台或计划任务，其中可靠性比速度更重要。 |
| `RetryConfig.AGGRESSIVE` | 5 | 500毫秒 | 20秒 | 关键操作中，快速从瞬时错误中恢复比减少API调用更为重要。 |
| `RetryConfig.PRODUCTION`   | 3            | 1s            | 20s       | General production use.                                                                                  |

您可以直接使用它们，也可以创建自定义配置：

```kotlin
// Or create a custom configuration
val customClient = RetryingLLMClient(
    delegate = client,
    config = RetryConfig(
        maxAttempts = 5,
        initialDelay = 1.seconds,
        maxDelay = 30.seconds,
        backoffMultiplier = 2.0,
        jitterFactor = 0.2
    )
)
```

### Retry error patterns { #retry-error-patterns }

默认情况下，`RetryingLLMClient` 能够识别常见的瞬时错误。此行为由 [`RetryConfig.retryablePatterns`](api:prompt-executor-clients::ai.koog.prompt.executor.clients.retry.RetryConfig.retryablePatterns) 模式控制。每个模式通过 [`RetryablePattern`](api:prompt-executor-clients::ai.koog.prompt.executor.clients.retry.RetryablePattern) 来检查失败请求中的错误信息，并决定是否应进行重试。

Koog 提供了预定义的重试配置和模式，这些配置和模式适用于所有支持的 LLM 提供商。您可以选择保留默认设置，也可以根据具体需求进行自定义。

#### Pattern types { #pattern-types }

您可以使用以下模式类型，并可任意组合多种模式：

* `RetryablePattern.Status`：匹配错误信息中特定的HTTP状态码（例如`429`、`500`、`502`等）。
* `RetryablePattern.Keyword`: 匹配错误消息中的关键词（例如 `rate limit` 或 `request timeout`）。
* `RetryablePattern.Regex`: 匹配错误消息中的正则表达式。
* `RetryablePattern.Custom`: 使用 lambda 函数匹配自定义逻辑。

如果任何模式返回`true`，则错误被视为可重试，LLM客户端将重试该请求。

#### Default patterns { #default-patterns }

除非您自定义重试配置，否则将默认采用以下模式：

* **HTTP status codes**:
    * `429`: Rate limit
    * `500`: Internal server error
    * `502`: Bad gateway
    * `503`: Service unavailable
    * `504`: Gateway timeout
    * `529`: Anthropic overloaded

* **Error keywords**:
    * rate limit
    * too many requests
    * request timeout
    * connection timeout
    * read timeout
    * write timeout
    * 连接被对端重置
    * connection refused
    * temporarily unavailable
    * service unavailable

这些默认模式在Koog中被定义为[`RetryConfig.DEFAULT_PATTERNS`](api:prompt-executor-clients::ai.koog.prompt.executor.clients.retry.RetryConfig.Companion.DEFAULT_PATTERNS)。

#### Custom patterns { #custom-patterns }

您可以根据具体需求定义自定义模式：

```kotlin
val config = RetryConfig(
    retryablePatterns = listOf(
        RetryablePattern.Status(429),   // Specific status code
        RetryablePattern.Keyword("quota"),  // Keyword in error message
        RetryablePattern.Regex(Regex("ERR_\\d+")),  // Custom regex pattern
        RetryablePattern.Custom { error ->  // Custom logic
            error.contains("temporary") && error.length > 20
        }
    )
)
```

您也可以将自定义模式附加到默认的 `RetryConfig.DEFAULT_PATTERNS` 中：

```kotlin
val config = RetryConfig(
    retryablePatterns = RetryConfig.DEFAULT_PATTERNS + listOf(
        RetryablePattern.Keyword("custom_error")
    )
)
```

### Streaming with retry { #streaming-with-retry }

流式操作可选择性地进行重试。此功能默认处于禁用状态。

```kotlin
val config = RetryConfig(
    maxAttempts = 3
)

val client = RetryingLLMClient(baseClient, config)
val stream = client.executeStreaming(prompt, OpenAIModels.Chat.GPT4o)
```

!!!note 流式重试仅适用于在接收到首个令牌之前发生的连接故障。一旦流式传输开始，重试逻辑将被禁用。若在流式传输过程中发生错误，操作将被终止。

### 使用提示执行器重试 { #retry-with-prompt-executors }

在使用提示执行器时，你可以在创建执行器之前，为底层的LLM客户端添加重试机制，无论是在Kotlin还是Java中。要了解更多关于提示执行器的信息，请参阅[提示执行器](prompt-executors.md)。

=== "Kotlin"

    ```kotlin
    // Single provider executor with retry
    val resilientClient = RetryingLLMClient(
        OpenAILLMClient(System.getenv("OPENAI_API_KEY")),
        RetryConfig.PRODUCTION
    )
    val executor = MultiLLMPromptExecutor(resilientClient)

    // Multi-provider executor with flexible client configuration
    val multiExecutor = MultiLLMPromptExecutor(
        LLMProvider.OpenAI to RetryingLLMClient(
            OpenAILLMClient(System.getenv("OPENAI_API_KEY")),
            RetryConfig.CONSERVATIVE
        ),
        LLMProvider.Anthropic to RetryingLLMClient(
            AnthropicLLMClient(System.getenv("ANTHROPIC_API_KEY")),
            RetryConfig.AGGRESSIVE  
        ),
        // The Bedrock client already has a built-in AWS SDK retry 
        LLMProvider.Bedrock to BedrockLLMClient(
            identityProvider = StaticCredentialsProvider {
                accessKeyId = System.getenv("AWS_ACCESS_KEY_ID")
                secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY")
                sessionToken = System.getenv("AWS_SESSION_TOKEN")
            },
        ),
    )
    ```

=== "Java"

    ```java
    // Single provider executor with retry (Java)
    RetryingLLMClient resilientClient = new RetryingLLMClient(
        new OpenAILLMClient(System.getenv("OPENAI_API_KEY")),
        RetryConfig.Companion.getPRODUCTION()
    );

    MultiLLMPromptExecutor executor = new MultiLLMPromptExecutor(resilientClient);

    // Multi-provider executor with flexible client configuration (Java)
    LLMClient openai = new RetryingLLMClient(
        new OpenAILLMClient(System.getenv("OPENAI_API_KEY")),
        RetryConfig.Companion.getCONSERVATIVE()
    );

    LLMClient anthropic = new RetryingLLMClient(
        new AnthropicLLMClient(System.getenv("ANTHROPIC_API_KEY")),
        RetryConfig.Companion.getAGGRESSIVE()
    );

    Map<LLMProvider, LLMClient> clients = Map.of(
        LLMProvider.OpenAI, openai,
        LLMProvider.Anthropic, anthropic
    );

    MultiLLMPromptExecutor multiExecutor = new MultiLLMPromptExecutor(clients);
    ```

## Timeout configuration { #timeout-configuration }

所有 LLM 客户端均支持在 Kotlin 和 Java 中配置超时设置，以防止请求挂起。您可以在创建客户端时通过 [`ConnectionTimeoutConfig`](api:prompt-executor-clients::ai.koog.prompt.executor.clients.ConnectionTimeoutConfig) 类为网络连接指定超时值。

`ConnectionTimeoutConfig` 具有以下属性：

| 属性 | 默认值 | 描述 |
|------------------------|----------------------|---------------------------------------------------------------|
| `connectTimeoutMillis` | 60秒（60,000毫秒） | 连接到服务器的最大时间限制。 |
| `requestTimeoutMillis` | 15分钟（90万） | 整个请求完成的最大时间限制。 |
| `socketTimeoutMillis` | 15分钟（90万） | 已建立连接上等待数据的最大时间。 |

您可以根据具体需求自定义这些值。例如：

=== "Kotlin"

    ```kotlin
    val client = OpenAILLMClient(
        apiKey = apiKey,
        settings = OpenAIClientSettings(
            timeoutConfig = ConnectionTimeoutConfig(
                connectTimeoutMillis = 5000,    // 5 seconds to establish connection
                requestTimeoutMillis = 60000,    // 60 seconds for the entire request
                socketTimeoutMillis = 120000   // 120 seconds for data on the socket
            )
        )
    )
    ```

=== "Java"

    ```java
    String apiKey = System.getenv("OPENAI_API_KEY");
    ConnectionTimeoutConfig timeouts = new ConnectionTimeoutConfig(
        5000L,   // connectTimeoutMillis
        60000L,  // requestTimeoutMillis
        120000L  // socketTimeoutMillis
    );
    OpenAIClientSettings settings = new OpenAIClientSettings(
        "https://api.openai.com", // baseUrl
        timeouts,
        "v1/chat/completions",    // chatCompletionsPath
        "v1/responses",           // responsesAPIPath
        "v1/embeddings",          // embeddingsPath
        "v1/moderations",         // moderationsPath
        "v1/models"               // modelsPath
    );
    OpenAILLMClient client = new OpenAILLMClient(apiKey, settings);
    ```

!!! tip
    对于长时间运行或流式调用，请为`requestTimeoutMillis`和`socketTimeoutMillis`设置更高的值。

## Error handling { #error-handling }

在生产环境中使用 LLM 时，需要实现错误处理，包括：

- **Try-catch 块**用于处理意外错误。
- **记录带上下文的错误**以便调试。
- 关键操作的**后备方案**。
- **监控重试模式**以识别反复出现的问题。

以下是Kotlin和Java中错误处理的示例：

=== "Kotlin"

    ```kotlin
    val logger = LoggerFactory.getLogger("Example")
    val resilientClient = RetryingLLMClient(
        OpenAILLMClient(System.getenv("OPENAI_API_KEY")),
        RetryConfig.PRODUCTION
    )
    val prompt = prompt("test") { user("Hello") }
    val model = OpenAIModels.Chat.GPT4o

    fun processResponse(response: Any) { /* implmenentation */ }
    fun scheduleRetryLater() { /* implmenentation */ }
    fun notifyAdministrator() { /* implmenentation */ }
    fun useDefaultResponse() { /* implmenentation */ }

    try {
        val response = resilientClient.execute(prompt, model)
        processResponse(response)
    } catch (e: Exception) {
        logger.error("LLM operation failed", e)

        when {
            e.message?.contains("rate limit") == true -> {
                // Handle rate limiting specifically
                scheduleRetryLater()
            }
            e.message?.contains("invalid api key") == true -> {
                // Handle authentication errors
                notifyAdministrator()
            }
            else -> {
                // Fall back to an alternative solution
                useDefaultResponse()
            }
        }
    }
    ```

=== "Java"

    ```java
    Logger logger = LoggerFactory.getLogger("Example");
    RetryingLLMClient resilientClient = new RetryingLLMClient(
            new OpenAILLMClient(System.getenv("OPENAI_API_KEY")),
            RetryConfig.PRODUCTION
    );
    Prompt prompt = Prompt.builder("test")
            .user("Hello")
            .build();
    MultiLLMPromptExecutor promptExecutor = new MultiLLMPromptExecutor(resilientClient);

    Consumer<List<Message.Response>> processResponse = (resp) -> { /* implementation */ };
    Runnable scheduleRetryLater = () -> { /* implementation */ };
    Runnable notifyAdministrator = () -> { /* implementation */ };
    Runnable useDefaultResponse = () -> { /* implementation */ };

    try {
        List<Message.Response> response = promptExecutor.execute(prompt, OpenAIModels.Chat.GPT4o);
        processResponse.accept(response);
    } catch (Exception e) {
        logger.error("LLM operation failed", e);
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("rate limit")) {
            scheduleRetryLater.run();
        } else if (msg.contains("invalid api key")) {
            notifyAdministrator.run();
        } else {
            useDefaultResponse.run();
        }
    }
    ```
