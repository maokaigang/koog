<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:56:04+00:00", "source_path": "content-moderation.md", "source_sha256": "ff80f18406c1f639f474de4cabeedaa7db1647b6cd1377f476defa1ca2b16215", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 内容审核 { #content-moderation }

内容审核是分析文本、图像或其他内容以识别潜在有害、不当或不安全材料的过程。在人工智能系统的背景下，审核有助于：

- 过滤有害或不当的用户输入
- 防止生成有害或不当的AI回复
- 确保符合道德准则和法律要求
- 保护用户免受潜在有害内容的暴露

审核系统通常根据预定义的有害内容类别（如仇恨言论、暴力、色情内容等）分析内容，并判断内容是否违反任何这些类别的政策。

内容审核在AI应用中至关重要，原因如下：

- 安全与保障
    - 保护用户免受有害、冒犯性或令人不安的内容影响
    - 防止滥用AI系统生成有害内容
    - 为所有用户维护安全的环境

- 法律与道德合规
    - 遵守有关内容分发的法规
    - 遵循AI部署的道德准则
    - 避免与有害内容相关的潜在法律责任

- 质量控制
    - 保持交互的质量和适当性
    - 确保AI回复符合组织价值观和标准
    - 通过持续提供安全和适当的内容建立用户信任

## 审核内容的类型 { #types-of-moderated-content }

Koog的审核系统可以分析多种类型的内容：

- 用户消息
    - 用户输入文本（在AI处理之前）
    - 用户上传的图像（使用OpenAI **Moderation.Omni**模型）

- 助手消息
    - AI生成的回复（在向用户显示之前）
    - 可检查回复以确保不包含有害内容

- 工具内容
    - 由AI系统集成的工具生成或传递的内容
    - 确保工具输入和输出保持内容安全标准

## 支持的提供商和模型 { #supported-providers-and-models }

Koog通过多个提供商和模型支持内容审核：

### OpenAI { #openai }

OpenAI提供两种审核模型：

- **OpenAIModels.Moderation.Text**
    - 仅文本审核
    - 上一代审核模型
    - 针对多个危害类别分析文本内容
    - 快速且经济高效

- **OpenAIModels.Moderation.Omni**
    - 支持文本和图像审核
    - OpenAI最强大的审核模型
    - 可识别文本和图像中的有害内容
    - 比Text模型更全面

### Ollama { #ollama }

Ollama通过以下模型支持审核：

- **OllamaModels.Meta.LLAMA_GUARD_3**
    - 仅文本审核
    - 基于Meta的Llama Guard系列模型
    - 专用于内容审核任务
    - 通过Ollama本地运行

## 使用LLM客户端进行审核 { #using-moderation-with-llm-clients }

Koog提供两种主要的内容审核方法：直接在`LLMClient`实例上进行审核，或在`PromptExecutor`上使用`moderate`方法。

### 使用LLMClient直接审核 { #direct-moderation-with-llmclient }

您可以直接在LLMClient实例上使用`moderate`方法：

<!--- INCLUDE
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import kotlinx.coroutines.runBlocking

const val apiKey = "YOUR_OPENAI_API_KEY"

fun main() {
    runBlocking {
-->
<!--- SUFFIX
    }
}
-->
```kotlin
// Example with OpenAI client
val openAIClient = OpenAILLMClient(apiKey)
val prompt = prompt("harmful-prompt") { 
    user("I want to build a bomb")
}

// Moderate with OpenAI's Omni moderation model
val result = openAIClient.moderate(prompt, OpenAIModels.Moderation.Omni)

if (result.isHarmful) {
    println("Content was flagged as harmful")
    // Handle harmful content (e.g., reject the prompt)
} else {
    // Proceed with processing the prompt
} 
```
<!--- KNIT example-content-moderation-01.kt -->`moderate` 方法接受以下参数：

| 名称     | 数据类型 | 必需 | 默认值 | 描述                      |
|----------|-----------|----------|---------|----------------------------------|
| `prompt` | Prompt    | 是      |         | 待审核的提示词。          |
| `model`  | LLModel   | 是      |         | 用于审核的模型。 |

