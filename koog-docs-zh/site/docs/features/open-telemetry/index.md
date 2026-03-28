<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:04:59+00:00", "source_path": "features/open-telemetry/index.md", "source_sha256": "c30379e50abb68d1244f4c65ab8241be9d2790a282d0fe3261a907720382e6e1", "source_tag": "0.7.3", "translation_status": "changed"} -->
# OpenTelemetry 支持 { #opentelemetry-support }

本文档详细介绍了 Koog 代理框架对 OpenTelemetry 的支持，用于追踪和监控您的 AI 代理。

## 概述 { #overview }

OpenTelemetry 是一个可观测性框架，提供用于生成、收集和导出应用程序遥测数据（追踪）的工具。Koog 的 OpenTelemetry 功能允许您对 AI 代理进行插装以收集遥测数据，这可以帮助您：

- 监控代理的性能和行为
- 调试复杂代理工作流中的问题
- 可视化代理的执行流程
- 追踪 LLM 调用和工具使用情况
- 分析代理行为模式

## 关键 OpenTelemetry 概念 { #key-opentelemetry-concepts }

- **跨度**：跨度表示分布式追踪中的单个工作单元或操作。它们指示应用程序中特定活动的开始和结束，例如代理执行、函数调用、LLM 调用或工具调用。
- **属性**：属性提供有关遥测相关项目（例如跨度）的元数据。属性以键值对的形式表示。
- **事件**：事件是跨度生命周期中的特定时间点（与跨度相关的事件），表示发生的可能值得注意的事情。
- **导出器**：导出器是负责将收集到的遥测数据发送到各种后端或目的地的组件。
- **收集器**：收集器接收、处理和导出遥测数据。它们充当应用程序和可观测性后端之间的中介。
- **采样器**：采样器根据采样策略决定是否应记录追踪。它们用于管理遥测数据的量。
- **资源**：资源表示产生遥测数据的实体。它们由资源属性标识，资源属性是提供有关资源信息的键值对。

Koog 中的 OpenTelemetry 功能会自动为各种代理事件创建跨度，包括：

- 代理执行开始和结束
- 节点执行
- LLM 调用
- 工具调用

## 安装 { #installation }

