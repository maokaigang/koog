<!-- koog-zh-meta: {"last_synced_at": "2026-03-29T03:04:02+00:00", "source_path": "features/long-term-memory.md", "source_sha256": "86687e3c47224a0807e51676e36d693f5ea514f4d1d54f75f5036d790feb5754", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 长期记忆 { #long-term-memory }

功能（实验性）

`LongTermMemory` 功能通过两组独立的设置，为 Koog AI 智能体添加持久化记忆：
- **检索** — 从记忆存储中获取相关上下文，增强 LLM 提示（检索增强生成或 RAG）
- **摄取** — 将对话消息持久化到记忆存储中，供未来检索

## 快速开始 { #quick-start }

> **注意：** `LongTermMemory` 是一个实验性的 API。请使用 `@OptIn(ExperimentalAgentsApi::class)` 注解你的代码，或在文件顶部添加 `@file:OptIn(ExperimentalAgentsApi::class)`。

=== "Kotlin"

    ```kotlin
    @OptIn(ExperimentalAgentsApi::class)
    val myStorage = InMemoryRecordStorage() // or your vector DB adapter

    @OptIn(ExperimentalAgentsApi::class)
    val agent = AIAgent(
        promptExecutor = executor,
        strategy = singleRunStrategy(),
        agentConfig = agentConfig,
        toolRegistry = ToolRegistry.EMPTY
    ) {
        install(LongTermMemory) {
            retrieval {
                storage = myStorage
                searchStrategy = KeywordSearchStrategy(topK = 5)
            }
        }
    }

    agent.run("What did we discuss yesterday?")
    ```

=== "Java"

    ```java
    InMemoryRecordStorage myStorage = new InMemoryRecordStorage();

    AIAgent agent = AIAgent.builder()
        .promptExecutor(executor)
        .llmModel(OpenAIModels.Chat.GPT4o)
        .systemPrompt("You are a helpful assistant.")
        .install(LongTermMemory.Feature, config -> {
            config.retrieval(
                new LongTermMemory.RetrievalSettingsBuilder()
                    .withStorage(myStorage)
                    .withSearchStrategy(query ->
                        new KeywordSearchRequest(query, 15, 0.5, null)
                    )
                    .build()
            );
        })
        .build();

    Object result = agent.run("What did we discuss yesterday?");
    ```

## 仅检索（RAG） { #retrieval-only-rag }

当您拥有预先填充的知识库时，可以使用检索而不进行摄取：

=== "Kotlin"

    ```kotlin
    @OptIn(ExperimentalAgentsApi::class)
    install(LongTermMemory) {
        retrieval {
            storage = myVectorDbStorage
            namespace = "my-collection"  // optional: scope to a specific namespace/collection
            searchStrategy = SimilaritySearchStrategy(topK = 3, similarityThreshold = 0.7)
            promptAugmenter = SystemPromptAugmenter()
        }
    }
    ```

=== "Java"

    ```java
    var retrievalSettings = new LongTermMemory.RetrievalSettingsBuilder()
        .withStorage(myVectorDbStorage)
        .withSearchStrategy(
            SearchStrategy.builder().similarity().withTopK(3).withSimilarityThreshold(0.7).build()
        )
        .withPromptAugmenter(PromptAugmenter.builder().system().build())
        .build();
    ```

### 即时增强器 { #prompt-augmenters }

| Augmenter | Behavior |
|---|---|
| `SystemPromptAugmenter()` | 在提示符开头插入上下文作为系统消息（如果没有系统消息则不执行任何操作） |
| `UserPromptAugmenter()` | 在最后一条用户消息之前插入上下文作为单独的用户消息 |
| `PromptAugmenter { prompt, context -> ... }` | 通过 lambda 进行自定义增强 |

### 搜索策略 { #search-strategies }

| Strategy                                                  | Behavior                 |
|-----------------------------------------------------------|--------------------------|
| `KeywordSearchStrategy()` | 全文/词法关键字匹配 |
| `SimilaritySearchStrategy()` | 向量相似度语义搜索 |
| `query -> new KeywordSearchRequest(query, 20, 0.0, null)` | 通过 lambda 自定义搜索 |

## 仅摄入 { #ingestion-only }

使用摄取而不检索来随着时间的推移建立内存存储：

=== "Kotlin"

    ```kotlin
    @OptIn(ExperimentalAgentsApi::class)
    install(LongTermMemory) {
        ingestion {
            storage = myVectorDbStorage
            namespace = "my-collection"  // optional: scope to a specific namespace/collection
            extractor = FilteringMemoryRecordExtractor(
                messageRolesToExtract = setOf(Message.Role.User, Message.Role.Assistant)
            )
            timing = IngestionTiming.ON_LLM_CALL
        }
    }
    ```

=== "Java"

    ```java
    var ingestionSettings = new LongTermMemory.IngestionSettingsBuilder()
        .withStorage(myVectorDbStorage)
        .withExtractor(
            MemoryRecordExtractor.builder()
                .filtering()
                .withExtractRoles(new HashSet<>(Arrays.asList(Message.Role.User, Message.Role.Assistant)))
                .withLastMessageOnly(false)
                .build()
        )
        .withTiming(IngestionTiming.ON_LLM_CALL)
        .build();
    ```

### 摄入时间 { #ingestion-timing }

| Timing | Behavior |
|---|---|
| `ON_LLM_CALL` | 在每个 LLM 呼叫/流上摄取消息（启用会话内 RAG） |
| `ON_AGENT_COMPLETION` | 代理运行完成后立即提取所有消息 |

## 从策略节点访问长期记忆 { #accessing-long-term-memory-from-strategy-nodes }

在策略节点内使用 `withLongTermMemory { }` 直接搜索或添加记录：

```kotlin
@OptIn(ExperimentalAgentsApi::class)
val myNode by node<String, Unit> {
    withLongTermMemory {
        // Manually add records
        val record = MemoryRecord(content = "important fact")
        this.getIngestionStorage()?.add(listOf(record), ingestionSettings?.namespace)

        // Manually search
        val request = SimilaritySearchRequest(query = input, limit = 5)
        val results = this.getRetrievalStorage()?.search(request, retrievalSettings?.namespace)
    }
}
```

使用 `longTermMemory()` 直接获取特征实例：

```kotlin
@OptIn(ExperimentalAgentsApi::class)
val myNode by node<String, Unit> {
    val memory = longTermMemory()
    val storage = memory.getIngestionStorage()
}
```

## 自定义内存记录提取器 { #custom-memory-record-extractor }

实现 `MemoryRecordExtractor` 来控制消息在存储之前如何转换：

```kotlin
@OptIn(ExperimentalAgentsApi::class)
val summarizingExtractor = MemoryRecordExtractor { messages ->
    messages
        .filter { it.role == Message.Role.Assistant }
        .map { MemoryRecord(content = summarize(it.content)) }
}

install(LongTermMemory) {
    ingestion {
        storage = myStorage
        extractor = summarizingExtractor
    }
}
```

## 实施自定义存储 { #implementing-custom-storage }

实现 `RetrievalStorage` 和/或 `IngestionStorage` 连接到您的矢量数据库：

```kotlin
class MyVectorDbStorage : RetrievalStorage, IngestionStorage {
    override suspend fun search(
        request: SearchRequest, namespace: String?
    ): List<SearchResult> {
        // Query your vector DB
    }

    override suspend fun add(
        records: List<MemoryRecord>, namespace: String?
    ) {
        // Upsert into your vector DB
    }
}
```

为了进行测试，请使用内置的 `InMemoryRecordStorage`，它通过基于关键字的搜索将记录保存在内存中。