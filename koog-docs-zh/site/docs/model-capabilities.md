<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:10:24+00:00", "source_path": "model-capabilities.md", "source_sha256": "595a37fe9ed01a50350c454d059d86b81224419deba86218da43cb6322f3a0e5", "source_tag": "0.7.3", "translation_status": "changed"} -->
Koog 提供了一套抽象和实现，用于以与提供商无关的方式处理来自不同 LLM 提供商的大型语言模型（LLMs）。该集合包含以下类：

- **LLMCapability**：一个类层次结构，定义了 LLMs 可以支持的各种能力，例如：
    - 用于控制响应随机性的温度调整
    - 用于与外部系统交互的工具集成
    - 用于处理视觉数据的视觉处理
    - 用于向量表示的嵌入生成
    - 用于文本生成任务的补全
    - 用于结构化数据的模式支持（JSON，包含简单和完整变体）
    - 用于探索性响应的推测

- **LLModel**：一个数据类，表示一个特定的 LLM，包含其提供商、唯一标识符和支持的能力。

这为以统一方式与不同的 LLM 提供商交互奠定了基础，允许应用程序在处理各种模型的同时，抽象掉提供商特定的细节。

## LLM 能力 { #llm-capabilities }

LLM 能力代表了大型语言模型可以支持的特定功能或特性。在 Koog 框架中，能力用于定义特定模型可以做什么以及如何配置它。每个能力都表示为 `LLMCapability` 类的子类或数据对象。

在应用程序中配置 LLM 时，您通过创建 `LLModel` 实例时将其添加到 `capabilities` 列表中来指定其支持的能力。这使得框架能够正确地与模型交互并适当地使用其功能。

### 核心能力 { #core-capabilities }

以下列表包含了 Koog 框架中模型可用的核心、LLM 特定能力：

- **推测**（`LLMCapability.Speculation`）：让模型生成具有不同可能性的推测性或探索性响应。适用于需要更广泛潜在结果的创造性或假设性场景。

- **温度**（`LLMCapability.Temperature`）：允许调整模型响应的随机性或创造性水平。较高的温度值产生更多样化的输出，而较低的值导致更集中和确定性的响应。

- **工具**（`LLMCapability.Tools`）：表示支持外部工具使用或集成。此能力允许模型运行特定工具或与外部系统交互。

- **工具选择**（`LLMCapability.ToolChoice`）：配置工具调用如何与 LLM 配合工作。根据模型的不同，可以配置为：
    - 自动在生成文本或工具调用之间选择
    - 仅生成工具调用，从不生成文本
    - 仅生成文本，从不生成工具调用
    - 强制调用已定义工具中的特定工具

- **多选**（`LLMCapability.MultipleChoices`）：让模型为单个提示生成多个独立的回复选项。

### 媒体处理能力 { #media-processing-capabilities }

以下列表代表了一组用于处理图像或音频等媒体内容的能力：- **视觉** (`LLMCapability.Vision`)：一个用于处理、分析和从视觉数据中推断见解的视觉能力类。
  支持以下类型的视觉数据：
    - **图像** (`LLMCapability.Vision.Image`)：处理图像相关的视觉任务，如图像分析、识别和解读。
    - **视频** (`LLMCapability.Vision.Video`)：处理视频数据，包括分析和理解视频内容。

- **音频** (`LLMCapability.Audio`)：提供音频相关功能，如转录、音频生成或基于音频的交互。

- **文档** (`LLMCapability.Document`)：支持处理和操作基于文档的输入和输出。

### 文本处理能力 { #text-processing-capabilities }

以下能力列表代表了文本生成和处理功能：

- **嵌入** (`LLMCapability.Embed`)：让模型从输入文本生成向量嵌入，支持相似性比较、聚类和其他基于向量的分析。

- **补全** (`LLMCapability.Completion`)：包括基于给定输入上下文生成文本或内容，例如完成句子、生成建议或生成与输入数据一致的内容。

- **提示缓存** (`LLMCapability.PromptCaching`)：支持提示的缓存功能，可能提高重复或类似查询的性能。

- **审核** (`LLMCapability.Moderation`)：让模型分析文本中可能有害的内容，并根据骚扰、仇恨言论、自残、色情内容、暴力等不同类别进行分类。

