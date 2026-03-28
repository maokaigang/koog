<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:09:47+00:00", "source_path": "structured-output.md", "source_sha256": "2b71415c81b8a071d8e9245e99ff1bde076c3824a90a1e1fd2ff3160c236f5c2", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 结构化输出 { #structured-output }

## 简介 { #introduction }

结构化输出 API 提供了一种确保大型语言模型（LLMs）的响应符合特定数据结构的方法。
这对于构建可靠的 AI 应用程序至关重要，因为您需要可预测的、格式良好的数据，而不是自由格式的文本。

本页解释了如何使用此 API 来定义数据结构、生成模式，以及向 LLMs 请求结构化响应。

## 关键组件与概念 { #key-components-and-concepts }

结构化输出 API 包含几个关键组件：

1.  **数据结构定义**：使用 kotlinx.serialization 和 LLM 特定注解标注的 Kotlin 数据类。
2.  **JSON 模式生成**：从 Kotlin 数据类生成 JSON 模式的工具。
3.  **结构化 LLM 请求**：请求 LLMs 响应符合定义结构的方法。
4.  **响应处理**：处理和验证结构化响应。

## 定义数据结构 { #defining-data-structures }

使用结构化输出 API 的第一步是使用 Kotlin 数据类定义您的数据结构。

### 基本结构 { #basic-structure }

<!--- INCLUDE
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
-->
```kotlin
@Serializable
@SerialName("WeatherForecast")
@LLMDescription("Weather forecast for a given location")
data class WeatherForecast(
    @property:LLMDescription("Temperature in Celsius")
    val temperature: Int,
    @property:LLMDescription("Weather conditions (e.g., sunny, cloudy, rainy)")
    val conditions: String,
    @property:LLMDescription("Chance of precipitation in percentage")
    val precipitation: Int
)
```
<!--- KNIT example-structured-data-01.kt -->

### 关键注解 { #key-annotations }

- `@Serializable`：kotlinx.serialization 处理该类所必需。
- `@SerialName`：指定序列化时使用的名称。
- `@LLMDescription`：为 LLM 提供类的描述。对于字段注解，请使用 `@property:LLMDescription`。

### 支持的功能 { #supported-features }

API 支持广泛的数据结构功能：

#### 嵌套类 { #nested-classes }

<!--- INCLUDE
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
-->
```kotlin
@Serializable
@SerialName("WeatherForecast")
data class WeatherForecast(
    // Other fields
    @property:LLMDescription("Coordinates of the location")
    val latLon: LatLon
) {
    @Serializable
    @SerialName("LatLon")
    data class LatLon(
        @property:LLMDescription("Latitude of the location")
        val lat: Double,
        @property:LLMDescription("Longitude of the location")
        val lon: Double
    )
}
```
<!--- KNIT example-structured-data-02.kt -->

#### 集合（列表和映射） { #collections-lists-and-maps }

<!--- INCLUDE
import ai.koog.agents.core.tools.annotations.LLMDescription
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherNews(val temperature: Double)

@Serializable
data class WeatherSource(val url: Url)
-->
```kotlin
@Serializable
@SerialName("WeatherForecast")
data class WeatherForecast(
    // Other fields
    @property:LLMDescription("List of news articles")
    val news: List<WeatherNews>,
    @property:LLMDescription("Map of weather sources")
    val sources: Map<String, WeatherSource>
)
```
<!--- KNIT example-structured-data-03.kt -->

#### 枚举 { #enums }

<!--- INCLUDE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
-->
```kotlin
@Serializable
@SerialName("Pollution")
enum class Pollution { Low, Medium, High }
```
<!--- KNIT example-structured-data-04.kt -->

#### 使用密封类实现多态性 { #polymorphism-with-sealed-classes }

<!--- INCLUDE
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
-->
```kotlin
@Serializable
@SerialName("WeatherAlert")
sealed class WeatherAlert {
    abstract val severity: Severity
    abstract val message: String

    @Serializable
    @SerialName("Severity")
    enum class Severity { Low, Moderate, Severe, Extreme }

    @Serializable
    @SerialName("StormAlert")
    data class StormAlert(
        override val severity: Severity,
        override val message: String,
        @property:LLMDescription("Wind speed in km/h")
        val windSpeed: Double
    ) : WeatherAlert()

    @Serializable
    @SerialName("FloodAlert")
    data class FloodAlert(
        override val severity: Severity,
        override val message: String,
        @property:LLMDescription("Expected rainfall in mm")
        val expectedRainfall: Double
    ) : WeatherAlert()
}
```
<!--- KNIT example-structured-data-05.kt -->

