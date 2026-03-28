<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:10:08+00:00", "source_path": "spring-ai-integration.md", "source_sha256": "466e66ad1e1d23402ea6f94bde7f97ee4aa123a9973312ef2ffec1e624e1779b", "source_tag": "0.7.3", "translation_status": "changed"} -->
# Spring AI 集成 { #spring-ai-integration }

Koog 提供了 Spring AI 集成启动器，将 Spring AI 的模型抽象与 Koog 智能体框架桥接起来。
如果您已使用 Spring AI 进行模型访问，这些启动器可以让您在现有 Spring AI 配置之上接入 Koog 的智能体编排功能——无需替换现有配置。

## 与 `koog-spring-boot-starter` 的区别 { #how-it-differs-from-koog-spring-boot-starter }

| | `koog-spring-boot-starter` | `koog-spring-ai` 启动器 |
|---|---|---|
| **LLM 传输** | Koog 自有的 HTTP 客户端（每个提供商一个：OpenAI、Anthropic、Google 等） | 委托给 Spring AI 的 `ChatModel` / `EmbeddingModel` —— 任何 Spring AI 支持的提供商均可自动工作 |
| **配置** | 每个提供商的 `ai.koog.*` 属性 | 由 Spring AI 启动器管理的标准 `spring.ai.*` 属性 |
| **适用场景** | 希望 Koog 直接管理 LLM 连接 | 已使用 Spring AI 进行模型访问，并希望在之上接入 Koog 的智能体编排功能 |

两种方案相互独立——根据您偏好的 LLM 连接管理方式选择其一。
关于直接的 Koog 启动器方案，请参阅 [Spring Boot 集成](spring-boot.md)。

## 可用启动器 { #available-starters }

| 模块 | 用途 |
|---|---|
| `koog-spring-ai-starter-model-chat` | 将 Spring AI `ChatModel`（可选包含 `ModerationModel`）适配为 Koog `LLMClient` 和 `PromptExecutor` |
| `koog-spring-ai-starter-model-embedding` | 将 Spring AI `EmbeddingModel` 适配为 Koog `LLMEmbeddingProvider` |

每个启动器都是完全独立的 Spring Boot 启动器，拥有自己的自动配置、配置属性和调度器管理。

## 聊天模型启动器 { #chat-model-starter }

### 概述 { #overview }

`koog-spring-ai-starter-model-chat` 启动器将 Spring AI 的聊天模型抽象与 Koog 智能体框架桥接起来。
它自动配置：

- 一个委托给 Spring AI `ChatModel` 的 Koog `LLMClient`（`SpringAiLLMClient`）
- 一个由所有可用 `LLMClient` bean 组装而成的 `PromptExecutor`（`MultiLLMPromptExecutor`）

工具始终由 Koog 智能体框架执行——Spring AI 仅接收工具定义/模式。
所有携带工具的请求都会将 `internalToolExecutionEnabled` 标志设置为 `false`。

### 添加依赖 { #add-dependency }

在添加任何 Spring AI 模型启动器（例如用于 Google 的启动器）时，同时添加此依赖：

=== "Gradle (Kotlin DSL)"

    ```kotlin
    // build.gradle.kts
    dependencies {
        implementation("ai.koog:koog-agents-jvm:$koogVersion")
        implementation("ai.koog:koog-spring-ai-starter-model-chat:$koogVersion")
        implementation("org.springframework.ai:spring-ai-starter-model-google-genai")
    }
    ```

=== "Maven"

    ```xml
    <dependencies>
        <dependency>
            <groupId>ai.koog</groupId>
            <artifactId>koog-agents-jvm</artifactId>
            <version>${koog.version}</version>
        </dependency>
        <dependency>
            <groupId>ai.koog</groupId>
            <artifactId>koog-spring-ai-starter-model-chat</artifactId>
            <version>${koog.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-google-genai</artifactId>
        </dependency>
    </dependencies>
    ```

Make sure that your project has:

- Spring Boot 3 (it requires Java 17 or higher)
- Kotlin libraries with version 2.3.10+ (kotlin-stdlib)
- A Spring AI model starter for your chosen provider

### Available providers
Anthropic, Azure OpenAI, Bedrock Converse, Deepseek, Google GenAI, HuggingFace, MiniMax, Mistral AI, OCI GenAI, Ollama, OpenAI, Vertex AI, ZhiPu AI

### Configure

Modify your Spring Boot properties if needed:

```properties
# put your API key for Gemini Developer API or pass it via an environment variable
spring.ai.google.genai.api-key=YOUR_GOOGLE_API_KEY
# default values
spring.ai.model.chat=google-genai
koog.spring.ai.chat.enabled=true
koog.spring.ai.chat.dispatcher.type=AUTO
```

