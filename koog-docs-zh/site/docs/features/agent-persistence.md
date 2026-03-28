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
            // Use in-memory storage for snapshots
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
            // Use in-memory storage for snapshots
            cfg.setStorage(new InMemoryPersistenceStorageProvider());
        })
    .build();
    ```
    <!--- KNIT example-agent-persistence-java-01.java -->

## Configuration options

The Agent Persistence feature has three main configuration options:

- **Storage provider**: the provider used to save and retrieve checkpoints.
- **Continuous persistence**: automatic creation of checkpoints after each node is run.
- **Rollback strategy**: determines which state will be restored when rolling back to a checkpoint.

### Storage provider

Set the storage provider that will be used to save and retrieve checkpoints:

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

The framework includes the following built-in providers:

- `InMemoryPersistenceStorageProvider`: stores checkpoints in memory (lost when the application restarts).
- `FilePersistenceStorageProvider`: persists checkpoints to the file system.
- `NoPersistenceStorageProvider`: a no-op implementation that does not store checkpoints. This is the default provider.

You can also implement custom storage providers by implementing the `PersistenceStorageProvider` interface.
For more information, see [Custom storage providers](#custom-storage-providers).

### Continuous persistence

Continuous persistence means that a checkpoint is automatically created after each node is run.
To disable continuous persistence, use the code below:

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

If continuous persistence is disabled, you can still create checkpoints manually.

## Basic usage

### Creating a checkpoint

To learn how to create a checkpoint at a specific point in your agent's execution, see the code sample below:

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

### Restoring from a checkpoint

To restore the state of an agent from a specific checkpoint, follow the code sample below:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.context.AIAgentContext
    import ai.koog.agents.snapshot.feature.persistence
    -->
    ```kotlin
    suspend fun example(context: AIAgentContext, checkpointId: String) {
        // Roll back to a specific checkpoint
        context.persistence().rollbackToCheckpoint(checkpointId, context)

        // Or roll back to the latest checkpoint
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

#### Rolling back all side-effects produced by tools

It's quite common for some tools to produce side-effects. Specifically, when you are running your agents on the backend, 
some of the tools would likely perform some database transactions. This makes it much harder for your agent to travel back in time.

Imagine you have a tool `createUser` that creates a new user in your database. And your agent has populated multiple tool calls overtime:

```
tool call: createUser "Alex"

->>>> checkpoint-1 <<<<-

tool call: createUser "Daniel"
tool call: createUser "Maria"
```
 <!--- KNIT example-agent-persistence-01.txt -->

And now you would like to roll back to a checkpoint. Restoring the agent's state (including message history, and strategy graph node) alone would not
be sufficient to achieve the exact state of the world before the checkpoint. You should also restore the side-effects produced by your tool calls. In our example,
this would mean removing `Maria` and `Daniel` from the database.

With Koog Persistence you can achieve that by providing a `RollbackToolRegistry` to `Persistence` feature config:

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
            // For every `createUser` tool call there will be a `removeUser` invocation in the reverse order 
            // when rolling back to the desired execution point.
            // Note: `removeUser` tool should take the same exact arguments as `createUser`. 
            // It's the developer's responsibility to make sure that `removeUser` invocation rolls back all side-effects of `createUser`:
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

### Using extension functions

The Agent Persistence feature provides convenient extension functions for working with checkpoints:

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

## Advanced usage

### Custom storage providers

You can implement custom storage providers by implementing the `PersistenceStorageProvider` interface:

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
    <!--- KNIT example-agent-persistence-java-08.java -->

To use your custom provider in the feature configuration, set it as the storage when configuring the Agent Persistence
feature in your agent.

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

### Setting execution points

For advanced control, you can directly set the execution point of an agent:

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

This allows for more fine-grained control over the agent's state beyond just restoring from checkpoints.