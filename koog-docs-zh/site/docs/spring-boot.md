<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:17:08+00:00", "source_path": "spring-boot.md", "source_sha256": "cbc8f9b94f02945bb7a4f1d286d729ace40c146aee3971a806edc3b14a3ded1a", "source_tag": "0.7.3", "translation_status": "changed"} -->
# Spring Boot 集成 { #spring-boot-integration }

Koog 通过其自动配置启动器提供无缝的 Spring Boot 集成，使得在 Spring Boot 应用中集成 AI 代理变得非常简单，只需最少的设置。

## 概述 { #overview }

`koog-spring-boot-starter` 会根据您的应用属性自动配置 LLM 客户端，并提供可直接用于依赖注入的 Bean。它支持所有主要的 LLM 提供商，包括：

- OpenAI
- Anthropic
- Google
- OpenRouter
- DeepSeek
- Mistral
- Ollama

## 快速开始 { #getting-started }

### 1. 添加依赖 { #1-add-dependency }

将 Koog Spring Boot 启动器添加到您的 Gradle 构建配置中：

```kotlin
dependencies {
    implementation("ai.koog:koog-spring-boot-starter:$koogVersion")
}
```

或者对于 Maven
```xml
<dependency>
    <groupId>ai.koog</groupId>
    <artifactId>koog-spring-boot-starter</artifactId>
    <version>$koogVersion</version>
</dependency>
```

请确保您的 Kotlin 或 Java 项目满足以下条件：
- Spring Boot 3（需要 Java 17 或更高版本）
- Kotlin 版本 2.3.10+
- kotlinx-serialization 版本 1.10.0（即 kotlinx-serialization-core-jvm 和 kotlinx-serialization-json-jvm）

### 2. 配置提供商 { #2-configure-providers }

在 `application.properties` 中配置您首选的 LLM 提供商：

```properties
# OpenAI Configuration { #openai-configuration }
ai.koog.openai.enabled=true
ai.koog.openai.api-key=${OPENAI_API_KEY}
ai.koog.openai.base-url=https://api.openai.com
# Anthropic Configuration { #anthropic-configuration }
ai.koog.anthropic.enabled=true
ai.koog.anthropic.api-key=${ANTHROPIC_API_KEY}
ai.koog.anthropic.base-url=https://api.anthropic.com
# Google Configuration { #google-configuration }
ai.koog.google.enabled=true
ai.koog.google.api-key=${GOOGLE_API_KEY}
ai.koog.google.base-url=https://generativelanguage.googleapis.com
# OpenRouter Configuration { #openrouter-configuration }
ai.koog.openrouter.enabled=true
ai.koog.openrouter.api-key=${OPENROUTER_API_KEY}
ai.koog.openrouter.base-url=https://openrouter.ai
# DeepSeek Configuration { #deepseek-configuration }
ai.koog.deepseek.enabled=true
ai.koog.deepseek.api-key=${DEEPSEEK_API_KEY}
ai.koog.deepseek.base-url=https://api.deepseek.com
# Mistral Configuration { #mistral-configuration }
ai.koog.mistral.enabled=true
ai.koog.mistral.api-key=${MISTRALAI_API_KEY}
ai.koog.mistral.base-url=https://api.mistral.ai
# Ollama Configuration (local - no API key required) { #ollama-configuration-local-no-api-key-required }
ai.koog.ollama.enabled=true
ai.koog.ollama.base-url=http://127.0.0.1:11434
```

或者使用 YAML 格式（`application.yml`）：

```yaml
ai:
    koog:
        openai:
            enabled: true
            api-key: ${OPENAI_API_KEY}
            base-url: https://api.openai.com
        anthropic:
            enabled: true
            api-key: ${ANTHROPIC_API_KEY}
            base-url: https://api.anthropic.com
        google:
            enabled: true
            api-key: ${GOOGLE_API_KEY}
            base-url: https://generativelanguage.googleapis.com
        openrouter:
            enabled: true
            api-key: ${OPENROUTER_API_KEY}
            base-url: https://openrouter.ai
        deepseek:
            enabled: true
            api-key: ${DEEPSEEK_API_KEY}
            base-url: https://api.deepseek.com
        mistral:
            enabled: true
            api-key: ${MISTRALAI_API_KEY}
            base-url: https://api.mistral.ai
        ollama:
            enabled: true # Set it to `true` explicitly to activate !!!
            base-url: http://127.0.0.1:11434
```