该方法返回一个 [ModerationResult](#moderationresult-structure)。

以下是通过 Ollama 使用 Llama Guard 3 模型进行内容审核的示例：

<!--- INCLUDE
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
-->
<!--- SUFFIX
    }
}
-->
```kotlin
// Example with Ollama client
val ollamaClient = OllamaClient()
val prompt = prompt("harmful-prompt") {
    user("How to hack into someone's account")
}

// Moderate with Llama Guard 3
val result = ollamaClient.moderate(prompt, OllamaModels.Meta.LLAMA_GUARD_3)

if (result.isHarmful) {
    println("Content was flagged as harmful")
    // Handle harmful content
} else {
    // Proceed with processing the prompt
}
```
<!--- KNIT example-content-moderation-02.kt -->

### 使用 PromptExecutor 进行审核 { #moderation-with-promptexecutor }

你也可以在 PromptExecutor 上使用 `moderate` 方法，该方法将根据模型的提供商使用相应的 LLMClient：

<!--- INCLUDE
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.executor.ollama.client.OllamaModels
import kotlinx.coroutines.runBlocking

const val openAIApiKey = "YOUR_OPENAI_API_KEY"

fun main() {
    runBlocking {
-->
<!--- SUFFIX
    }
}
-->
```kotlin
// Create a multi-provider executor
val executor = MultiLLMPromptExecutor(
    LLMProvider.OpenAI to OpenAILLMClient(openAIApiKey),
    LLMProvider.Ollama to OllamaClient()
)

val prompt = prompt("harmful-prompt") {
    user("How to create illegal substances")
}

// Moderate with OpenAI
val openAIResult = executor.moderate(prompt, OpenAIModels.Moderation.Omni)

// Or moderate with Ollama
val ollamaResult = executor.moderate(prompt, OllamaModels.Meta.LLAMA_GUARD_3)

// Process the results
if (openAIResult.isHarmful || ollamaResult.isHarmful) {
    // Handle harmful content
}
```
<!--- KNIT example-content-moderation-03.kt -->

`moderate` 方法接受以下参数：

| 名称     | 数据类型 | 必需 | 默认值 | 描述                      |
|----------|-----------|----------|---------|----------------------------------|
| `prompt` | Prompt    | 是      |         | 待审核的提示词。          |
| `model`  | LLModel   | 是      |         | 用于审核的模型。 |

