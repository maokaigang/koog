<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:59:19+00:00", "source_path": "features/agent-persistence.md", "source_sha256": "1d146b6fbabf799998d993c0691ebd00fea6efc664d2684a98f9e195a8810028", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 智能体持久化 { #agent-persistence }

智能体持久化是 Koog 框架中为 AI 智能体提供检查点功能的一项特性。
它允许您在执行过程中的特定点保存和恢复智能体的状态，从而实现以下能力：

- 从特定点恢复智能体执行
- 回滚到先前状态
- 跨会话持久化智能体状态

## 核心概念 { #key-concepts }

### 检查点 { #checkpoints }

检查点捕获智能体在其执行过程中特定点的完整状态，包括：

- 消息历史记录（用户、系统、助手和工具之间的所有交互）
- 当前正在执行的节点
- 当前节点的输入数据
- 创建时间戳

检查点通过唯一 ID 进行标识，并与特定的智能体相关联。

## 安装 { #installation }

要使用智能体持久化功能，请将其添加到您的智能体配置中：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.snapshot.feature.Persistence
    import ai.koog.agents.snapshot.providers.InMemoryPersistenceStorageProvider
    import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import ai.koog.agents.core.agent.context.RollbackStrategy
    val executor = simpleOllamaAIExecutor()
    -->
    
    ```kotlin
    val agent = AIAgent(
        promptExecutor = executor,
        llmModel = OllamaModels.Meta.LLAMA_3_2,
    ) {
        install(Persistence) {
            // 使用内存存储快照
            storage = InMemoryPersistenceStorageProvider()
        }
    }
    ```
    <!--- KNIT example-agent-persistence-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    AIAgent<String, String> agent = AIAgent.<String, String>builder()
        .promptExecutor(SimplePromptExecutorsKt.simpleOllamaAIExecutor("http://localhost:11434"))
        .llmModel(OllamaModels.Meta.LLAMA_3_2)
        .install(Persistence.Feature, cfg -> {
            // 使用内存存储快照
            cfg.setStorage(new InMemoryPersistenceStorageProvider());
        })
    .build();
    ```
    <!--- KNIT example-agent-persistence-java-01.java -->

## 配置选项 { #configuration-options }

智能体持久化功能有三个主要配置选项：

- **存储提供程序**：用于保存和检索检查点的提供程序。
- **持续持久化**：在每个节点运行后自动创建检查点。
- **回滚策略**：确定回滚到检查点时要恢复的状态。

### 存储提供程序 { #storage-provider }

设置用于保存和检索检查点的存储提供程序：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.snapshot.feature.Persistence
    import ai.koog.agents.snapshot.providers.InMemoryPersistenceStorageProvider
    import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    val agent = AIAgent(
        promptExecutor = simpleOllamaAIExecutor(),
        llmModel = OllamaModels.Meta.LLAMA_3_2,
    ) {
    -->
    <!--- SUFFIX 
    } 
    -->
    ```kotlin
    install(Persistence) {
        storage = InMemoryPersistenceStorageProvider()
    }
    ```
    <!--- KNIT example-agent-persistence-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    AIAgent<String, String> agent = AIAgent.<String, String>builder()
        .promptExecutor(SimplePromptExecutorsKt.simpleOllamaAIExecutor("http://localhost:11434"))
        .llmModel(OllamaModels.Meta.LLAMA_3_2)
        .install(Persistence.Feature, cfg -> {
            cfg.setStorage(new InMemoryPersistenceStorageProvider());
        })
        .build();
    ```
    <!--- KNIT example-agent-persistence-java-02.java -->

框架包含以下内置提供程序：

