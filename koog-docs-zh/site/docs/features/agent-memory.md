<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:00:07+00:00", "source_path": "features/agent-memory.md", "source_sha256": "0defbe298c4d1c2b89ed842fdfb7dddabf29434b056d779f258f42f3267a25d2", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 智能体记忆 { #agent-memory }

## 功能概述 { #feature-overview }

AgentMemory 功能是 Koog 框架的一个组件，它允许 AI 智能体在对话中存储、检索和使用信息。

### 目的 { #purpose }

AgentMemory 功能通过以下方式解决 AI 智能体交互中保持上下文连贯性的挑战：

- 存储从对话中提取的重要事实。
- 按概念、主题和范围组织信息。
- 在未来的交互中按需检索相关信息。
- 基于用户偏好和历史记录实现个性化。

### 架构 { #architecture }

AgentMemory 功能建立在分层结构之上。该结构的元素在以下部分列出并解释。

#### 事实 { #facts }

***事实*** 是存储在记忆中的独立信息片段。事实代表实际存储的信息。事实有两种类型：

- **SingleFact**：与概念关联的单个值。例如，IDE 用户当前偏好的主题：
<!--- INCLUDE
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.SingleFact
import kotlin.time.Clock
-->
```kotlin
// Storing favorite IDE theme (single value)
val themeFact = SingleFact(
    concept = Concept(
        "ide-theme", 
        "User's preferred IDE theme", 
        factType = FactType.SINGLE),
    value = "Dark Theme",
    timestamp = Clock.System.now().toEpochMilliseconds(),
)
```
<!--- KNIT example-agent-memory-01.kt -->
- **MultipleFacts**：与概念关联的多个值。例如，用户掌握的所有语言：
<!--- INCLUDE
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MultipleFacts
import kotlin.time.Clock
-->
```kotlin
// Storing programming languages (multiple values)
val languagesFact = MultipleFacts(
    concept = Concept(
        "programming-languages",
        "Languages the user knows",
        factType = FactType.MULTIPLE
    ),
    values = listOf("Kotlin", "Java", "Python"),
    timestamp = Clock.System.now().toEpochMilliseconds(),
)
```
<!--- KNIT example-agent-memory-02.kt -->

#### 概念 { #concepts }

***概念*** 是具有关联元数据的信息类别。

- **Keyword**：概念的唯一标识符。
- **Description**：对概念所代表内容的详细说明。
- **FactType**：概念存储的是单个事实还是多个事实（`FactType.SINGLE` 或 `FactType.MULTIPLE`）。

#### 主题 { #subjects }

***主题*** 是事实可以关联的实体。

主题的常见示例包括：

- **User**：个人偏好和设置
- **Environment**：与应用程序环境相关的信息

有一个预定义的 `MemorySubject.Everything`，您可以用作所有事实的默认主题。此外，您可以通过扩展 `MemorySubject` 抽象类来定义自己的自定义记忆主题：

<!--- INCLUDE
import ai.koog.agents.memory.model.MemorySubject
import kotlinx.serialization.Serializable
-->
```kotlin
object MemorySubjects {
    /**
     * Information specific to the local machine environment
     * Examples: Installed tools, SDKs, OS configuration, available commands
     */
    @Serializable
    data object Machine : MemorySubject() {
        override val name: String = "machine"
        override val promptDescription: String =
            "Technical environment (installed tools, package managers, packages, SDKs, OS, etc.)"
        override val priorityLevel: Int = 1
    }

    /**
     * Information specific to the user
     * Examples: Conversation preferences, issue history, contact information
     */
    @Serializable
    data object User : MemorySubject() {
        override val name: String = "user"
        override val promptDescription: String =
            "User information (conversation preferences, issue history, contact details, etc.)"
        override val priorityLevel: Int = 1
    }
}
```
<!--- KNIT example-agent-memory-03.kt -->

#### 范围 { #scopes }

***记忆范围*** 是事实相关的上下文：