### 提供示例 { #providing-examples }

您可以提供示例来帮助 LLM 理解预期的格式：

<!--- INCLUDE
import ai.koog.agents.example.exampleStructuredData03.WeatherForecast
import ai.koog.agents.example.exampleStructuredData03.WeatherNews
import ai.koog.agents.example.exampleStructuredData03.WeatherSource
import io.ktor.http.*
-->
```kotlin
val exampleForecasts = listOf(
  WeatherForecast(
    news = listOf(WeatherNews(0.0), WeatherNews(5.0)),
    sources = mutableMapOf(
      "openweathermap" to WeatherSource(Url("https://api.openweathermap.org/data/2.5/weather")),
      "googleweather" to WeatherSource(Url("https://weather.google.com"))
    )
    // Other fields
  ),
  WeatherForecast(
    news = listOf(WeatherNews(25.0), WeatherNews(35.0)),
    sources = mutableMapOf(
      "openweathermap" to WeatherSource(Url("https://api.openweathermap.org/data/2.5/weather")),
      "googleweather" to WeatherSource(Url("https://weather.google.com"))
    )
  )
)

```
<!--- KNIT example-structured-data-06.kt -->

## 请求结构化响应 { #requesting-structured-responses }

在 Koog 中，您可以在三个主要层面使用结构化输出：

1.  **提示执行器层**：使用提示执行器进行直接的 LLM 调用
2.  **智能体 LLM 上下文层**：在智能体会话中用于对话上下文
3.  **节点层**：创建具有结构化输出能力的可重用智能体节点

### 第一层：提示执行器 { #layer-1-prompt-executor }

提示执行器层提供了进行结构化 LLM 调用的最直接方式。使用 `executeStructured` 方法进行单一的独立请求：

此方法执行提示并确保响应结构正确，通过：

- 根据[模型能力](./model-capabilities.md)自动选择最佳的结构化输出方法
- 在需要时将结构化输出指令注入到原始提示中
- 在可用时使用原生结构化输出支持
- 可选地，在解析失败时通过辅助 LLM 提供自动错误纠正（通过 `fixingParser` 参数）

以下是使用 `executeStructured` 方法的示例：<!--- INCLUDE
import ai.koog.agents.example.exampleStructuredData03.WeatherForecast
import ai.koog.agents.example.exampleStructuredData06.exampleForecasts
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.executor.model.StructureFixingParser
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
-->
<!--- SUFFIX
    }
}
-->
```kotlin
// Define a simple, single-provider prompt executor
val promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_KEY"))

// Make an LLM call that returns a structured response
val structuredResponse = promptExecutor.executeStructured<WeatherForecast>(
        // Define the prompt (both system and user messages)
        prompt = prompt("structured-data") {
            system(
                """
                You are a weather forecasting assistant.
                When asked for a weather forecast, provide a realistic but fictional forecast.
                """.trimIndent()
            )
            user(
              "What is the weather forecast for Amsterdam?"
            )
        },
        // Define the main model that will execute the request
        model = OpenAIModels.Chat.GPT4oMini,
        // Optional: provide examples to help the model understand the format
        examples = exampleForecasts,
        // Optional: provide a fixing parser for error correction
        fixingParser = StructureFixingParser(
            model = OpenAIModels.Chat.GPT4o,
            retries = 3
        )
    )
```
<!--- KNIT example-structured-data-07.kt -->

`executeStructured` 方法接受以下参数：