### 模式能力 { #schema-capabilities }

以下列表展示了与处理结构化数据相关的能力：

- **模式** (`LLMCapability.Schema`)：一个用于结构化模式能力的类，涉及使用特定格式进行数据交互和编码。
  包括对以下格式的支持：
    - **JSON** (`LLMCapability.Schema.JSON`)：JSON 模式支持，具有不同级别：
        - **基础** (`LLMCapability.Schema.JSON.Basic`)：提供轻量级或基础的 JSON 处理能力。
        - **标准** (`LLMCapability.Schema.JSON.Standard`)：为复杂数据结构提供全面的 JSON 模式支持。

## 创建模型 (LLModel) 配置 { #creating-a-model-llmodel-configuration }

要以通用、与提供商无关的方式定义模型，请创建一个模型配置作为 `LLModel` 类的实例，并包含以下参数：| 名称              | 数据类型                 | 必填 | 默认值 | 描述                                                                                                                                                                                    |
|-------------------|---------------------------|----------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `provider`        | LLMProvider               | 是      |         | LLM的提供者，例如Google或OpenAI。这标识了创建或托管该模型的公司或组织。                                                                |
| `id`              | String                    | 是      |         | LLM实例的唯一标识符。通常代表特定的模型版本或名称。例如：`gpt-4-turbo`、`claude-3-opus`、`llama-3-2`。                              |
| `capabilities`    | List&lt;LLMCapability&gt; | 是      |         | LLM支持的功能列表，例如温度调整、工具使用或基于模式的任务。这些功能定义了模型能做什么以及如何配置。 |
| `contextLength`   | Long                      | 是      |         | LLM的上下文长度。这是LLM能处理的最大令牌数。                                                                                                       |
| `maxOutputTokens` | Long                      | 否       | `null`  | 提供者为LLM生成的最大令牌数。                                                                                                                |

### 示例 { #examples }

本节提供创建具有不同功能的`LLModel`实例的详细示例。

以下代码展示了具有核心功能的基本LLM配置：

<!--- INCLUDE
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

-->

```kotlin
val basicModel = LLModel(
    provider = LLMProvider.OpenAI,
    id = "gpt-4-turbo",
    capabilities = listOf(
        LLMCapability.Temperature,
        LLMCapability.Tools,
        LLMCapability.Schema.JSON.Standard
    ),
    contextLength = 128_000
)
```

<!--- KNIT example-model-capabilities-01.kt -->

以下模型配置是具有视觉功能的多模态LLM：

<!--- INCLUDE
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

-->

```kotlin
val visionModel = LLModel(
    provider = LLMProvider.OpenAI,
    id = "gpt-4-vision",
    capabilities = listOf(
        LLMCapability.Temperature,
        LLMCapability.Vision.Image,
        LLMCapability.MultipleChoices
    ),
    contextLength = 1_047_576,
    maxOutputTokens = 32_768
)
```

<!--- KNIT example-model-capabilities-02.kt -->

具有音频处理功能的LLM：

<!--- INCLUDE
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

-->

```kotlin
val audioModel = LLModel(
    provider = LLMProvider.Anthropic,
    id = "claude-3-opus",
    capabilities = listOf(
        LLMCapability.Audio,
        LLMCapability.Temperature,
        LLMCapability.PromptCaching
    ),
    contextLength = 200_000
)
```

<!--- KNIT example-model-capabilities-03.kt -->


除了将模型创建为`LLModel`实例并必须指定所有相关参数外，Koog还包含一组预定义模型及其支持功能的配置。
要使用预定义的Ollama模型，请按如下方式指定：

<!--- INCLUDE
import ai.koog.prompt.executor.ollama.client.OllamaModels

-->

```kotlin
val metaModel = OllamaModels.Meta.LLAMA_3_2
```

<!--- KNIT example-model-capabilities-04.kt -->


要检查模型是否支持特定功能，请使用`contains`方法检查`capabilities`列表中是否存在该功能：

<!--- INCLUDE
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.executor.ollama.client.OllamaModels

val basicModel = OllamaModels.Meta.LLAMA_3_2
val visionModel = OllamaModels.Meta.LLAMA_3_2

