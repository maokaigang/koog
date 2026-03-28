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

    
    ```kotlin
    val agent = AIAgent(
        promptExecutor = executor,
        llmModel = OllamaModels.Meta.LLAMA_3_2,
    ) {
        install(Persistence) {
            // Use in-memory storage for snapshots
            storage = InMemoryPersistenceStorageProvider()
        }
    }
    ```

=== "Java"

    ```java
    AIAgent<String, String> agent = AIAgent.<String, String>builder()
        .promptExecutor(SimplePromptExecutorsKt.simpleOllamaAIExecutor("http://localhost:11434"))
        .llmModel(OllamaModels.Meta.LLAMA_3_2)
        .install(Persistence.Feature, cfg -> {
            // Use in-memory storage for snapshots
            cfg.setStorage(new InMemoryPersistenceStorageProvider());
        })
    .build();
    ```

## 配置选项 { #configuration-options }

Agent Persistence 功能提供三种主要配置选项：

- **存储提供方**：用于保存和检索检查点的提供方。
- **持续持久化**：每个节点运行后自动创建检查点。
- **回滚策略**：决定回滚到检查点时要恢复的状态。

### 存储提供商 { #storage-provider }

设置将用于保存和检索检查点的存储提供程序：

=== "Kotlin"

    ```kotlin
    install(Persistence) {
        storage = InMemoryPersistenceStorageProvider()
    }
    ```

=== "Java"

    ```java
    AIAgent<String, String> agent = AIAgent.<String, String>builder()
        .promptExecutor(SimplePromptExecutorsKt.simpleOllamaAIExecutor("http://localhost:11434"))
        .llmModel(OllamaModels.Meta.LLAMA_3_2)
        .install(Persistence.Feature, cfg -> {
            cfg.setStorage(new InMemoryPersistenceStorageProvider());
        })
        .build();
    ```

该框架包含以下内置提供程序：

- `InMemoryPersistenceStorageProvider`: 在内存中存储检查点（应用重启时丢失）。
- `FilePersistenceStorageProvider`: 将检查点持久化到文件系统。
- `NoPersistenceStorageProvider`: 一个不存储检查点的无操作实现。这是默认的提供程序。

