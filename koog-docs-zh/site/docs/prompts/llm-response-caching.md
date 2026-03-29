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

    ```kotlin
    // Create a prompt executor
    val client = OpenAILLMClient(System.getenv("OPENAI_API_KEY"))
    val promptExecutor = MultiLLMPromptExecutor(client)

    // Create a cached prompt executor
    val cachedExecutor = CachedPromptExecutor(
        cache = FilePromptCache(Path("path/to/your/cache/directory")),
        nested = promptExecutor
    )

    // Run cached prompt executor for the first time
    // This will perform an actual LLM request
    val firstTime = measureTimeMillis {
        val firstResponse = cachedExecutor.execute(prompt, OpenAIModels.Chat.GPT4o)
        println("First response: ${firstResponse.first().content}")
    }
    println("First execution took: ${firstTime}ms")

    // Run cached prompt executor for the second time
    // This will return the result immediately from the cache
    val secondTime = measureTimeMillis {
        val secondResponse = cachedExecutor.execute(prompt, OpenAIModels.Chat.GPT4o)
        println("Second response: ${secondResponse.first().content}")
    }
    println("Second execution took: ${secondTime}ms")
    ```

=== "Java"

    ```java
    // Create a prompt
    Prompt prompt = Prompt.builder("test")
            .user("Hello")
            .build();

    // Create a prompt executor
    OpenAILLMClient client = new OpenAILLMClient(System.getenv("OPENAI_API_KEY"));
    MultiLLMPromptExecutor promptExecutor = new MultiLLMPromptExecutor(client);

    // Create a cached prompt executor
    FilePromptCache cache = new FilePromptCache(Path.of("path/to/your/cache/directory"), null);
    CachedPromptExecutor cachedExecutor = new CachedPromptExecutor(cache, promptExecutor, Clock.System.INSTANCE);

    // Run cached prompt executor for the first time
    // This will perform an actual LLM request
    long start1 = System.nanoTime();
    List<Message.Response> firstResponse = cachedExecutor.execute(prompt, OllamaModels.Meta.LLAMA_3_2);
    long firstTimeMs = (System.nanoTime() - start1) / 1_000_000L;
    System.out.println("First response: " + firstResponse.getFirst().getContent());
    System.out.println("First execution took: " + firstTimeMs + "ms");

    // Run cached prompt executor for the second time
    // This will return the result immediately from the cache
    long start2 = System.nanoTime();
    List<Message.Response> secondResponse = cachedExecutor.execute(prompt, OllamaModels.Meta.LLAMA_3_2);
    long secondTimeMs = (System.nanoTime() - start2) / 1_000_000L;
    System.out.println("Second response: " + secondResponse.getFirst().getContent());
    System.out.println("Second execution took: " + secondTimeMs + "ms");
    ```

该示例会产生如下输出：

```
First response: Hello! It seems like we're starting a new conversation. What can I help you with today?
First execution took: 48ms
Second response: Hello! It seems like we're starting a new conversation. What can I help you with today?
Second execution took: 1ms
```
第二次响应直接从缓存中取回，因此只用了 1ms。

!!!note
    * If you call `executeStreaming()` in Kotlin or `executeStreamingWithPublisher()` in Java with the cached prompt executor, it produces a response as a single chunk.
    * If you call `moderate()` with the cached prompt executor in either Kotlin or Java, it forwards the request to the nested prompt executor and does not use the cache.
    * Caching of multiple choice responses (`executeMultipleChoices()`) is not supported in either Kotlin or Java.
