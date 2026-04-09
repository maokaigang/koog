<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:01:04+00:00", "source_path": "features/chat-memory/index.md", "source_sha256": "9966cba95e31a1c4cdb894040421f7badf468924133352418898382cab2240a5", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 聊天记忆 { #chat-memory }

`ChatMemory` 功能使 AI 智能体能够存储对话历史记录，并在多次运行中检索它。
安装后，智能体会在每次运行开始时自动加载之前的消息，并在运行完成时存储更新后的对话，从而实现自然的多轮聊天。

核心能力：

- 按会话 ID 自动加载和存储对话历史记录
- 通过 `ChatHistoryProvider` 提供可插拔的存储后端
- 内置预处理器以限制历史记录大小和过滤消息
- 支持自定义预处理器以实现任意消息转换

## 添加依赖项 { #add-dependencies }

聊天记忆是一个可选的[功能](../index.md)，在 Koog 中默认不可用。
要为你的 Koog 智能体实现聊天记忆，请添加 [`ai.koog:agents-features-memory`](https://mvnrepository.com/artifact/ai.koog/agents-features-memory) 的依赖项：

=== "Gradle (Kotlin)"

    ```kotlin title="build.gradle.kts"
    dependencies {
        implementation("ai.koog:agents-features-memory:$koogVersion")
    }
    ```

=== "Gradle（Groovy）"

    ```groovy title="build.gradle"
    dependencies {
        implementation 'ai.koog:agents-features-memory:$koogVersion'
    }
    ```

=== "Maven"

    ```xml title="pom.xml"
    <dependency>
        <groupId>ai.koog</groupId>
        <artifactId>agents-features-memory-jvm</artifactId>
        <version>$koogVersion</version>
    </dependency>
    ```

!!! note
    `ChatMemory` 功能自 Koog 版本 **0.7.0** 起可用。

## 启用聊天记忆 { #enable-chat-memory }

在创建智能体时，使用`install()`方法安装`ChatMemory`：

=== "Kotlin"

    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
        llmModel = OpenAIModels.Chat.GPT4oMini
    ) {
        install(ChatMemory)
    }
    ```

=== "Java"

    ```java
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(executor)
        .llmModel(OpenAIModels.Chat.GPT4oMini)
        .install(ChatMemory.Feature)
        .build();
    ```

默认情况下，它使用一个无[预处理器](#preprocessors)的内存[聊天历史记录提供者](#history-providers)。配置`ChatMemory`功能以使用自定义聊天历史记录提供程序和预处理器，例如：

=== "Kotlin"

    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
        llmModel = OpenAIModels.Chat.GPT4oMini
    ) {
        install(ChatMemory) {
            chatHistoryProvider = MyDatabaseChatHistoryProvider()
            windowSize(20)
            filterMessages { it is Message.User || it is Message.Assistant }
        }
    }
    ```

=== "Java"

    ```java
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(executor)
        .llmModel(OpenAIModels.Chat.GPT4oMini)
        .install(ChatMemory.Feature, config -> config
                .chatHistoryProvider(new MyDatabaseChatHistoryProvider())
                .windowSize(20)
                .filterMessages(msg -> msg instanceof Message.User || msg instanceof Message.Assistant))
        .build();
    ```

## 会话ID { #session-ids }

将会话ID作为第二个参数传递给`agent.run()`。`ChatMemory`使用此ID来存储和加载对话：

```kotlin
// First run - the agent saves the chat history at the end
agent.run("What is the capital of France?", "session-1")

// Second run — the agent loads the previous exchange
agent.run("And what about Germany?", "session-1")
```

不同的会话ID会生成完全隔离的历史记录。

## 历史数据提供者 { #history-providers }

默认的 `InMemoryChatHistoryProvider` 是线程安全的，但不具备持久性（重启后历史记录会丢失）。在生产环境中，请实现你自己的 `ChatHistoryProvider` 来持久化存储消息。

```kotlin
class MyDatabaseChatHistoryProvider(private val db: Database) : ChatHistoryProvider {
    override suspend fun store(conversationId: String, messages: List<Message>) {
        db.saveMessages(conversationId, messages)
    }

    override suspend fun load(conversationId: String): List<Message> {
        return db.loadMessages(conversationId) ?: emptyList()
    }
}
```