`ai.koog.PROVIDER.api-key` 和 `ai.koog.PROVIDER.enabled` 属性都用于激活提供商。

如果提供商支持 API 密钥（如 OpenAI、Anthropic、Google），则 `ai.koog.PROVIDER.enabled` 默认设置为 `true`。

如果提供商不支持 API 密钥，例如 Ollama，则 `ai.koog.PROVIDER.enabled` 默认设置为 `false`，并且需要在应用配置中显式启用该提供商。

提供商的基础 URL 在 Spring Boot 启动器中已设置为默认值，但您可以在应用中覆盖它们。

!!! tip "环境变量"

    建议使用环境变量来管理 API 密钥，以确保其安全性并避免将其纳入版本控制。
    Spring 配置使用 LLM 提供商已知的环境变量。
    例如，设置环境变量 `OPENAI_API_KEY` 就足以激活 OpenAI 的 Spring 配置。

| LLM 提供商 | 环境变量 |
|--------------|-----------------------|
| Open AI      | `OPENAI_API_KEY`      |
| Anthropic    | `ANTHROPIC_API_KEY`   |
| Google       | `GOOGLE_API_KEY`      |
| OpenRouter   | `OPENROUTER_API_KEY`  |
| DeepSeek     | `DEEPSEEK_API_KEY`    |
| Mistral      | `MISTRALAI_API_KEY`   |

### 3. 在项目中使用 { #3-use-in-your-project }

以下是在 Spring MVC RestController 中使用自动配置的执行器的示例。它需要满足以下条件：
- 添加 spring-boot-starter-web 依赖
- 对于 Kotlin，需要添加 kotlinx-coroutines-core 和 kotlinx-coroutines-reactor 依赖（Java 版本调用阻塞的 `execute` 方法）
- 通过属性启用 Anthropic（ai.koog.anthropic.enabled=true）

=== "Kotlin"

    ```kotlin
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
    import ai.koog.prompt.executor.model.PromptExecutor
    import org.springframework.http.ResponseEntity
    import org.springframework.web.bind.annotation.PostMapping
    import org.springframework.web.bind.annotation.RequestBody
    import org.springframework.web.bind.annotation.RequestMapping
    import org.springframework.web.bind.annotation.RestController

    @RestController
    @RequestMapping("/api/chat")
    class ChatController(private val anthropicExecutor: PromptExecutor) {

        @PostMapping
        suspend fun chat(@RequestBody request: ChatRequest): ResponseEntity<ChatResponse> {
            return try {
                val prompt = prompt("chat") {
                    system("You are a helpful assistant")
                    user(request.message)
                }

                val result = anthropicExecutor.execute(prompt, AnthropicModels.Haiku_4_5)
                ResponseEntity.ok(ChatResponse(result.first().content))
            } catch (e: Exception) {
                ResponseEntity.internalServerError()
                    .body(ChatResponse("Error processing request"))
            }
        }
    }

    data class ChatRequest(val message: String)
    data class ChatResponse(val response: String)
    ```

=== "Java"

    ```java
    import ai.koog.prompt.dsl.Prompt;
    import ai.koog.prompt.executor.clients.anthropic.AnthropicModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import ai.koog.prompt.message.Message;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestBody;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;

    import java.util.List;

    @RestController
    @RequestMapping("/api/chat")
    public class ChatController {
        private final PromptExecutor anthropicExecutor;

        public ChatController(PromptExecutor anthropicExecutor) {
            this.anthropicExecutor = anthropicExecutor;
        }

        @PostMapping
        public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
            try {
                Prompt prompt = Prompt.builder("chat")
                        .system("You are a helpful assistant")
                        .user(request.message())
                        .build();

                List<Message.Response> result = anthropicExecutor.execute(prompt, AnthropicModels.Haiku_4_5);
                return ResponseEntity.ok(new ChatResponse(result.get(0).getContent()));
            } catch (Exception e) {
                return ResponseEntity.internalServerError()
                        .body(new ChatResponse("Error processing request"));
            }
        }
    }

    record ChatRequest(String message) {
    }

    record ChatResponse(String response) {
    }
    ```

