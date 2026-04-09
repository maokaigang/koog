<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T16:36:09+00:00", "source_path": "features/open-telemetry/index.md", "source_sha256": "c30379e50abb68d1244f4c65ab8241be9d2790a282d0fe3261a907720382e6e1", "source_tag": "0.7.3", "translation_status": "changed"} -->
# OpenTelemetry 支持 { #opentelemetry-support }

本页详细介绍了 Koog 智能体框架对 OpenTelemetry 的支持，用于追踪和监控您的 AI 智能体。

## 概述 { #overview }

OpenTelemetry 是一个可观测性框架，提供用于生成、收集和导出应用程序遥测数据（追踪）的工具。Koog 的 OpenTelemetry 功能允许您对 AI 智能体进行插装以收集遥测数据，这可以帮助您：

- 监控智能体的性能和行为
- 调试复杂智能体工作流中的问题
- 可视化智能体的执行流程
- 追踪 LLM 调用和工具使用情况
- 分析智能体行为模式

## 关键 OpenTelemetry 概念 { #key-opentelemetry-concepts }

- **跨度（Spans）**：跨度表示分布式追踪中的单个工作单元或操作。它们指示应用程序中特定活动的开始和结束，例如智能体执行、函数调用、LLM 调用或工具调用。
- **属性（Attributes）**：属性提供关于遥测相关项目（例如跨度）的元数据。属性以键值对的形式表示。
- **事件（Events）**：事件是跨度生命周期中的特定时间点（与跨度相关的事件），表示可能值得注意的事件。
- **导出器（Exporters）**：导出器是负责将收集到的遥测数据发送到各种后端或目的地的组件。
- **收集器（Collectors）**：收集器接收、处理和导出遥测数据。它们充当应用程序和可观测性后端之间的中介。
- **采样器（Samplers）**：采样器根据采样策略决定是否应记录追踪。它们用于管理遥测数据的量。
- **资源（Resources）**：资源表示产生遥测数据的实体。它们由资源属性标识，资源属性是提供有关资源信息的键值对。

Koog 中的 OpenTelemetry 功能会自动为各种智能体事件创建跨度，包括：

- 智能体执行开始和结束
- 节点执行
- LLM 调用
- 工具调用

## 安装 { #installation }

要在 Koog 中使用 OpenTelemetry，请将 OpenTelemetry 功能添加到您的智能体中：

=== "Kotlin"

    ```kotlin
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant.",
        installFeatures = {
            install(OpenTelemetry) {
                // Configuration options go here
            }
        }
    )
    ```

=== "Java"

    ```java
    var agent = AIAgent.builder()
        .promptExecutor(promptExecutor)
        .llmModel(OpenAIModels.Chat.GPT4o)
        .systemPrompt("You are a helpful assistant.")
        .install(OpenTelemetry.Feature, config -> {
            // Configuration options go here
        })
        .build();
    ```

## 配置 { #configuration }

### 基本配置 { #basic-configuration }

以下是您在智能体中配置 OpenTelemetry 功能时设置的可用属性的完整列表：

| 名称 | 数据类型 | 默认值 | 描述 |
|------------------|--------------------|------------------------------|------------------------------------------------------------------------------|
| `serviceName` | `String` | `ai.koog` | 正在检测的服务的名称。 |
| `serviceVersion` | `String` | 当前 Koog 库版本 | 正在检测的服务的版本。 |
| `isVerbose` | `Boolean` | `false` | 是否启用详细日志记录以调试 OpenTelemetry 配置。 |
| `sdk` | `OpenTelemetrySdk` |  | 用于遥测收集的 OpenTelemetry SDK 实例。 |
| `tracer` | `Tracer` |  | 用于创建跨度的 OpenTelemetry 跟踪器实例。 |

!!! note
`sdk` 和 `tracer` 属性是您可以访问的公共属性，但您只能使用下面列出的公共方法来设置它们。

`OpenTelemetryConfig` 类还包括表示与不同配置项相关的操作的方法。以下是使用一组基本配置项安装 OpenTelemetry 功能的示例：

=== "Kotlin"

    ```kotlin
    install(OpenTelemetry) {
        // Set your service configuration
        setServiceInfo("my-agent-service", "1.0.0")

        // Add the Logging exporter
        addSpanExporter(LoggingSpanExporter.create())
    }
    ```

=== "Java"

    ```java
    install(OpenTelemetry.Feature, config -> {
        // Set your service configuration
        config.setServiceInfo("my-agent-service", "1.0.0");

        // Add the Logging exporter
        config.addSpanExporter(LoggingSpanExporter.create());
    })
    ```