| 名称           | 数据类型              | 必填 | 默认值       | 描述                                                                                                     |
|----------------|------------------------|----------|---------------|-----------------------------------------------------------------------------------------------------------------|
| `prompt`       | Prompt                 | 是      |               | 要执行的提示词。更多信息请参阅 [提示词](prompts/index.md)。                                   |
| `model`        | LLModel                | 是      |               | 用于执行提示词的主模型。                                                                           |
| `examples`     | List<T>                | 否       | `emptyList()` | 可选的示例列表，用于帮助模型理解期望的格式。                                     |
| `fixingParser` | StructureFixingParser? | 否       | `null`        | 可选的解析器，通过使用辅助 LLM 智能修复解析错误来处理格式错误的响应。提供时，会自动重试失败的解析并进行错误纠正。 |

该方法返回一个 `Result<StructuredResponse<T>>`，其中包含成功解析的结构化数据或错误。

### 第二层：代理 LLM 上下文 { #layer-2-agent-llm-context }

代理 LLM 上下文层允许您在代理会话中请求结构化响应。这对于构建需要在流程中特定点获取结构化数据的对话代理非常有用。

在 `writeSession` 中使用 `requestLLMStructured` 方法进行基于代理的交互：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.example.exampleStructuredData03.WeatherForecast
import ai.koog.agents.example.exampleStructuredData06.exampleForecasts
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.StructureFixingParser

val strategy = strategy<Unit, Unit>("strategy-name") {
    val node by node<Unit, Unit> {
-->
<!--- SUFFIX
    }
}
-->
```kotlin
val structuredResponse = llm.writeSession {
    requestLLMStructured<WeatherForecast>(
        examples = exampleForecasts,
        fixingParser = StructureFixingParser(
            model = OpenAIModels.Chat.GPT4o,
            retries = 3
        )
    )
}
```
<!--- KNIT example-structured-data-08.kt -->

`fixingParser` 参数为格式错误的 JSON 响应提供自动错误纠正。当解析失败时，它会使用辅助 LLM 智能地修复响应，最多重试指定次数。

**StructureFixingParser 参数：**
- `model: LLModel` - 用于修复格式错误的 JSON 输出的 LLM
- `retries: Int` - 最大修复尝试次数（默认值：3）
- `prompt` - 修复过程的可选自定义提示函数（默认为内置的修复提示词）

修复过程会迭代地将解析错误传递给辅助模型，该模型尝试在保留原始数据并进行最小更改的情况下纠正 JSON。

#### 与代理策略集成 { #integrating-with-agent-strategies }

您可以将结构化数据处理集成到代理策略中：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.example.exampleStructuredData03.WeatherForecast
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.message.Message
import ai.koog.prompt.executor.model.StructureFixingParser
-->
```kotlin
val agentStrategy = strategy("weather-forecast") {
    val setup by nodeLLMRequest()

    val getStructuredForecast by node<Message.Response, String> { _ ->
        val structuredResponse = llm.writeSession {
            requestLLMStructured<WeatherForecast>(
                fixingParser = StructureFixingParser(
                    model = OpenAIModels.Chat.GPT4o,
                    retries = 3
                )
            )
        }

        """
        Response structure:
        $structuredResponse
        """.trimIndent()
    }

    edge(nodeStart forwardTo setup)
    edge(setup forwardTo getStructuredForecast)
    edge(getStructuredForecast forwardTo nodeFinish)
}
```
<!--- KNIT example-structured-data-09.kt -->

### 第三层：节点层 { #layer-3-node-layer }

节点层为代理工作流中的结构化输出提供了最高级别的抽象。使用 `nodeLLMRequestStructured` 创建可重用的处理结构化数据的代理节点。

这将创建一个代理节点，该节点：
- 接受 `String` 输入（用户消息）
- 将消息附加到 LLM 提示词
- 向 LLM 请求结构化输出
- 返回 `Result<StructuredResponse<MyStruct>>`