- **Agent**：特定于某个智能体。
- **Feature**：特定于某个功能。
- **Product**：特定于某个产品。
- **CrossProduct**：跨多个产品相关。

## 配置与初始化 { #configuration-and-initialization }

该功能通过 `AgentMemory` 类与智能体管道集成，该类提供了保存和加载事实的方法，并且可以作为功能安装在智能体配置中。

### 配置 { #configuration }

`AgentMemory.Config` 类是 AgentMemory 功能的配置类。

<!--- INCLUDE
import ai.koog.agents.core.feature.config.FeatureConfig
import ai.koog.agents.memory.config.MemoryScopesProfile
import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.agents.memory.providers.NoMemory
-->
```kotlin
class Config(
    var memoryProvider: AgentMemoryProvider = NoMemory,
    var scopesProfile: MemoryScopesProfile = MemoryScopesProfile(),

    var agentName: String,
    var featureName: String,
    var organizationName: String,
    var productName: String
) : FeatureConfig()
```
<!--- KNIT example-agent-memory-04.kt -->

### 安装 { #installation }

要在智能体中安装 AgentMemory 功能，请遵循以下代码示例提供的模式。

<!--- INCLUDE
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.agents.example.exampleAgentMemory06.memoryProvider
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.ollama.client.OllamaModels
-->
```kotlin
val agent = AIAgent(
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2,
) {
    install(AgentMemory) {
        memoryProvider = memoryProvider
        agentName = "your-agent-name"
        featureName = "your-feature-name"
        organizationName = "your-organization-name"
        productName = "your-product-name"
    }
}
```
<!--- KNIT example-agent-memory-05.kt -->

## 示例与快速入门 { #examples-and-quickstarts }

### 基本用法 { #basic-usage }

以下代码片段演示了记忆存储的基本设置，以及如何将事实保存到记忆中和从记忆中加载事实。

1) 设置记忆存储
<!--- INCLUDE
import ai.koog.agents.memory.providers.LocalFileMemoryProvider
import ai.koog.agents.memory.providers.LocalMemoryConfig
import ai.koog.agents.memory.storage.SimpleStorage
import ai.koog.rag.base.files.JVMFileSystemProvider
import kotlin.io.path.Path
-->
```kotlin
// Create a memory provider
val memoryProvider = LocalFileMemoryProvider(
    config = LocalMemoryConfig("customer-support-memory"),
    storage = SimpleStorage(JVMFileSystemProvider.ReadWrite),
    fs = JVMFileSystemProvider.ReadWrite,
    root = Path("path/to/memory/root")
)
```
<!--- KNIT example-agent-memory-06.kt -->

2) 将事实存储到记忆中
<!--- INCLUDE
import ai.koog.agents.example.exampleAgentMemory03.MemorySubjects
import ai.koog.agents.example.exampleAgentMemory06.memoryProvider
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.SingleFact
import kotlin.time.Clock

suspend fun main() {
-->
<!--- SUFFIX
}
-->
```kotlin
memoryProvider.save(
    fact = SingleFact(
        concept = Concept("greeting", "User's name", FactType.SINGLE),
        value = "John",
        timestamp = Clock.System.now().toEpochMilliseconds(),
    ),
    subject = MemorySubjects.User,
    scope = MemoryScope.Product("my-app"),
)
```
<!--- KNIT example-agent-memory-07.kt -->

3) 检索事实
<!--- INCLUDE
import ai.koog.agents.example.exampleAgentMemory03.MemorySubjects
import ai.koog.agents.example.exampleAgentMemory06.memoryProvider
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemoryScope

suspend fun main() {
-->
<!--- SUFFIX
}
-->
```kotlin
// Get the stored information
val greeting = memoryProvider.load(
    concept = Concept("greeting", "User's name", FactType.SINGLE),
    subject = MemorySubjects.User,
    scope = MemoryScope.Product("my-app")
)
if (greeting.size > 1) {
    println("Memories found: ${greeting.joinToString(", ")}")
} else {
    println("Information not found. First time here?")
}
```
<!--- KNIT example-agent-memory-08.kt -->