If you have a single `ChatModel` bean, everything works automatically —
the adapter wraps it into a Koog `LLMClient` and creates a ready-to-use `PromptExecutor`.

### Usage Example

Inject the `PromptExecutor` and use it to run a Koog agent:

=== "Kotlin"

    ```kotlin
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.prompt.executor.clients.google.GoogleModels
    import ai.koog.prompt.executor.model.PromptExecutor
    import org.springframework.stereotype.Service

    @Service
    class MyAgentService(private val promptExecutor: PromptExecutor) {

        suspend fun askAgent(userMessage: String): String {
            val agent = AIAgent(
                promptExecutor = promptExecutor,
                llmModel = GoogleModels.Gemini2_5Flash,
                systemPrompt = "You are a helpful assistant."
            )

            return agent.run(userMessage)
        }
    }
    ```

=== "Java"

    ```java
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.prompt.executor.clients.google.GoogleModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import org.springframework.stereotype.Service;

    @Service
    public class MyAgentService {
        private final PromptExecutor promptExecutor;

        public MyAgentService(PromptExecutor promptExecutor) {
            this.promptExecutor = promptExecutor;
        }

        public String askAgent(String userMessage) {
            var agent = AIAgent.builder()
                    .promptExecutor(promptExecutor)
                    .llmModel(GoogleModels.Gemini2_5Flash)
                    .systemPrompt("You are a helpful assistant.")
                    .build();

            return agent.run(userMessage);
        }
    }
    ```

Or provide your own `PromptExecutor` bean to override the auto-configured one entirely.

### Configuration Properties (`koog.spring.ai.chat`)

| Property | Type | Default | Description |
|---|---|---|---|
| `enabled` | `Boolean` | `true` | Enable/disable the chat auto-configuration |
| `chat-model-bean-name` | `String?` | `null` | Bean name of the `ChatModel` to use (for multi-model contexts) |
| `moderation-model-bean-name` | `String?` | `null` | Bean name of the `ModerationModel` to use (for multi-model contexts) |
| `provider` | `String?` | `null` | LLM provider id (e.g. `openai`, `anthropic`, `google`). When set, overrides auto-detection from the `ChatModel` class name. Falls back to `spring-ai` if auto-detection fails. |
| `dispatcher.type` | `AUTO` / `IO` | `AUTO` | Dispatcher for blocking model calls |
| `dispatcher.parallelism` | `Int` | `0` (= unbounded) | Max concurrency for `IO` dispatcher (0 = no limit) |

### Dispatcher Types

- **`AUTO`** (default): Uses a Spring-managed `AsyncTaskExecutor` if available (e.g., when `spring.threads.virtual.enabled=true` in Spring Boot 3.2+), otherwise falls back to `Dispatchers.IO`. This lets you opt into virtual threads with a single standard Spring Boot property.
- **`IO`**: Always uses `Dispatchers.IO`. When `dispatcher.parallelism` is greater than 0, uses `Dispatchers.IO.limitedParallelism(parallelism)` to cap concurrency.

### Multi-model Contexts

When multiple `ChatModel` or `ModerationModel` beans are registered, specify which one to use:

```properties
koog.spring.ai.chat.chat-model-bean-name=openAiChatModel
koog.spring.ai.chat.moderation-model-bean-name=openAiModerationModel
```

Without a selector, the auto-configuration activates only when a single candidate exists.

### Extension Points

- **`ChatOptionsCustomizer`**: Register a Spring bean implementing this functional interface to apply provider-specific `ChatOptions` tuning:

=== "Kotlin"

    ```kotlin
    @Bean
    fun chatOptionsCustomizer() = ChatOptionsCustomizer { options, params, model ->
        // Apply custom options based on the model or request parameters
        options
    }
    ```

=== "Java"

    ```java
    @Bean
    public ChatOptionsCustomizer chatOptionsCustomizer() {
        return (options, params, model) -> {
            // Apply custom options based on the model or request parameters
            return options;
        };
    }
    ```

  The auto-configuration picks it up automatically via optional injection.

- **Custom `LLMClient`**: Register your own `LLMClient` bean to override the auto-configured adapter entirely.
- **Custom `PromptExecutor`**: Register your own `PromptExecutor` bean to override the auto-configured `MultiLLMPromptExecutor`.

## Next Steps

- Learn about the [basic agents](agents/basic-agents.md) to build minimal AI workflows
- Explore [graph-based agents](agents/graph-based-agents.md) for advanced use cases
- See the [tools overview](tools-overview.md) to extend your agents' capabilities
- Check out [examples](examples.md) for real-world implementations
- Read the [Spring Boot Integration](spring-boot.md) guide for the direct Koog starter approach