- `InMemoryPersistenceStorageProvider`：将检查点存储在内存中（应用程序重启后丢失）。
- `FilePersistenceStorageProvider`：将检查点持久化到文件系统。
- `NoPersistenceStorageProvider`：一个不存储检查点的空操作实现。这是默认提供程序。您也可以通过实现 `PersistenceStorageProvider` 接口来实现自定义存储提供程序。
更多信息，请参阅[自定义存储提供程序](#custom-storage-providers)。

### 持续持久化 { #continuous-persistence }

持续持久化意味着每个节点运行后都会自动创建一个检查点。
要禁用持续持久化，请使用以下代码：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.snapshot.feature.Persistence
    import ai.koog.agents.snapshot.providers.InMemoryPersistenceStorageProvider
    import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    val agent = AIAgent(
        promptExecutor = simpleOllamaAIExecutor(),
        llmModel = OllamaModels.Meta.LLAMA_3_2,
    ) {
    -->
    <!--- SUFFIX 
    } 
    -->
    
    ```kotlin
    install(Persistence) {
        enableAutomaticPersistence = false
    }
    ```
    <!--- KNIT example-agent-persistence-03.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    AIAgent<String, String> agent = AIAgent.<String, String>builder()
        .promptExecutor(SimplePromptExecutorsKt.simpleOllamaAIExecutor("http://localhost:11434"))
        .llmModel(OllamaModels.Meta.LLAMA_3_2)
        .install(Persistence.Feature, cfg -> {
            cfg.setEnableAutomaticPersistence(true);
        })
        .build();
    ```
    <!--- KNIT example-agent-persistence-java-03.java -->

如果禁用了持续持久化，您仍然可以手动创建检查点。

## 基本用法 { #basic-usage }

### 创建检查点 { #creating-a-checkpoint }

要了解如何在智能体执行的特定点创建检查点，请参阅以下代码示例：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.context.AIAgentContext
    import ai.koog.agents.snapshot.feature.persistence
    import ai.koog.serialization.typeToken
    
    const val outputData = "some-output-data"
    val outputType = typeToken<String>()
    -->
    ```kotlin
    suspend fun example(context: AIAgentContext) {
        // 使用当前状态创建检查点
        val checkpoint = context.persistence().createCheckpointAfterNode(
            agentContext = context,
            nodePath = context.executionInfo.path(),
            lastOutput = outputData,
            lastOutputType = outputType,
            checkpointId = context.runId,
            version = 0L
        )

        // 检查点 ID 可以存储以备后用
        val checkpointId = checkpoint?.checkpointId
    }
    ```
    <!--- KNIT example-agent-persistence-04.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-agent-persistence-java-04.java -->

### 从检查点恢复 { #restoring-from-a-checkpoint }

要从特定检查点恢复智能体的状态，请按照以下代码示例操作：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.context.AIAgentContext
    import ai.koog.agents.snapshot.feature.persistence
    -->
    ```kotlin
    suspend fun example(context: AIAgentContext, checkpointId: String) {
        // 回滚到特定检查点
        context.persistence().rollbackToCheckpoint(checkpointId, context)

        // 或回滚到最新检查点
        context.persistence().rollbackToLatestCheckpoint(context)
    }
    ```
    <!--- KNIT example-agent-persistence-05.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-agent-persistence-java-05.java -->

#### 回滚工具产生的所有副作用 { #rolling-back-all-side-effects-produced-by-tools }

某些工具产生副作用是很常见的。具体来说，当您在后台运行智能体时，一些工具很可能会执行某些数据库事务。这使得您的智能体更难回到过去。

假设您有一个工具 `createUser`，可以在数据库中创建新用户。并且您的智能体随着时间的推移已经调用了多个工具：

```
tool call: createUser "Alex"

->>>> checkpoint-1 <<<<-

tool call: createUser "Daniel"
tool call: createUser "Maria"
```
 <!--- KNIT example-agent-persistence-01.txt -->现在您希望回滚到某个检查点。仅恢复代理的状态（包括消息历史和策略图节点）不足以完全还原检查点之前的世界状态。您还需要恢复工具调用产生的副作用。在我们的示例中，这意味着从数据库中移除 `Maria` 和 `Daniel`。

通过 Koog 持久化功能，您可以通过为 `Persistence` 功能配置提供 `RollbackToolRegistry` 来实现这一点：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.snapshot.feature.Persistence
    import ai.koog.agents.snapshot.providers.InMemoryPersistenceStorageProvider
    import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import ai.koog.agents.snapshot.feature.RollbackToolRegistry
    fun createUser(name: String) {}
    fun removeUser(name: String) {}
    val agent = AIAgent(
        promptExecutor = simpleOllamaAIExecutor(),
        llmModel = OllamaModels.Meta.LLAMA_3_2,
    ) {
    -->
    <!--- SUFFIX 
    } 
    -->
    ```kotlin
    install(Persistence) {
        enableAutomaticPersistence = true
        rollbackToolRegistry = RollbackToolRegistry {
            // 对于每个 `createUser` 工具调用，在回滚到目标执行点时，
            // 都会按相反顺序调用一次 `removeUser`。
            // 注意：`removeUser` 工具应接受与 `createUser` 完全相同的参数。
            // 开发者需确保 `removeUser` 调用能回滚 `createUser` 的所有副作用：
            registerRollback(::createUser, ::removeUser)
        }
    }
    ```
    <!--- KNIT example-agent-persistence-06.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-agent-persistence-java-06.java -->

### 使用扩展函数 { #using-extension-functions }

代理持久化功能提供了便捷的扩展函数来处理检查点：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.context.AIAgentContext
    import ai.koog.agents.example.exampleAgentPersistence04.outputData
    import ai.koog.agents.example.exampleAgentPersistence04.outputType
    import ai.koog.agents.snapshot.feature.persistence
    import ai.koog.agents.snapshot.feature.withPersistence
    -->
    ```kotlin
    suspend fun example(context: AIAgentContext) {
        // 访问检查点功能
        val checkpointFeature = context.persistence()

        // 或使用检查点功能执行操作
        context.withPersistence { ctx ->
            // 'this' 指向检查点功能
            createCheckpointAfterNode(
                agentContext = ctx,
                nodePath = ctx.executionInfo.path(),
                lastOutput = outputData,
                lastOutputType = outputType,
                checkpointId = ctx.runId,
                version = 0L
            )
        }
    }
    ```
    <!--- KNIT example-agent-persistence-07.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-agent-persistence-java-07.java -->

## 高级用法 { #advanced-usage }

### 自定义存储提供程序 { #custom-storage-providers }

您可以通过实现 `PersistenceStorageProvider` 接口来自定义存储提供程序：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.snapshot.feature.AgentCheckpointData
    import ai.koog.agents.snapshot.providers.PersistenceStorageProvider
    /*
    // KNIT: Ignore example
    -->
    <!--- SUFFIX
    */
    -->
    ```kotlin
    class MyCustomStorageProvider<MyFilterType> : PersistenceStorageProvider<MyFilterType> {
        override suspend fun getCheckpoints(sessionId: String, filter: MyFilterType?): List<AgentCheckpointData> {
            TODO("Not yet implemented")
        }

        override suspend fun saveCheckpoint(sessionId: String, agentCheckpointData: AgentCheckpointData) {
            TODO("Not yet implemented")
        }

        override suspend fun getLatestCheckpoint(sessionId: String, filter: MyFilterType?): AgentCheckpointData? {
            TODO("Not yet implemented")
        }
    }
    ```
    <!--- KNIT example-agent-persistence-08.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-agent-persistence-java-08.java -->要在功能配置中使用您的自定义提供程序，请在代理中配置代理持久化功能时将其设置为存储。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.snapshot.feature.AgentCheckpointData
    import ai.koog.agents.snapshot.feature.Persistence
    import ai.koog.agents.snapshot.providers.PersistenceStorageProvider
    import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    class MyCustomStorageProvider<MyFilterType> : PersistenceStorageProvider<MyFilterType> {
        override suspend fun getCheckpoints(sessionId: String, filter: MyFilterType?): List<AgentCheckpointData> {
            TODO("Not yet implemented")
        }
        override suspend fun saveCheckpoint(sessionId: String, agentCheckpointData: AgentCheckpointData) {
            TODO("Not yet implemented")
        }
        override suspend fun getLatestCheckpoint(sessionId: String, filter: MyFilterType?): AgentCheckpointData? {
            TODO("Not yet implemented")
        }
    }
    val agent = AIAgent(
        promptExecutor = simpleOllamaAIExecutor(),
        llmModel = OllamaModels.Meta.LLAMA_3_2,
    ) {
    -->
    <!--- SUFFIX 
    } 
    -->
    ```kotlin
    install(Persistence) {
        storage = MyCustomStorageProvider<Any>()
    }
    ```
    <!--- KNIT example-agent-persistence-09.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-agent-persistence-java-09.java -->

### 设置执行点 { #setting-execution-points }

如需进行高级控制，您可以直接设置代理的执行点：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.context.AIAgentContext
    import ai.koog.agents.snapshot.feature.persistence
    import ai.koog.prompt.message.Message.User
    import ai.koog.serialization.JSONPrimitive
    
    val customInput = JSONPrimitive("custom-input")
    val customOutput = JSONPrimitive("custom-output")
    val customMessageHistory = emptyList<User>()
    -->
    ```kotlin
    fun example(context: AIAgentContext) {
        // 您可以在某个节点之前设置执行点，并为其提供输入：
        context.persistence().setExecutionPoint(
            agentContext = context,
            nodePath = context.executionInfo.path(),
            messageHistory = customMessageHistory,
            input = customInput
        )

        // 或在某个节点之后设置执行点，并提供该节点的输出：
        context.persistence().setExecutionPointAfterNode(
            agentContext = context,
            nodePath = context.executionInfo.path(),
            messageHistory = customMessageHistory,
            output = customOutput
        )
    }

    ```
    <!--- KNIT example-agent-persistence-10.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-agent-persistence-java-10.java -->

这允许对代理状态进行更精细的控制，而不仅限于从检查点恢复。