-->

```kotlin
// Check if models support specific capabilities
val supportsTools = basicModel.supports(LLMCapability.Tools) // true
val supportsVideo = visionModel.supports(LLMCapability.Vision.Video) // false

// Check for schema capabilities
val jsonCapability = basicModel.capabilities?.filterIsInstance<LLMCapability.Schema.JSON>()?.firstOrNull()
val hasFullJsonSupport = jsonCapability is LLMCapability.Schema.JSON.Standard // true
```

<!--- KNIT example-model-capabilities-05.kt -->

### 各模型的LLM功能 { #llm-capabilities-by-model }

本参考展示了不同提供者的每个模型支持哪些LLM功能。

在下表中：- `✓` 表示模型支持该能力
- `-` 表示模型不支持该能力
- 对于 JSON Schema，`Full` 或 `Simple` 表示模型支持的 JSON Schema 能力变体

??? "Google 模型"
    #### Google 模型

    | 模型                  | 温度 | JSON Schema | 补全 | 多项选择 | 工具 | 工具选择 | 视觉（图像） | 视觉（视频） | 音频 |
    |------------------------|-------------|-------------|------------|------------------|-------|-------------|----------------|----------------|-------|
    | Gemini2_5Pro           | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | ✓              | ✓     |
    | Gemini2_5Flash         | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | ✓              | ✓     |
    | Gemini2_5FlashLite     | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | ✓              | ✓     |
    | Gemini2_0Flash         | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | ✓              | ✓     |
    | Gemini2_0Flash001      | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | ✓              | ✓     |
    | Gemini2_0FlashLite     | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | ✓              | ✓     |
    | Gemini2_0FlashLite001  | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | ✓              | ✓     |

??? "OpenAI 模型"
    #### OpenAI 模型| 模型                    | 温度参数 | JSON 模式 | 补全 | 多项选择 | 工具 | 工具选择 | 视觉（图像） | 视觉（视频） | 音频 | 推测 | 审核 |
|--------------------------|-------------|-------------|------------|------------------|-------|-------------|----------------|----------------|-------|-------------|------------|
| Reasoning.O4Mini         | -           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | -              | -     | ✓           | -          |
| Reasoning.O3Mini         | -           | 完整        | ✓          | ✓                | ✓     | ✓           | -              | -              | -     | ✓           | -          |
| Reasoning.O3             | -           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | -              | -     | ✓           | -          |
| Reasoning.O1             | -           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | -              | -     | ✓           | -          |
| Chat.GPT4o               | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | -              | -     | ✓           | -          |
| Chat.GPT4_1              | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | -              | -     | ✓           | -          |
| Chat.GPT5                | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | -              | -     | ✓           | -          |
| Chat.GPT5Mini            | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | -              | -     | ✓           | -          |
| Chat.GPT5Nano            | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | -              | -     | ✓           | -          |
| Audio.GptAudio           | ✓           | -           | ✓          | -                | ✓     | ✓           | -              | -              | ✓     | -           | -          |
| Audio.GPT4oMiniAudio     | ✓           | -           | ✓          | -                | ✓     | ✓           | -              | -              | ✓     | -           | -          |
| Audio.GPT4oAudio         | ✓           | -           | ✓          | -                | ✓     | ✓           | -              | -              | ✓     | -           | -          |
| CostOptimized.GPT4_1Nano | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | -              | -     | ✓           | -          |
| CostOptimized.GPT4_1Mini | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | -              | -     | ✓           | -          |
| CostOptimized.GPT4oMini  | ✓           | 完整        | ✓          | ✓                | ✓     | ✓           | ✓              | -              | -     | ✓           | -          |
| Moderation.Omni          | -           | -           | -          | -                | -     | -           | ✓              | -              | -     | -           | ✓          |

