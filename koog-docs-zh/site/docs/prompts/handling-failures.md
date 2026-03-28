<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:04:44+00:00", "source_path": "prompts/handling-failures.md", "source_sha256": "5f6f1c637ffca1bb4e830c200a97215a966ebf5dca6a9fe52a2a99927ed2ab1d", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 处理故障 { #handling-failures }

本页介绍如何使用内置的重试和超时机制处理 LLM 客户端和提示执行器的故障。

## 重试功能 { #retry-functionality }

在使用 LLM 提供商时，可能会遇到速率限制或临时服务不可用等瞬时错误。
`RetryingLLMClient` 装饰器为 Kotlin 和 Java 中的任何 LLM 客户端添加了自动重试逻辑。

### 基本用法 { #basic-usage }

使用重试功能包装任何现有客户端：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.clients.retry.RetryingLLMClient
    import ai.koog.prompt.dsl.prompt
    import kotlinx.coroutines.runBlocking
    fun main() {
        runBlocking {
            val apiKey = System.getenv("OPENAI_API_KEY")
            val prompt = prompt("test") {
                user("Hello")
            }
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    // 使用重试功能包装任何客户端
    val client = OpenAILLMClient(apiKey)
    val resilientClient = RetryingLLMClient(client)

    // 现在所有操作都会在瞬时错误时自动重试
    val response = resilientClient.execute(prompt, OpenAIModels.Chat.GPT4o)
    ```
    <!--- KNIT example-handling-failures-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    OpenAILLMClient client = new OpenAILLMClient(apiKey);
    RetryingLLMClient resilientClient = new RetryingLLMClient(client);

    // 现在所有操作都会在瞬时错误时自动重试
    List<Message.Response> response = resilientClient.execute(prompt, OpenAIModels.Chat.GPT4o);
    ```
    <!--- KNIT example-handling-failures-java-01.java -->

### 配置重试行为 { #configuring-retry-behavior }

默认情况下，`RetryingLLMClient` 将 LLM 客户端配置为最多重试 3 次，初始延迟 1 秒，最大延迟 30 秒。
您可以通过传递给 `RetryingLLMClient` 的 `RetryConfig` 指定不同的重试配置。
例如：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.clients.retry.RetryConfig
    import ai.koog.prompt.executor.clients.retry.RetryingLLMClient
    val apiKey = System.getenv("OPENAI_API_KEY")
    val client = OpenAILLMClient(apiKey)
    -->
    ```kotlin
    // 使用预定义配置
    val conservativeClient = RetryingLLMClient(
        delegate = client,
        config = RetryConfig.CONSERVATIVE
    )
    ```
    <!--- KNIT example-handling-failures-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    OpenAILLMClient client = new OpenAILLMClient(apiKey);
    // 使用预定义配置
    RetryingLLMClient conservativeClient = new RetryingLLMClient(
        client,
        RetryConfig.Companion.getCONSERVATIVE()
    );
    ```
    <!--- KNIT example-handling-failures-java-02.java -->

Koog 通过 Kotlin 中的 `RetryConfig` 和 Java 中的 `RetryConfig.Companion` 提供了几种预定义的重试配置：| 配置 (Kotlin)     | 最大重试次数 | 初始延迟 | 最大延迟 | 适用场景                                                                                                 |
|----------------------------|--------------|---------------|-----------|----------------------------------------------------------------------------------------------------------|
| `RetryConfig.DISABLED`     | 1 (不重试)   | -             | -         | 开发、测试和调试场景。                                                                                   |
| `RetryConfig.CONSERVATIVE` | 3            | 2秒           | 30秒      | 后台或定时任务，可靠性比速度更重要。                                                                     |
| `RetryConfig.AGGRESSIVE`   | 5            | 500毫秒       | 20秒      | 关键操作，快速从瞬时错误中恢复比减少 API 调用更重要。                                     |
| `RetryConfig.PRODUCTION`   | 3            | 1秒           | 20秒      | 通用生产环境。                                                                                           |

您可以直接使用这些配置，或创建自定义配置：

<!--- INCLUDE
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.retry.RetryConfig
import ai.koog.prompt.executor.clients.retry.RetryingLLMClient
import kotlin.time.Duration.Companion.seconds

val apiKey = System.getenv("OPENAI_API_KEY")
val client = OpenAILLMClient(apiKey)
-->
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
<!--- KNIT example-handling-failures-03.kt -->

### 重试错误模式 { #retry-error-patterns }

默认情况下，`RetryingLLMClient` 会识别常见的瞬时错误。
此行为由 [`RetryConfig.retryablePatterns`](api:prompt-executor-clients::ai.koog.prompt.executor.clients.retry.RetryConfig.retryablePatterns) 模式控制。
每个模式由
[`RetryablePattern`](api:prompt-executor-clients::ai.koog.prompt.executor.clients.retry.RetryablePattern)
表示，用于检查失败请求中的错误信息，并确定是否应重试。

Koog 提供了预定义的重试配置和模式，适用于所有支持的 LLM 提供商。
您可以保留默认设置，或根据具体需求进行自定义。

#### 模式类型 { #pattern-types }

您可以使用以下模式类型，并任意组合它们：

* `RetryablePattern.Status`：匹配错误信息中的特定 HTTP 状态码（例如 `429`、`500`、`502` 等）。
* `RetryablePattern.Keyword`：匹配错误信息中的关键词（例如 `rate limit` 或 `request timeout`）。
* `RetryablePattern.Regex`：匹配错误信息中的正则表达式。
* `RetryablePattern.Custom`：使用 lambda 函数匹配自定义逻辑。

如果任何模式返回 `true`，则错误被视为可重试，LLM 客户端将重试请求。

#### 默认模式 { #default-patterns }

除非您自定义重试配置，否则默认使用以下模式：

* **HTTP 状态码**：
    * `429`：速率限制
    * `500`：内部服务器错误
    * `502`：错误的网关
    * `503`：服务不可用
    * `504`：网关超时
    * `529`：Anthropic 过载

* **错误关键词**：
    * 速率限制
    * 请求过多
    * 请求超时
    * 连接超时
    * 读取超时
    * 写入超时
    * 连接被对端重置
    * 连接被拒绝
    * 暂时不可用
    * 服务不可用

这些默认模式在 Koog 中定义为 [`RetryConfig.DEFAULT_PATTERNS`](api:prompt-executor-clients::ai.koog.prompt.executor.clients.retry.RetryConfig.Companion.DEFAULT_PATTERNS)。

#### 自定义模式 { #custom-patterns }

您可以根据具体需求定义自定义模式：

<!--- INCLUDE
import ai.koog.prompt.executor.clients.retry.RetryConfig
import ai.koog.prompt.executor.clients.retry.RetryablePattern
-->
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
<!--- KNIT example-handling-failures-04.kt -->

您还可以将自定义模式附加到默认的 `RetryConfig.DEFAULT_PATTERNS`：<!--- INCLUDE
import ai.koog.prompt.executor.clients.retry.RetryConfig
import ai.koog.prompt.executor.clients.retry.RetryablePattern
-->
```kotlin
val config = RetryConfig(
    retryablePatterns = RetryConfig.DEFAULT_PATTERNS + listOf(
        RetryablePattern.Keyword("custom_error")
    )
)
```
<!--- KNIT example-handling-failures-05.kt -->

### 流式传输与重试 { #streaming-with-retry }

流式操作可选择性地启用重试机制。此功能默认处于禁用状态。

<!--- INCLUDE
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.retry.RetryConfig
import ai.koog.prompt.executor.clients.retry.RetryingLLMClient
import ai.koog.prompt.dsl.prompt
import kotlinx.coroutines.runBlocking
fun main() {
    runBlocking {
        val baseClient = OpenAILLMClient(System.getenv("OPENAI_API_KEY"))
        val prompt = prompt("test") {
            user("Generate a story")
        }
-->
<!--- SUFFIX
    }
}
-->
```kotlin
val config = RetryConfig(
    maxAttempts = 3
)

val client = RetryingLLMClient(baseClient, config)
val stream = client.executeStreaming(prompt, OpenAIModels.Chat.GPT4o)
```
<!--- KNIT example-handling-failures-06.kt -->

!!!note
    流式重试仅适用于在接收到首个令牌前发生的连接故障。
    一旦流式传输开始，重试逻辑将被禁用。
    若在流式传输过程中发生错误，操作将被终止。

### 提示执行器中的重试机制 { #retry-with-prompt-executors }

在使用提示执行器时，您可以在创建执行器之前，为底层的 LLM 客户端包装重试机制，这适用于 Kotlin 和 Java 两种场景。
要了解更多关于提示执行器的信息，请参阅[提示执行器](prompt-executors.md)。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
    import ai.koog.prompt.executor.clients.bedrock.BedrockLLMClient
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.clients.retry.RetryConfig
    import ai.koog.prompt.executor.clients.retry.RetryingLLMClient
    import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
    import ai.koog.prompt.llm.LLMProvider
    import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
    -->
    ```kotlin
    // 带重试的单提供商执行器
    val resilientClient = RetryingLLMClient(
        OpenAILLMClient(System.getenv("OPENAI_API_KEY")),
        RetryConfig.PRODUCTION
    )
    val executor = MultiLLMPromptExecutor(resilientClient)

    // 支持灵活客户端配置的多提供商执行器
    val multiExecutor = MultiLLMPromptExecutor(
        LLMProvider.OpenAI to RetryingLLMClient(
            OpenAILLMClient(System.getenv("OPENAI_API_KEY")),
            RetryConfig.CONSERVATIVE
        ),
        LLMProvider.Anthropic to RetryingLLMClient(
            AnthropicLLMClient(System.getenv("ANTHROPIC_API_KEY")),
            RetryConfig.AGGRESSIVE  
        ),
        // Bedrock 客户端已内置 AWS SDK 重试机制
        LLMProvider.Bedrock to BedrockLLMClient(
            identityProvider = StaticCredentialsProvider {
                accessKeyId = System.getenv("AWS_ACCESS_KEY_ID")
                secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY")
                sessionToken = System.getenv("AWS_SESSION_TOKEN")
            },
        ),
    )
    ```
    <!--- KNIT example-handling-failures-07.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // 带重试的单提供商执行器 (Java)
    RetryingLLMClient resilientClient = new RetryingLLMClient(
        new OpenAILLMClient(System.getenv("OPENAI_API_KEY")),
        RetryConfig.Companion.getPRODUCTION()
    );

    MultiLLMPromptExecutor executor = new MultiLLMPromptExecutor(resilientClient);

    // 支持灵活客户端配置的多提供商执行器 (Java)
    LLMClient openai = new RetryingLLMClient(
        new OpenAILLMClient(System.getenv("OPENAI_API_KEY")),
        RetryConfig.Companion.getCONSERVATIVE()
    );

    LLMClient anthropic = new RetryingLLMClient(
        new AnthropicLLMClient(System.getenv("ANTHROPIC_API_KEY")),
        RetryConfig.Companion.getAGGRESSIVE()
    );

```    Map<LLMProvider, LLMClient> clients = Map.of(
        LLMProvider.OpenAI, openai,
        LLMProvider.Anthropic, anthropic
    );

    MultiLLMPromptExecutor multiExecutor = new MultiLLMPromptExecutor(clients);
    ```
    <!--- KNIT example-handling-failures-java-03.java -->

## 超时配置 { #timeout-configuration }

所有 LLM 客户端均支持在 Kotlin 和 Java 中配置超时，以防止请求挂起。
您可以在创建客户端时使用 [`ConnectionTimeoutConfig`](api:prompt-executor-clients::ai.koog.prompt.executor.clients.ConnectionTimeoutConfig) 类为网络连接指定超时值。

`ConnectionTimeoutConfig` 具有以下属性：

| 属性               | 默认值               | 描述                                                   |
|--------------------|----------------------|-------------------------------------------------------|
| `connectTimeoutMillis` | 60 秒 (60,000)       | 建立到服务器的连接的最大时间。                         |
| `requestTimeoutMillis` | 15 分钟 (900,000)    | 整个请求完成的最大时间。                               |
| `socketTimeoutMillis`  | 15 分钟 (900,000)    | 在已建立的连接上等待数据的最大时间。                   |

您可以根据具体需求自定义这些值。例如：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.executor.clients.ConnectionTimeoutConfig
    import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    val apiKey = System.getenv("OPENAI_API_KEY")    
    -->
    ```kotlin
    val client = OpenAILLMClient(
        apiKey = apiKey,
        settings = OpenAIClientSettings(
            timeoutConfig = ConnectionTimeoutConfig(
                connectTimeoutMillis = 5000,    // 5 秒建立连接
                requestTimeoutMillis = 60000,    // 60 秒完成整个请求
                socketTimeoutMillis = 120000   // 120 秒在套接字上传输数据
            )
        )
    )
    ```
    <!--- KNIT example-handling-failures-08.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
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
    <!--- KNIT example-handling-failures-java-04.java -->

!!! tip
    对于长时间运行或流式调用，请为 `requestTimeoutMillis` 和 `socketTimeoutMillis` 设置更高的值。

## 错误处理 { #error-handling }

在生产环境中使用 LLM 时，您需要实现错误处理，包括：

- **Try-catch 块** 以处理意外错误。
- **记录带有上下文的错误** 以便调试。
- **关键操作的备用方案**。
- **监控重试模式** 以识别重复出现的问题。

以下是 Kotlin 和 Java 中的错误处理示例：

=== "Kotlin"<!--- INCLUDE
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.clients.retry.RetryingLLMClient
    import ai.koog.prompt.executor.clients.retry.RetryConfig
    import ai.koog.prompt.dsl.prompt
    import kotlinx.coroutines.runBlocking
    import org.slf4j.LoggerFactory
    fun main() {
        runBlocking {
    -->
<!--- SUFFIX
        }
    }
    -->
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
            // 专门处理速率限制
            scheduleRetryLater()
        }
        e.message?.contains("invalid api key") == true -> {
            // 处理认证错误
            notifyAdministrator()
        }
        else -> {
            // 回退到备用方案
            useDefaultResponse()
        }
    }
}
```
<!--- KNIT example-handling-failures-09.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.Prompt;
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.clients.retry.RetryConfig;
    import ai.koog.prompt.executor.clients.retry.RetryingLLMClient;
    import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor;
    import ai.koog.prompt.message.Message;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import java.util.List;
    import java.util.function.Consumer;
    class exampleHandlingFailuresJava05 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
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
    <!--- KNIT example-handling-failures-java-05.java -->