#### 使用记忆节点AgentMemory 功能提供了以下预定义记忆节点，可用于智能体策略： { #using-memory-nodes }

* [nodeLoadAllFactsFromMemory](api:agents-features-memory::ai.koog.agents.memory.feature.nodes.nodeLoadAllFactsFromMemory)：从记忆中加载给定概念下关于主体的所有事实。
* [nodeLoadFromMemory](api:agents-features-memory::ai.koog.agents.memory.feature.nodes.nodeLoadFromMemory)：从记忆中加载给定概念下的特定事实。
* [nodeSaveToMemory](api:agents-features-memory::ai.koog.agents.memory.feature.nodes.nodeSaveToMemory)：将事实保存到记忆中。
* [nodeSaveToMemoryAutoDetectFacts](api:agents-features-memory::ai.koog.agents.memory.feature.nodes.nodeSaveToMemoryAutoDetectFacts)：自动从聊天历史中检测并提取事实，然后将其保存到记忆中。使用 LLM 来识别概念。

以下是一个如何在智能体策略中实现节点的示例：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.subgraph
import ai.koog.agents.example.exampleAgentMemory03.MemorySubjects
import ai.koog.agents.memory.feature.nodes.nodeSaveToMemoryAutoDetectFacts
import ai.koog.agents.memory.feature.withMemory
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
-->
```kotlin
val strategy = strategy("example-agent") {
    // Node to automatically detect and save facts
    val detectFacts by nodeSaveToMemoryAutoDetectFacts<Unit>(
        subjects = listOf(MemorySubjects.User, MemorySubjects.Machine)
    )

    // Node to load specific facts
    val loadPreferences by node<Unit, Unit> {
        withMemory {
            loadFactsToAgent(
                llm = llm,
                concept = Concept("user-preference", "User's preferred programming language", FactType.SINGLE),
                subjects = listOf(MemorySubjects.User)
            )
        }
    }

    // Connect nodes in the strategy
    edge(nodeStart forwardTo detectFacts)
    edge(detectFacts forwardTo loadPreferences)
    edge(loadPreferences forwardTo nodeFinish)
}
```
<!--- KNIT example-agent-memory-09.kt -->


#### 确保记忆安全 { #making-memory-secure }

您可以使用加密技术来确保敏感信息在记忆提供者使用的加密存储中得到保护。

<!--- INCLUDE
import ai.koog.agents.memory.storage.EncryptedStorage
import ai.koog.rag.base.files.JVMFileSystemProvider
import ai.koog.agents.memory.storage.Aes256GCMEncryptor
-->
```kotlin
// Simple encrypted storage setup
val secureStorage = EncryptedStorage(
    fs = JVMFileSystemProvider.ReadWrite,
    encryption = Aes256GCMEncryptor("your-secret-key")
)
```
<!--- KNIT example-agent-memory-10.kt -->

#### 示例：记住用户偏好 { #example-remembering-user-preferences }

以下是一个在实际场景中如何使用 AgentMemory 来记住用户偏好的示例，具体是记住用户最喜欢的编程语言。

<!--- INCLUDE
import ai.koog.agents.example.exampleAgentMemory03.MemorySubjects
import ai.koog.agents.example.exampleAgentMemory06.memoryProvider
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.SingleFact
import kotlin.time.Clock

suspend fun main() {
-->
<!--- SUFFIX
}
-->
```kotlin
memoryProvider.save(
    fact = SingleFact(
        concept = Concept("preferred-language", "What programming language is preferred by the user?", FactType.SINGLE),
        value = "Kotlin",
        timestamp = Clock.System.now().toEpochMilliseconds(),
    ),
    subject = MemorySubjects.User,
    scope = MemoryScope.Product("my-app")
)
```
<!--- KNIT example-agent-memory-11.kt -->