## 预处理器 { #preprocessors }

预处理器在加载时（智能体接收消息前）和存储时（保存消息前）对消息列表进行转换。它们按照您在`ChatMemory`功能配置中添加的顺序依次执行。

### 内置预处理器 { #built-in-preprocessors }

| 配置方法 | 预处理器类 | 行为 |
|--------------------------|------------------------------|---------------------------------------|
| `windowSize(n)` | `WindowSizePreProcessor` | 仅保留最后`n`条消息 |
| `filterMessages { ... }` | `FilterMessagesPreProcessor` | 保留符合谓词条件的消息 |

### 预处理器的顺序 { #order-of-preprocessors }

预处理器按顺序运行，每个输出都作为下一个输入。这意味着顺序至关重要。

```kotlin
// Effect: keep last 10 messages, then filter short ones from those 10
windowSize(10)
filterMessages { it.content.length <= 100 }

// Effect: filter short messages first, then keep last 10 of the survivors
filterMessages { it.content.length <= 100 }
windowSize(10)
```

### 自定义预处理器 { #custom-preprocessors }

要创建自定义预处理器，需实现 `ChatMemoryPreProcessor` 接口：

```kotlin
class RedactEmailsPreProcessor : ChatMemoryPreProcessor {
    override fun preprocess(messages: List<Message>): List<Message> {
        return messages.map { message ->
            // Replace email addresses in message content
            Message.User(message.content.replace(Regex("[\\w.]+@[\\w.]+"), "[REDACTED]"))
        }
    }
}
```

然后将其添加到配置中：

```kotlin
install(ChatMemory) {
    addPreProcessor(RedactEmailsPreProcessor())
    windowSize(50)
}
```

## 聊天记忆与智能体持久化 { #chat-memory-vs-agent-persistence }

`ChatMemory` 将每次 `agent.run()` 调用视为一个原子化的、自包含的循环。智能体在运行前加载聊天历史，并在成功运行后存储历史记录。如果智能体在运行过程中崩溃，则不会存储当前的聊天消息，这意味着聊天历史将保持运行前的状态。

[持久化](../agent-persistence.md) 在运行过程中捕获智能体的内部执行状态（图节点、消息历史、输入和输出）作为检查点。若智能体发生崩溃，可从最后一个检查点恢复执行。

|                    | ChatMemory                                       | Persistence                                                        |
|--------------------|--------------------------------------------------|--------------------------------------------------------------------|
| **它保存了什么** | 对话消息 | 执行状态 |
| **保存时** | 在`agent.run()`完成后 | 在每次图节点运行后或在运行过程中手动定义的点处 |
| **崩溃行为** | 运行中的任务已丢失；先前历史记录完好 | 可以从上一个检查点恢复 |
| **典型用法** | 多轮对话连续性 | 具有崩溃恢复功能的长时运行智能体 |

如果您的智能体执行长时间运行的任务，且执行过程中发生崩溃会造成较大损失，建议同时启用以下功能：

```kotlin
val agent = AIAgent(
    promptExecutor = executor,
    llmModel = OpenAIModels.Chat.GPT4oMini,
    systemPrompt = "You are a helpful assistant.",
) {
    install(ChatMemory) {
        chatHistoryProvider = MyDatabaseProvider()
        windowSize(50)
    }
    install(Persistence) {
        storage = MyPersistenceStorageProvider()
        enableAutomaticPersistence = true
    }
}
```

## 最佳实践 { #best-practices }

- **始终设置窗口大小**以防止对话无限增长。
- **预处理器的顺序需谨慎安排**，因为先滤波后加窗与先加窗后滤波会产生不同的结果。
- **使用有意义的会话ID**以实现历史隔离：用户ID、聊天线程ID或UUID都是不错的选择。
- **为生产环境实现持久化提供程序**，因为默认的 `InMemoryChatHistoryProvider` 会在重启时丢失历史记录。

## 下一步 { #next-steps }

- 了解如何[构建一个简单的CLI聊天循环，包含记忆功能](chat-agent-with-memory.md)
- 查看一个[带记忆功能的聊天端点](chat-backend-with-memory.md)的示例