#### 节点层示例 { #node-layer-example }

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStructured
import ai.koog.agents.example.exampleStructuredData03.WeatherForecast
import ai.koog.agents.example.exampleStructuredData06.exampleForecasts
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.structure.StructuredResponse
import ai.koog.prompt.executor.model.StructureFixingParser
-->
```kotlin
val agentStrategy = strategy("weather-forecast") {
    val setup by node<Unit, String> { _ ->
        "Please provide a weather forecast for Amsterdam"
    }
    
    // Create a structured output node using delegate syntax
    val getWeatherForecast by nodeLLMRequestStructured<WeatherForecast>(
        name = "forecast-node",
        examples = exampleForecasts,
        fixingParser = StructureFixingParser(
            model = OpenAIModels.Chat.GPT4o,
            retries = 3
        )
    )
    
    val processResult by node<Result<StructuredResponse<WeatherForecast>>, String> { result ->
        when {
            result.isSuccess -> {
                val forecast = result.getOrNull()?.data
                "Weather forecast: $forecast"
            }
            result.isFailure -> {
                "Failed to get structured forecast: ${result.exceptionOrNull()?.message}"
            }
            else -> "Unknown result state"
        }
    }

    edge(nodeStart forwardTo setup)
    edge(setup forwardTo getWeatherForecast)
    edge(getWeatherForecast forwardTo processResult)
    edge(processResult forwardTo nodeFinish)
}
```
<!--- KNIT example-structured-data-10.kt -->

#### 完整代码示例以下是使用结构化输出 API 的完整示例： { #full-code-sample }

<!--- INCLUDE
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.structure.json.generator.BasicJsonSchemaGenerator
import ai.koog.prompt.structure.json.JsonStructure
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
-->
```kotlin
// Note: Import statements are omitted for brevity
@Serializable
@SerialName("SimpleWeatherForecast")
@LLMDescription("Simple weather forecast for a location")
data class SimpleWeatherForecast(
    @property:LLMDescription("Location name")
    val location: String,
    @property:LLMDescription("Temperature in Celsius")
    val temperature: Int,
    @property:LLMDescription("Weather conditions (e.g., sunny, cloudy, rainy)")
    val conditions: String
)

val token = System.getenv("OPENAI_KEY") ?: error("Environment variable OPENAI_KEY is not set")

fun main(): Unit = runBlocking {
    // Create sample forecasts
    val exampleForecasts = listOf(
        SimpleWeatherForecast(
            location = "New York",
            temperature = 25,
            conditions = "Sunny"
        ),
        SimpleWeatherForecast(
            location = "London",
            temperature = 18,
            conditions = "Cloudy"
        )
    )

    // Generate JSON Schema
    val forecastStructure = JsonStructure.create<SimpleWeatherForecast>(
        schemaGenerator = BasicJsonSchemaGenerator.Default,
        examples = exampleForecasts
    )

    // Define the agent strategy
    val agentStrategy = strategy("weather-forecast") {
        val setup by nodeLLMRequest()
  
        val getStructuredForecast by node<Message.Response, String> { _ ->
            val structuredResponse = llm.writeSession {
                requestLLMStructured<SimpleWeatherForecast>()
            }
  
            """
            Response structure:
            $structuredResponse
            """.trimIndent()
        }
  
        edge(nodeStart forwardTo setup)
        edge(setup forwardTo getStructuredForecast)
        edge(getStructuredForecast forwardTo nodeFinish)
    }


    // Configure and run the agent
    val agentConfig = AIAgentConfig(
        prompt = prompt("weather-forecast-prompt") {
            system(
                """
                You are a weather forecasting assistant.
                When asked for a weather forecast, provide a realistic but fictional forecast.
                """.trimIndent()
            )
        },
        model = OpenAIModels.Chat.GPT4o,
        maxAgentIterations = 5
    )

    val runner = AIAgent(
        promptExecutor = simpleOpenAIExecutor(token),
        toolRegistry = ToolRegistry.EMPTY,
        strategy = agentStrategy,
        agentConfig = agentConfig
    )

    runner.run("Get weather forecast for Paris")
}
```
<!--- KNIT example-structured-data-11.kt -->

## 高级用法 { #advanced-usage }

以上示例展示了简化的 API，它能根据模型能力自动选择最佳的结构化输出方案。  
如需对结构化输出过程进行更精细的控制，可以使用高级 API，通过手动创建模式及提供特定于供应商的配置来实现。

### 手动创建模式与配置 { #manual-schema-creation-and-configuration }

除了依赖自动模式生成，您还可以使用 `JsonStructure.create` 显式创建模式，并通过 `StructuredOutput` 类手动配置结构化输出行为。

关键区别在于，您不再传递如 `examples` 和 `fixingParser` 这样的简单参数，而是创建一个 `StructuredRequestConfig` 对象，从而实现对以下方面的精细控制：

