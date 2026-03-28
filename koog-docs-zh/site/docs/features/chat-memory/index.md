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
    `ChatMemory` 功能从 Koog 版本 **0.7.0** 开始可用。

## 启用聊天记忆 { #enable-chat-memory }

在创建智能体时，使用 `install()` 方法安装 `ChatMemory`：

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

默认情况下，它使用一个无[预处理器](#preprocessors)的内存[聊天历史记录提供程序](#history-providers)。
配置 `ChatMemory` 功能以使用自定义的聊天历史记录提供程序和预处理器，例如：

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

## 会话 ID { #session-ids }

将会话 ID 作为第二个参数提供给 `agent.run()`。
`ChatMemory` 使用此 ID 来存储和加载对话：

```kotlin
// First run - the agent saves the chat history at the end
agent.run("What is the capital of France?", "session-1")

// Second run — the agent loads the previous exchange
agent.run("And what about Germany?", "session-1")
```

不同的会话ID会产生完全隔离的历史记录。

## 历史记录提供器 { #history-providers }

默认的 `InMemoryChatHistoryProvider` 是线程安全的，但不具备持久性（重启后历史记录会丢失）。
在生产环境中，请实现您自己的 `ChatHistoryProvider`，以持久化存储消息。

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

预处理器在加载时（代理看到消息列表之前）和存储时（保存之前）对消息列表进行转换。
它们按照您添加到 `ChatMemory` 功能配置中的顺序依次运行。

### 内置预处理器 { #built-in-preprocessors }

| 配置方法            | 预处理器类           | 行为                              |
|--------------------------|------------------------------|---------------------------------------|
| `windowSize(n)`          | `WindowSizePreProcessor`     | 仅保留最后 `n` 条消息      |
| `filterMessages { ... }` | `FilterMessagesPreProcessor` | 保留符合谓词条件的消息 |

### 预处理器的顺序 { #order-of-preprocessors }

预处理器按顺序运行，每个预处理的输出作为下一个的输入。
这意味着顺序很重要。

```kotlin
// Effect: keep last 10 messages, then filter short ones from those 10
windowSize(10)
filterMessages { it.content.length <= 100 }

// Effect: filter short messages first, then keep last 10 of the survivors
filterMessages { it.content.length <= 100 }
windowSize(10)
```

### 自定义预处理器 { #custom-preprocessors }

要创建自定义预处理器，请实现 `ChatMemoryPreProcessor` 接口：

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

## 聊天记忆与代理持久化 { #chat-memory-vs-agent-persistence }

`ChatMemory` 将每次 `agent.run()` 调用视为一个原子性的、自包含的循环。
代理在运行前加载聊天历史记录，并在成功运行后存储它。
如果代理在运行期间崩溃，它不会存储当前的聊天消息，
这意味着聊天历史记录将保持运行前的状态。

[持久化](../agent-persistence.md) 在运行期间捕获代理的内部执行状态
（图节点、消息历史记录、输入和输出）作为检查点。
如果代理崩溃，它可以从最后一个检查点恢复。

|                    | 聊天记忆                                       | 持久化                                                        |
|--------------------|--------------------------------------------------|--------------------------------------------------------------------|
| **保存内容**  | 对话消息                            | 执行状态                                                    |
| **保存时机**  | `agent.run()` 完成后                    | 每个图节点之后或在运行期间手动定义的检查点 |
| **崩溃行为** | 进行中的运行会丢失；之前的历史记录保持不变 | 可以从最后一个检查点恢复                                    |
| **典型用途**    | 多轮对话连续性                       | 需要崩溃恢复的长时运行代理                            |

如果您的代理执行长时运行任务，且中途崩溃会造成较大损失，请考虑
同时安装这两个功能：

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

- **始终设置窗口大小**，以防止对话无限增长。
- **仔细安排预处理器顺序**，因为先过滤后窗口化与先窗口化后过滤会产生不同的结果。
- **使用有意义的会话ID** 进行历史记录隔离：用户ID、聊天线程ID或UUID都是不错的选择。
- **在生产环境中实现持久化提供器**，因为默认的 `InMemoryChatHistoryProvider` 在重启时会丢失历史记录。

## 后续步骤- 学习如何[构建一个带有记忆的简单 CLI 聊天循环](chat-agent-with-memory.md) { #next-steps }
- 查看一个[带有记忆的聊天端点示例](chat-backend-with-memory.md)
