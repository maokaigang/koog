<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:59:15+00:00", "source_path": "embeddings.md", "source_sha256": "b8b7665901f313de35e840099dad9fea1b36d456f2c32feed0d145b5492eb90e", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 嵌入向量 { #embeddings }

`embeddings` 模块提供了生成和比较文本与代码嵌入向量的功能。嵌入向量是捕获语义含义的向量表示，可实现高效的相似性比较。

## 概述 { #overview }

该模块包含两个主要组件：

1. **embeddings-base**：嵌入向量的核心接口和数据结构。
2. **embeddings-llm**：使用 Ollama 进行本地嵌入向量生成的实现。

## 快速开始 { #getting-started }

以下部分包含如何使用嵌入向量的基本示例，涵盖以下方式：

- 通过 Ollama 使用本地嵌入模型
- 使用 OpenAI 嵌入模型

### 本地嵌入向量 { #local-embeddings }

要使用本地模型的嵌入功能，您需要在系统上安装并运行 Ollama。
有关安装和运行说明，请参阅 [官方 Ollama GitHub 仓库](https://github.com/ollama/ollama)。

```kotlin
fun main() {
    runBlocking {
        // Create an OllamaClient instance
        val client = OllamaClient()
        // Create an embedder
        val embedder = LLMEmbedder(client, OllamaModels.Embeddings.NOMIC_EMBED_TEXT)
        // Create embeddings
        val embedding = embedder.embed("This is the text to embed")
        // Print embeddings to the output
        println(embedding)
    }
}
```

要使用 Ollama 嵌入模型，请确保满足以下先决条件：

- 已安装并运行 [Ollama](https://ollama.com/download)
- 使用以下命令将嵌入模型下载到本地机器：
    ```bash
    ollama pull <ollama-model-id>
    ```

    将 `<ollama-model-id>` 替换为特定模型的 Ollama 标识符。有关可用嵌入模型及其标识符的更多信息，请参阅 [Ollama 模型概览](#ollama-models-overview)。

### Ollama 模型概览 { #ollama-models-overview }

下表提供了可用 Ollama 嵌入模型的概览。

| 模型 ID          | Ollama ID         | 参数量 | 维度 | 上下文长度 | 性能                                                                   | 权衡                                                                  |
|-------------------|-------------------|------------|------------|----------------|-----------------------------------------------------------------------|--------------------------------------------------------------------|
| NOMIC_EMBED_TEXT  | nomic-embed-text  | 137M       | 768        | 8192           | 适用于语义搜索和文本相似性任务的高质量嵌入向量                             | 在质量和效率之间取得平衡                                                |
| ALL_MINILM        | all-minilm        | 33M        | 384        | 512            | 推理速度快，通用文本嵌入质量良好                                          | 模型尺寸较小，上下文长度有限，但非常高效                                 |
| MULTILINGUAL_E5   | zylonai/multilingual-e5-large   | 300M       | 768        | 512            | 在 100 多种语言中表现强劲                                                | 模型尺寸较大，但提供出色的多语言能力                                     |
| BGE_LARGE         | bge-large         | 335M       | 1024       | 512            | 适用于英文文本检索和语义搜索的卓越性能                                    | 模型尺寸较大，但提供高质量的嵌入向量                                     |
| MXBAI_EMBED_LARGE | mxbai-embed-large | -          | -          | -              | 文本数据的高维嵌入向量                                                    | 专为创建高维嵌入向量而设计                                              |

有关这些模型的更多信息，请参阅 Ollama 的 [嵌入模型](https://ollama.com/blog/embedding-models) 博客文章。### 选择模型

以下是根据您的需求选择 Ollama 嵌入模型的一些通用建议：

- 对于通用文本嵌入，使用 `NOMIC_EMBED_TEXT`。
- 对于多语言支持，使用 `MULTILINGUAL_E5`。
- 对于最高质量（以性能为代价），使用 `BGE_LARGE`。
- 对于最高效率（以部分质量为代价），使用 `ALL_MINILM`。
- 对于高维嵌入，使用 `MXBAI_EMBED_LARGE`。

## OpenAI 嵌入

要使用 OpenAI 嵌入模型创建嵌入，请使用 `OpenAILLMClient` 实例的 `embed` 方法，如下例所示。

```kotlin
suspend fun openAIEmbed(text: String) {
    // Get the OpenAI API token from the OPENAI_KEY environment variable
    val token = System.getenv("OPENAI_KEY") ?: error("Environment variable OPENAI_KEY is not set")
    // Create an OpenAILLMClient instance
    val client = OpenAILLMClient(token)
    // Create an embedder
    val embedder = LLMEmbedder(client, OpenAIModels.Embeddings.TextEmbeddingAda002)
    // Create embeddings
    val embedding = embedder.embed(text)
    // Print embeddings to the output
    println(embedding)
}
```

## AWS Bedrock 嵌入 { #openai-embeddings }

要使用 AWS Bedrock 嵌入模型创建嵌入，请使用 `BedrockLLMClient` 实例的 `embed` 方法以及您选择的模型。示例：

```kotlin
suspend fun bedrockEmbed(text: String) {
    // Get AWS credentials from environment/configuration
    val awsAccessKeyId = System.getenv("AWS_ACCESS_KEY_ID") ?: error("AWS_ACCESS_KEY_ID not set")
    val awsSecretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY") ?: error("AWS_SECRET_ACCESS_KEY not set")
    // (Optional) AWS_SESSION_TOKEN for temporary credentials
    val awsSessionToken = System.getenv("AWS_SESSION_TOKEN")
    // Create a BedrockLLMClient instance
    val client = BedrockLLMClient(
        identityProvider = StaticCredentialsProvider {
            this.accessKeyId = awsAccessKeyId
            this.secretAccessKey = awsSecretAccessKey
            awsSessionToken?.let { this.sessionToken = it }
        },
        settings = BedrockClientSettings()
    )
    // Create an embedder
    val embedder = LLMEmbedder(client, BedrockModels.Embeddings.AmazonTitanEmbedText)
    // Create embeddings
    val embedding = embedder.embed(text)
    // Print embeddings to the output
    println(embedding)
}
```

### 支持的 AWS Bedrock 嵌入模型

| 提供商   | 模型名称                     | 模型 ID                         | 输入  | 输出      | 维度       | 上下文长度     | 备注                                                                                                 |
|----------|------------------------------|--------------------------------|-------|-----------|------------|----------------|-------------------------------------------------------------------------------------------------------|
| Amazon   | Titan Embeddings G1 - Text   | `amazon.titan-embed-text-v1`   | 文本  | 嵌入      | 1,536      | 8192           | 支持 25+ 种语言，针对检索、语义相似性、聚类优化；长文档需分段处理以用于搜索。                         |
| Amazon   | Titan Text Embeddings V2     | `amazon.titan-embed-text-v2:0` | 文本  | 嵌入      | 1,024      | 8192           | 高精度、灵活维度、多语言（100+）；较小维度节省存储空间，输出归一化。                                   |
| Cohere   | Cohere Embed English v3      | `cohere.embed-english-v3`      | 文本  | 嵌入      | 1,024      | 8192           | SOTA 英语文本嵌入，适用于搜索、检索和理解文本细微差别。                               |
| Cohere   | Cohere Embed Multilingual v3 | `cohere.embed-multilingual-v3` | 文本  | 嵌入      | 1,024      | 8192           | 多语言嵌入，SOTA 适用于跨语言的搜索和语义理解。                                       |

> 有关最新的模型支持信息，请参阅 [AWS Bedrock 支持的模型文档](https://docs.aws.amazon.com/bedrock/latest/userguide/models-supported.html)。

## 示例

以下示例展示了如何使用嵌入来比较代码与文本或其他代码片段。

### 代码到文本比较

比较代码片段与自然语言描述，以找到语义匹配：

```kotlin
suspend fun compareCodeToText(embedder: Embedder) { // Embedder type
    // Code snippet
    val code = """
        fun factorial(n: Int): Int {
            return if (n <= 1) 1 else n * factorial(n - 1)
        }
    """.trimIndent()

    // Text descriptions
    val description1 = "A recursive function that calculates the factorial of a number"
    val description2 = "A function that sorts an array of integers"

    // Generate embeddings
    val codeEmbedding = embedder.embed(code)
    val desc1Embedding = embedder.embed(description1)
    val desc2Embedding = embedder.embed(description2)

    // Calculate differences (lower value means more similar)
    val diff1 = embedder.diff(codeEmbedding, desc1Embedding)
    val diff2 = embedder.diff(codeEmbedding, desc2Embedding)

    println("Difference between code and description 1: $diff1")
    println("Difference between code and description 2: $diff2")

    // The code should be more similar to description1 than description2
    if (diff1 < diff2) {
        println("The code is more similar to: '$description1'")
    } else {
        println("The code is more similar to: '$description2'")
    }
}
```

### 代码到代码比较 { #code-to-text-comparison }

比较代码片段以找到语义相似性，无论语法差异如何：

```kotlin
suspend fun compareCodeToCode(embedder: Embedder) { // Embedder type
    // Two implementations of the same algorithm in different languages
    val kotlinCode = """
        fun fibonacci(n: Int): Int {
            return if (n <= 1) n else fibonacci(n - 1) + fibonacci(n - 2)
        }
    """.trimIndent()

    val pythonCode = """
        def fibonacci(n):
            if n <= 1:
                return n
            else:
                return fibonacci(n-1) + fibonacci(n-2)
    """.trimIndent()

    val javaCode = """
        public static int bubbleSort(int[] arr) {
            int n = arr.length;
            for (int i = 0; i < n-1; i++) {
                for (int j = 0; j < n-i-1; j++) {
                    if (arr[j] > arr[j+1]) {
                        int temp = arr[j];
                        arr[j] = arr[j+1];
                        arr[j+1] = temp;
                    }
                }
            }
            return arr;
        }
    """.trimIndent()

    // Generate embeddings
    val kotlinEmbedding = embedder.embed(kotlinCode)
    val pythonEmbedding = embedder.embed(pythonCode)
    val javaEmbedding = embedder.embed(javaCode)

    // Calculate differences
    val diffKotlinPython = embedder.diff(kotlinEmbedding, pythonEmbedding)
    val diffKotlinJava = embedder.diff(kotlinEmbedding, javaEmbedding)

    println("Difference between Kotlin and Python implementations: $diffKotlinPython")
    println("Difference between Kotlin and Java implementations: $diffKotlinJava")

    // The Kotlin and Python implementations should be more similar
    if (diffKotlinPython < diffKotlinJava) {
        println("The Kotlin code is more similar to the Python code")
    } else {
        println("The Kotlin code is more similar to the Java code")
    }
}
```

## API 文档

有关嵌入的完整 API 参考，请参阅以下模块的参考文档：- [embeddings-base](api:embeddings-base::ai.koog.embeddings.base)：提供核心接口与数据结构，用于表示和比较文本及代码的嵌入向量。
- [embeddings-llm](api:embeddings-llm::)：包含用于处理本地嵌入模型的相关实现。