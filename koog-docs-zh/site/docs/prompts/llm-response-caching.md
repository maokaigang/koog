<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:11:47+00:00", "source_path": "prompts/llm-response-caching.md", "source_sha256": "2db2c65d2d23ad47befa9fca662a62d358b47eb67d8f7930395a9ed54b9445da", "source_tag": "0.7.3", "translation_status": "changed"} -->
# LLM 响应缓存 { #llm-response-caching }

对于使用提示执行器重复运行的请求，您可以缓存 LLM 响应，以优化性能并降低 Kotlin 和 Java 中的成本。
在 Koog 中，所有提示执行器均可通过 `CachedPromptExecutor` 实现缓存功能，
它是 `PromptExecutor` 的封装器，增加了缓存功能。
它允许您存储先前执行的提示的响应，并在再次运行相同提示时检索它们。

要在 Kotlin 或 Java 中创建缓存的提示执行器，请执行以下操作：

1. 创建您希望缓存响应的提示执行器。
2. 通过提供所需的缓存和您创建的提示执行器来创建 `CachedPromptExecutor` 实例。
3. 使用所需的提示和模型运行创建的 `CachedPromptExecutor`。

以下是一个示例：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
    import ai.koog.prompt.executor.cached.CachedPromptExecutor
    import ai.koog.prompt.cache.files.FilePromptCache
    import kotlin.system.measureTimeMillis
    import ai.koog.prompt.dsl.prompt
    import kotlin.io.path.Path
    import kotlinx.coroutines.runBlocking
    fun main() {
        runBlocking {
            val prompt = prompt("test") {
                user("Hello")
            }
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    // 创建提示执行器
    val client = OpenAILLMClient(System.getenv("OPENAI_API_KEY"))
    val promptExecutor = MultiLLMPromptExecutor(client)

    // 创建缓存的提示执行器
    val cachedExecutor = CachedPromptExecutor(
        cache = FilePromptCache(Path("path/to/your/cache/directory")),
        nested = promptExecutor
    )

    // 首次运行缓存的提示执行器
    // 这将执行实际的 LLM 请求
    val firstTime = measureTimeMillis {
        val firstResponse = cachedExecutor.execute(prompt, OpenAIModels.Chat.GPT4o)
        println("First response: ${firstResponse.first().content}")
    }
    println("First execution took: ${firstTime}ms")

    // 第二次运行缓存的提示执行器
    // 这将立即从缓存返回结果
    val secondTime = measureTimeMillis {
        val secondResponse = cachedExecutor.execute(prompt, OpenAIModels.Chat.GPT4o)
        println("Second response: ${secondResponse.first().content}")
    }
    println("Second execution took: ${secondTime}ms")
    ```
    <!--- KNIT example-llm-response-caching-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // 创建提示
    Prompt prompt = Prompt.builder("test")
            .user("Hello")
            .build();

    // 创建提示执行器
    OpenAILLMClient client = new OpenAILLMClient(System.getenv("OPENAI_API_KEY"));
    MultiLLMPromptExecutor promptExecutor = new MultiLLMPromptExecutor(client);

    // 创建缓存的提示执行器
    FilePromptCache cache = new FilePromptCache(Path.of("path/to/your/cache/directory"), null);
    CachedPromptExecutor cachedExecutor = new CachedPromptExecutor(cache, promptExecutor, Clock.System.INSTANCE);

    // 首次运行缓存的提示执行器
    // 这将执行实际的 LLM 请求
    long start1 = System.nanoTime();
    List<Message.Response> firstResponse = cachedExecutor.execute(prompt, OllamaModels.Meta.LLAMA_3_2);
    long firstTimeMs = (System.nanoTime() - start1) / 1_000_000L;
    System.out.println("First response: " + firstResponse.getFirst().getContent());
    System.out.println("First execution took: " + firstTimeMs + "ms");
    ```    // 第二次运行缓存的提示词执行器
    // 这将立即从缓存中返回结果
    long start2 = System.nanoTime();
    List<Message.Response> secondResponse = cachedExecutor.execute(prompt, OllamaModels.Meta.LLAMA_3_2);
    long secondTimeMs = (System.nanoTime() - start2) / 1_000_000L;
    System.out.println("第二次响应: " + secondResponse.getFirst().getContent());
    System.out.println("第二次执行耗时: " + secondTimeMs + "ms");
    ```
    <!--- KNIT example-llm-response-caching-java-01.java -->

该示例产生以下输出：

```
First response: Hello! It seems like we're starting a new conversation. What can I help you with today?
First execution took: 48ms
Second response: Hello! It seems like we're starting a new conversation. What can I help you with today?
Second execution took: 1ms
```
第二次响应从缓存中获取，仅耗时1毫秒。

!!!注意
    * 如果在 Kotlin 中调用 `executeStreaming()`，或在 Java 中调用 `executeStreamingWithPublisher()` 并使用缓存的提示词执行器，会以单个数据块的形式生成响应。
    * 如果在 Kotlin 或 Java 中使用缓存的提示词执行器调用 `moderate()`，会将请求转发给嵌套的提示词执行器，而不会使用缓存。
    * 在 Kotlin 或 Java 中不支持对多项选择响应（`executeMultipleChoices()`）进行缓存。