- **模式生成**：选择特定的生成器（标准、基础或供应商特定）
- **输出模式**：原生结构化输出支持与手动提示
- **供应商映射**：为不同的 LLM 供应商配置不同设置
- **回退策略**：当供应商特定配置不可用时的默认行为

<!--- INCLUDE
import ai.koog.agents.example.exampleStructuredData03.WeatherForecast
import ai.koog.agents.example.exampleStructuredData06.exampleForecasts
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.structure.StructuredRequest
import ai.koog.prompt.executor.model.StructureFixingParser
import ai.koog.prompt.structure.json.JsonStructure
import ai.koog.prompt.structure.json.generator.StandardJsonSchemaGenerator
import ai.koog.prompt.executor.clients.openai.base.structure.OpenAIBasicJsonSchemaGenerator
import ai.koog.prompt.llm.LLMProvider
import kotlinx.coroutines.runBlocking
import ai.koog.prompt.structure.StructuredRequestConfig

fun main() {
    runBlocking {
-->
<!--- SUFFIX
    }
}
-->
```kotlin
// Create different schema structures with different generators
val genericStructure = JsonStructure.create<WeatherForecast>(
    schemaGenerator = StandardJsonSchemaGenerator,
    examples = exampleForecasts
)

val openAiStructure = JsonStructure.create<WeatherForecast>(
    schemaGenerator = OpenAIBasicJsonSchemaGenerator,
    examples = exampleForecasts
)

val promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_KEY"))

// The advanced API uses StructuredRequestConfig instead of simple parameters
val structuredResponse = promptExecutor.executeStructured(
    prompt = prompt("structured-data") {
        system("You are a weather forecasting assistant.")
        user("What is the weather forecast for Amsterdam?")
    },
    model = OpenAIModels.Chat.GPT4oMini,
    config = StructuredRequestConfig(
        byProvider = mapOf(
            LLMProvider.OpenAI to StructuredRequest.Native(openAiStructure),
        ),
        default = StructuredRequest.Manual(genericStructure)
    ),
    fixingParser = StructureFixingParser(
        model = AnthropicModels.Haiku_4_5,
        retries = 2
    )
)
```
<!--- KNIT example-structured-data-12.kt -->

### 模式生成器 { #schema-generators }

根据您的需求，可以使用不同的模式生成器：

- **StandardJsonSchemaGenerator**：完整的 JSON 模式，支持多态性、定义和递归引用
- **BasicJsonSchemaGenerator**：简化模式，不支持多态性，兼容更多模型  
- **供应商特定生成器**：针对特定 LLM 供应商（如 OpenAI、Google 等）优化的模式

### 跨所有层级的用法 { #usage-across-all-layers }

高级配置在 API 的所有三个层级中均保持一致。方法名称保持不变，仅参数从简单参数变为更高级的 `StructuredRequestConfig`：

- **提示执行器**：`executeStructured(prompt, model, config: StructuredRequestConfig<T>)`
- **代理 LLM 上下文**：`requestLLMStructured(config: StructuredRequestConfig<T>)`
- **节点层**：`nodeLLMRequestStructured(config: StructuredRequestConfig<T>)`

对于大多数用例，推荐使用简化的 API（仅使用 `examples` 和 `fixingParser` 参数），而高级 API 则在需要额外控制时提供支持。

## 最佳实践 { #best-practices }

1. **使用清晰的描述**：通过 `@LLMDescription` 注解提供清晰详细的描述，帮助 LLM 理解预期的数据结构。

2. **提供示例**：包含有效数据结构的示例，以指导 LLM。

3. **优雅处理错误**：实现适当的错误处理机制，以应对 LLM 可能无法生成有效结构的情况。

4. **使用合适的模式类型**：根据您的需求及所用 LLM 的能力，选择合适的模式格式和类型。

5. **在不同模型上测试**：不同的 LLM 在遵循结构化格式方面的能力可能有所不同，因此尽可能在多个模型上进行测试。

6. **从简单开始**：从简单的结构入手，根据需要逐步增加复杂性。7. **谨慎使用多态**：虽然 API 支持通过密封类实现多态，但需注意 LLM 可能更难正确处理。