有关可用方法的参考，请参阅以下部分。

#### setServiceInfo { #setserviceinfo }

设置服务信息，包括名称和版本。采用以下参数：

| 名称 | 数据类型 | 必填 | 默认值 | 描述 |
|--------------------|-----------|----------|---------------|-------------------------------------------------------------|
| `serviceName` | String | 是 |  | 正在检测的服务的名称。 |
| `serviceVersion` | String | 是 |  | 正在检测的服务的版本。 |

#### addSpanExporter { #addspanexporter }

添加跨度导出器以将遥测数据发送到外部系统。采用以下参数：

| 名称 | 数据类型 | 必填 | 默认值 | 描述 |
|------------|----------------|----------|---------------|-------------------------------------------------------------------------------|
| `exporter` | `SpanExporter` | 是 |  | 要添加到自定义跨度导出器列表中的 `SpanExporter` 实例。 |

#### addSpanProcessor { #addspanprocessor }

添加一个跨度处理器工厂以在导出跨度之前对其进行处理。采用以下参数：

| 名称 | 数据类型 | 必填 | 默认值 | 描述 |
|-------------|-----------------------------------|----------|---------------|--------------------------------------------------------------------------------------------------------------|
| `processor` | `(SpanExporter) -> SpanProcessor` | 是 |  | 为给定导出器创建跨度处理器的函数。允许您自定义每个导出器的处理。 |

#### addResourceAttributes { #addresourceattributes }

添加资源属性以提供有关服务的附加上下文。采用以下参数：

| 名称 | 数据类型 | 必填 | 默认值 | 描述 |
|--------------|---------------------------|----------|---------------|------------------------------------------------------------------------|
| `attributes` | `Map<AttributeKey<T>, T>` | 是 |  | 提供有关服务的其他详细信息的键值对。 |

#### setSampler { #setsampler }

设置采样策略以控制收集哪些范围。采用以下参数：

| 名称 | 数据类型 | 必填 | 默认值 | 描述 |
|-----------|-----------|----------|---------------|------------------------------------------------------------------|
| `sampler` | `Sampler` | 是 |  | 为 OpenTelemetry 配置设置的采样器实例。 |

#### setVerbose { #setverbose }

启用或禁用详细日志记录。采用以下参数：

| 名称 | 数据类型 | 必填 | 默认值 | 描述 |
|-----------|-----------|----------|---------------|-----------------------------------------------------------------|
| `verbose` | `Boolean` | 是 | `false` | 如果为 `true`，应用程序会收集更详细的遥测数据。 |

!!! note

    出于安全原因，默认情况下会屏蔽 OpenTelemetry span 的某些内容。例如，LLM消息被屏蔽为`HIDDEN:non-empty`而不是实际的消息内容。要获取内容，请将 `verbose` 参数的值设置为 `true`。

#### setSdk { #setsdk }

注入预先配置的 OpenTelemetrySdk 实例。

- 调用 `setSdk(sdk)` 后，传入的 SDK 会被原样使用，并且此前通过 `addSpanExporter`、`addSpanProcessor`、`addResourceAttributes` 或 `setSampler` 设置的自定义配置都会被忽略。
- 跟踪器的检测范围名称/版本与您的服务信息一致。

| 名称 | 数据类型 | 必填 | 描述 |
|-------|--------------------|----------|---------------------------------------|
| `sdk` | `OpenTelemetrySdk` | 是 | 在智能体中使用的 SDK 实例。 |

### 高级配置 { #advanced-configuration }

对于更高级的配置，您还可以自定义以下配置选项：

- 采样器：配置采样策略，调整采集数据的频率和数量。
- 资源属性：添加有关生成遥测数据的过程的更多信息。

=== "Kotlin"

    ```kotlin
    install(OpenTelemetry) {
        // Set your service configuration
        setServiceInfo("my-agent-service", "1.0.0")

        // Add the Logging exporter
        addSpanExporter(LoggingSpanExporter.create())

        // Set the sampler
        setSampler(Sampler.traceIdRatioBased(0.5))

        // Add resource attributes
        addResourceAttributes(mapOf(
            AttributeKey.stringKey("custom.attribute") to "custom-value")
        )
    }
    ```