Spring Framework injected the executor for Anthropic by bean name (`anthropicExecutor`),
but you can also inject multiple `PromptExecutor` beans using `@Qualifier` annotation (see "Multiple beans error" below).

## Advanced usage
### LLM Provider Fallback

After configuring multiple LLM providers you can send request to multiple LLMs via `MultiLLMPromptExecutor`:

=== "Kotlin"

    ```kotlin
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.clients.anthropic.AnthropicModels.Haiku_4_5
    import ai.koog.prompt.executor.clients.openai.OpenAIModels.Chat.GPT4oMini
    import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels.Claude3Haiku
    import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
    import org.slf4j.Logger
    import org.slf4j.LoggerFactory
    import org.springframework.stereotype.Service

    @Service
    class RobustAIService(private val multiLLMPromptExecutor: MultiLLMPromptExecutor) {

        private val llms = listOf(GPT4oMini, Haiku_4_5, Claude3Haiku)

        suspend fun generateWithFallback(input: String): String {
            val prompt = prompt("robust") {
                system("You are a helpful AI assistant")
                user(input)
            }

            for (llm in llms) {
                try {
                    val result = multiLLMPromptExecutor.execute(prompt, llm)
                    return result.first().content
                } catch (e: Exception) {
                    logger.warn("{} executor failed, trying next: {}", llm.id, e.message)
                }
            }

            throw IllegalStateException("All AI providers failed")
        }

        companion object {
            private val logger = LoggerFactory.getLogger(RobustAIService::class.java)
        }
    }
    ```

=== "Java"

    ```java
    import ai.koog.prompt.dsl.Prompt;
    import ai.koog.prompt.executor.clients.anthropic.AnthropicModels;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels;
    import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor;
    import ai.koog.prompt.llm.LLModel;
    import ai.koog.prompt.message.Message;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.stereotype.Service;

    import java.util.List;

    @Service
    public class RobustAIService {
        private static final Logger logger = LoggerFactory.getLogger(RobustAIService.class);

        private final List<LLModel> llms = List.of(OpenAIModels.Chat.GPT4oMini, AnthropicModels.Haiku_4_5, OpenRouterModels.Claude3Haiku);

        private final MultiLLMPromptExecutor multiLLMPromptExecutor;

        public RobustAIService(MultiLLMPromptExecutor multiLLMPromptExecutor) {
            this.multiLLMPromptExecutor = multiLLMPromptExecutor;
        }

        public String generateWithFallback(String input) {
            Prompt prompt = Prompt.builder("robust")
                .system("You are a helpful AI assistant")
                .user(input)
                .build();

            for (LLModel llm : llms) {
                try {
                    List<Message.Response> result = multiLLMPromptExecutor.execute(prompt, llm);
                    return result.get(0).getContent();
                } catch (Exception e) {
                    logger.warn("{} executor failed, trying next: {}", llm.getId(), e.getMessage());
                }
            }

            throw new IllegalStateException("All AI providers failed");
        }
    }
    ```

You can also register your own `MultiLLMPromptExecutor` bean and pass a `FallbackPromptExecutorSettings` to it.
To override the auto-configuration for your beans you can use `@Primary` annotation.

## Configuration Reference

### Available Properties

