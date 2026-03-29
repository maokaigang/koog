<!-- koog-zh-meta: {"last_synced_at": "2026-03-29T03:04:35+00:00", "source_path": "spring-boot.md", "source_sha256": "cbc8f9b94f02945bb7a4f1d286d729ace40c146aee3971a806edc3b14a3ded1a", "source_tag": "0.7.3", "translation_status": "changed"} -->
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
- kotlinx-serialization 版本 1.10.0（即 kotlinx-serialization-core-jvm 与 kotlinx-serialization-json-jvm）

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

如果支持 API 按键（例如 OpenAI、Anthropic、Google），则 `ai.koog.PROVIDER.enabled` 默认设置为 `true`。

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
- 对于 Kotlin，需要额外添加 `kotlinx-coroutines-core` 和 `kotlinx-coroutines-reactor` 依赖。
- 通过属性启用 Anthropic（`ai.koog.anthropic.enabled=true`）。

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

Spring Framework 通过 bean 名称（`anthropicExecutor`）注入了 Anthropic 的执行器，但你也可以使用 `@Qualifier` 注解注入多个 `PromptExecutor` bean（参见下方的“多个 bean 错误”说明）。

## 高级用法 { #advanced-usage }
### LLM 提供者回退 { #llm-provider-fallback }

配置多个 LLM 提供者后，您可以通过 `MultiLLMPromptExecutor` 向多个 LLM 发送请求：

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

您也可以注册自己的`MultiLLMPromptExecutor` bean并向其传递`FallbackPromptExecutorSettings`。要覆盖您bean的自动配置，可以使用`@Primary`注解。

## 配置参考 { #configuration-reference }

### 可用属性 { #available-properties }

| 属性 | 描述 | Bean 条件 | 默认 |
|-------------------------------|---------------------|----------------------------------------|---------------------------------------------|
| `ai.koog.openai.api-key` | OpenAI API 密钥 | `openAIExecutor` bean 所需 | - |
| `ai.koog.openai.base-url` | OpenAI 基础 URL | 可选 | `https://api.openai.com` |
| `ai.koog.anthropic.api-key` | Anthropic API 密钥 | `anthropicExecutor` bean 所需 | - |
| `ai.koog.anthropic.base-url` | Anthropic 基础 URL | 可选 | `https://api.anthropic.com` |
| `ai.koog.google.api-key` | Google API 密钥 | `googleExecutor` bean 所需 | - |
| `ai.koog.google.base-url` | Google 基础 URL | 可选 | `https://generativelanguage.googleapis.com` |
| `ai.koog.openrouter.api-key` | OpenRouter API 密钥 | `openRouterExecutor` bean 所需 | - |
| `ai.koog.openrouter.base-url` | OpenRouter 基础 URL | 可选 | `https://openrouter.ai` |
| `ai.koog.deepseek.api-key` | DeepSeek API 密钥 | `deepSeekExecutor` bean 所需 | - |
| `ai.koog.deepseek.base-url` | DeepSeek 基础 URL | 可选 | `https://api.deepseek.com` |
| `ai.koog.mistral.api-key` | Mistral API 密钥 | `mistralAIExecutor` bean 所需 | - |
| `ai.koog.mistral.base-url` | Mistral 基础 URL | 可选 | `https://api.mistral.ai` |
| `ai.koog.ollama.base-url` | Ollama 基础 URL | 可选 | `http://127.0.0.1:11434` |

### Bean 名称 { #bean-names }

自动配置会创建以下 bean（当配置时）：

- `openAIExecutor` - OpenAI 执行器（需要 `ai.koog.openai.api-key`）
- `anthropicExecutor` - Anthropic 执行器（需要 `ai.koog.anthropic.api-key`）
- `googleExecutor` - Google 执行器（需要 `ai.koog.google.api-key`）
- `openRouterExecutor` - OpenRouter 执行器（需要 `ai.koog.openrouter.api-key`）
- `deepSeekExecutor` - DeepSeek 执行器（需要 `ai.koog.deepseek.api-key`）
- `mistralAIExecutor` - Mistral AI执行器（需要`ai.koog.mistral.api-key`）
- `ollamaExecutor` - Ollama 执行器（需要 `ai.koog.ollama.enabled=true`）
- `multiLLMPromptExecutor` - MultiLLMPromptExecutor

## 故障排查 { #troubleshooting }

### 常见问题 { #common-issues }

**错误：没有符合条件的 'PromptExecutor' 类型 Bean 可用**

**解决方案：** 确保您已在属性文件中配置至少一个提供程序。

**错误：找到多个符合条件的 'PromptExecutor' 类型 Bean**

**解决方案：** 使用 `@Qualifier` 来指定所需的 bean：

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

**错误：API 键为必填项但未提供**

**解决方案：** 请检查您的环境变量是否正确设置，并确保您的 Spring Boot 应用程序能够访问这些变量。

## 最佳实践 { #best-practices }

1. **环境变量**：始终使用环境变量来存储 API 密钥
2. **可为空注入**：使用可为空类型处理提供程序未配置的情况
3. **回退逻辑**：在使用多个服务提供商时实现回退机制
4. **错误处理**：在生产代码中，始终将执行器调用包裹在 try-catch 块中
5. **测试**：在测试中使用模拟对象，避免进行实际的API调用
6. **配置验证**：在使用执行器之前检查其是否可用

## 下一步 { #next-steps }

- 了解[基础智能体](agents/basic-agents.md)，构建极简AI工作流
- 探索[基于图的智能体](agents/graph-based-agents.md)以了解高级用例
- 请参阅[工具概览](tools-overview.md)以扩展您的智能体能力
- 查看 [示例](examples.md) 获取实际应用案例
- 阅读[术语表](glossary.md)以更好地理解该框架
