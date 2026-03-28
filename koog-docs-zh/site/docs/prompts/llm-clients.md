<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:09:41+00:00", "source_path": "prompts/llm-clients.md", "source_sha256": "74105a7a2c63b65dc31c120ba5bedf9177bfa0824b06afb914bcc4d2d6f05b51", "source_tag": "0.7.3", "translation_status": "changed"} -->
# LLM 客户端 { #llm-clients }

LLM 客户端专为直接与 LLM 提供方交互而设计。
每个客户端都实现了 [`LLMClient`](api:prompt-executor-clients::ai.koog.prompt.executor.clients.LLMClient) 接口，该接口提供了执行提示词和流式传输响应的方法。

当您与单个 LLM 提供方协作且无需高级生命周期管理时，可以使用 LLM 客户端。
如果您需要管理多个 LLM 提供方，请使用 [提示词执行器](prompt-executors.md)。

下表展示了可用的 LLM 客户端及其功能。| LLM 提供商                                      | LLMClient                                                                                                                                                                                                   | 工具<br/>调用 | 流式响应 | 多<br/>选项 | 嵌入向量 | 内容审核 | <div style="width:50px">模型<br/>列表</div> | <div style="width:200px">备注</div>                                                                                        |
|-----------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|-----------|----------------------|------------|------------|-------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| [OpenAI](https://platform.openai.com/docs/overview) | [OpenAILLMClient](api:prompt-executor-openai-client::ai.koog.prompt.executor.clients.openai.OpenAILLMClient)                | ✓                | ✓         | ✓                    | ✓          | ✓[^1]      | ✓                                               |                                                                                                                             |
| [Anthropic](https://www.anthropic.com/)             | [AnthropicLLMClient](api:prompt-executor-anthropic-client::ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient)      | ✓                | ✓         | -                    | -          | -          | -                                               | -                                                                                                                           |
| [Google](https://ai.google.dev/)                    | [GoogleLLMClient](api:prompt-executor-google-client::ai.koog.prompt.executor.clients.google.GoogleLLMClient)                  | ✓                | ✓         | ✓                    | ✓          | -          | ✓                                               | -                                                                                                                           |
| [DeepSeek](https://www.deepseek.com/)               | [DeepSeekLLMClient](api:prompt-executor-deepseek-client::ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient)         | ✓                | ✓         | ✓                    | -          | -          | ✓                                               | 兼容 OpenAI 的聊天客户端。                                                                                              |
| [OpenRouter](https://openrouter.ai/)                | [OpenRouterLLMClient](api:prompt-executor-openrouter-client::ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient) | ✓                | ✓         | ✓                    | -          | -          | ✓                                               | 兼容 OpenAI 的路由器客户端。                                                                                            |
| [Amazon Bedrock](https://aws.amazon.com/bedrock/)   | [BedrockLLMClient](api:prompt-executor-bedrock-client::ai.koog.prompt.executor.clients.bedrock.BedrockLLMClient)              | ✓                | ✓         | -                    | ✓          | ✓[^2]      | -                                               | 仅支持 JVM 的 AWS SDK 客户端，兼容多种模型系列。                                                              |
| [Mistral](https://mistral.ai/)                      | [MistralAILLMClient](api:prompt-executor-mistralai-client::ai.koog.prompt.executor.clients.mistralai.MistralAILLMClient)    | ✓                | ✓         | ✓                    | ✓          | ✓[^3]      | ✓                                               | 兼容 OpenAI 的客户端。                                                                                                   |
| [阿里巴巴](https://www.alibabacloud.com/en?_p_lc=1)  | [DashScopeLLMClient](api:prompt-executor-dashscope-client::ai.koog.prompt.executor.clients.dashscope.DashscopeLLMClient)      | ✓                | ✓         | ✓                    | -          | -          | ✓                                               | 兼容 OpenAI 的客户端，支持提供商特定参数（`enableSearch`、`parallelToolCalls`、`enableThinking`）。 |
| [Ollama](https://ollama.com/)                       | [OllamaClient](api:prompt-executor-ollama-client::ai.koog.prompt.executor.ollama.client.OllamaClient)                            | ✓                | ✓         | -                    | ✓          | ✓          | -                                               | 本地服务器客户端，支持模型管理 API。                                                                             |## 运行提示词

要使用 LLM 客户端运行提示词，请执行以下操作：

1.  创建一个 LLM 客户端，用于处理您的应用程序与 LLM 提供商之间的连接。
2.  调用 `execute()` 方法，并将提示词和 LLM 作为参数传入。

以下是一个使用 `OpenAILLMClient` 运行提示词的示例：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.params.LLMParams
    import kotlinx.coroutines.runBlocking
    -->

    ```kotlin
    fun main() = runBlocking {
        // 创建一个 OpenAI 客户端
        val apiKey = System.getenv("OPENAI_API_KEY")
        val client = OpenAILLMClient(apiKey)

        // 创建一个提示词
        val prompt = prompt("prompt_name", LLMParams()) {
            // 添加系统消息以设置上下文
            system("You are a helpful assistant.")

            // 添加用户消息
            user("Tell me about Kotlin")

            // 您也可以添加助手消息作为少样本示例
            assistant("Kotlin is a modern programming language...")

            // 添加另一条用户消息
            user("What are its key features?")
        }

        // 运行提示词
        val response = client.execute(prompt, OpenAIModels.Chat.GPT4o)
        // 打印响应
        println(response)
    }
    ```
    <!--- KNIT example-llm-clients-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // 创建一个 OpenAI 客户端
    String apiKey = System.getenv("OPENAI_API_KEY");
    OpenAILLMClient client = new OpenAILLMClient(apiKey);

    // 创建一个提示词
    Prompt prompt = Prompt.builder("prompt_name")
        // 添加系统消息以设置上下文
        .system("You are a helpful assistant.")
        
        // 添加用户消息
        .user("Tell me about Kotlin")

        // 您也可以添加助手消息作为少样本示例
        .assistant("Kotlin is a modern programming language...")

        // 添加另一条用户消息
        .user("What are its key features?")
        .build();

    // 运行提示词
    List<Message.Response> response = client.execute(prompt, OpenAIModels.Chat.GPT4o, Collections.emptyList());
    // 打印响应
    System.out.println(response);

    client.close();
    ```
    <!--- KNIT example-llm-clients-java-01.java -->

## 流式响应 { #running-a-prompt }

!!! note
    适用于所有 LLM 客户端。

当您需要在生成时处理响应时，可以在 Kotlin 中使用 `executeStreaming()` 方法，或在 Java 中使用 `executeStreamingWithPublisher()` 来流式传输模型输出。

流式 API 提供不同类型的帧：

-   **增量帧** (`TextDelta`, `ReasoningDelta`, `ToolCallDelta`) — 以块形式到达的增量内容
-   **完整帧** (`TextComplete`, `ReasoningComplete`, `ToolCallComplete`) — 接收完所有增量后的完整内容
-   **结束帧** (`End`) — 表示流完成，并包含完成原因

对于支持推理的模型（例如 Claude Sonnet 4.5 或 GPT-o1），在流式传输期间会发出推理帧。
有关处理帧的更多详细信息，请参阅 [流式 API 文档](../streaming-api.md)。

=== "Kotlin"<!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.streaming.StreamFrame
    import kotlinx.coroutines.runBlocking
    fun main() = runBlocking {
    -->
<!--- SUFFIX
    }
    -->
```kotlin
// 使用您的 API 密钥设置 OpenAI 客户端
val token = System.getenv("OPENAI_API_KEY")
val client = OpenAILLMClient(token)

val response = client.executeStreaming(
    prompt = prompt("stream_demo") { user("Stream this response in short chunks.") },
    model = OpenAIModels.Chat.GPT4_1
)

response.collect { frame ->
    when (frame) {
        is StreamFrame.TextDelta -> print(frame.text)
        is StreamFrame.ReasoningDelta -> print("[Reasoning] ${frame.text}")
        is StreamFrame.ToolCallComplete -> println("\nTool call: ${frame.name}")
        is StreamFrame.End -> println("\n[done] Reason: ${frame.finishReason}")
        else -> {} // 根据需要处理其他帧类型
    }
}
```
<!--- KNIT example-llm-clients-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // 使用您的 API 密钥设置 OpenAI 客户端
    String token = System.getenv("OPENAI_API_KEY");
    OpenAILLMClient client = new OpenAILLMClient(token);

    Prompt prompt = Prompt.builder("stream_demo")
                .user("Stream this response in short chunks.")
                .build();
    
    Publisher<StreamFrame> response = client.executeStreamingWithPublisher(prompt, OpenAIModels.Chat.GPT4_1);

    // 订阅 Publisher 以消费帧
    response.subscribe(new Subscriber<StreamFrame>() {
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(StreamFrame frame) {
            switch (frame) {
                case StreamFrame.TextDelta delta ->
                        System.out.print(delta.getText());
                case StreamFrame.ReasoningDelta reasoning ->
                        System.out.print("[Reasoning] " + reasoning.getText());
                case StreamFrame.ToolCallComplete toolCall ->
                        System.out.println("\nTool call: " + toolCall.getName());
                case StreamFrame.End end ->
                        System.out.println("\n[done] Reason: " + end.getFinishReason());
                default -> {} // 处理其他帧类型
            }
        }

        @Override
        public void onError(Throwable t) {
            t.printStackTrace();
        }

        @Override
        public void onComplete() { }
    });
    ```
    <!--- KNIT example-llm-clients-java-02.java -->

## 多选响应 { #streaming-responses }

!!! note
    适用于除 `GoogleLLMClient`、`BedrockLLMClient` 和 `OllamaClient` 之外的所有 LLM 客户端

您可以通过使用 `executeMultipleChoices()` 方法，在单次调用中请求模型的多个备选响应。
这需要额外在执行的提示中指定 [`numberOfChoices`](prompt-creation/index.md#prompt-parameters) LLM 参数。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.params.LLMParams
    import kotlinx.coroutines.runBlocking
    -->
    ```kotlin
    fun main() = runBlocking {
        val apiKey = System.getenv("OPENAI_API_KEY")
        val client = OpenAILLMClient(apiKey)
``````kotlin
val choices = client.executeMultipleChoices(
    prompt = prompt("n_best", params = LLMParams(numberOfChoices = 3)) {
        system("你是一位创意助手。")
        user("给我三个不同的故事开篇句子。")
    },
    model = OpenAIModels.Chat.GPT4o
)

choices.forEachIndexed { i, choice ->
    val text = choice.joinToString(" ") { it.content }
    println("第 ${i + 1} 句: $text")
}
```
<!--- KNIT example-llm-clients-03.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    String apiKey = System.getenv("OPENAI_API_KEY");
    OpenAILLMClient client = new OpenAILLMClient(apiKey);

    // 配置参数（LLMParams 构造函数在 Java 中需要全部 8 个参数）
    LLMParams params = new LLMParams(
        null, // temperature
        null, // maxTokens
        3,    // numberOfChoices
        null, // speculation
        null, // schema
        null, // toolChoice
        null, // user
        null  // additionalProperties
    );

    Prompt prompt = Prompt.builder("n_best")
        .system("你是一位创意助手。")
        .user("给我三个不同的故事开篇句子。")
        .build()
        .withParams(params);

    // LLMChoice 是 List<Message.Response> 的类型别名
    List<List<Message.Response>> choices = client.executeMultipleChoices(
        prompt, 
        OpenAIModels.Chat.GPT4o
    );

    for (int i = 0; i < choices.size(); i++) {
        List<Message.Response> choice = choices.get(i);
        StringBuilder text = new StringBuilder();
        for (Message.Response msg : choice) {
            text.append(msg.getContent()).append(" ");
        }
        System.out.println("第 " + (i + 1) + " 句: " + text.toString().trim());
    }
    ```
    <!--- KNIT example-llm-clients-java-03.java -->

## 列出可用模型 { #multiple-choices }

!!! note
    适用于所有 LLM 客户端，除了 `AnthropicLLMClient`、`BedrockLLMClient` 和 `OllamaClient`。

要获取 LLM 客户端支持的可用模型 ID 列表，请使用 `models()` 方法：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.llm.LLModel
    import kotlinx.coroutines.runBlocking
    -->
    ```kotlin
    fun main() = runBlocking {
        val apiKey = System.getenv("OPENAI_API_KEY")
        val client = OpenAILLMClient(apiKey)

        val models: List<LLModel> = client.models()
        models.forEach { println(it.id) }
    }
    ```
    <!--- KNIT example-llm-clients-04.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    String apiKey = System.getenv("OPENAI_API_KEY");
    OpenAILLMClient client = new OpenAILLMClient(apiKey);

    List<LLModel> models = client.models();
    for (LLModel model : models) {
        System.out.println(model.getId());
    }
    ```
    <!--- KNIT example-llm-clients-java-04.java -->

## 嵌入 { #listing-available-models }

!!! note
    适用于 `OpenAILLMClient`、`GoogleLLMClient`、`BedrockLLMClient`、`MistralAILLMClient` 和 `OllamaClient`。

你可以使用 `embed()` 方法将文本转换为嵌入向量。
选择一个嵌入模型并将你的文本传递给此方法：<!--- INCLUDE
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import kotlinx.coroutines.runBlocking
-->
```kotlin
fun main() = runBlocking {
    val apiKey = System.getenv("OPENAI_API_KEY")
    val client = OpenAILLMClient(apiKey)

    val embedding = client.embed(
        text = "This is a sample text for embedding",
        model = OpenAIModels.Embeddings.TextEmbedding3Large
    )

    println("Embedding size: ${embedding.size}")
}
```
<!--- KNIT example-llm-clients-05.kt -->

## 内容审核 { #embeddings }

!!! note
    适用于以下 LLM 客户端：`OpenAILLMClient`、`BedrockLLMClient`、`MistralAILLMClient`、`OllamaClient`。

您可以使用 `moderate()` 方法配合审核模型来检查提示是否包含不当内容：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import kotlinx.coroutines.runBlocking
    -->
    ```kotlin
    fun main() = runBlocking {
        val apiKey = System.getenv("OPENAI_API_KEY")
        val client = OpenAILLMClient(apiKey)

        val result = client.moderate(
            prompt = prompt("moderation") {
                user("This is a test message that may contain offensive content.")
            },
            model = OpenAIModels.Moderation.Omni
        )

        println(result)
    }
    ```
    <!--- KNIT example-llm-clients-06.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    String apiKey = System.getenv("OPENAI_API_KEY");
    OpenAILLMClient client = new OpenAILLMClient(apiKey);

    Prompt prompt = Prompt.builder("moderation")
        .user("This is a test message that may contain offensive content.")
        .build();

    ModerationResult result = client.moderate(prompt, OpenAIModels.Moderation.Omni);
    System.out.println(result);
    ```
    <!--- KNIT example-llm-clients-java-05.java -->

## 与提示执行器集成 { #moderation }

[提示执行器](prompt-executors.md) 封装了 LLM 客户端，并提供额外功能，例如路由、回退机制以及跨提供商的统一使用方式。
建议在生产环境中使用它们，因为它们在处理多个提供商时提供了灵活性。

[^1]: 通过 OpenAI Moderation API 支持内容审核。
[^2]: 内容审核需要配置 Guardrails。
[^3]: 通过 Mistral `v1/moderations` 端点支持内容审核。