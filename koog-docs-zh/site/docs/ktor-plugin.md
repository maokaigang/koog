<!-- koog-zh-meta: {"last_synced_at": "2026-03-29T03:04:21+00:00", "source_path": "ktor-plugin.md", "source_sha256": "d19fcd56839b70cdecb4fa2874144b3c4e575faa3f970489709df2a938fb26b2", "source_tag": "0.7.3", "translation_status": "changed"} -->
# Ktor 集成：Koog 插件 { #ktor-integration-koog-plugin }

Koog 自然地融入您的 Ktor 服务器，让您能够使用符合语言习惯的 Kotlin API 来编写服务器端 AI 应用程序。

只需安装一次 Koog 插件，在 application.conf/YAML 或代码中配置您的 LLM 提供商，然后就可以直接从您的路由中调用智能体。无需再跨模块连接 LLM 客户端——您的路由只需请求一个智能体即可开始工作。

## 概述 { #overview }

`koog-ktor` 模块为服务器端智能体开发提供了符合语言习惯的 Kotlin/Ktor 集成：

- 即插即用的 Ktor 插件：在您的应用程序中 `install(Koog)`
- 支持多家主流 LLM 提供商。
- 包括 `OpenAI`、`Anthropic`、`Google`、`OpenRouter`、`DeepSeek` 和 `Ollama`。
- 通过 YAML/CONF 和/或代码进行集中配置
- 智能体设置（包括提示、工具、功能）；为路由提供简单的扩展函数
- 直接使用 LLM（执行、executeStreaming、中等）
- 集成仅限 JVM 的模型上下文协议（MCP）工具

## 添加依赖 { #add-dependency }

```kotlin
dependencies {
    implementation("ai.koog:koog-ktor:$koogVersion")
}
```

## 快速开始 { #quick-start }

1) 配置提供商（在 `application.yaml` 或 `application.conf` 中）

使用 `koog.<provider>` 下的嵌套键。插件会自动识别它们。

```yaml
# application.yaml (Ktor config) { #application-yaml-ktor-config }
koog:
  openai:
    apikey: ${OPENAI_API_KEY}
    baseUrl: https://api.openai.com
  anthropic:
    apikey: ${ANTHROPIC_API_KEY}
    baseUrl: https://api.anthropic.com
  google:
    apikey: ${GOOGLE_API_KEY}
    baseUrl: https://generativelanguage.googleapis.com
  openrouter:
    apikey: ${OPENROUTER_API_KEY}
    baseUrl: https://openrouter.ai
  deepseek:
    apikey: ${DEEPSEEK_API_KEY}
    baseUrl: https://api.deepseek.com
  # Ollama is enabled when any koog.ollama.* key exists
  ollama:
    enable: true
    baseUrl: http://localhost:11434
```

可选：配置当请求的提供商未配置时，直接 LLM 调用所使用的后备方案。

```yaml
koog:
  llm:
    fallback:
      provider: openai
      # see Model identifiers section below
      model: openai.chat.gpt4_1
```

2) 安装插件并定义路由

```kotlin
fun Application.module() {
    install(Koog) {
        // You can also configure providers programmatically (see below)
    }

    routing {
        route("/ai") {
            post("/chat") {
                val userInput = call.receiveText()
                // Create and run a default single‑run agent using a specific model
                val output = aiAgent(
                    strategy = reActStrategy(),
                    model = OpenAIModels.Chat.GPT4_1,
                    input = userInput
                )
                call.respond(HttpStatusCode.OK, output)
            }
        }
    }
}
```

注意

- aiAgent 需要一个具体的模型（LLModel）——请按路由或按使用场景选择。
- 对于较低级别的 LLM 访问，请直接使用 llm()（PromptExecutor）。

## 直接从路由使用 LLM { #direct-llm-usage-from-routes }

```kotlin
post("/llm-chat") {
    val userInput = call.receiveText()

    val messages = llm().execute(
        prompt("chat") {
            system("You are a helpful assistant that clarifies questions")
            user(userInput)
        },
        GoogleModels.Gemini2_5Pro
    )

    // Join all assistant messages into a single string
    val text = messages.joinToString(separator = "") { it.content }
    call.respond(HttpStatusCode.OK, text)
}
```

流式处理