### 高级用法 { #advanced-usage }

#### 使用记忆的自定义节点 { #custom-nodes-with-memory }

您也可以在任意节点中使用来自 `withMemory` 子句的记忆。现成的 `loadFactsToAgent` 和 `saveFactsFromHistory` 高级抽象将事实保存到历史记录、从中加载事实，并更新 LLM 聊天：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.subgraph
import ai.koog.agents.example.exampleAgentMemory03.MemorySubjects
import ai.koog.agents.memory.feature.withMemory
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemoryScope

fun main() {
    val strategy = strategy<Unit, Unit>("example-agent") {
-->
<!--- SUFFIX
    }
}
-->
```kotlin
val loadProjectInfo by node<Unit, Unit> {
    withMemory {
        loadFactsToAgent(
            llm = llm,
            concept = Concept("preferred-language", "What programming language is preferred by the user?", FactType.SINGLE)
        )
    }
}

val saveProjectInfo by node<Unit, Unit> {
    withMemory {
        saveFactsFromHistory(
            llm = llm,
            concept = Concept("preferred-language", "What programming language is preferred by the user?", FactType.SINGLE),
            subject = MemorySubjects.User,
            scope = MemoryScope.Product("my-app")
        )
    }
}
```
<!--- KNIT example-agent-memory-12.kt -->

#### 自动事实检测 { #automatic-fact-detection }

您还可以要求 LLM 使用 `nodeSaveToMemoryAutoDetectFacts` 方法从智能体的历史记录中检测所有事实：

<!--- INCLUDE
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.subgraph
import ai.koog.agents.example.exampleAgentMemory03.MemorySubjects
import ai.koog.agents.memory.feature.nodes.nodeSaveToMemoryAutoDetectFacts

fun main() {
    val strategy = strategy<Unit, Unit>("example-agent") {

-->
<!--- SUFFIX
    }
}
-->
```kotlin
val saveAutoDetect by nodeSaveToMemoryAutoDetectFacts<Unit>(
    subjects = listOf(MemorySubjects.User, MemorySubjects.Machine)
)
```
<!--- KNIT example-agent-memory-13.kt -->

在上面的示例中，LLM 将搜索与用户相关的事实和与项目相关的事实，确定概念，并将其保存到记忆中。

## 最佳实践 { #best-practices }

1. **从简单开始**
    - 首先使用无加密的基本存储
    - 先使用单一事实，再过渡到多个事实

2. **良好组织**
    - 使用清晰的概念名称
    - 添加有用的描述
    - 将相关信息保存在同一主体下

3. **处理错误**
<!--- INCLUDE
import ai.koog.agents.example.exampleAgentMemory03.MemorySubjects
import ai.koog.agents.example.exampleAgentMemory06.memoryProvider
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.SingleFact
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