??? "Anthropic 模型"
    #### Anthropic 模型    | 模型        | 温度        | JSON 模式 | 补全       | 工具   | 工具选择     | 视觉（图像）   |
    |------------|-------------|-------------|------------|-------|-------------|----------------|
    | Opus_4_6   | ✓           | -           | ✓          | ✓     | ✓           | ✓              |
    | Opus_4_5   | ✓           | -           | ✓          | ✓     | ✓           | ✓              |
    | Opus_4_1   | ✓           | -           | ✓          | ✓     | ✓           | ✓              |
    | Opus_4     | ✓           | -           | ✓          | ✓     | ✓           | ✓              |
    | Sonnet_4_5 | ✓           | -           | ✓          | ✓     | ✓           | ✓              |
    | Sonnet_4   | ✓           | -           | ✓          | ✓     | ✓           | ✓              |
    | Haiku_4_5  | ✓           | -           | ✓          | ✓     | ✓           | ✓              |
    | Haiku_3    | ✓           | -           | ✓          | ✓     | ✓           | ✓              |

??? "Ollama 模型"
    #### Ollama 模型

    ##### Meta 模型

    | 模型         | 温度        | JSON 模式 | 工具   | 审核       |
    |---------------|-------------|-------------|-------|------------|
    | LLAMA_3_2_3B  | ✓           | 简单        | ✓     | -          |
    | LLAMA_3_2     | ✓           | 简单        | ✓     | -          |
    | LLAMA_4       | ✓           | 简单        | ✓     | -          |
    | LLAMA_GUARD_3 | -           | -           | -     | ✓          |

    ##### 阿里巴巴模型

    | 模型              | 温度        | JSON 模式 | 工具   |
    |--------------------|-------------|-------------|-------|
    | QWEN_2_5_05B       | ✓           | 简单        | ✓     |
    | QWEN_3_06B         | ✓           | 简单        | ✓     |
    | QWQ                | ✓           | 简单        | ✓     |
    | QWEN_CODER_2_5_32B | ✓           | 简单        | ✓     |

    ##### Groq 模型

    | 模型                     | 温度        | JSON 模式 | 工具   |
    |---------------------------|-------------|-------------|-------|
    | LLAMA_3_GROK_TOOL_USE_8B  | ✓           | 完整        | ✓     |
    | LLAMA_3_GROK_TOOL_USE_70B | ✓           | 完整        | ✓     |

    ##### Granite 模型

    | 模型              | 温度        | JSON 模式 | 工具   | 视觉（图像）   |
    |--------------------|-------------|-------------|-------|----------------|
    | GRANITE_3_2_VISION | ✓           | 简单        | ✓     | ✓              |

??? "DeepSeek 模型"
    #### DeepSeek 模型

    | 模型            | 温度        | JSON 模式 | 补全       | 推测       | 工具   | 工具选择     | 视觉（图像）   |
    |------------------|-------------|-------------|------------|-------------|-------|-------------|----------------|
    | DeepSeekChat     | ✓           | 完整        | ✓          | -           | ✓     | ✓           | -              |
    | DeepSeekReasoner | ✓           | 完整        | ✓          | -           | ✓     | ✓           | -              |

??? "OpenRouter 模型"
    #### OpenRouter 模型| 模型               | 温度控制 | JSON 模式 | 补全 | 推测 | 工具 | 工具选择 | 视觉（图像） |
|---------------------|----------|-------------|------|------|------|----------|--------------|
| Phi4Reasoning       | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | -            |
| Claude3Opus         | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| Claude3Sonnet       | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| Claude3Haiku        | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| Claude3_5Sonnet     | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| Claude3_7Sonnet     | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| Claude4Sonnet       | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| Claude4_1Opus       | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| GPT4oMini           | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| GPT5                | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | -            |
| GPT5Mini            | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | -            |
| GPT5Nano            | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | -            |
| GPT_OSS_120b        | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | -            |
| GPT4                | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | -            |
| GPT4o               | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| GPT4Turbo           | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| GPT35Turbo          | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | -            |
| Llama3              | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | -            |
| Llama3Instruct      | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | -            |
| Mistral7B           | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | -            |
| Mixtral8x7B         | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | -            |
| Claude3VisionSonnet | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| Claude3VisionOpus   | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| Claude3VisionHaiku  | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| DeepSeekV30324      | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | -            |
| Gemini2_5FlashLite  | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| Gemini2_5Flash      | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |
| Gemini2_5Pro        | ✓        | 完整        | ✓    | ✓    | ✓    | ✓        | ✓            |