```kotlin
get("/stream") {
    val flow = llm().executeStreaming(
        prompt("streaming") { user("Stream this response, please") },
        OpenRouterModels.GPT4o
    )

    // Example: buffer and send as one chunk
    val sb = StringBuilder()
    flow.collect { chunk -> sb.append(chunk) }
    call.respondText(sb.toString())
}
```

内容审核

```kotlin
post("/moderated-chat") {
    val userInput = call.receiveText()

    val moderation = llm().moderate(
        prompt("moderation") { user(userInput) },
        OpenAIModels.Moderation.Omni
    )

    if (moderation.isHarmful) {
        call.respond(HttpStatusCode.BadRequest, "Harmful content detected")
        return@post
    }

    val output = aiAgent(
        strategy = reActStrategy(),
        model = OpenAIModels.Chat.GPT4_1,
        input = userInput
    )
    call.respond(HttpStatusCode.OK, output)
}
```

## 编程式配置（在代码中） { #programmatic-configuration-in-code }

所有提供商和智能体行为都可以通过 install(Koog) {} 进行配置。

```kotlin
install(Koog) {
    llm {
        openAI(apiKey = System.getenv("OPENAI_API_KEY") ?: "") {
            baseUrl = "https://api.openai.com"
            timeouts { // Default values shown below
                requestTimeout = 15.minutes
                connectTimeout = 60.seconds
                socketTimeout = 15.minutes
            }
        }
        anthropic(apiKey = System.getenv("ANTHROPIC_API_KEY") ?: "")
        google(apiKey = System.getenv("GOOGLE_API_KEY") ?: "")
        openRouter(apiKey = System.getenv("OPENROUTER_API_KEY") ?: "")
        deepSeek(apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "")
        ollama { baseUrl = "http://localhost:11434" }

        // Optional fallback used by PromptExecutor when a provider isn’t configured
        fallback {
            provider = LLMProvider.OpenAI
            model = OpenAIModels.Chat.GPT4_1
        }
    }

    agentConfig {
        // Provide a reusable base prompt for your agents
        prompt(name = "agent") {
            system("You are a helpful server‑side agent")
        }

        // Limit runaway tools/loops
        maxAgentIterations = 10

        // Register tools available to agents by default
        registerTools {
            // tool(::yourTool) // see Tools Overview for details
        }

        // Install agent features (tracing, etc.)
        // install(OpenTelemetry) { /* ... */ }
    }
}
```

## 配置中的模型标识符（后备方案） { #model-identifiers-in-config-fallback }

在 YAML/CONF 中配置 llm.fallback 时，请使用以下标识符格式：

- OpenAI：openai.chat.gpt4_1、openai.reasoning.o3、openai.costoptimized.gpt4_1mini、openai.audio.gpt4oaudio、openai.moderation.omni
- Anthropic：anthropic.sonnet_4_5、anthropic.opus_4、anthropic.haiku_4_5
- Google：google.gemini2_5pro、google.gemini2_0flash001
- OpenRouter：openrouter.gpt4o、openrouter.gpt4、openrouter.claude3sonnet
- DeepSeek：deepseek.deepseek-chat、deepseek.deepseek-reasoner
- Ollama：ollama.meta.llama3.2、ollama.alibaba.qwq:32b、ollama.groq.llama3-grok-tool-use:8b

注意

- 对于 OpenAI，您必须包含类别（聊天、推理、成本优化、音频、嵌入、审核）。
- 对于 Ollama，同时支持 `ollama.model` 和 `ollama.<maker>.<model>` 两种格式。

## MCP 工具（仅限 JVM） { #mcp-tools-jvm-only }

在 JVM 上，您可以将来自 MCP 服务器的工具添加到您的智能体工具注册表中：

```kotlin
install(Koog) {
    agentConfig {
        mcp {
            // Register via SSE
            sse("https://your-mcp-server.com/sse")

            // Or register via spawned process (stdio transport)
            // process(Runtime.getRuntime().exec("your-mcp-binary ..."))

            // Or from an existing MCP client instance
            // client(existingMcpClient)
        }
    }
}
```
## 为什么选择 Koog + Ktor？- Kotlin‑优先，在您的服务器中进行类型安全的智能体开发 { #why-koog-ktor }
- 集中式配置，搭配清晰、可测试的路由代码
- 为每条路由选用合适的模型，或为直接的 LLM 调用自动回退
- 生产就绪功能：工具调用、内容审核、流式响应与追踪