=== "Java"

    ```java
    install(OpenTelemetry.Feature, config -> {
        // Set your service configuration
        config.setServiceInfo("my-agent-service", "1.0.0");

        // Add the Logging exporter
        config.addSpanExporter(LoggingSpanExporter.create());

        // Set the sampler
        config.setSampler(Sampler.traceIdRatioBased(0.5));

        // Add resource attributes
        config.addResourceAttributes(Map.of(
            AttributeKey.stringKey("custom.attribute"), "custom-value"
        ));
    })
    ```

#### 采样器 { #sampler }

要定义采样器，请使用 `opentelemetry-java` SDK 中的 `Sampler` 类 (`io.opentelemetry.sdk.trace.samplers.Sampler`) 的相应方法来表示您要使用的采样策略。

默认的采样策略如下：

- `Sampler.alwaysOn()`：默认采样策略，对每个跨度（迹线）进行采样。

有关可用采样器和采样策略的更多信息，请参阅 OpenTelemetry [采样器](https://opentelemetry.io/docs/languages/java/sdk/#sampler) 文档。

#### 资源属性 { #resource-attributes }

资源属性表示有关生成遥测数据的过程的附加信息。 Koog 包括一组默认设置的资源属性：

- `service.name`
- `service.version`
- `service.instance.time`
- `os.type`
- `os.version`
- `os.arch`

`service.name` 属性的默认值为 `ai.koog`，而默认的 `service.version` 值为当前使用的 Koog 库版本。

除了默认资源属性之外，您还可以添加自定义属性。要将自定义属性添加到 Koog 中的 OpenTelemetry 配置，请在 OpenTelemetry 配置中使用 `addResourceAttributes()` 方法，该方法将键和值作为其参数。

=== "Kotlin"

    ```kotlin
    addResourceAttributes(mapOf(
        AttributeKey.stringKey("custom.attribute") to "custom-value")
    )
    ```

=== "Java"

    ```java
    config.addResourceAttributes(Map.of(
        AttributeKey.stringKey("custom.attribute"), "custom-value"
    ));
    ```

## Span 类型和属性 { #span-types-and-attributes }

OpenTelemetry 功能会自动创建不同类型的跨度来跟踪智能体中的各种操作：

- **创建智能体跨度**（`CreateAgentSpan`）：运行智能体时创建，在智能体关闭或进程终止时结束。
- **调用智能体跨度**（`InvokeAgentSpan`）：表示一次智能体调用。
- **策略跨度**（`StrategySpan`）：表示智能体策略的执行，也就是顶层执行流程。
- **节点执行跨度**（`NodeExecuteSpan`）：表示智能体策略中某个节点的执行。这是 Koog 自定义的跨度。
- **子图执行跨度**（`SubgraphExecuteSpan`）：表示智能体策略内某个子图的执行。这也是 Koog 自定义的跨度。
- **推理跨度**（`InferenceSpan`）：表示一次 LLM 调用。
- **工具执行跨度**（`ExecuteToolSpan`）：表示一次工具调用。
- **MCP 客户端跨度**（`McpClientSpan`）：表示 MCP（Model Context Protocol）客户端操作。此跨度遵循 MCP 的 OpenTelemetry 语义约定。

Span 以嵌套的层次结构进行组织。下面是一个跨度结构的例子：

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

### 跨度属性 { #span-attributes }

Span 属性提供与 Span 相关的元数据。每个跨度都有其一组属性，而某些跨度还可以重复属性。

Koog 支持遵循 OpenTelemetry 的 [生成人工智能事件的语义约定](https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-spans/) 的预定义属性列表。例如，约定定义了一个名为 `gen_ai.conversation.id` 的属性，该属性通常是跨度的必需属性。在 Koog 中，此属性的值是智能体运行的唯一标识符，在调用 `agent.run()` 方法时自动设置。

此外，Koog 还包括自定义的、Koog 特定的属性。您可以通过 `koog.` 前缀来识别大多数属性。以下是可用的自定义属性：

- `koog.strategy.name`：智能体策略的名称。策略是 Koog 中用于描述
  智能体用途的实体。用于 `StrategySpan` 跨度。
- `koog.node.id`：正在执行的节点标识符（名称）。用于 `NodeExecuteSpan` 跨度。
- `koog.node.input`：执行开始时传递给节点的输入。节点启动时出现在 `NodeExecuteSpan` 上。
- `koog.node.output`：节点完成后产生的输出。当节点成功完成时出现在 `NodeExecuteSpan` 上。
- `koog.subgraph.id`：正在执行的子图标识符（名称）。用于 `SubgraphExecuteSpan` 跨度。
- `koog.subgraph.input`：在执行开始时传递给子图的输入。当子图开始时出现在 `SubgraphExecuteSpan` 上。
- `koog.subgraph.output`：子图完成后产生的输出。当子图成功完成时出现在 `SubgraphExecuteSpan` 上。

### 事件 { #events }

跨度还可以附加_事件_。事件描述某个相关动作发生的具体时间点，例如 LLM 调用开始或结束的时刻。事件同样带有属性，也可以包含事件 `body` 字段。

根据 OpenTelemetry 的 [生成人工智能事件的语义约定](https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-events/) 支持以下事件类型：

- **系统消息事件**（`SystemMessageEvent`）：传递给模型的系统指令。
- **用户消息事件**（`UserMessageEvent`）：传递给模型的用户消息。
- **助手消息事件**（`AssistantMessageEvent`）：传递给模型的助手消息。
- **工具消息事件**（`ToolMessageEvent`）：传递给模型的工具或函数调用结果。
- **候选项事件**（`ChoiceEvent`）：来自模型的响应消息。
- **审核结果事件**（`ModerationResponseEvent`）：模型返回的审核结果或信号。

!!! note
`opentelemetry-java` SDK 在添加事件时不支持事件正文字段参数。因此，在 Koog 的 OpenTelemetry 支持中，事件体字段会作为一个单独属性存储，键名为 `body`，值类型为字符串。该字符串包含事件体字段的内容或载荷，通常是类似 JSON 的对象。事件体字段的示例可参考 [OpenTelemetry 文档](https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-events/#examples)。关于 `opentelemetry-java` 对事件体字段的支持情况，请参见相关 [GitHub issue](https://github.com/open-telemetry/semantic-conventions/issues/1870)。

## 导出器 { #exporters }

导出器将收集的遥测数据发送到 OpenTelemetry Collector 或其他类型的目的地或后端实现。要添加导出器，请在安装 OpenTelemetry 功能时使用 `addSpanExporter()` 方法。该方法采用以下参数：

| 名称 | 数据类型 | 必填 | 默认 | 描述 |
|------------|--------------|----------|---------|-----------------------------------------------------------------------------|
| `exporter` | `SpanExporter` | 是 |  | 要添加到自定义跨度导出器列表中的 `SpanExporter` 实例。 |

以下部分提供了有关 `opentelemetry-java` SDK 中一些最常用导出器的信息。

!!! note
如果您没有配置任何自定义导出器，Koog 默认情况下将使用控制台 LoggingSpanExporter。这有助于本地开发和调试。

### 日志导出器 { #logging-exporter }

将跟踪信息输出到控制台的日志记录导出器。 `LoggingSpanExporter` (`io.opentelemetry.exporter.logging.LoggingSpanExporter`) 是 `opentelemetry-java` SDK 的一部分。

这种类型的导出对于开发和调试目的很有用。

=== "Kotlin"

    ```kotlin
    install(OpenTelemetry) {
        // Add the logging exporter
        addSpanExporter(LoggingSpanExporter.create())
        // Add more exporters as needed
    }
    ```

=== "Java"

    ```java
    install(OpenTelemetry.Feature, config -> {
        // Add the logging exporter
        config.addSpanExporter(LoggingSpanExporter.create());
        // Add more exporters as needed
    })
    ```

### OpenTelemetry HTTP 导出器 { #opentelemetry-http-exporter }

OpenTelemetry HTTP 导出器 (`OtlpHttpSpanExporter`) 是 `opentelemetry-java` SDK (`io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter`) 的一部分，并通过 HTTP 将跨度数据发送到后端。

=== "Kotlin"

    ```kotlin
    install(OpenTelemetry) {
        // Add OpenTelemetry HTTP exporter
        addSpanExporter(
            OtlpHttpSpanExporter.builder()
                // Set the maximum time to wait for the collector to process an exported batch of spans
                .setTimeout(30, TimeUnit.SECONDS)
                // Set the OpenTelemetry endpoint to connect to
                .setEndpoint("http://localhost:3000/api/public/otel/v1/traces")
                // Add the authorization header
                .addHeader("Authorization", "Basic $AUTH_STRING")
                .build()
        )
    }
    ```

=== "Java"

    ```java
    install(OpenTelemetry.Feature, config -> {
        // Add OpenTelemetry HTTP exporter
        config.addSpanExporter(
            OtlpHttpSpanExporter.builder()
                // Set the maximum time to wait for the collector to process an exported batch of spans
                .setTimeout(30, TimeUnit.SECONDS)
                // Set the OpenTelemetry endpoint to connect to
                .setEndpoint("http://localhost:3000/api/public/otel/v1/traces")
                // Add the authorization header
                .addHeader("Authorization", "Basic " + AUTH_STRING)
                .build()
        );
    })
    ```

### OpenTelemetry gRPC 导出器 { #opentelemetry-grpc-exporter }

OpenTelemetry gRPC 导出器 (`OtlpGrpcSpanExporter`) 是 `opentelemetry-java` SDK (`io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter`) 的一部分。它通过 gRPC 将遥测数据导出到后端，并允许您定义接收数据的后端、收集器或端点的主机与端口。默认端口是 `4317`。

=== "Kotlin"

    ```kotlin
    install(OpenTelemetry) {
        // Add OpenTelemetry gRPC exporter
        addSpanExporter(
            OtlpGrpcSpanExporter.builder()
                // Set the host and the port
                .setEndpoint("http://localhost:4317")
                .build()
        )
    }
    ```

=== "Java"

    ```java
    install(OpenTelemetry.Feature, config -> {
        // Add OpenTelemetry gRPC exporter
        config.addSpanExporter(
            OtlpGrpcSpanExporter.builder()
                // Set the host and the port
                .setEndpoint("http://localhost:4317")
                .build()
        );
    })
    ```

## 与 Langfuse 集成 { #integration-with-langfuse }

Langfuse 为 LLM/智能体工作负载提供跟踪可视化和分析。

您可以配置 Koog 使用辅助函数将 OpenTelemetry 跟踪直接导出到 Langfuse：

=== "Kotlin"

    ```kotlin
    install(OpenTelemetry) {
        addLangfuseExporter(
            langfuseUrl = "https://cloud.langfuse.com",
            langfusePublicKey = "...",
            langfuseSecretKey = "..."
        )
    }
    ```

=== "Java"

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

请阅读 [完整的文档](opentelemetry-langfuse-exporter.md) 有关与 Langfuse 集成的信息。

## 与 W&B Weave 集成 { #integration-with-w-b-weave }

W&B Weave 为 LLM/智能体工作负载提供跟踪可视化和分析。与 W&B Weave 的集成可以通过预定义的导出器进行配置：

=== "Kotlin"

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

=== "Java"

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

请阅读 [完整的文档](opentelemetry-weave-exporter.md) 有关与 W&B Weave 集成的信息。

## 与 Jaeger 集成 { #integration-with-jaeger }

Jaeger 是一个常用的分布式追踪系统，可与 OpenTelemetry 搭配使用。Koog 仓库 `examples` 目录下的 `opentelemetry` 示例展示了如何将 OpenTelemetry、Jaeger 和 Koog 智能体结合使用。

### 前置条件 { #prerequisites }

要测试 Koog 与 Jaeger 的集成，请先运行下面的命令，使用提供的 `docker-compose.yaml` 启动 Jaeger all-in-one 进程：

```bash
docker compose up -d
```

提供的 Docker Compose YAML 文件内容如下：

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

启动后，打开 `http://localhost:16686` 即可访问 Jaeger UI 并查看追踪数据。

### 示例 { #example }

要将遥测数据导出到 Jaeger，示例中使用了 `opentelemetry-java` SDK 提供的 `LoggingSpanExporter`
（`io.opentelemetry.exporter.logging.LoggingSpanExporter`）和 `OtlpGrpcSpanExporter`
（`io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter`）。

完整代码示例如下：

=== "Kotlin"

    ```kotlin
    fun main() = runBlocking {
        val agent = AIAgent(
            promptExecutor = promptExecutor,
            llmModel = OpenAIModels.Chat.O4Mini,
            systemPrompt = "You are a code assistant. Provide concise code examples."
        ) {
            install(OpenTelemetry) {
                // Add a console logger for local debugging
                addSpanExporter(LoggingSpanExporter.create())

                // Send traces to OpenTelemetry collector
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

            println(
                "Agent run completed with result: '$result'.\n" +
                    "Check the Jaeger UI at http://localhost:16686 to inspect the trace."
            )
        }
    }
    ```

=== "Java"

    ```java
    public static void main(String[] args) {
        var agent = AIAgent.builder()
            .promptExecutor(promptExecutor)
            .llmModel(OpenAIModels.Chat.O4Mini)
            .systemPrompt("You are a code assistant. Provide concise code examples.")
            .install(OpenTelemetry.Feature, config -> {
                // Add a console logger for local debugging
                config.addSpanExporter(LoggingSpanExporter.create());

                // Send traces to OpenTelemetry collector
                config.addSpanExporter(
                    OtlpGrpcSpanExporter.builder()
                        .setEndpoint("http://localhost:4317")
                        .build()
                );
            })
            .build();

        System.out.println("Running the agent with OpenTelemetry tracing...");

        var result = agent.run("Tell me a joke about programming");

        System.out.println(
            "Agent run completed with result: '" + result + "'.\n"
                + "Check the Jaeger UI at http://localhost:16686 to inspect the trace."
        );
    }
    ```

## 故障排查 { #troubleshooting }

### 常见问题 { #common-issues }

1. **Jaeger、Langfuse 或 W&B Weave 中没有看到追踪数据**
    - 确认目标服务已启动，并且 OpenTelemetry 端口 `4317` 可访问。
    - 检查 OpenTelemetry 导出器是否配置了正确的端点。
    - 智能体执行结束后等待几秒钟，确保追踪数据有时间被导出。

2. **缺少 span 或追踪不完整**
    - 确认智能体执行已成功完成。
    - 不要在智能体执行结束后立刻关闭应用。
    - 可以在执行结束后增加一点延迟，给 span 导出留出时间。

3. **span 数量过多**
    - 可以通过配置 `sampler` 属性使用不同的采样策略。
    - 例如，使用 `Sampler.traceIdRatioBased(0.1)` 仅采样 10% 的追踪。

4. **多个 span 适配器互相覆盖**
    - 当前 OpenTelemetry 智能体特性还不支持同时应用多个 span 适配器，见 [KG-265](https://youtrack.jetbrains.com/issue/KG-265/Adding-Weave-exporter-breaks-Langfuse-exporter)。

## MCP（Model Context Protocol）遥测支持 { #mcp-model-context-protocol-telemetry-support }

Koog 为 MCP 操作提供了完整的 OpenTelemetry 插装，并遵循 [OpenTelemetry 针对 MCP 的官方语义约定](https://github.com/open-telemetry/semantic-conventions/pull/2083)。

### 概述 { #overview }

MCP 遥测支持包括：

- 使用 MCP 特定属性对工具执行 span 进行**自动增强**
- 为 MCP 客户端操作（`tools/call`）提供**客户端侧插装**
- **完整遵循语义约定**，覆盖所有必填、条件必填和推荐属性

### MCP 属性 { #mcp-attributes }

MCP 遥测遵循 OpenTelemetry 语义约定，包含以下属性分组：

**必填属性：**
- `mcp.method.name`：MCP 方法名，例如 `"tools/call"`

**条件必填属性：**
- `gen_ai.tool.name`：当操作涉及工具时
- `gen_ai.prompt.name`：当操作涉及提示词时
- `jsonrpc.request.id`：当执行的是请求而非通知时
- `error.type`：当操作失败时

**推荐属性：**
- `mcp.session.id`：会话标识符
- `mcp.protocol.version`：MCP 协议版本，例如 `"2025-06-18"`
- `network.transport`：传输类型，stdio 为 `"pipe"`，HTTP 为 `"tcp"`
- `server.address` 和 `server.port`：客户端操作对应的服务端地址与端口

### Span 命名约定 { #span-naming-convention }

MCP span 使用如下命名格式：`{mcp.method.name} {target}`

其中 `{target}` 是对应的工具名或提示词名。示例：
- `"tools/call search"`：调用名为 `search` 的工具

### 最佳实践 { #best-practices }

- 在持久化 MCP 会话中**始终设置 session ID**，便于跟踪会话
- 从 JSON-RPC 请求中**传递 request ID**，以便完整串联请求链路
- **监控指标**，及时识别 MCP 操作中的性能瓶颈

### 示例：带遥测的完整 MCP 客户端 { #example-full-mcp-client-with-telemetry }

=== "Kotlin"

    ```kotlin
    // Create MCP tools registry
    val toolRegistry = McpToolRegistryProvider.fromSseUrl("http://localhost:3000")

    // Create agent with OpenTelemetry enabled and pass the tool registry
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

    // Run agent - MCP tool calls will be automatically instrumented
    agent.use {
        it.run("Use the search tool to find information")
    }
    ```

这套配置只需极少代码改动，就能为 MCP 操作提供完整的可观测性，同时遵循 OpenTelemetry 的最佳实践与语义约定。