| Property                      | Description         | Bean Condition                         | Default                                     |
|-------------------------------|---------------------|----------------------------------------|---------------------------------------------|
| `ai.koog.openai.api-key`      | OpenAI API key      | Required for `openAIExecutor` bean     | -                                           |
| `ai.koog.openai.base-url`     | OpenAI base URL     | Optional                               | `https://api.openai.com`                    |
| `ai.koog.anthropic.api-key`   | Anthropic API key   | Required for `anthropicExecutor` bean  | -                                           |
| `ai.koog.anthropic.base-url`  | Anthropic base URL  | Optional                               | `https://api.anthropic.com`                 |
| `ai.koog.google.api-key`      | Google API key      | Required for `googleExecutor` bean     | -                                           |
| `ai.koog.google.base-url`     | Google base URL     | Optional                               | `https://generativelanguage.googleapis.com` |
| `ai.koog.openrouter.api-key`  | OpenRouter API key  | Required for `openRouterExecutor` bean | -                                           |
| `ai.koog.openrouter.base-url` | OpenRouter base URL | Optional                               | `https://openrouter.ai`                     |
| `ai.koog.deepseek.api-key`    | DeepSeek API key    | Required for `deepSeekExecutor` bean   | -                                           |
| `ai.koog.deepseek.base-url`   | DeepSeek base URL   | Optional                               | `https://api.deepseek.com`                  |
| `ai.koog.mistral.api-key`     | Mistral API key     | Required for `mistralAIExecutor` bean  | -                                           |
| `ai.koog.mistral.base-url`    | Mistral base URL    | Optional                               | `https://api.mistral.ai`                    |
| `ai.koog.ollama.base-url`     | Ollama base URL     | Optional                               | `http://127.0.0.1:11434`                    |

### Bean Names

The auto-configuration creates the following beans (when configured):

- `openAIExecutor` - OpenAI executor (requires `ai.koog.openai.api-key`)
- `anthropicExecutor` - Anthropic executor (requires `ai.koog.anthropic.api-key`)
- `googleExecutor` - Google executor (requires `ai.koog.google.api-key`)
- `openRouterExecutor` - OpenRouter executor (requires `ai.koog.openrouter.api-key`)
- `deepSeekExecutor` - DeepSeek executor (requires `ai.koog.deepseek.api-key`)
- `mistralAIExecutor` - Mistral AI executor (requires `ai.koog.mistral.api-key`)
- `ollamaExecutor` - Ollama executor (requires `ai.koog.ollama.enabled=true`)
- `multiLLMPromptExecutor` - MultiLLMPromptExecutor

## Troubleshooting

### Common Issues

**Error: No qualifying bean of type 'PromptExecutor' available**

**Solution:** Ensure you have configured at least one provider in your properties file.

**Error: Multiple qualifying beans of type 'PromptExecutor' available**

**Solution:** Use `@Qualifier` to specify which bean you want:

=== "Kotlin"

    ```kotlin
    @Service
    class MyService(
        @Qualifier("openAIExecutor") private val openAIExecutor: PromptExecutor,
        @Qualifier("anthropicExecutor") private val anthropicExecutor: PromptExecutor
    ) {
        // ...
    }
    ```

=== "Java"

    ```java
    @Service
    public class MyService {
        private final PromptExecutor openAIExecutor;
        private final PromptExecutor anthropicExecutor;

        public MyService(@Qualifier("openAIExecutor") PromptExecutor openAIExecutor,
                         @Qualifier("anthropicExecutor") PromptExecutor anthropicExecutor) {
            this.openAIExecutor = openAIExecutor;
            this.anthropicExecutor = anthropicExecutor;
        }
        // ...
    }
    ```

**Error: API key is required but not provided**

**Solution:** Check that your environment variables are properly set and accessible to your Spring Boot application.

## Best Practices

1. **Environment Variables**: Always use environment variables for API keys
2. **Nullable Injection**: Use nullable types to handle cases where providers aren't configured
3. **Fallback Logic**: Implement fallback mechanisms when using multiple providers
4. **Error Handling**: Always wrap executor calls in try-catch blocks for production code
5. **Testing**: Use mocks in tests to avoid making actual API calls
6. **Configuration Validation**: Check if executors are available before using them

## Next Steps

- Learn about the [basic agents](agents/basic-agents.md) to build minimal AI workflows
- Explore [graph-based agents](agents/graph-based-agents.md) for advanced use cases
- See the [tools overview](tools-overview.md) to extend your agents' capabilities
- Check out [examples](examples.md) for real-world implementations
- Read the [glossary](glossary.md) to understand the framework better