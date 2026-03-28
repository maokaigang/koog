<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:53:30+00:00", "source_path": "agent-events.md", "source_sha256": "bcb7d32435432fdd5a8fabc5250ca08cd3c4c250bb08fff80d263adc7b179c4c", "source_tag": "0.7.3", "translation_status": "changed"} -->
# Agent 事件 { #agent-events }

Agent 事件是作为 Agent 工作流一部分发生的动作或交互。包括：

- Agent 生命周期事件
- 策略事件
- 节点执行事件
- LLM 调用事件
- LLM 流式事件
- 工具执行事件

注意：功能事件在 agents-core 模块中定义，位于包 `ai.koog.agents.core.feature.model.events` 下。诸如 `agents-features-trace` 和 `agents-features-event-handler` 等功能会消费这些事件，以处理和转发在 Agent 执行期间创建的消息。

## 预定义事件类型 { #predefined-event-types }

Koog 提供了可在自定义消息处理器中使用的预定义事件类型。预定义事件可根据其关联的实体分为以下几类：

- [Agent 事件](#agent-events)
- [策略事件](#strategy-events)
- [节点事件](#node-events)
- [子图事件](#subgraph-events)
- [LLM 调用事件](#llm-call-events)
- [LLM 流式事件](#llm-streaming-events)
- [工具执行事件](#tool-execution-events)

### Agent 事件 { #agent-events }

#### AgentStartingEvent { #agentstartingevent }

表示 Agent 运行的开始。包含以下字段：

| 名称            | 数据类型           | 必需 | 默认值 | 描述                                                                 |
|-----------------|---------------------|----------|---------|----------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                    |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。 |
| `agentId`       | String              | 是      |         | AI Agent 的唯一标识符。                                     |
| `runId`         | String              | 是      |         | AI Agent 运行的唯一标识符。                                 |

#### AgentCompletedEvent { #agentcompletedevent }

表示 Agent 运行的结束。包含以下字段：

| 名称            | 数据类型           | 必需 | 默认值 | 描述                                                                     |
|-----------------|---------------------|----------|---------|---------------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                         |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。 |
| `agentId`       | String              | 是      |         | AI Agent 的唯一标识符。                                          |
| `runId`         | String              | 是      |         | AI Agent 运行的唯一标识符。                                      |
| `result`        | String              | 是      |         | Agent 运行的结果。若无结果，可为 `null`。               |

#### AgentExecutionFailedEvent { #agentexecutionfailedevent }

表示在 Agent 运行期间发生错误。包含以下字段：| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                                                     |
|-----------------|---------------------|----------|---------|-----------------------------------------------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                                                                                         |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。                                 |
| `agentId`       | String              | 是      |         | AI 代理的唯一标识符。                                                                          |
| `runId`         | String              | 是      |         | AI 代理运行实例的唯一标识符。                                                                      |
| `error`         | AIAgentError        | 是      |         | 代理运行期间发生的具体错误。更多信息请参阅 [AIAgentError](#aiagenterror)。 |

#### AgentClosingEvent { #agentclosingevent }

表示代理的关闭或终止。包含以下字段：

| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                |
|-----------------|---------------------|----------|---------|----------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                    |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。 |
| `agentId`       | String              | 是      |         | AI 代理的唯一标识符。                                     |

<a id="aiagenterror"></a>
`AIAgentError` 类提供代理运行期间发生错误的更多详细信息。包含以下字段：

| 名称         | 数据类型 | 必填 | 默认值 | 描述                                                      |
|--------------|-----------|----------|---------|------------------------------------------------------------------|
| `message`    | String    | 是      |         | 提供具体错误详细信息的消息。 |
| `stackTrace` | String    | 是      |         | 直至最后执行代码的堆栈记录集合。    |
| `cause`      | String    | 否       | null    | 错误原因（如果可用）。                            |

<a id="agentexecutioninfo"></a>
`AgentExecutionInfo` 类提供执行路径的上下文信息，支持追踪代理运行内的嵌套执行上下文。包含以下字段：| 名称       | 数据类型           | 必填 | 默认值 | 描述                                                                                    |
|------------|---------------------|----------|---------|------------------------------------------------------------------------------------------------|
| `parent`   | AgentExecutionInfo  | 否       | null    | 对父执行上下文的引用。若为 null，则表示根执行层级。  |
| `partName` | String              | 是      |         | 表示当前执行部分或片段的名称字符串。                |

### 策略事件 { #strategy-events }

#### GraphStrategyStartingEvent { #graphstrategystartingevent }

表示基于图的策略运行开始。包含以下字段：

| 名称            | 数据类型              | 必填 | 默认值 | 描述                                                                |
|-----------------|------------------------|----------|---------|----------------------------------------------------------------------------|
| `eventId`       | String                 | 是      |         | 事件或事件组的唯一标识符。                    |
| `executionInfo` | AgentExecutionInfo     | 是      |         | 提供与此事件关联的执行上下文信息。 |
| `runId`         | String                 | 是      |         | 策略运行的唯一标识符。                                 |
| `strategyName`  | String                 | 是      |         | 策略名称。                                                  |
| `graph`         | StrategyEventGraph     | 是      |         | 表示策略工作流的图结构。                    |

#### FunctionalStrategyStartingEvent { #functionalstrategystartingevent }

表示功能型策略运行开始。包含以下字段：

| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                |
|-----------------|---------------------|----------|---------|----------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                    |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。 |
| `runId`         | String              | 是      |         | 策略运行的唯一标识符。                                 |
| `strategyName`  | String              | 是      |         | 策略名称。                                                  |

#### StrategyCompletedEvent { #strategycompletedevent }

表示策略运行结束。包含以下字段：| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                 |
|-----------------|---------------------|----------|---------|----------------------------------------------------------------------------|
| `eventId`       | String              | 是       |         | 事件或事件组的唯一标识符。                                                 |
| `executionInfo` | AgentExecutionInfo  | 是       |         | 提供与此事件关联的执行上下文信息。                                         |
| `runId`         | String              | 是       |         | 策略运行的唯一标识符。                                                     |
| `strategyName`  | String              | 是       |         | 策略名称。                                                                 |
| `result`        | String              | 是       |         | 运行结果。若无结果，可为 `null`。                                |

### 节点事件 { #node-events }

#### NodeExecutionStartingEvent { #nodeexecutionstartingevent }

表示节点运行的开始。包含以下字段：

| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                 |
|-----------------|---------------------|----------|---------|----------------------------------------------------------------------------|
| `eventId`       | String              | 是       |         | 事件或事件组的唯一标识符。                                                 |
| `executionInfo` | AgentExecutionInfo  | 是       |         | 提供与此事件关联的执行上下文信息。                                         |
| `runId`         | String              | 是       |         | 策略运行的唯一标识符。                                                     |
| `nodeName`      | String              | 是       |         | 开始运行的节点名称。                                                       |
| `input`         | JsonElement         | 否       | null    | 节点的输入值。                                                             |

#### NodeExecutionCompletedEvent { #nodeexecutioncompletedevent }

表示节点运行的结束。包含以下字段：

| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                 |
|-----------------|---------------------|----------|---------|----------------------------------------------------------------------------|
| `eventId`       | String              | 是       |         | 事件或事件组的唯一标识符。                                                 |
| `executionInfo` | AgentExecutionInfo  | 是       |         | 提供与此事件关联的执行上下文信息。                                         |
| `runId`         | String              | 是       |         | 策略运行的唯一标识符。                                                     |
| `nodeName`      | String              | 是       |         | 结束运行的节点名称。                                                       |
| `input`         | JsonElement         | 否       | null    | 节点的输入值。                                                             |
| `output`        | JsonElement         | 否       | null    | 节点产生的输出值。                                                         |#### NodeExecutionFailedEvent

表示节点运行期间发生的错误。包含以下字段：

| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                                                     |
|-----------------|---------------------|----------|---------|-----------------------------------------------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                                                                                         |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。                                                                                 |
| `runId`         | String              | 是      |         | 策略运行的唯一标识符。                                                                                            |
| `nodeName`      | String              | 是      |         | 发生错误的节点名称。                                                                                              |
| `input`         | JsonElement         | 否       | null    | 提供给节点的输入数据。                                                                                            |
| `error`         | AIAgentError        | 是      |         | 节点运行期间发生的具体错误。更多信息请参阅 [AIAgentError](#aiagenterror)。                                     |

### 子图事件 { #subgraph-events }

#### SubgraphExecutionStartingEvent

表示子图运行的开始。包含以下字段：

| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                |
|-----------------|---------------------|----------|---------|----------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                                                    |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。                                              |
| `runId`         | String              | 是      |         | 策略运行的唯一标识符。                                                         |
| `subgraphName`  | String              | 是      |         | 开始运行的子图名称。                                                           |
| `input`         | JsonElement         | 否       | null    | 子图的输入值。                                                                |

#### SubgraphExecutionCompletedEvent { #subgraphexecutionstartingevent }

表示子图运行的结束。包含以下字段：| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                 |
|-----------------|---------------------|----------|---------|----------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                                                    |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。                                             |
| `runId`         | String              | 是      |         | 策略运行的唯一标识符。                                                         |
| `subgraphName`  | String              | 是      |         | 运行结束的子图名称。                                                           |
| `input`         | JsonElement         | 否       | null    | 子图的输入值。                                                                |
| `output`        | JsonElement         | 否       | null    | 子图产生的输出值。                                                             |

#### SubgraphExecutionFailedEvent { #subgraphexecutioncompletedevent }

表示子图运行期间发生的错误。包含以下字段：

| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                                                     |
|-----------------|---------------------|----------|---------|-----------------------------------------------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                                                                                         |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。                                                                                 |
| `runId`         | String              | 是      |         | 策略运行的唯一标识符。                                                                                              |
| `subgraphName`  | String              | 是      |         | 发生错误的子图名称。                                                                                              |
| `input`         | JsonElement         | 否       | null    | 提供给子图的输入数据。                                                                                            |
| `error`         | AIAgentError        | 是      |         | 子图运行期间发生的具体错误。更多信息请参阅 [AIAgentError](#aiagenterror)。                                   |

### LLM 调用事件 { #llm-call-events }

#### LLMCallStartingEvent

表示 LLM 调用的开始。包含以下字段：| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                        |
|-----------------|---------------------|----------|---------|------------------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                            |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。    |
| `runId`         | String              | 是      |         | LLM 运行的唯一标识符。                                              |
| `prompt`        | Prompt              | 是      |         | 发送给模型的提示。更多信息请参阅 [Prompt](#prompt)。 |
| `model`         | ModelInfo           | 是      |         | 模型信息。请参阅 [ModelInfo](#modelinfo)。                                |
| `tools`         | List<String>        | 是      |         | 模型可以调用的工具列表。                                         |

<a id="prompt"></a>
`Prompt` 类表示提示的数据结构，包含消息列表、唯一标识符以及语言模型设置的可选参数。包含以下字段：

| 名称       | 数据类型           | 必填 | 默认值     | 描述                                                  |
|------------|---------------------|----------|-------------|--------------------------------------------------------------|
| `messages` | List<Message>       | 是      |             | 提示所包含的消息列表。            |
| `id`       | String              | 是      |             | 提示的唯一标识符。                        |
| `params`   | LLMParams           | 否       | LLMParams() | 控制 LLM 生成内容方式的设置。 |

<a id="modelinfo"></a>
`ModelInfo` 类表示语言模型的信息，包括其提供商、模型标识符和特性。包含以下字段：

| 名称              | 数据类型 | 必填 | 默认值 | 描述                                                              |
|-------------------|-----------|----------|---------|--------------------------------------------------------------------------|
| `provider`        | String    | 是      |         | 提供商标识符（例如 "openai"、"google"、"anthropic"）。         |
| `model`           | String    | 是      |         | 模型标识符（例如 "gpt-4"、"claude-3"）。                        |
| `displayName`     | String    | 否       | null    | 模型的可选人类可读显示名称。                      |
| `contextLength`   | Long      | 否       | null    | 模型可处理的最大令牌数。                          |
| `maxOutputTokens` | Long      | 否       | null    | 模型可生成的最大令牌数。                         |

#### LLMCallCompletedEvent表示一个 LLM 调用的结束。包含以下字段： { #llmcallstartingevent }

| 名称                 | 数据类型              | 必填 | 默认值 | 描述                                                                     |
|----------------------|------------------------|----------|---------|---------------------------------------------------------------------------------|
| `eventId`            | String                 | 是      |         | 事件或事件组的唯一标识符。                         |
| `executionInfo`      | AgentExecutionInfo     | 是      |         | 提供与此事件关联的执行上下文信息。 |
| `runId`              | String                 | 是      |         | LLM 运行的唯一标识符。                                           |
| `prompt`             | Prompt                 | 是      |         | 调用中使用的提示词。                                                    |
| `model`              | ModelInfo              | 是      |         | 模型信息。参见 [ModelInfo](#modelinfo)。                             |
| `responses`          | List<Message.Response> | 是      |         | 模型返回的一个或多个响应。                                    |
| `moderationResponse` | ModerationResult       | 否       | null    | 审核响应（如果有）。                                                |

### LLM 流式事件 { #llm-streaming-events }

#### LLMStreamingStartingEvent

表示一个 LLM 流式调用的开始。包含以下字段：

| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                     |
|-----------------|---------------------|----------|---------|---------------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                         |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。 |
| `runId`         | String              | 是      |         | LLM 运行的唯一标识符。                                           |
| `prompt`        | Prompt              | 是      |         | 发送给模型的提示词。                                           |
| `model`         | ModelInfo           | 是      |         | 模型信息。参见 [ModelInfo](#modelinfo)。                             |
| `tools`         | List<String>        | 是      |         | 模型可以调用的工具列表。                                      |

#### LLMStreamingFrameReceivedEvent { #llmstreamingstartingevent }

表示从 LLM 接收到的流式数据帧。包含以下字段：| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                     |
|-----------------|---------------------|----------|---------|---------------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                         |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。 |
| `runId`         | String              | 是      |         | LLM 运行的唯一标识符。                                           |
| `prompt`        | Prompt              | 是      |         | 发送给模型的提示词。                                           |
| `model`         | ModelInfo           | 是      |         | 模型信息。详见 [ModelInfo](#modelinfo)。                             |
| `frame`         | StreamFrame         | 是      |         | 从流中接收到的帧。                                             |

#### LLMStreamingFailedEvent { #llmstreamingframereceivedevent }

表示 LLM 流式调用期间发生错误。包含以下字段：

| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                                                 |
|-----------------|---------------------|----------|---------|-------------------------------------------------------------------------------------------------------------|
| `eventId`       | String              | 是      |         | 事件或事件组的唯一标识符。                                                     |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。                             |
| `runId`         | String              | 是      |         | LLM 运行的唯一标识符。                                                                       |
| `prompt`        | Prompt              | 是      |         | 发送给模型的提示词。                                                                       |
| `model`         | ModelInfo           | 是      |         | 模型信息。详见 [ModelInfo](#modelinfo)。                                                         |
| `error`         | AIAgentError        | 是      |         | 流式处理期间发生的具体错误。更多信息请参阅 [AIAgentError](#aiagenterror)。 |

#### LLMStreamingCompletedEvent { #llmstreamingfailedevent }

表示 LLM 流式调用的结束。包含以下字段：| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                     |
|-----------------|---------------------|----------|---------|---------------------------------------------------------------------------------|
| `eventId`       | 字符串              | 是      |         | 事件或事件组的唯一标识符。                         |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。 |
| `runId`         | 字符串              | 是      |         | LLM 运行的唯一标识符。                                           |
| `prompt`        | Prompt              | 是      |         | 发送给模型的提示词。                                           |
| `model`         | ModelInfo           | 是      |         | 模型信息。参见 [ModelInfo](#modelinfo)。                             |
| `tools`         | 字符串列表        | 是      |         | 模型可以调用的工具列表。                                      |

### 工具执行事件 { #tool-execution-events }

#### ToolCallStartingEvent

表示模型调用工具的事件。包含以下字段：

| 名称            | 数据类型           | 必填 | 默认值 | 描述                                                                |
|-----------------|---------------------|----------|---------|----------------------------------------------------------------------------|
| `eventId`       | 字符串              | 是      |         | 事件或事件组的唯一标识符。                    |
| `executionInfo` | AgentExecutionInfo  | 是      |         | 提供与此事件关联的执行上下文信息。 |
| `runId`         | 字符串              | 是      |         | 策略/代理运行的唯一标识符。                           |
| `toolCallId`    | 字符串              | 否       | null    | 工具调用的标识符（如果可用）。                             |
| `toolName`      | 字符串              | 是      |         | 工具的名称。                                                      |
| `toolArgs`      | JsonObject          | 是      |         | 提供给工具的参数。                               |

#### ToolValidationFailedEvent { #toolcallstartingevent }

表示工具调用期间发生验证错误的事件。包含以下字段：| 名称              | 数据类型           | 必填     | 默认值  | 描述                                                                       |
|-------------------|---------------------|----------|---------|----------------------------------------------------------------------------|
| `eventId`         | String              | 是       |         | 事件或事件组的唯一标识符。                                                  |
| `executionInfo`   | AgentExecutionInfo  | 是       |         | 提供与此事件关联的执行上下文信息。                                          |
| `runId`           | String              | 是       |         | 策略/代理运行的唯一标识符。                                                 |
| `toolCallId`      | String              | 否       | null    | 工具调用的标识符（如果可用）。                                              |
| `toolName`        | String              | 是       |         | 验证失败的工具名称。                                                        |
| `toolArgs`        | JsonObject          | 是       |         | 提供给工具的输入参数。                                                      |
| `toolDescription` | String              | 否       | null    | 遇到验证错误的工具描述。                                                    |
| `message`         | String              | 否       | null    | 描述验证错误的消息。                                                        |
| `error`           | AIAgentError        | 是       |         | 发生的具体错误。更多信息请参阅 [AIAgentError](#aiagenterror)。          |

#### ToolCallFailedEvent { #toolvalidationfailedevent }

表示工具执行失败。包含以下字段：| 名称              | 数据类型           | 必填     | 默认值  | 描述                                                                                                                     |
|-------------------|---------------------|----------|---------|-------------------------------------------------------------------------------------------------------------------------|
| `eventId`         | String              | 是       |         | 事件或事件组的唯一标识符。                                                                                               |
| `executionInfo`   | AgentExecutionInfo  | 是       |         | 提供与此事件关联的执行上下文信息。                                                                                       |
| `runId`           | String              | 是       |         | 策略/代理运行的唯一标识符。                                                                                              |
| `toolCallId`      | String              | 否       | null    | 工具调用的标识符（如果可用）。                                                                                           |
| `toolName`        | String              | 是       |         | 工具的名称。                                                                                                             |
| `toolArgs`        | JsonObject          | 是       |         | 提供给工具的参数。                                                                                                       |
| `toolDescription` | String              | 否       | null    | 失败工具的描述。                                                                                                         |
| `error`           | AIAgentError        | 是       |         | 调用工具时发生的具体错误。更多信息请参阅 [AIAgentError](#aiagenterror)。                                                |

#### ToolCallCompletedEvent { #toolcallfailedevent }

表示一个成功的工具调用并返回结果。包含以下字段：| 名称              | 数据类型           | 必填     | 默认值  | 描述                                                                       |
|-------------------|---------------------|----------|---------|----------------------------------------------------------------------------|
| `eventId`         | String              | 是       |         | 事件或事件组的唯一标识符。                                                  |
| `executionInfo`   | AgentExecutionInfo  | 是       |         | 提供与此事件关联的执行上下文信息。                                          |
| `runId`           | String              | 是       |         | 运行的唯一标识符。                                                          |
| `toolCallId`      | String              | 否       | null    | 工具调用的标识符。                                                          |
| `toolName`        | String              | 是       |         | 工具的名称。                                                                |
| `toolArgs`        | JsonObject          | 是       |         | 提供给工具的参数。                                                          |
| `toolDescription` | String              | 否       | null    | 已执行工具的描述。                                                          |
| `result`          | JsonElement         | 否       | null    | 工具调用的结果。                                                            |

## FAQ与故障排除

以下部分包含与追踪功能相关的常见问题与解答。

### 如何仅追踪智能体执行的特定部分？

使用 `messageFilter` 属性过滤事件。例如，仅追踪节点执行：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.feature.model.events.LLMCallCompletedEvent
    import ai.koog.agents.core.feature.model.events.LLMCallStartingEvent
    import ai.koog.agents.example.exampleTracing01.outputPath
    import ai.koog.agents.features.tracing.feature.Tracing
    import ai.koog.agents.features.tracing.writer.TraceFeatureMessageFileWriter
    import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import kotlinx.coroutines.runBlocking
    import kotlinx.io.buffered
    import kotlinx.io.files.Path
    import kotlinx.io.files.SystemFileSystem
    const val input = "What's the weather like in New York?"
    fun main() {
    runBlocking {
    // Creating an agent
    val agent = AIAgent(
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2,
    ) {
    val writer = TraceFeatureMessageFileWriter(
    outputPath,
    { path: Path -> SystemFileSystem.sink(path).buffered() }
    )
    -->
    <!--- SUFFIX
            }
        }
    }
    -->
    ```kotlin
    install(Tracing) {
        val fileWriter = TraceFeatureMessageFileWriter(
            outputPath, 
            { path: Path -> SystemFileSystem.sink(path).buffered() }
        )
        addMessageProcessor(fileWriter)
        
        // Only trace LLM calls
        fileWriter.setMessageFilter { message ->
            message is LLMCallStartingEvent || message is LLMCallCompletedEvent
        }
    }
    ```
    <!--- KNIT example-events-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-events-java-01.java -->

### Can I use multiple message processors?

Yes, you can add multiple message processors to trace to different destinations simultaneously:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.feature.remote.server.config.DefaultServerConnectionConfig
    import ai.koog.agents.example.exampleTracing01.outputPath
    import ai.koog.agents.features.tracing.feature.Tracing
    import ai.koog.agents.features.tracing.writer.TraceFeatureMessageFileWriter
    import ai.koog.agents.features.tracing.writer.TraceFeatureMessageLogWriter
    import ai.koog.agents.features.tracing.writer.TraceFeatureMessageRemoteWriter
    import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import io.github.oshai.kotlinlogging.KotlinLogging
    import kotlinx.coroutines.runBlocking
    import kotlinx.io.buffered
    import kotlinx.io.files.Path
    import kotlinx.io.files.SystemFileSystem
    const val input = "What's the weather like in New York?"
    val syncOpener = { path: Path -> SystemFileSystem.sink(path).buffered() }
    val logger = KotlinLogging.logger {}
    val connectionConfig = DefaultServerConnectionConfig(host = ai.koog.agents.example.exampleTracing06.host, port = ai.koog.agents.example.exampleTracing06.port)
    fun main() {
    runBlocking {
    // Creating an agent
    val agent = AIAgent(
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2,
    ) {
    -->
    <!--- SUFFIX
            }
        }
    }
    -->
    ```kotlin
    install(Tracing) {
        addMessageProcessor(TraceFeatureMessageLogWriter(logger))
        addMessageProcessor(TraceFeatureMessageFileWriter(outputPath, syncOpener))
        addMessageProcessor(TraceFeatureMessageRemoteWriter(connectionConfig))
    }
    ```
    <!--- KNIT example-events-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-events-java-02.java -->

### How can I create a custom message processor?

Implement the `FeatureMessageProcessor` interface:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.feature.model.events.NodeExecutionStartingEvent
    import ai.koog.agents.core.feature.model.events.LLMCallCompletedEvent
    import ai.koog.agents.core.feature.message.FeatureMessage
    import ai.koog.agents.core.feature.message.FeatureMessageProcessor
    import ai.koog.agents.features.tracing.feature.Tracing
    import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import kotlinx.coroutines.runBlocking
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.asStateFlow
    fun main() {
    runBlocking {
    // Creating an agent
    val agent = AIAgent(
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2,
    ) {
    -->
    <!--- SUFFIX
            }
        }
    }
    -->
    ```kotlin
    class CustomTraceProcessor : FeatureMessageProcessor() {

        // Current open state of the processor
        private var _isOpen = MutableStateFlow(false)

        override val isOpen: StateFlow<Boolean>
            get() = _isOpen.asStateFlow()
        
        override suspend fun processMessage(message: FeatureMessage) {
            // Custom processing logic
            when (message) {
                is NodeExecutionStartingEvent -> {
                    // Process node start event
                }

                is LLMCallCompletedEvent -> {
                    // Process LLM call end event 
                }
                // Handle other event types 
            }
        }

        override suspend fun close() {
            // Close connections of established
        }
    }

    // Use your custom processor
    install(Tracing) {
        addMessageProcessor(CustomTraceProcessor())
    }
    ```
    <!--- KNIT example-events-03.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-events-java-03.java -->

For more information about existing event types that can be handled by message processors, see [Predefined event types](#predefined-event-types).