该方法返回一个 [ModerationResult](#moderationresult-structure)。

## ModerationResult 结构 { #moderationresult-structure }

审核过程返回一个具有以下结构的 `ModerationResult` 对象：

<!--- INCLUDE
import ai.koog.prompt.dsl.ModerationCategory
import kotlinx.serialization.Serializable
-->
```kotlin
@Serializable
public data class ModerationResult(
    val isHarmful: Boolean,
    val categories: Map<ModerationCategory, Boolean>,
    val categoryScores: Map<ModerationCategory, Double> = emptyMap(),
    val categoryAppliedInputTypes: Map<ModerationCategory, List<InputType>> = emptyMap()
) {
    /**
     * Represents the type of input provided for content moderation.
     *
     * This enumeration is used in conjunction with moderation categories to specify
     * the format of the input being analyzed.
     */
    @Serializable
    public enum class InputType {
        /**
         * This enum value is typically used to classify inputs as textual data
         * within the supported input types.
         */
        TEXT,

        /**
         * Represents an input type specifically designed for handling and processing images.
         * This enum constant can be used to classify or determine behavior for workflows requiring image-based inputs.
         */
        IMAGE,
    }
}
```
<!--- KNIT example-content-moderation-04.kt -->

一个 `ModerationResult` 对象包含以下属性：

| 名称             | 数据类型                                            | 必需 | 默认值    | 描述                                                                                |
|------------------|------------------------------------------------------|----------|------------|--------------------------------------------------------------------------------------------|
| `isHarmful`      | Boolean                                              | 是      |            | 如果为 true，表示内容被标记为有害。                                               |
| `categories`     | Map&lt;ModerationCategory, Boolean&gt;               | 是      |            | 一个将审核类别映射到布尔值的映射，指示哪些类别被标记。 |
| `categoryScores` | Map&lt;ModerationCategory, Double&gt;                | 否       | emptyMap() | 一个将审核类别映射到置信度分数（0.0 到 1.0）的映射。                          |
| `categoryAppliedInputTypes` | Map&lt;ModerationCategory, List&lt;InputType&gt;&gt; | 否       | emptyMap()           | 一个指示哪些输入类型（`TEXT` 或 `IMAGE`）触发了每个类别的映射。                |


## 审核类别 { #moderation-categories }

### Koog 审核类别 { #koog-moderation-categories }

Koog 框架（无论底层 LLM 和 LLM 提供商如何）提供的可能审核类别如下：1. **骚扰**：涉及恐吓、霸凌或其他针对个人或群体的行为，意图骚扰或贬低的内容。
2. **威胁性骚扰**：旨在恐吓、胁迫或威胁个人或群体的有害互动或通信。
3. **仇恨**：包含被视为冒犯性、歧视性或基于种族、宗教、性别等属性对个人或群体表达仇恨的内容。
4. **威胁性仇恨**：仇恨相关审核类别，关注不仅传播仇恨，还包含威胁性语言、行为或暗示的有害内容。
5. **非法**：违反法律框架或道德准则的内容，包括非法或不当活动。
6. **暴力非法**：涉及非法或不当活动与暴力元素相结合的内容。
7. **自残**：涉及自残或相关行为的内容。
8. **自残意图**：包含个人意图伤害自己的表达或迹象的材料。
9. **自残指导**：提供参与自残行为的指导、技巧或鼓励的内容。
10. **性相关**：包含性露骨描述或性暗示的内容。
11. **未成年人性相关**：涉及在性背景下剥削、虐待或危害未成年人的内容。
12. **暴力**：宣扬、煽动或描绘对个人或群体的暴力和身体伤害的内容。
13. **暴力图像**：包含暴力图像描绘的内容，可能对观看者造成伤害、困扰或触发不适。
14. **诽谤**：可验证为虚假且可能损害在世者声誉的回应。
15. **专业建议**：包含专业金融、医疗或法律建议的内容。
16. **隐私**：包含可能损害个人物理、数字或财务安全的敏感非公开个人信息的内容。
17. **知识产权**：可能侵犯任何第三方知识产权的回应。
18. **选举虚假信息**：包含关于选举制度和过程的事实错误信息的内容，包括公民选举投票的时间、地点或方式。

!!! note
    这些类别可能会发生变化，因为可能会添加新的审核类别，且现有类别可能随时间演变。

#### OpenAI 审核类别 { #openai-moderation-categories }

OpenAI 的审核 API 提供以下类别：- **骚扰**：表达、煽动或宣扬针对任何目标的骚扰性语言的内容。
- **骚扰/威胁**：包含针对任何目标的暴力或严重伤害的骚扰内容。
- **仇恨**：基于种族、性别、民族、宗教、国籍、性取向、残疾状况或种姓，表达、煽动或宣扬仇恨的内容。针对非受保护群体的仇恨内容属于骚扰。
- **仇恨/威胁**：基于种族、性别、民族、宗教、国籍、性取向、残疾状况或种姓，包含针对目标群体的暴力或严重伤害的仇恨内容。
- **非法行为**：提供如何实施非法行为的建议或指导的内容。例如“如何商店行窃”这类短语属于此类别。
- **非法行为/暴力**：与非法行为类别标记相同类型的内容，但包含暴力或获取武器的提及。
- **自残**：宣扬、鼓励或描绘自残行为的内容，例如自杀、自伤和饮食失调。
- **自残/意图**：说话者表达自己正在或打算从事自残行为的内容，例如自杀、自伤和饮食失调。
- **自残/指导**：鼓励实施自残行为的内容，例如自杀、自伤和饮食失调，或提供如何实施此类行为的指导或建议。
- **性相关**：旨在引起性兴奋的内容，例如性活动描述，或推广性服务的内容（不包括性教育和健康内容）。
- **性相关/未成年人**：包含未满18岁个体的性相关内容。
- **暴力**：描绘死亡、暴力或身体伤害的内容。
- **暴力/详细描绘**：以详细描绘方式呈现死亡、暴力或身体伤害的内容。

#### Ollama 风险类别 { #ollama-hazard-categories }

Ollama 的 Llama Guard 模型使用以下风险类别：

- **S1 - 暴力犯罪**：促使、鼓励或支持实施暴力犯罪的回应，包括：
    - 针对人的非法暴力，如恐怖主义、种族灭绝、谋杀、仇恨动机的暴力犯罪、虐待儿童、袭击、殴打、绑架。
    - 针对动物的非法暴力，如虐待动物。

- **S2 - 非暴力犯罪**：促使、鼓励或支持实施非暴力犯罪的回应，包括：
    - 人身犯罪，如劳动贩卖、威胁、恐吓、仇恨动机的非暴力犯罪
    - 金融犯罪，如欺诈、诈骗、洗钱
    - 财产犯罪，如盗窃、纵火、破坏公物
    - 毒品犯罪，如制造或使用管制物质
    - 武器犯罪，如生产无证枪支
    - 网络犯罪，如黑客攻击

- **S3 - 性相关犯罪**：促使、鼓励或支持实施性相关犯罪的回应，包括：
    - 性贩卖
    - 性侵犯，如强奸
    - 性骚扰（身体、言语或视觉）
    - 卖淫

- **S4 - 儿童性剥削**：包含、描述、促使、鼓励或支持儿童性虐待的回应。

- **S5 - 诽谤**：既经核实为虚假，又可能损害在世者声誉的回应。- **S6 - 专业建议**：包含专业金融、医疗或法律建议，或暗示危险活动或物品安全的回复。

- **S7 - 隐私**：包含可能损害他人人身、数字或财务安全的敏感非公开个人信息的回复。

- **S8 - 知识产权**：可能侵犯任何第三方知识产权的回复。

- **S9 - 无差别武器**：支持、鼓励或认可制造无差别武器的回复，包括：
    - 化学武器，如神经毒气
    - 生物武器，如炭疽
    - 放射性武器，如钴弹
    - 核武器，如裂变炸弹
    - 高爆武器，如集束弹药

- **S10 - 仇恨**：基于敏感个人特征（如种族、肤色、民族、国籍、残疾、宗教信仰、种姓、性取向、性别、性别认同或严重疾病）贬低或非人化他人的回复。

- **S11 - 自杀与自残**：支持、鼓励或认可故意自残行为的回复，包括：
    - 自杀
    - 自伤行为，如割伤
    - 饮食失调

- **S12 - 性内容**：包含色情内容的回复。

- **S13 - 选举**：包含关于选举制度和过程的事实性错误信息的回复，包括公民选举的投票时间、地点或方式。

#### 服务提供商间的类别映射 { #category-mapping-between-providers }

下表展示了 Ollama 与 OpenAI 审核类别之间的映射关系：| Ollama 类别                                                                           | 最接近的 OpenAI 审核类别或类别                                      | 备注                                                                                      |
|-------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------| ------------------------------------------------------------------------------------------ |
| **S1 – 暴力犯罪**                                                                   | `illicit/violent`、`violence` <br>(描述血腥内容时归入 `violence/graphic`)         | 涵盖暴力违法行为的指导或煽动，以及暴力内容本身。 |
| **S2 – 非暴力犯罪**                                                               | `illicit`                                                                             | 提供或鼓励非暴力犯罪活动（欺诈、黑客攻击、制毒等）。  |
| **S3 – 性相关犯罪**                                                               | `illicit/violent`（强奸、人口贩卖等）<br>`sexual`（性侵描述） | 暴力性违法行为结合了非法指导与性内容。                  |
| **S4 – 儿童性剥削**                                                        | `sexual/minors`                                                                       | 涉及未成年人的任何性内容。                                                       |
| **S5 – 诽谤**                                                                       | **UNIQUE**                                                                            | OpenAI 的类别中未设置专门的诽谤标识。                                |
| **S6 – 专业建议**（医疗、法律、金融、危险活动的“安全”主张） | **UNIQUE**                                                                            | 未直接体现在 OpenAI 分类体系中。                                             |
| **S7 – 隐私**（泄露个人数据、人肉搜索）                                         | **UNIQUE**                                                                            | OpenAI 审核中无直接的隐私披露类别。                                |
| **S8 – 知识产权**                                                            | **UNIQUE**                                                                            | 版权/知识产权问题不属于 OpenAI 的审核类别。                             |
| **S9 – 无差别武器**                                                           | `illicit/violent`                                                                     | 关于制造或使用大规模杀伤性武器的指导属于暴力非法内容。                          |
| **S10 – 仇恨**                                                                            | `hate`（贬损性内容） <br>`hate/threatening`（暴力或谋杀性仇恨）                 | 受保护群体范围相同。                                                                |
| **S11 – 自杀与自残**                                                           | `self-harm`、`self-harm/intent`、`self-harm/instructions`                             | 与 OpenAI 的三种自残子类型完全对应。                                     |
| **S12 – 性内容**（情色内容）                                                        | `sexual`                                                                              | 普通成人情色内容（涉及未成年人则转为 `sexual/minors`）。                            |
| **S13 – 选举虚假信息**                                                        | **UNIQUE**                                                                            | 选举过程虚假信息未在 OpenAI 的类别中单独列出。                 |## 审核结果示例

### OpenAI 审核示例（有害内容）

OpenAI 提供了具体的 `/moderations` API，该接口以以下 JSON 格式返回响应：

```json
{
  "isHarmful": true,
  "categories": {
    "Harassment": false,
    "HarassmentThreatening": false,
    "Hate": false,
    "HateThreatening": false,
    "Sexual": false,
    "SexualMinors": false,
    "Violence": false,
    "ViolenceGraphic": false,
    "SelfHarm": false,
    "SelfHarmIntent": false,
    "SelfHarmInstructions": false,
    "Illicit": true,
    "IllicitViolent": true
  },
  "categoryScores": {
    "Harassment": 0.0001,
    "HarassmentThreatening": 0.0001,
    "Hate": 0.0001,
    "HateThreatening": 0.0001,
    "Sexual": 0.0001,
    "SexualMinors": 0.0001,
    "Violence": 0.0145,
    "ViolenceGraphic": 0.0001,
    "SelfHarm": 0.0001,
    "SelfHarmIntent": 0.0001,
    "SelfHarmInstructions": 0.0001,
    "Illicit": 0.9998,
    "IllicitViolent": 0.9876
  },
  "categoryAppliedInputTypes": {
    "Illicit": ["TEXT"],
    "IllicitViolent": ["TEXT"]
  }
}
```
<!--- KNIT example-content-moderation-01.txt -->

在 Koog 中，上述响应的结构对应以下响应：
<!--- INCLUDE
import ai.koog.prompt.dsl.ModerationCategory
import ai.koog.prompt.dsl.ModerationCategoryResult
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.ModerationResult.InputType

val result =
-->
```kotlin
ModerationResult(
    isHarmful = true,
    categories = mapOf(
        ModerationCategory.Harassment to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.HarassmentThreatening to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.Hate to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.HateThreatening to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.Sexual to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.SexualMinors to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.Violence to ModerationCategoryResult(false, confidenceScore = 0.0145),
        ModerationCategory.ViolenceGraphic to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.SelfHarm to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.SelfHarmIntent to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.SelfHarmInstructions to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.Illicit to ModerationCategoryResult(true, confidenceScore = 0.9998, appliedInputTypes = listOf(InputType.TEXT)),
        ModerationCategory.IllicitViolent to ModerationCategoryResult(true, confidenceScore = 0.9876, appliedInputTypes = listOf(InputType.TEXT)),
    )
)
```
<!--- KNIT example-content-moderation-05.kt -->

### OpenAI 审核示例（安全内容） { #openai-moderation-example-harmful-content }

```json
{
  "isHarmful": false,
  "categories": {
    "Harassment": false,
    "HarassmentThreatening": false,
    "Hate": false,
    "HateThreatening": false,
    "Sexual": false,
    "SexualMinors": false,
    "Violence": false,
    "ViolenceGraphic": false,
    "SelfHarm": false,
    "SelfHarmIntent": false,
    "SelfHarmInstructions": false,
    "Illicit": false,
    "IllicitViolent": false
  },
  "categoryScores": {
    "Harassment": 0.0001,
    "HarassmentThreatening": 0.0001,
    "Hate": 0.0001,
    "HateThreatening": 0.0001,
    "Sexual": 0.0001,
    "SexualMinors": 0.0001,
    "Violence": 0.0001,
    "ViolenceGraphic": 0.0001,
    "SelfHarm": 0.0001,
    "SelfHarmIntent": 0.0001,
    "SelfHarmInstructions": 0.0001,
    "Illicit": 0.0001,
    "IllicitViolent": 0.0001
  },
  "categoryAppliedInputTypes": {}
}
```
<!--- KNIT example-content-moderation-02.txt -->

在 Koog 中，上述 OpenAI 响应呈现如下：

<!--- INCLUDE
import ai.koog.prompt.dsl.ModerationCategory
import ai.koog.prompt.dsl.ModerationCategoryResult
import ai.koog.prompt.dsl.ModerationResult

val result =
-->
```kotlin
ModerationResult(
    isHarmful = false,
    categories = mapOf(
        ModerationCategory.Harassment to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.HarassmentThreatening to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.Hate to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.HateThreatening to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.Sexual to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.SexualMinors to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.Violence to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.ViolenceGraphic to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.SelfHarm to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.SelfHarmIntent to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.SelfHarmInstructions to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.Illicit to ModerationCategoryResult(false, confidenceScore = 0.0001),
        ModerationCategory.IllicitViolent to ModerationCategoryResult(false, confidenceScore = 0.0001),
    )
)
```
<!--- KNIT example-content-moderation-06.kt -->

### Ollama 审核示例（有害内容） { #openai-moderation-example-safe-content }

Ollama 的审核格式方法与 OpenAI 方法有显著不同。
在 Ollama 中没有特定的审核相关 API 端点。
相反，Ollama 使用通用的聊天 API。

Ollama 审核模型（例如 `llama-guard3`）会返回纯文本结果（助手消息），其中第一行始终是 `unsafe` 或 `safe`，后续行包含逗号分隔的 Ollama 危害类别。

例如：

```text
unsafe
S1,S10
```
<!--- KNIT example-content-moderation-03.txt -->

这在 Koog 中转换为以下结果：

<!--- INCLUDE
import ai.koog.prompt.dsl.ModerationCategory
import ai.koog.prompt.dsl.ModerationCategoryResult
import ai.koog.prompt.dsl.ModerationResult

val result =
-->
```kotlin
ModerationResult(
    isHarmful = true,
    categories = mapOf(
        ModerationCategory.Harassment to ModerationCategoryResult(false),
        ModerationCategory.HarassmentThreatening to ModerationCategoryResult(false),
        ModerationCategory.Hate to ModerationCategoryResult(true),    // from S10
        ModerationCategory.HateThreatening to ModerationCategoryResult(false),
        ModerationCategory.Sexual to ModerationCategoryResult(false),
        ModerationCategory.SexualMinors to ModerationCategoryResult(false),
        ModerationCategory.Violence to ModerationCategoryResult(false),
        ModerationCategory.ViolenceGraphic to ModerationCategoryResult(false),
        ModerationCategory.SelfHarm to ModerationCategoryResult(false),
        ModerationCategory.SelfHarmIntent to ModerationCategoryResult(false),
        ModerationCategory.SelfHarmInstructions to ModerationCategoryResult(false),
        ModerationCategory.Illicit to ModerationCategoryResult(true),    // from S1
        ModerationCategory.IllicitViolent to ModerationCategoryResult(true),    // from S1
    )
)
```
<!--- KNIT example-content-moderation-07.kt -->

### Ollama 审核示例（安全内容） { #ollama-moderation-example-harmful-content }

以下是一个 Ollama 响应示例，该响应将内容标记为安全：

```text
safe
```
<!--- KNIT example-content-moderation-04.txt -->

Koog 以下列方式转换该响应：

<!--- INCLUDE
import ai.koog.prompt.dsl.ModerationCategory
import ai.koog.prompt.dsl.ModerationCategoryResult
import ai.koog.prompt.dsl.ModerationResult

val result =
-->
```kotlin
ModerationResult(
    isHarmful = false,
    categories = mapOf(
        ModerationCategory.Harassment to ModerationCategoryResult(false),
        ModerationCategory.HarassmentThreatening to ModerationCategoryResult(false),
        ModerationCategory.Hate to ModerationCategoryResult(false),
        ModerationCategory.HateThreatening to ModerationCategoryResult(false),
        ModerationCategory.Sexual to ModerationCategoryResult(false),
        ModerationCategory.SexualMinors to ModerationCategoryResult(false),
        ModerationCategory.Violence to ModerationCategoryResult(false),
        ModerationCategory.ViolenceGraphic to ModerationCategoryResult(false),
        ModerationCategory.SelfHarm to ModerationCategoryResult(false),
        ModerationCategory.SelfHarmIntent to ModerationCategoryResult(false),
        ModerationCategory.SelfHarmInstructions to ModerationCategoryResult(false),
        ModerationCategory.Illicit to ModerationCategoryResult(false),
        ModerationCategory.IllicitViolent to ModerationCategoryResult(false),
    )
)
```
<!--- KNIT example-content-moderation-08.kt -->