要在 Koog 中使用 OpenTelemetry，请将 OpenTelemetry 功能添加到您的代理中：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    -->
    ```kotlin
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant.",
        installFeatures = {
            install(OpenTelemetry) {
                // 配置选项放在这里
            }
        }
    )
    ```
    <!--- KNIT example-opentelemetry-support-01.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    public class exampleOpentelemetrySupportJava01 {
        public static void main(String[] args) {
            var promptExecutor = PromptExecutor.builder()
                .openAI("openai-api-key")
                .build();
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    var agent = AIAgent.builder()
        .promptExecutor(promptExecutor)
        .llmModel(OpenAIModels.Chat.GPT4o)
        .systemPrompt("You are a helpful assistant.")
        .install(OpenTelemetry.Feature, config -> {
            // 配置选项放在这里
        })
        .build();
    ```
    <!--- KNIT exampleOpentelemetrySupportJava01.java -->

## 配置 { #configuration }

### 基本配置 { #basic-configuration }

以下是配置代理中的 OpenTelemetry 功能时可设置的完整可用属性列表：| 名称             | 数据类型          | 默认值                       | 描述                                                                  |
|------------------|--------------------|------------------------------|------------------------------------------------------------------------------|
| `serviceName`    | `String`           | `ai.koog`                    | 被插桩服务的名称。                                  |
| `serviceVersion` | `String`           | 当前 Koog 库版本 | 被插桩服务的版本。                               |
| `isVerbose`      | `Boolean`          | `false`                      | 是否启用详细日志以调试 OpenTelemetry 配置。 |
| `sdk`            | `OpenTelemetrySdk` |                              | 用于遥测收集的 OpenTelemetry SDK 实例。              |
| `tracer`         | `Tracer`           |                              | 用于创建跨度的 OpenTelemetry 追踪器实例。                   |

!!! note
`sdk` 和 `tracer` 属性是您可以访问的公共属性，但只能通过下面列出的公共方法进行设置。

`OpenTelemetryConfig` 类还包含代表不同配置项相关操作的方法。以下是一个安装 OpenTelemetry 功能并配置基本配置项的示例：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import io.opentelemetry.exporter.logging.LoggingSpanExporter
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant."
    ) {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    install(OpenTelemetry) {
        // 设置您的服务配置
        setServiceInfo("my-agent-service", "1.0.0")
        
        // 添加日志导出器
        addSpanExporter(LoggingSpanExporter.create())
    }
    ```
    <!--- KNIT example-opentelemetry-support-02.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import io.opentelemetry.exporter.logging.LoggingSpanExporter;
    public class exampleOpentelemetrySupportJava02 {
        public static void main(String[] args) {
            var promptExecutor = PromptExecutor.builder()
                .openAI("openai-api-key")
                .build();
            
            var agent = AIAgent.builder()
                .promptExecutor(promptExecutor)
                .llmModel(OpenAIModels.Chat.GPT4o)
                .systemPrompt("You are a helpful assistant.")
                .
    -->
    <!--- SUFFIX
                .build();
        }
    }
    -->
    ```java
    install(OpenTelemetry.Feature, config -> {
        // 设置您的服务配置
        config.setServiceInfo("my-agent-service", "1.0.0");

        // 添加日志导出器
        config.addSpanExporter(LoggingSpanExporter.create());
    })
    ```
    <!--- KNIT exampleOpentelemetrySupportJava02.java -->

有关可用方法的参考，请参阅以下部分。

#### setServiceInfo { #setserviceinfo }

设置服务信息，包括名称和版本。接受以下参数：

| 名称               | 数据类型 | 必需 | 默认值 | 描述                                                 |
|--------------------|-----------|----------|---------------|-------------------------------------------------------------|
| `serviceName`      | String    | 是      |               | 被插桩服务的名称。                 |
| `serviceVersion`   | String    | 是      |               | 被插桩服务的版本。              |

#### addSpanExporter { #addspanexporter }

添加跨度导出器以将遥测数据发送到外部系统。接受以下参数：| 名称       | 数据类型      | 必填 | 默认值 | 描述                                                                   |
|------------|----------------|----------|---------------|-------------------------------------------------------------------------------|
| `exporter` | `SpanExporter` | 是      |               | 要添加到自定义跨度导出器列表中的 `SpanExporter` 实例。 |

#### addSpanProcessor { #addspanprocessor }

添加一个跨度处理器工厂，用于在跨度导出前进行处理。接受以下参数：

| 名称        | 数据类型                         | 必填 | 默认值 | 描述                                                                                                  |
|-------------|-----------------------------------|----------|---------------|--------------------------------------------------------------------------------------------------------------|
| `processor` | `(SpanExporter) -> SpanProcessor` | 是      |               | 为给定导出器创建跨度处理器的函数。允许您针对每个导出器自定义处理过程。   |

#### addResourceAttributes { #addresourceattributes }

添加资源属性，以提供有关服务的额外上下文信息。接受以下参数：

| 名称         | 数据类型                 | 必填 | 默认值 | 描述                                                            |
|--------------|---------------------------|----------|---------------|------------------------------------------------------------------------|
| `attributes` | `Map<AttributeKey<T>, T>` | 是      |               | 提供有关服务额外详情的键值对。 |

#### setSampler { #setsampler }

设置采样策略以控制收集哪些跨度。接受以下参数：

| 名称      | 数据类型 | 必填 | 默认值 | 描述                                                      |
|-----------|-----------|----------|---------------|------------------------------------------------------------------|
| `sampler` | `Sampler` | 是      |               | 要为 OpenTelemetry 配置设置的采样器实例。 |

#### setVerbose { #setverbose }

启用或禁用详细日志记录。接受以下参数：

| 名称      | 数据类型 | 必填 | 默认值 | 描述                                                     |
|-----------|-----------|----------|---------------|-----------------------------------------------------------------|
| `verbose` | `Boolean` | 是      | `false`       | 如果为 true，应用程序将收集更详细的遥测数据。 |

!!! note

    出于安全原因，OpenTelemetry 跨度的部分内容默认会被屏蔽。例如，LLM 消息会被屏蔽为 `HIDDEN:non-empty`，而不是实际的消息内容。要获取内容，请将 `verbose` 参数的值设置为 `true`。

#### setSdk { #setsdk }

注入一个预配置的 OpenTelemetrySdk 实例。

- 当您调用 setSdk(sdk) 时，提供的 SDK 将按原样使用，而通过 addSpanExporter、addSpanProcessor、addResourceAttributes 或 setSampler 应用的任何自定义配置将被忽略。
- 追踪器的仪器作用域名称/版本将与您的服务信息对齐。| 名称  | 数据类型          | 是否必需 | 描述                                  |
|-------|--------------------|----------|---------------------------------------|
| `sdk` | `OpenTelemetrySdk` | 是       | 在代理中使用的 SDK 实例。 |

### 高级配置 { #advanced-configuration }

如需进行更高级的配置，您还可以自定义以下配置选项：

- 采样器：配置采样策略以调整收集数据的频率和数量。
- 资源属性：添加有关生成遥测数据的进程的更多信息。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import io.opentelemetry.api.common.AttributeKey
    import io.opentelemetry.exporter.logging.LoggingSpanExporter
    import io.opentelemetry.sdk.trace.samplers.Sampler
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant."
    ) {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    install(OpenTelemetry) {
        // 设置您的服务配置
        setServiceInfo("my-agent-service", "1.0.0")
        
        // 添加日志导出器
        addSpanExporter(LoggingSpanExporter.create())
        
        // 设置采样器
        setSampler(Sampler.traceIdRatioBased(0.5)) 
    
        // 添加资源属性
        addResourceAttributes(mapOf(
            AttributeKey.stringKey("custom.attribute") to "custom-value")
        )
    }
    ```
    <!--- KNIT example-opentelemetry-support-03.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import io.opentelemetry.api.common.AttributeKey;
    import io.opentelemetry.exporter.logging.LoggingSpanExporter;
    import io.opentelemetry.sdk.trace.samplers.Sampler;
    import java.util.Map;
    public class exampleOpentelemetrySupportJava03 {
        public static void main(String[] args) {
            var promptExecutor = PromptExecutor.builder()
                .openAI("openai-api-key")
                .build();
            var agent = AIAgent.builder()
                .promptExecutor(promptExecutor)
                .llmModel(OpenAIModels.Chat.GPT4o)
                .systemPrompt("You are a helpful assistant.")
                .
    -->
    <!--- SUFFIX
                .build();
        }
    }
    -->
    ```java
    install(OpenTelemetry.Feature, config -> {
        // 设置您的服务配置
        config.setServiceInfo("my-agent-service", "1.0.0");

        // 添加日志导出器
        config.addSpanExporter(LoggingSpanExporter.create());

        // 设置采样器
        config.setSampler(Sampler.traceIdRatioBased(0.5));

        // 添加资源属性
        config.addResourceAttributes(Map.of(
            AttributeKey.stringKey("custom.attribute"), "custom-value"
        ));
    })
    ```
    <!--- KNIT exampleOpentelemetrySupportJava03.java -->

#### 采样器 { #sampler }

要定义采样器，请使用 `Sampler` 类（`io.opentelemetry.sdk.trace.samplers.Sampler`）的相应方法，该方法来自代表您要使用的采样策略的 `opentelemetry-java` SDK。

默认采样策略如下：

- `Sampler.alwaysOn()`：默认采样策略，其中每个跨度（跟踪）都会被采样。

有关可用采样器和采样策略的更多信息，请参阅 OpenTelemetry [采样器](https://opentelemetry.io/docs/languages/java/sdk/#sampler) 文档。

#### 资源属性 { #resource-attributes }

资源属性代表有关生成遥测数据的进程的附加信息。Koog 包含一组默认设置的资源属性：

- `service.name`
- `service.version`
- `service.instance.time`
- `os.type`
- `os.version`
- `os.arch`

`service.name` 属性的默认值为 `ai.koog`，而默认的 `service.version` 值是当前使用的 Koog 库版本。

除了默认资源属性外，您还可以添加自定义属性。要在 Koog 的 OpenTelemetry 配置中添加自定义属性，请在 OpenTelemetry 配置中使用 `addResourceAttributes()` 方法，该方法以键和值作为参数。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import io.opentelemetry.api.common.AttributeKey
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant.",
        installFeatures = {
            install(OpenTelemetry) {
    -->
    <!--- SUFFIX
            }
        }
    )
    -->
    ```kotlin
    addResourceAttributes(mapOf(
        AttributeKey.stringKey("custom.attribute") to "custom-value")
    )
    ```
    <!--- KNIT example-opentelemetry-support-04.kt -->

=== "Java"<!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import io.opentelemetry.api.common.AttributeKey;
    import java.util.Map;
    public class exampleOpentelemetrySupportJava04 {
        public static void main(String[] args) {
            var promptExecutor = PromptExecutor.builder()
                .openAI("openai-api-key")
                .build();
            var agent = AIAgent.builder()
                .promptExecutor(promptExecutor)
                .llmModel(OpenAIModels.Chat.GPT4o)
                .systemPrompt("You are a helpful assistant.")
                .install(OpenTelemetry.Feature, config -> {
    -->
<!--- SUFFIX
                })
                .build();
        }
    }
    -->
```java
config.addResourceAttributes(Map.of(
    AttributeKey.stringKey("custom.attribute"), "custom-value"
));
```
<!--- KNIT exampleOpentelemetrySupportJava04.java -->

## Span 类型与属性 { #span-types-and-attributes }

OpenTelemetry 功能会自动创建不同类型的 Span 来追踪您代理中的各种操作：

- **CreateAgentSpan**：在运行代理时创建，在代理关闭或进程终止时结束。
- **InvokeAgentSpan**：代理的调用。
- **StrategySpan**：代理策略的执行（顶层执行流程）。
- **NodeExecuteSpan**：代理策略中节点的执行。这是一个自定义的、Koog 特定的 Span。
- **SubgraphExecuteSpan**：代理策略中子图的执行。这是一个自定义的、Koog 特定的 Span。
- **InferenceSpan**：一次 LLM 调用。
- **ExecuteToolSpan**：一次工具调用。
- **McpClientSpan**：一次 MCP（模型上下文协议）客户端操作。此 Span 遵循 OpenTelemetry 针对 MCP 的语义约定。

Span 以嵌套的层次结构组织。以下是一个 Span 结构的示例：

```text
CreateAgentSpan
    InvokeAgentSpan
        StrategySpan
            NodeExecuteSpan
                InferenceSpan
            NodeExecuteSpan
                ExecuteToolSpan
            SubgraphExecuteSpan
                NodeExecuteSpan
                    InferenceSpan
```
<!--- KNIT example-opentelemetry-support-01.txt -->

### Span 属性 { #span-attributes }

Span 属性提供与 Span 相关的元数据。每个 Span 都有其自身的属性集，而某些 Span 也可以重复属性。

Koog 支持一系列预定义的属性，这些属性遵循 OpenTelemetry 的[生成式 AI 事件语义约定](https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-spans/)。例如，约定定义了一个名为 `gen_ai.conversation.id` 的属性，这通常是 Span 的必需属性。在 Koog 中，此属性的值是代理运行的唯一标识符，会在您调用 `agent.run()` 方法时自动设置。

此外，Koog 还包含自定义的、Koog 特定的属性。您可以通过 `koog.` 前缀识别大多数此类属性。以下是可用的自定义属性：

- `koog.strategy.name`：代理策略的名称。策略是一个与 Koog 相关的实体，用于描述代理的目的。用于 `StrategySpan` Span。
- `koog.node.id`：正在执行的节点的标识符（名称）。用于 `NodeExecuteSpan` Span。
- `koog.node.input`：在节点执行开始时传递给节点的输入。在节点启动时出现在 `NodeExecuteSpan` 上。
- `koog.node.output`：节点成功完成时产生的输出。在节点成功完成时出现在 `NodeExecuteSpan` 上。
- `koog.subgraph.id`：正在执行的子图的标识符（名称）。用于 `SubgraphExecuteSpan` Span。
- `koog.subgraph.input`：在子图执行开始时传递给子图的输入。在子图启动时出现在 `SubgraphExecuteSpan` 上。
- `koog.subgraph.output`：子图成功完成时产生的输出。在子图成功完成时出现在 `SubgraphExecuteSpan` 上。

### 事件 { #events }

Span 还可以附加一个*事件*。事件描述了某个相关事件发生的特定时间点。例如，当一次 LLM 调用开始或结束时。事件也具有属性，并且额外包含事件*正文字段*。以下事件类型遵循 OpenTelemetry 的[生成式 AI 事件语义约定](https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-events/)：

- **SystemMessageEvent**：传递给模型的系统指令。
- **UserMessageEvent**：传递给模型的用户消息。
- **AssistantMessageEvent**：传递给模型的助手消息。
- **ToolMessageEvent**：传递给模型的工具或函数调用响应。
- **ChoiceEvent**：模型的响应消息。
- **ModerationResponseEvent**：模型审核结果或信号。

!!! note   
`optentelemetry-java` SDK 在添加事件时不支持事件体字段参数。因此，在 Koog 的 OpenTelemetry 支持中，事件体字段是一个单独的属性，其键为 `body`，值类型为字符串。该字符串包含事件体字段的内容或有效载荷，通常是一个类似 JSON 的对象。有关事件体字段的示例，请参阅 [OpenTelemetry 文档](https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-events/#examples)。关于 `opentelemetry-java` 中对事件体字段的支持状态，请参阅相关的 [GitHub 议题](https://github.com/open-telemetry/semantic-conventions/issues/1870)。

## 导出器 { #exporters }

导出器将收集到的遥测数据发送到 OpenTelemetry Collector 或其他类型的目标或后端实现。要添加导出器，请在安装 OpenTelemetry 功能时使用 `addSpanExporter()` 方法。该方法接受以下参数：

| 名称       | 数据类型    | 必需 | 默认值 | 描述                                                                 |
|------------|--------------|----------|---------|-----------------------------------------------------------------------------|
| `exporter` | SpanExporter | 是      |         | 要添加到自定义跨度导出器列表中的 SpanExporter 实例。 |

以下部分提供了一些来自 `opentelemetry-java` SDK 的最常用导出器的信息。

!!! note
如果未配置任何自定义导出器，Koog 将默认使用控制台 LoggingSpanExporter。这有助于本地开发和调试。

### 日志导出器 { #logging-exporter }

一种将跟踪信息输出到控制台的日志导出器。`LoggingSpanExporter` (`io.opentelemetry.exporter.logging.LoggingSpanExporter`) 是 `opentelemetry-java` SDK 的一部分。

此类导出适用于开发和调试目的。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import io.opentelemetry.exporter.logging.LoggingSpanExporter
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant."
    ) {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    install(OpenTelemetry) {
        // 添加日志导出器
        addSpanExporter(LoggingSpanExporter.create())
        // 根据需要添加更多导出器
    }
    ```
    <!--- KNIT example-opentelemetry-support-05.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import io.opentelemetry.exporter.logging.LoggingSpanExporter;
    public class exampleOpentelemetrySupportJava05 {
        public static void main(String[] args) {
            var promptExecutor = PromptExecutor.builder()
                .openAI("openai-api-key")
                .build();
            var agent = AIAgent.builder()
                .promptExecutor(promptExecutor)
                .llmModel(OpenAIModels.Chat.GPT4o)
                .systemPrompt("You are a helpful assistant.")
                .
    -->
    <!--- SUFFIX
                .build();
        }
    }
    -->
    ```java
    install(OpenTelemetry.Feature, config -> {
        // 添加日志导出器
        config.addSpanExporter(LoggingSpanExporter.create());
        // 根据需要添加更多导出器
    })
    ```
    <!--- KNIT exampleOpentelemetrySupportJava05.java -->

### OpenTelemetry HTTP 导出器 { #opentelemetry-http-exporter }

OpenTelemetry HTTP 导出器 (`OtlpHttpSpanExporter`) 是 `opentelemetry-java` SDK (`io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter`) 的一部分，并通过 HTTP 将跨度数据发送到后端。

=== "Kotlin"<!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
    import java.util.concurrent.TimeUnit
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    const val AUTH_STRING = ""
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant."
    ) {
    -->
<!--- SUFFIX
    }
    -->
```kotlin
install(OpenTelemetry) {
    // 添加 OpenTelemetry HTTP 导出器
    addSpanExporter(
        OtlpHttpSpanExporter.builder()
            // 设置等待收集器处理导出批次的最大时间
            .setTimeout(30, TimeUnit.SECONDS)
            // 设置要连接的 OpenTelemetry 端点
            .setEndpoint("http://localhost:3000/api/public/otel/v1/traces")
            // 添加授权头
            .addHeader("Authorization", "Basic $AUTH_STRING")
            .build()
    )
}
```
<!--- KNIT example-opentelemetry-support-06.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
    import java.util.concurrent.TimeUnit;
    public class exampleOpentelemetrySupportJava06 {
        public static void main(String[] args) {
            var promptExecutor = PromptExecutor.builder()
                .openAI("openai-api-key")
                .build();
            String AUTH_STRING = "";
            var agent = AIAgent.builder()
                .promptExecutor(promptExecutor)
                .llmModel(OpenAIModels.Chat.GPT4o)
                .systemPrompt("You are a helpful assistant.")
                .
    -->
    <!--- SUFFIX
                .build();
        }
    }
    -->
    ```java
    install(OpenTelemetry.Feature, config -> {
        // 添加 OpenTelemetry HTTP 导出器
        config.addSpanExporter(
            OtlpHttpSpanExporter.builder()
                // 设置等待收集器处理导出批次的最大时间
                .setTimeout(30, TimeUnit.SECONDS)
                // 设置要连接的 OpenTelemetry 端点
                .setEndpoint("http://localhost:3000/api/public/otel/v1/traces")
                // 添加授权头
                .addHeader("Authorization", "Basic " + AUTH_STRING)
                .build()
        );
    })
    ```
    <!--- KNIT exampleOpentelemetrySupportJava06.java -->

### OpenTelemetry gRPC 导出器 { #opentelemetry-grpc-exporter }

OpenTelemetry gRPC 导出器 (`OtlpGrpcSpanExporter`) 是 `opentelemetry-java` SDK
(`io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter`) 的一部分。它通过 gRPC 将遥测数据导出到后端，并允许您定义接收数据的后端、收集器或端点的主机和端口。默认端口为
`4317`。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant."
    ) {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    install(OpenTelemetry) {
        // 添加 OpenTelemetry gRPC 导出器
        addSpanExporter(
            OtlpGrpcSpanExporter.builder()
                // 设置主机和端口
                .setEndpoint("http://localhost:4317")
                .build()
        )
    }
    ```
    <!--- KNIT example-opentelemetry-support-07.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
    public class exampleOpentelemetrySupportJava07 {
        public static void main(String[] args) {
            var promptExecutor = PromptExecutor.builder()
                .openAI("openai-api-key")
                .build();
            var agent = AIAgent.builder()
                .promptExecutor(promptExecutor)
                .llmModel(OpenAIModels.Chat.GPT4o)
                .systemPrompt("You are a helpful assistant.")
                .
    -->
    <!--- SUFFIX
                .build();
        }
    }
    -->
    ```java
    install(OpenTelemetry.Feature, config -> {
        // 添加 OpenTelemetry gRPC 导出器
        config.addSpanExporter(
            OtlpGrpcSpanExporter.builder()
                // 设置主机和端口
                .setEndpoint("http://localhost:4317")
                .build()
        );
    })
    ```
    <!--- KNIT exampleOpentelemetrySupportJava07.java -->

## 与 Langfuse 集成 { #integration-with-langfuse }

Langfuse 为 LLM/agent 工作负载提供追踪可视化和分析功能。

您可以通过辅助函数配置 Koog，将 OpenTelemetry 追踪直接导出到 Langfuse：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor 
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant."
    ) {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    install(OpenTelemetry) {
        addLangfuseExporter(
            langfuseUrl = "https://cloud.langfuse.com",
            langfusePublicKey = "...",
            langfuseSecretKey = "..."
        )
    }
    ```
    <!--- KNIT example-opentelemetry-support-08.kt -->

=== "Java"<!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    public class exampleOpentelemetrySupportJava08 {
        public static void main(String[] args) {
            var promptExecutor = PromptExecutor.builder()
                .openAI("openai-api-key")
                .build();
            var agent = AIAgent.builder()
                .promptExecutor(promptExecutor)
                .llmModel(OpenAIModels.Chat.GPT4o)
                .systemPrompt("You are a helpful assistant.")
                .
    -->
<!--- SUFFIX
                .build();
        }
    }
    -->
```java
install(OpenTelemetry.Feature, config -> {
    config.addLangfuseExporter(
        "https://cloud.langfuse.com",
        "...",
        "...",
        null,
        null
    );
})
```
<!--- KNIT exampleOpentelemetrySupportJava08.java -->

请阅读关于与 Langfuse 集成的[完整文档](opentelemetry-langfuse-exporter.md)。

## 与 W&B Weave 集成 { #integration-with-w-b-weave }

W&B Weave 为 LLM/agent 工作负载提供追踪可视化和分析功能。可以通过预定义的导出器配置与 W&B Weave 的集成：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant."
    ) {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    install(OpenTelemetry) {
        addWeaveExporter(
            weaveOtelBaseUrl = "https://trace.wandb.ai",
            weaveEntity = "my-team",
            weaveProjectName = "my-project",
            weaveApiKey = "..."
        )
    }
    ```
    <!--- KNIT example-opentelemetry-support-09.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    public class exampleOpentelemetrySupportJava09 {
        public static void main(String[] args) {
            var promptExecutor = PromptExecutor.builder()
                .openAI("openai-api-key")
                .build();
            var agent = AIAgent.builder()
                .promptExecutor(promptExecutor)
                .llmModel(OpenAIModels.Chat.GPT4o)
                .systemPrompt("You are a helpful assistant.")
                .
    -->
    <!--- SUFFIX
                .build();
        }
    }
    -->
    ```java
    install(OpenTelemetry.Feature, config -> {
        config.addWeaveExporter(
            "https://trace.wandb.ai",
            "my-team",
            "my-project",
            "..."
        );
    })
    ```
    <!--- KNIT exampleOpentelemetrySupportJava09.java -->

请阅读关于与 W&B Weave 集成的[完整文档](opentelemetry-weave-exporter.md)。

## 与 Jaeger 集成 { #integration-with-jaeger }

Jaeger 是一个流行的分布式追踪系统，可与 OpenTelemetry 协同工作。Koog 仓库中 `examples` 内的 `opentelemetry` 目录包含了一个使用 OpenTelemetry 与 Jaeger 以及 Koog 代理的示例。

### 前提条件 { #prerequisites }

要测试 OpenTelemetry 与 Koog 和 Jaeger，请使用提供的 `docker-compose.yaml` 文件，通过运行以下命令启动 Jaeger OpenTelemetry 一体化进程：

```bash
docker compose up -d
```
<!--- KNIT example-opentelemetry-support-02.txt -->

提供的 Docker Compose YAML 文件包含以下内容：

```yaml
# docker-compose.yaml { #docker-compose-yaml }
services:
  jaeger-all-in-one:
    image: jaegertracing/all-in-one:1.39
    container_name: jaeger-all-in-one
    environment:
      - COLLECTOR_OTLP_ENABLED=true
    ports:
      - "4317:4317"
      - "16686:16686"
```
<!--- KNIT example-opentelemetry-support-03.txt -->

要访问 Jaeger UI 并查看您的追踪数据，请打开 `http://localhost:16686`。

### 示例 { #example }

为了导出遥测数据供 Jaeger 使用，该示例使用了来自 `opentelemetry-java` SDK 的 `LoggingSpanExporter`
(`io.opentelemetry.exporter.logging.LoggingSpanExporter`) 和 `OtlpGrpcSpanExporter`
(`io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter`)。

以下是完整的代码示例：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.utils.io.use
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import io.opentelemetry.exporter.logging.LoggingSpanExporter
    import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
    import kotlinx.coroutines.runBlocking
    val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    -->
    <!--- SUFFIX
    --> 
    ```kotlin
    fun main() = runBlocking {
        val agent = AIAgent(
            promptExecutor = promptExecutor,
            llmModel = OpenAIModels.Chat.O4Mini,
            systemPrompt = "You are a code assistant. Provide concise code examples."
        ) {
            install(OpenTelemetry) {
                // 添加控制台日志记录器用于本地调试
                addSpanExporter(LoggingSpanExporter.create())

                // 将追踪发送到 OpenTelemetry 收集器
                addSpanExporter(
                    OtlpGrpcSpanExporter.builder()
                        .setEndpoint("http://localhost:4317")
                        .build()
                )
            }
        }

        agent.use { agent ->
            println("Running the agent with OpenTelemetry tracing...")

            val result = agent.run("Tell me a joke about programming")
```println("Agent run完成，结果：'$result'。" +
                    "\n请访问Jaeger UI http://localhost:16686 查看追踪信息")
        }
    }
    ```
    <!--- KNIT example-opentelemetry-support-10.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    import io.opentelemetry.exporter.logging.LoggingSpanExporter;
    import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
    public class exampleOpentelemetrySupportJava10 {
        static PromptExecutor promptExecutor = PromptExecutor.builder()
            .openAI("openai-api-key")
            .build();
    -->
    <!--- SUFFIX
    }
    -->
    ```java
    public static void main(String[] args) {
        var agent = AIAgent.builder()
            .promptExecutor(promptExecutor)
            .llmModel(OpenAIModels.Chat.O4Mini)
            .systemPrompt("你是一个代码助手。请提供简洁的代码示例。")
            .install(OpenTelemetry.Feature, config -> {
                // 为本地调试添加控制台日志记录器
                config.addSpanExporter(LoggingSpanExporter.create());

                // 将追踪信息发送到OpenTelemetry收集器
                config.addSpanExporter(
                    OtlpGrpcSpanExporter.builder()
                        .setEndpoint("http://localhost:4317")
                        .build()
                );
            })
            .build();

        System.out.println("正在运行带有OpenTelemetry追踪的Agent...");

        var result = agent.run("讲一个关于编程的笑话");

        System.out.println(
            "Agent运行完成，结果：'" + result + "'。" +
                "\n请访问Jaeger UI http://localhost:16686 查看追踪信息"
        );
    }
    ```
    <!--- KNIT exampleOpentelemetrySupportJava10.java -->

## 故障排除 { #troubleshooting }

### 常见问题 { #common-issues }

1. **Jaeger、Langfuse或W&B Weave中未显示追踪信息**
    - 确保服务正在运行，且OpenTelemetry端口（4317）可访问。
    - 检查OpenTelemetry导出器是否配置了正确的端点。
    - 确保在Agent执行后等待几秒钟，以便追踪信息被导出。

2. **缺少Span或追踪信息不完整**
    - 确认Agent执行成功完成。
    - 确保在Agent执行后没有过快关闭应用程序。
    - 在Agent执行后添加延迟，以便Span有足够时间被导出。

3. **Span数量过多**
    - 考虑通过配置`sampler`属性使用不同的采样策略。
    - 例如，使用`Sampler.traceIdRatioBased(0.1)`仅采样10%的追踪信息。

4. **Span适配器相互覆盖**
    - 目前，OpenTelemetry Agent功能不支持应用多个Span适配器[KG-265](https://youtrack.jetbrains.com/issue/KG-265/Adding-Weave-exporter-breaks-Langfuse-exporter)。

## MCP（模型上下文协议）遥测支持 { #mcp-model-context-protocol-telemetry-support }

Koog为MCP操作提供了全面的OpenTelemetry插装，遵循[官方OpenTelemetry语义约定中关于MCP的部分](https://github.com/open-telemetry/semantic-conventions/pull/2083)。

### 概述 { #overview }

MCP遥测支持包括：

- **自动丰富**工具执行Span，添加MCP特定属性
- **客户端插装**用于MCP客户端操作（工具/调用）
- **完全符合语义约定**，包含所有必需、条件必需及推荐的属性

### MCP属性 { #mcp-attributes }

MCP遥测遵循OpenTelemetry语义约定，包含以下属性组：

**必需属性：**
- `mcp.method.name`：MCP方法名称（例如"tools/call"）**条件必需属性：**
- `gen_ai.tool.name`：当操作涉及工具时
- `gen_ai.prompt.name`：当操作涉及提示时
- `jsonrpc.request.id`：当执行请求（非通知）时
- `error.type`：当操作失败时

**推荐属性：**
- `mcp.session.id`：会话标识符
- `mcp.protocol.version`：MCP 协议版本（例如 "2025-06-18"）
- `network.transport`：传输类型（"pipe" 用于标准输入输出，"tcp" 用于 HTTP）
- `server.address` 和 `server.port`：用于客户端操作

### 跨度命名约定 { #span-naming-convention }

MCP 跨度遵循命名约定：`{mcp.method.name} {target}`

其中 `{target}` 在适用时为工具名称或提示名称。示例：
- `"tools/call search"` - 调用名为 "search" 的工具

### 最佳实践 { #best-practices }

- **始终设置会话 ID**：在使用持久性 MCP 会话时，以启用会话追踪
- **传播请求 ID**：从 JSON-RPC 请求中传播，以实现完整的请求追踪
- **监控指标**：识别 MCP 操作中的性能瓶颈

### 示例：完整的 MCP 客户端与遥测 { #example-full-mcp-client-with-telemetry }

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
    import ai.koog.agents.mcp.McpToolRegistryProvider
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import ai.koog.utils.io.use
    import io.opentelemetry.exporter.logging.LoggingSpanExporter
    import kotlinx.coroutines.runBlocking
    fun main() {
        runBlocking {
            val promptExecutor = simpleOpenAIExecutor("openai-api-key")
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    // 创建 MCP 工具注册表
    val toolRegistry = McpToolRegistryProvider.fromSseUrl("http://localhost:3000")
    
    // 创建启用 OpenTelemetry 的代理并传入工具注册表
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant.",
        toolRegistry = toolRegistry
    ) {
        install(OpenTelemetry) {
            setServiceInfo("mcp-agent-service", "1.0.0")
            addSpanExporter(LoggingSpanExporter.create())
        }
    }
    
    // 运行代理 - MCP 工具调用将自动被检测
    agent.use {
        it.run("Use the search tool to find information")
    }
    ```
    <!--- KNIT example-opentelemetry-support-11.kt -->

此设置以最少的代码更改提供了对 MCP 操作的完整可观测性，遵循 OpenTelemetry 最佳实践和语义约定。