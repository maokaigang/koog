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

=== "Maven"```xml
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

确保你的项目具备：

- Spring Boot 3（要求 Java 17 或更高版本）
- Kotlin 库版本 2.3.10+（kotlin-stdlib）
- 适用于所选提供商的 Spring AI 模型启动器

### 可用提供商 { #available-providers }
Anthropic、Azure OpenAI、Bedrock Converse、Deepseek、Google GenAI、HuggingFace、MiniMax、Mistral AI、OCI GenAI、Ollama、OpenAI、Vertex AI、智谱 AI

### 配置 { #configure }

根据需要修改你的 Spring Boot 属性：

```properties
# put your API key for Gemini Developer API or pass it via an environment variable { #put-your-api-key-for-gemini-developer-api-or-pass-it-via-an-environment-variable }
spring.ai.google.genai.api-key=YOUR_GOOGLE_API_KEY
# default values { #default-values }
spring.ai.model.chat=google-genai
koog.spring.ai.chat.enabled=true
koog.spring.ai.chat.dispatcher.type=AUTO
```

如果你有一个单一的 `ChatModel` bean，一切都会自动运行——
适配器会将其包装成 Koog `LLMClient` 并创建一个可直接使用的 `PromptExecutor`。

### 使用示例 { #usage-example }

注入 `PromptExecutor` 并使用它来运行一个 Koog 智能体：

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

或者提供你自己的 `PromptExecutor` bean 来完全覆盖自动配置的那个。

### 配置属性（`koog.spring.ai.chat`）| 属性 | 类型 | 默认值 | 描述 | { #configuration-properties-koog-spring-ai-chat }
|---|---|---|---|
| `enabled` | `Boolean` | `true` | 启用/禁用聊天自动配置 |
| `chat-model-bean-name` | `String?` | `null` | 要使用的 `ChatModel` 的 Bean 名称（用于多模型上下文） |
| `moderation-model-bean-name` | `String?` | `null` | 要使用的 `ModerationModel` 的 Bean 名称（用于多模型上下文） |
| `provider` | `String?` | `null` | LLM 提供商 ID（例如 `openai`、`anthropic`、`google`）。设置后，将覆盖从 `ChatModel` 类名进行的自动检测。如果自动检测失败，则回退到 `spring-ai`。 |
| `dispatcher.type` | `AUTO` / `IO` | `AUTO` | 用于阻塞模型调用的调度器 |
| `dispatcher.parallelism` | `Int` | `0`（= 无限制） | `IO` 调度器的最大并发数（0 = 无限制） |

### 调度器类型 { #dispatcher-types }

- **`AUTO`**（默认）：如果可用，则使用 Spring 管理的 `AsyncTaskExecutor`（例如，当 Spring Boot 3.2+ 中存在 `spring.threads.virtual.enabled=true` 时），否则回退到 `Dispatchers.IO`。这允许您通过单个标准的 Spring Boot 属性选择使用虚拟线程。
- **`IO`**：始终使用 `Dispatchers.IO`。当 `dispatcher.parallelism` 大于 0 时，使用 `Dispatchers.IO.limitedParallelism(parallelism)` 来限制并发数。

### 多模型上下文 { #multi-model-contexts }

当注册了多个 `ChatModel` 或 `ModerationModel` Bean 时，请指定要使用哪一个：

```properties
koog.spring.ai.chat.chat-model-bean-name=openAiChatModel
koog.spring.ai.chat.moderation-model-bean-name=openAiModerationModel
```

如果没有选择器，自动配置仅在存在单个候选 Bean 时激活。

### 扩展点 { #extension-points }

- **`ChatOptionsCustomizer`**：注册一个实现此函数式接口的 Spring Bean，以应用特定于提供商的 `ChatOptions` 调优：

=== "Kotlin"

    ```kotlin
    @Bean
    fun chatOptionsCustomizer() = ChatOptionsCustomizer { options, params, model ->
        // 根据模型或请求参数应用自定义选项
        options
    }
    ```

=== "Java"

    ```java
    @Bean
    public ChatOptionsCustomizer chatOptionsCustomizer() {
        return (options, params, model) -> {
            // 根据模型或请求参数应用自定义选项
            return options;
        };
    }
    ```

  自动配置通过可选注入自动拾取它。

- **自定义 `LLMClient`**：注册您自己的 `LLMClient` Bean，以完全覆盖自动配置的适配器。
- **自定义 `PromptExecutor`**：注册您自己的 `PromptExecutor` Bean，以覆盖自动配置的 `MultiLLMPromptExecutor`。

## 后续步骤 { #next-steps }

- 了解[基础代理](agents/basic-agents.md)以构建最小化的 AI 工作流
- 探索[基于图的代理](agents/graph-based-agents.md)以应对高级用例
- 查看[工具概述](tools-overview.md)以扩展您代理的能力
- 查看[示例](examples.md)了解实际实现
- 阅读 [Spring Boot 集成](spring-boot.md)指南，了解直接的 Koog starter 方法