您也可以通过实现 `PersistenceStorageProvider` 接口来自定义存储提供程序。更多信息请参阅 [自定义存储提供程序](#custom-storage-providers)。

### 持续持久化 { #continuous-persistence }

持续持久化意味着每个节点运行后都会自动创建一个检查点。要禁用持续持久化，请使用以下代码：

=== "Kotlin"

    
    ```kotlin
    install(Persistence) {
        enableAutomaticPersistence = false
    }
    ```

=== "Java"

    ```java
    AIAgent<String, String> agent = AIAgent.<String, String>builder()
        .promptExecutor(SimplePromptExecutorsKt.simpleOllamaAIExecutor("http://localhost:11434"))
        .llmModel(OllamaModels.Meta.LLAMA_3_2)
        .install(Persistence.Feature, cfg -> {
            cfg.setEnableAutomaticPersistence(true);
        })
        .build();
    ```

如果禁用了持续持久化，您仍然可以手动创建检查点。

## 基本用法 { #basic-usage }

### 创建检查点 { #creating-a-checkpoint }

要了解如何在智能体执行的特定节点创建检查点，请参考以下代码示例：

=== "Kotlin"

    ```kotlin
    suspend fun example(context: AIAgentContext) {
        // Create a checkpoint with the current state
        val checkpoint = context.persistence().createCheckpointAfterNode(
            agentContext = context,
            nodePath = context.executionInfo.path(),
            lastOutput = outputData,
            lastOutputType = outputType,
            checkpointId = context.runId,
            version = 0L
        )

        // The checkpoint ID can be stored for later use
        val checkpointId = checkpoint?.checkpointId
    }
    ```

=== "Java"

    ```java
    ```

### 从检查点恢复 { #restoring-from-a-checkpoint }

要恢复代理在特定检查点的状态，请参考以下代码示例：

=== "Kotlin"

    ```kotlin
    suspend fun example(context: AIAgentContext, checkpointId: String) {
        // Roll back to a specific checkpoint
        context.persistence().rollbackToCheckpoint(checkpointId, context)

        // Or roll back to the latest checkpoint
        context.persistence().rollbackToLatestCheckpoint(context)
    }
    ```

=== "Java"

    ```java
    ```

#### 回滚工具产生的所有副作用 { #rolling-back-all-side-effects-produced-by-tools }

某些工具产生副作用是很常见的。具体来说，当你在后端运行智能体时，部分工具很可能会执行一些数据库事务操作。这使得你的智能体更难实现“时间回溯”功能。

假设你有一个工具 `createUser`，它可以在你的数据库中创建一个新用户。而你的智能体已经在一段时间内填充了多个工具调用：

```
tool call: createUser "Alex"

->>>> checkpoint-1 <<<<-

tool call: createUser "Daniel"
tool call: createUser "Maria"
```

现在您希望回滚到某个检查点。仅恢复代理的状态（包括消息历史和策略图节点）不足以完全还原检查点之前的世界状态。您还需要恢复工具调用所产生的副作用。在我们的示例中，这意味着需要从数据库中删除 `Maria` 和 `Daniel`。

通过Koog持久化功能，您可以通过向`Persistence`功能配置提供`RollbackToolRegistry`来实现：

=== "Kotlin"

    ```kotlin
    install(Persistence) {
        enableAutomaticPersistence = true
        rollbackToolRegistry = RollbackToolRegistry {
            // For every `createUser` tool call there will be a `removeUser` invocation in the reverse order 
            // when rolling back to the desired execution point.
            // Note: `removeUser` tool should take the same exact arguments as `createUser`. 
            // It's the developer's responsibility to make sure that `removeUser` invocation rolls back all side-effects of `createUser`:
            registerRollback(::createUser, ::removeUser)
        }
    }
    ```

=== "Java"

    ```java
    ```

### 使用扩展函数 { #using-extension-functions }

Agent Persistence 功能为处理检查点提供了便捷的扩展函数：

=== "Kotlin"

    ```kotlin
    suspend fun example(context: AIAgentContext) {
        // Access the checkpoint feature
        val checkpointFeature = context.persistence()

        // Or perform an action with the checkpoint feature
        context.withPersistence { ctx ->
            // 'this' is the checkpoint feature
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

=== "Java"

    ```java
    ```

## 高级用法 { #advanced-usage }

### 自定义存储提供程序 { #custom-storage-providers }

您可以通过实现 `PersistenceStorageProvider` 接口来定制存储提供程序：

=== "Kotlin"

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

=== "Java"

    ```java
    ```

要在功能配置中使用您的自定义提供程序，请在代理中配置代理持久化功能时将其设置为存储。

=== "Kotlin"

    ```kotlin
    install(Persistence) {
        storage = MyCustomStorageProvider<Any>()
    }
    ```

=== "Java"

    ```java
    ```

### 设置执行点 { #setting-execution-points }

对于高级控制，您可以直接设置代理的执行点：

=== "Kotlin"

    ```kotlin
    fun example(context: AIAgentContext) {
        // You can set the execution point before some node and provide an input for it:
        context.persistence().setExecutionPoint(
            agentContext = context,
            nodePath = context.executionInfo.path(),
            messageHistory = customMessageHistory,
            input = customInput
        )

        // Or after some node and provide an output from the node:
        context.persistence().setExecutionPointAfterNode(
            agentContext = context,
            nodePath = context.executionInfo.path(),
            messageHistory = customMessageHistory,
            output = customOutput
        )
    }

    ```

=== "Java"

    ```java
    ```

这允许对智能体状态进行更精细的控制，而不仅仅是从检查点恢复。