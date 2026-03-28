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

=== "Gradle (Groovy)"

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
    The `ChatMemory` feature is available starting from Koog version **0.7.0**.

## Enable chat memory

Install `ChatMemory` using the `install()` method when creating the agent:

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


By default, it uses an in-memory [chat history provider](#history-providers) with no [preprocessors](#preprocessors).
Configure the `ChatMemory` feature to use a custom chat history provider and preprocessors, for example:

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

## Session IDs

Provide the session ID as the second argument to `agent.run()`.
`ChatMemory` uses this ID to store and load conversations:

```kotlin
// First run - the agent saves the chat history at the end
agent.run("What is the capital of France?", "session-1")

// Second run — the agent loads the previous exchange
agent.run("And what about Germany?", "session-1")
```

Different session IDs produce fully isolated histories.

## History providers

The default `InMemoryChatHistoryProvider` is thread-safe but not persistent (history is lost on restart).
For production, implement your own `ChatHistoryProvider` that stores messages persistently.

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

## Preprocessors

Preprocessors transform the message list at both load time (before the agent sees it) and store time (before saving).
They run sequentially in the order you add them to the `ChatMemory` feature configuration.

### Built-in preprocessors

| Config method            | Preprocessor class           | Behavior                              |
|--------------------------|------------------------------|---------------------------------------|
| `windowSize(n)`          | `WindowSizePreProcessor`     | Keeps only the last `n` messages      |
| `filterMessages { ... }` | `FilterMessagesPreProcessor` | Keeps messages matching the predicate |

### Order of preprocessors

Preprocessors run sequentially, with each output being the next input.
This means that order matters.

```kotlin
// Effect: keep last 10 messages, then filter short ones from those 10
windowSize(10)
filterMessages { it.content.length <= 100 }

// Effect: filter short messages first, then keep last 10 of the survivors
filterMessages { it.content.length <= 100 }
windowSize(10)
```

### Custom preprocessors

To create a custom preprocessor, implement the `ChatMemoryPreProcessor` interface:

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

Then add it to the config:

```kotlin
install(ChatMemory) {
    addPreProcessor(RedactEmailsPreProcessor())
    windowSize(50)
}
```

## Chat memory vs agent persistence

`ChatMemory` treats each `agent.run()` call as an atomic, self-contained loop.
The agent loads chat history before running and stores it after a successful run.
If the agent crashes during the run, it does not store the current chat messages,
meaning that the chat history remains as it was before the run.

[Persistence](../agent-persistence.md) captures the agent's internal execution state
(graph node, message history, inputs, and outputs) as checkpoints during the run.
If the agent crashes, it can resume from the last checkpoint.

|                    | ChatMemory                                       | Persistence                                                        |
|--------------------|--------------------------------------------------|--------------------------------------------------------------------|
| **What it saves**  | Conversation messages                            | Execution state                                                    |
| **When it saves**  | After `agent.run()` completes                    | After each graph node or at manually defined points during the run |
| **Crash behavior** | In-progress run is lost; previous history intact | Can resume from last checkpoint                                    |
| **Typical use**    | Multi-turn chat continuity                       | Long-running agents with crash recovery                            |

If your agent performs long-running tasks where a mid-execution crash would be costly, consider
installing both features:

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

## Best practices

- **Always set a window size** to prevent unlimited conversation growth.
- **Order preprocessors carefully**, as filtering before windowing and windowing before filtering produce different results.
- **Use meaningful session IDs** for history isolation: user IDs, chat thread IDs, or UUIDs all work well.
- **Implement a persistent provider for production** because the default `InMemoryChatHistoryProvider` loses history on restart.

## Next steps

- Learn how to [build a simple CLI chat loop with memory](chat-agent-with-memory.md)
- See an example of a [chat endpoint with memory](chat-backend-with-memory.md)
