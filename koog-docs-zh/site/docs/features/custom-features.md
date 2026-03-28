<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:00:14+00:00", "source_path": "features/custom-features.md", "source_sha256": "1f9902cb1de19b5e692798a5dc041d22c7153eb638330f10e87e47826cf45698", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 自定义功能特性 { #custom-features }

功能特性提供了一种在运行时扩展和增强AI智能体能力的方式。它们采用模块化设计，支持灵活组合，您可以根据需求混合搭配使用。

除了Koog内置的[功能特性](index.md)外，您还可以通过扩展相应的功能接口来实现自定义特性。
本文档将基于当前Koog API介绍自定义功能特性的基础构建模块。

## 功能接口 { #feature-interfaces }

Koog提供以下可扩展接口用于实现自定义功能特性：

- [AIAgentGraphFeature](https://api.koog.ai/agents/agents-core/ai.koog.agents.core.feature/-a-i-agent-graph-feature/index.html)：适用于[已定义工作流的智能体](../agents/graph-based-agents.md)（基于图的智能体）的专属功能特性。
- [AIAgentFunctionalFeature](https://api.koog.ai/agents/agents-core/ai.koog.agents.core.feature/-a-i-agent-functional-feature/index.html)：适用于[函数式智能体](../agents/functional-agents.md)的功能特性。
- [AIAgentPlannerFeature](https://api.koog.ai/agents/agents-planner/ai.koog.agents.planner/-a-i-agent-planner-feature/index.html)：适用于[规划型智能体](../agents/planner-agents/index.md)的专属功能特性。

!!! note
    若要创建可同时安装于基于图、函数式和规划型智能体的自定义功能特性，需要实现所有相关接口。

## 实现自定义功能特性 { #implementing-custom-features }

实现自定义功能特性需按以下步骤创建功能结构：

1. 创建功能类
2. 定义配置类（继承自[FeatureConfig](https://api.koog.ai/agents/agents-core/ai.koog.agents.core.feature.config/-feature-config/index.html)类）
3. 创建伴生对象并实现部分或全部接口：[AIAgentGraphFeature](https://api.koog.ai/agents/agents-core/ai.koog.agents.core.feature/-a-i-agent-graph-feature/index.html)、[AIAgentFunctionalFeature](https://api.koog.ai/agents/agents-core/ai.koog.agents.core.feature/-a-i-agent-functional-feature/index.html)、[AIAgentPlannerFeature](https://api.koog.ai/agents/agents-planner/ai.koog.agents.planner/-a-i-agent-planner-feature/index.html)
4. 为功能特性设置唯一存储键值，该键值用于智能体流水线中的特性标识与检索。智能体运行时需要处理所有已注册特性，此键值用于从智能体内部注册特性映射中提取对应功能
5. 实现必需方法

以下代码示例展示了可同时安装于基于图、函数式和规划型智能体的自定义功能特性的通用实现模式：

<!--- INCLUDE
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.feature.AIAgentFunctionalFeature
import ai.koog.agents.core.feature.AIAgentGraphFeature
import ai.koog.agents.core.feature.AIAgentPlannerFeature
import ai.koog.agents.core.feature.config.FeatureConfig
import ai.koog.agents.core.feature.pipeline.AIAgentFunctionalPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentGraphPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentPlannerPipeline
-->
```kotlin
class MyFeature(val someProperty: String) {
    class Config : FeatureConfig() {
        var configProperty: String = "default"
    }

    companion object Feature : AIAgentGraphFeature<Config, MyFeature>, AIAgentFunctionalFeature<Config, MyFeature>, AIAgentPlannerFeature<Config, MyFeature> {
        // Unique storage key for retrieval in contexts
        override val key = createStorageKey<MyFeature>("my-feature")
        override fun createInitialConfig(agentConfig: AIAgentConfig): Config = Config()

        // Feature installation for graph-based agents
        override fun install(config: Config, pipeline: AIAgentGraphPipeline) : MyFeature {
            val feature = MyFeature(config.configProperty)

            pipeline.interceptAgentStarting(this) { context ->
                // Event handler implementation
            }
            return feature
        }

        // Feature installation for functional agents
        override fun install(config: Config, pipeline: AIAgentFunctionalPipeline) : MyFeature {
            val feature = MyFeature(config.configProperty)

            pipeline.interceptAgentStarting(this) { context ->
                // Event handler implementation
            }
            return feature
        }

        // Feature installation for planner agents
        override fun install(config: Config, pipeline: AIAgentPlannerPipeline) : MyFeature {
            val feature = MyFeature(config.configProperty)

            pipeline.interceptAgentStarting(this) { context ->
                // Event handler implementation
            }
            return feature
        }
    }
}
```
<!--- KNIT example-custom-features-01.kt -->

创建智能体时，通过`install`方法安装您的功能特性：

<!--- INCLUDE
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.agents.features.tracing.feature.Tracing

val MyFeature = Tracing
var configProperty = ""
-->
```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
    llmModel = OpenAIModels.Chat.GPT4o
) {
    install(MyFeature) {
        configProperty = "value"
    }
}
```
<!--- KNIT example-custom-features-02.kt -->

### 流水线拦截器 { #pipeline-interceptors }

拦截器代表智能体生命周期中可插入自定义逻辑的各类钩子点。Koog提供了一系列预定义拦截器，可用于观测各类运行时事件。

以下是通过功能特性的`install`方法可注册的拦截器列表。这些拦截器按类型分组，适用于基于图、函数式和规划型智能体流水线。为降低开发实际功能特性时的干扰并优化成本，建议仅注册所需拦截器。

智能体与环境生命周期：

- `interceptEnvironmentCreated`：智能体环境创建时的转换点
- `interceptAgentStarting`：智能体运行开始前触发
- `interceptAgentCompleted`：智能体运行成功完成时触发
- `interceptAgentExecutionFailed`：智能体运行失败时触发
- `interceptAgentClosing`：智能体运行关闭前触发（清理点）策略生命周期：

- `interceptStrategyStarting`：在策略执行开始前调用。
- `interceptStrategyCompleted`：在策略执行成功完成时调用。

LLM 调用生命周期：

- `interceptLLMCallStarting`：在 LLM 调用前调用。
- `interceptLLMCallCompleted`：在 LLM 调用后调用。

LLM 流式生命周期：

- `interceptLLMStreamingStarting`：在流式传输开始前调用。
- `interceptLLMStreamingFrameReceived`：在接收到每个流式帧时调用。
- `interceptLLMStreamingFailed`：在流式传输失败时调用。
- `interceptLLMStreamingCompleted`：在流式传输完成后调用。

工具调用生命周期：

- `interceptToolCallStarting`：在工具调用前调用。
- `interceptToolValidationFailed`：在工具输入验证失败时调用。
- `interceptToolCallFailed`：在工具执行失败时调用。
- `interceptToolCallCompleted`：在工具完成（并返回结果）后调用。

#### 图结构智能体专属拦截器 { #interceptors-specific-to-graph-based-agents }

以下拦截器仅在 `AIAgentGraphPipeline` 上可用，允许您观察节点和子图的生命周期事件。

节点执行生命周期：

- `interceptNodeExecutionStarting`：在节点开始执行前调用。
- `interceptNodeExecutionCompleted`：在节点执行完成后调用。
- `interceptNodeExecutionFailed`：在节点执行因错误失败时调用。

子图执行生命周期：

- `interceptSubgraphExecutionStarting`：在子图开始执行前立即调用。
- `interceptSubgraphExecutionCompleted`：在子图执行完成后调用。
- `interceptSubgraphExecutionFailed`：在子图执行失败时调用。

要使功能处理特定类型的事件，需要注册相应的管道拦截器。

### 过滤智能体事件 { #filtering-agent-events }

在智能体中安装功能时，您可能不希望处理该功能中注册的所有事件。要过滤掉某些事件，可以使用 [FeatureConfig.setEventFilter](https://api.koog.ai/agents/agents-core/ai.koog.agents.core.feature.config/-feature-config/set-event-filter.html) 函数应用过滤器。

以下示例展示了如何为功能仅允许 LLM 调用的开始和结束事件：

<!--- INCLUDE
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.feature.handler.AgentLifecycleEventType
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.agents.features.tracing.feature.Tracing

typealias MyFeature = Tracing

suspend fun main() {
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
        systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
        llmModel = OpenAIModels.Chat.GPT4o
    ) {
        install(Tracing) {
-->
<!--- SUFFIX
        }
    }
}
-->
```kotlin
install(MyFeature) {
    setEventFilter { context ->
        context.eventType is AgentLifecycleEventType.LLMCallStarting ||
            context.eventType is AgentLifecycleEventType.LLMCallCompleted
    }
}
```
<!--- KNIT example-custom-features-03.kt -->

#### 为功能禁用事件过滤 { #disabling-event-filtering-for-a-feature }

如果您的功能逻辑依赖于完整的智能体事件结构，事件过滤可能导致意外行为。为防止这种情况，需要在实现功能时通过覆盖 `setEventFilter` 来禁用事件过滤，以忽略安装功能时设置的任何自定义过滤器。

一个依赖于处理完整智能体事件流的功能示例是 [OpenTelemetry](open-telemetry/index.md)，因为它使用完整的智能体事件结构来构建继承的跨度结构。

以下是如何为功能禁用事件过滤的示例：

<!--- INCLUDE
import ai.koog.agents.core.feature.config.FeatureConfig
import ai.koog.agents.core.feature.handler.AgentLifecycleEventContext
-->
```kotlin
class MyFeatureConfig : FeatureConfig() {
    override fun setEventFilter(filter: (AgentLifecycleEventContext) -> Boolean) {
        // Deactivate event filtering for the feature
        throw UnsupportedOperationException("Event filtering is not allowed.")
    }
}
```
<!--- KNIT example-custom-features-04.kt -->

## 示例：基础日志记录功能 { #example-a-basic-logging-feature }

以下示例展示了如何实现一个记录智能体生命周期事件的基础日志记录功能。由于该功能应适用于图结构、功能和规划器智能体，为避免代码重复，所有智能体类型通用的拦截器在 `installCommon` 方法中实现。特定于各智能体类型的拦截器则在 `installGraphPipeline`、`installFunctionalPipeline` 和 `installPlannerPipeline` 方法中实现。

<!--- INCLUDE
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.feature.AIAgentFunctionalFeature
import ai.koog.agents.core.feature.AIAgentGraphFeature
import ai.koog.agents.core.feature.AIAgentPlannerFeature
import ai.koog.agents.core.feature.config.FeatureConfig
import ai.koog.agents.core.feature.pipeline.AIAgentFunctionalPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentGraphPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentPlannerPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentPipeline
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
-->
```kotlin
class LoggingFeature(val loggerName: String) {
    class Config : FeatureConfig() {
        var loggerName: String = "agent-logs"
    }

    companion object Feature :
        AIAgentGraphFeature<Config, LoggingFeature>,
        AIAgentFunctionalFeature<Config, LoggingFeature>,
        AIAgentPlannerFeature<Config, LoggingFeature> {

        override val key = createStorageKey<LoggingFeature>("logging-feature")

        override fun createInitialConfig(agentConfig: AIAgentConfig): Config = Config()

        override fun install(config: Config, pipeline: AIAgentGraphPipeline) : LoggingFeature {
            val logging = LoggingFeature(config.loggerName)
            val logger = KotlinLogging.logger(config.loggerName)

            installGraphPipeline(pipeline, logger)

            return logging
        }

        override fun install(config: Config, pipeline: AIAgentFunctionalPipeline) : LoggingFeature {
            val logging = LoggingFeature(config.loggerName)
            val logger = KotlinLogging.logger(config.loggerName)

            installFunctionalPipeline(pipeline, logger)

            return logging
        }

        override fun install(config: Config, pipeline: AIAgentPlannerPipeline) : LoggingFeature {
            val logging = LoggingFeature(config.loggerName)
            val logger = KotlinLogging.logger(config.loggerName)

            installPlannerPipeline(pipeline, logger)

            return logging
        }

        private fun installCommon(
            pipeline: AIAgentPipeline,
            logger: KLogger,
        ) {
            pipeline.interceptAgentStarting(this) { e ->
                logger.info { "Agent starting: runId=${e.runId}" }
            }
            pipeline.interceptStrategyStarting(this) { e ->
                logger.info { "Strategy ${e.strategy.name} starting" }
            }
            pipeline.interceptLLMCallStarting(this) { e ->
                logger.info { "Making LLM call with ${e.tools.size} tools" }
            }
            pipeline.interceptLLMCallCompleted(this) { e ->
                logger.info { "Received ${e.responses.size} response(s)" }
            }
        }

        private fun installGraphPipeline(
            pipeline: AIAgentGraphPipeline,
            logger: KLogger,
        ) {
            installCommon(pipeline, logger)

            pipeline.interceptNodeExecutionStarting(this) { e ->
                logger.info { "Node ${e.node.name} input: ${e.input}" }
            }
            pipeline.interceptNodeExecutionCompleted(this) { e ->
                logger.info { "Node ${e.node.name} output: ${e.output}" }
            }
        }

        private fun installFunctionalPipeline(
            pipeline: AIAgentFunctionalPipeline,
            logger: KLogger
        ) {
            installCommon(pipeline, logger)
        }

        private fun installPlannerPipeline(
            pipeline: AIAgentPlannerPipeline,
            logger: KLogger
        ) {
            installCommon(pipeline, logger)
        }
    }
}
```
<!--- KNIT example-custom-features-05.kt -->

以下是在代理中安装自定义日志功能的示例。该示例展示了基本功能安装过程，同时包含自定义配置属性 `loggerName`，可用于指定日志记录器的名称：

<!--- INCLUDE
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.feature.AIAgentFunctionalFeature
import ai.koog.agents.core.feature.AIAgentGraphFeature
import ai.koog.agents.core.feature.AIAgentPlannerFeature
import ai.koog.agents.core.feature.config.FeatureConfig
import ai.koog.agents.core.feature.pipeline.AIAgentFunctionalPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentGraphPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentPlannerPipeline
import ai.koog.agents.core.feature.pipeline.AIAgentPipeline
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

class LoggingFeature(val loggerName: String) {
    class Config : FeatureConfig() {
        var loggerName: String = "agent-logs"
    }

    companion object Feature :
        AIAgentGraphFeature<Config, LoggingFeature>,
        AIAgentFunctionalFeature<Config, LoggingFeature>,
        AIAgentPlannerFeature<Config, LoggingFeature> {

        override val key = createStorageKey<LoggingFeature>("logging-feature")

        override fun createInitialConfig(agentConfig: AIAgentConfig): Config = Config()

        override fun install(config: Config, pipeline: AIAgentGraphPipeline) : LoggingFeature {
            val logging = LoggingFeature(config.loggerName)
            val logger = KotlinLogging.logger(config.loggerName)

            installGraphPipeline(pipeline, logger)

            return logging
        }

        override fun install(config: Config, pipeline: AIAgentFunctionalPipeline) : LoggingFeature {
            val logging = LoggingFeature(config.loggerName)
            val logger = KotlinLogging.logger(config.loggerName)

            installFunctionalPipeline(pipeline, logger)

            return logging
        }

        override fun install(config: Config, pipeline: AIAgentPlannerPipeline) : LoggingFeature {
            val logging = LoggingFeature(config.loggerName)
            val logger = KotlinLogging.logger(config.loggerName)

            installPlannerPipeline(pipeline, logger)

            return logging
        }

        private fun installCommon(
            pipeline: AIAgentPipeline,
            logger: KLogger,
        ) {
            pipeline.interceptAgentStarting(this) { e ->
                logger.info { "Agent starting: runId=${e.runId}" }
            }
            pipeline.interceptStrategyStarting(this) { e ->
                logger.info { "Strategy ${e.strategy.name} starting" }
            }
            pipeline.interceptLLMCallStarting(this) { e ->
                logger.info { "Making LLM call with ${e.tools.size} tools" }
            }
            pipeline.interceptLLMCallCompleted(this) { e ->
                logger.info { "Received ${e.responses.size} response(s)" }
            }
        }

        private fun installGraphPipeline(
            pipeline: AIAgentGraphPipeline,
            logger: KLogger,
        ) {
            installCommon(pipeline, logger)

            pipeline.interceptNodeExecutionStarting(this) { e ->
                logger.info { "Node ${e.node.name} input: ${e.input}" }
            }
            pipeline.interceptNodeExecutionCompleted(this) { e ->
                logger.info { "Node ${e.node.name} output: ${e.output}" }
            }
        }

        private fun installFunctionalPipeline(
            pipeline: AIAgentFunctionalPipeline,
            logger: KLogger
        ) {
            installCommon(pipeline, logger)
        }

        private fun installPlannerPipeline(
            pipeline: AIAgentPlannerPipeline,
            logger: KLogger
        ) {
            installCommon(pipeline, logger)
        }
    }
}

suspend fun main() {
-->
<!--- SUFFIX
}
-->
```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
    llmModel = OpenAIModels.Chat.GPT4o
) {
    install(LoggingFeature) {
        loggerName = "my-custom-logger"
    }
}

agent.run("What is Kotlin?")
```
<!--- KNIT example-custom-features-06.kt -->