fun main() {
    runBlocking {
        val fact = SingleFact(
            concept = Concept("preferred-language", "What programming language is preferred by the user?", FactType.SINGLE),
            value = "Kotlin",
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        val subject = MemorySubjects.User
        val scope = MemoryScope.Product("my-app")
-->
<!--- SUFFIX
    }
}
-->
```kotlin
try {
    memoryProvider.save(fact, subject, scope)
} catch (e: Exception) {
    println("Oops! Couldn't save: ${e.message}")
}
```
<!--- KNIT example-agent-memory-14.kt -->

有关错误处理的更多详细信息，请参阅 [错误处理和边界情况](#error-handling-and-edge-cases)。

## 错误处理和边界情况 { #error-handling-and-edge-cases }

AgentMemory 功能包含多种处理边界情况的机制：

1. **NoMemory 提供者**：一个默认实现，当未指定记忆提供者时不存储任何内容。

2. **主体特异性处理**：加载事实时，该功能会根据定义的 `priorityLevel` 优先处理来自更具体主体的事实。

3. **范围过滤**：可以按范围过滤事实，以确保仅加载相关信息。

4. **时间戳跟踪**：事实存储时带有时间戳，以跟踪其创建时间。

5. **事实类型处理**：该功能支持单一事实和多个事实，并对每种类型进行适当处理。

## API 文档有关 AgentMemory 功能的完整 API 参考，请参阅 [agents-features-memory](api:agents-features-memory::) 模块的参考文档。 { #api-documentation }

特定包的 API 文档：

- [ai.koog.agents.local.memory.feature](api:agents-features-memory::ai.koog.agents.memory.feature)：包含 `AgentMemory` 类以及 AI 代理记忆功能的核心实现。
- [ai.koog.agents.local.memory.feature.nodes](api:agents-features-memory::ai.koog.agents.memory.feature.nodes)：包含可在子图中使用的预定义记忆相关节点。
- [ai.koog.agents.local.memory.config](api:agents-features-memory::ai.koog.agents.memory.config)：提供用于记忆操作的内存范围定义。
- [ai.koog.agents.local.memory.model](api:agents-features-memory::ai.koog.agents.memory.model)：包含核心数据结构和接口的定义，使代理能够在不同上下文和时间段内存储、组织和检索信息。
- [ai.koog.agents.local.memory.feature.history](api:agents-features-memory::ai.koog.agents.memory.feature.history)：提供历史压缩策略，用于从过去的会话活动或存储的记忆中检索和整合关于特定概念的事实知识。
- [ai.koog.agents.local.memory.providers](api:agents-features-memory::ai.koog.agents.memory.providers)：提供核心接口，该接口定义了以结构化、上下文感知的方式存储和检索知识的基本操作及其实现。
- [ai.koog.agents.local.memory.storage](api:agents-features-memory::ai.koog.agents.memory.storage)：提供核心接口以及针对不同平台和存储后端的文件操作的具体实现。

## FAQ 与故障排除 { #faq-and-troubleshooting }

### 如何实现自定义记忆提供者？ { #how-do-i-implement-a-custom-memory-provider }

要实现自定义记忆提供者，请创建一个实现 `AgentMemoryProvider` 接口的类：

<!--- INCLUDE
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.Fact
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import ai.koog.agents.memory.providers.AgentMemoryProvider

/* 
// KNIT: Ignore example
-->
<!--- SUFFIX
*/
-->
```kotlin
class MyCustomMemoryProvider : AgentMemoryProvider {
    override suspend fun save(fact: Fact, subject: MemorySubject, scope: MemoryScope) {
        // Implementation for saving facts
    }

    override suspend fun load(concept: Concept, subject: MemorySubject, scope: MemoryScope): List<Fact> {
        // Implementation for loading facts by concept
    }

    override suspend fun loadAll(subject: MemorySubject, scope: MemoryScope): List<Fact> {
        // Implementation for loading all facts
    }

    override suspend fun loadByDescription(
        description: String,
        subject: MemorySubject,
        scope: MemoryScope
    ): List<Fact> {
        // Implementation for loading facts by description
    }
}
```
<!--- KNIT example-agent-memory-15.kt -->

### 从多个主题加载时，事实如何确定优先级？ { #how-are-facts-prioritized-when-loading-from-multiple-subjects }

事实根据主题特异性确定优先级。加载事实时，如果同一概念有来自多个主题的事实，将使用最具体主题的事实。

### 我可以为同一概念存储多个值吗？ { #can-i-store-multiple-values-for-the-same-concept }

可以，通过使用 `MultipleFacts` 类型。定义概念时，将其 `factType` 设置为 `FactType.MULTIPLE`：
<!--- INCLUDE
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
-->
```kotlin
val concept = Concept(
    keyword = "user-skills",
    description = "Programming languages the user is skilled in",
    factType = FactType.MULTIPLE
)
```
<!--- KNIT example-agent-memory-16.kt -->

这允许您为该概念存储多个值，这些值将以列表形式检索。