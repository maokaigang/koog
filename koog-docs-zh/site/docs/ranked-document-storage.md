<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:15:00+00:00", "source_path": "ranked-document-storage.md", "source_sha256": "0398ca5df5643b28133d7396e494dfde751bdb9ae9972baa2b9ec0e658a65e0a", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 文档存储 { #document-storage }

为了让您能够为大型语言模型（LLM）提供最新且可搜索的信息源，Koog 支持资源增强生成（RAG）来存储和检索文档中的信息。

## 关键 RAG 特性 { #key-rag-features }

一个典型的 RAG 系统的核心组件包括：

- **文档存储**：包含信息的文档、文件或文本块的存储库。
- **向量嵌入**：捕获语义的文本数值表示。有关 Koog 中嵌入的更多信息，请参阅[嵌入](embeddings.md)。
- **检索机制**：根据查询查找最相关文档的系统。
- **生成组件**：使用检索到的信息生成响应的 LLM。

RAG 解决了传统 LLM 的几个限制：

- **知识截止**：RAG 可以访问最新信息，不受训练数据限制。
- **幻觉**：通过将响应基于检索到的文档，RAG 减少了虚构信息。
- **领域特异性**：RAG 可以通过定制知识库来适应特定领域。
- **透明度**：可以引用信息来源，使系统更具可解释性。

## 在 RAG 系统中查找信息 { #finding-information-in-a-rag-system }

在 RAG 系统中查找相关信息涉及将文档存储为向量嵌入，并根据它们与用户查询的相似性进行排序。这种方法适用于各种文档类型，包括 PDF、图像、文本文件，甚至单个文本块。

该过程包括：

1. **文档嵌入**：将文档转换为捕获其语义的向量表示。
2. **向量存储**：高效存储这些嵌入以便快速检索。
3. **相似性搜索**：查找与查询嵌入最相似的文档嵌入。
4. **排序**：根据相关性分数对文档进行排序。

## 在 Koog 中实现 RAG 系统 { #implementing-a-rag-system-in-koog }

要在 Koog 中实现 RAG 系统，请按照以下步骤操作：

1. 使用 Ollama 或 OpenAI 创建一个嵌入器。嵌入器是 `LLMEmbedder` 类的实例，它接受一个 LLM 客户端实例和模型作为参数。有关更多信息，请参阅[嵌入](embeddings.md)。
2. 基于创建的通用嵌入器创建一个文档嵌入器。
3. 创建一个文档存储。
4. 将文档添加到存储中。
5. 使用定义的查询查找最相关的文档。

这一系列步骤代表了一个*相关性搜索*流程，该流程返回给定用户查询的最相关文档。以下是一个代码示例，展示了如何实现上述描述的整个步骤序列：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.embeddings.local.LLMEmbedder
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import ai.koog.prompt.executor.ollama.client.OllamaClient
    import ai.koog.rag.base.mostRelevantDocuments
    import ai.koog.rag.vector.EmbeddingBasedDocumentStorage
    import ai.koog.rag.vector.InMemoryVectorStorage
    import ai.koog.rag.vector.JVMTextDocumentEmbedder
    import kotlinx.coroutines.runBlocking
    import java.nio.file.Path
    fun main() {
        runBlocking {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```kotlin
    // Create an embedder using Ollama
    val embedder = LLMEmbedder(OllamaClient(), OllamaModels.Embeddings.NOMIC_EMBED_TEXT)
    // You may also use OpenAI embeddings with:
    // val embedder = LLMEmbedder(OpenAILLMClient("API_KEY"), OpenAIModels.Embeddings.TextEmbeddingAda3Large)

    // Create a JVM-specific document embedder
    val documentEmbedder = JVMTextDocumentEmbedder(embedder)

    // Create a ranked document storage using in-memory vector storage
    val rankedDocumentStorage = EmbeddingBasedDocumentStorage(documentEmbedder, InMemoryVectorStorage())

    // Store documents in the storage
    rankedDocumentStorage.store(Path.of("./my/documents/doc1.txt"))
    rankedDocumentStorage.store(Path.of("./my/documents/doc2.txt"))
    rankedDocumentStorage.store(Path.of("./my/documents/doc3.txt"))
    // ... store more documents as needed
    rankedDocumentStorage.store(Path.of("./my/documents/doc100.txt"))

    // Find the most relevant documents for a user query
    val query = "I want to open a bank account but I'm getting a 404 when I open your website. I used to be your client with a different account 5 years ago before you changed your firm name"
    val relevantFiles = rankedDocumentStorage.mostRelevantDocuments(query, count = 3)

    // Process the relevant files
    relevantFiles.forEach { file ->
        println("Relevant file: ${file.toAbsolutePath()}")
        // Process the file content as needed
    }
    ```
    <!--- KNIT example-ranked-document-storage-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-ranked-document-storage-java-01.java -->


### Providing relevance search for use by AI agents

Once you have a ranked document storage system, you can use it to provide relevant context to an AI agent for answering user queries. This enhances the agent's ability to provide accurate and contextually appropriate responses.

Here is an example of how to implement the defined RAG system for an AI agent to be able to answer queries by getting information from the document storage:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.agent.config.AIAgentConfig
    import ai.koog.embeddings.local.LLMEmbedder
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import ai.koog.prompt.executor.ollama.client.OllamaClient
    import ai.koog.rag.base.mostRelevantDocuments
    import ai.koog.rag.vector.EmbeddingBasedDocumentStorage
    import ai.koog.rag.vector.InMemoryVectorStorage
    import ai.koog.rag.vector.JVMTextDocumentEmbedder
    import kotlin.io.path.pathString
    // Create an embedder using Ollama
    val embedder = LLMEmbedder(OllamaClient(), OllamaModels.Embeddings.NOMIC_EMBED_TEXT)
    // You may also use OpenAI embeddings with:
    // val embedder = LLMEmbedder(OpenAILLMClient("API_KEY"), OpenAIModels.Embeddings.TextEmbeddingAda3Large)
    // Create a JVM-specific document embedder
    val documentEmbedder = JVMTextDocumentEmbedder(embedder)
    // Create a ranked document storage using in-memory vector storage
    val rankedDocumentStorage = EmbeddingBasedDocumentStorage(documentEmbedder, InMemoryVectorStorage())
    const val apiKey = "apikey"
    -->
    ```kotlin
    suspend fun solveUserRequest(query: String) {
        // Retrieve top-5 documents from the document provider
        val relevantDocuments = rankedDocumentStorage.mostRelevantDocuments(query, count = 5)

        // Create an AI Agent with the relevant context
        val agentConfig = AIAgentConfig(
            prompt = prompt("context") {
                system("You are a helpful assistant. Use the provided context to answer the user's question accurately.")
                user {
                    +"Relevant context:"
                    relevantDocuments.forEach {
                        file(it.pathString, "text/plain")
                    }
                }
            },
            model = OpenAIModels.Chat.GPT4o, // Or a different model of your choice
            maxAgentIterations = 100,
        )

        val agent = AIAgent(
            promptExecutor = simpleOpenAIExecutor(apiKey),
            llmModel = OpenAIModels.Chat.GPT4o
        )


        // Run the agent to get a response
        val response = agent.run(query)

        // Return or process the response
        println("Agent response: $response")
    }
    ```
    <!--- KNIT example-ranked-document-storage-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-ranked-document-storage-java-02.java -->


### Providing relevance search as a tool

Instead of directly providing document content as context, you can also implement a tool that allows the agent to perform relevance searches on demand. This gives the agent more flexibility in deciding when and how to use the document storage.

Here is an example of how to implement a relevance search tool:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.tools.ToolRegistry
    import ai.koog.agents.core.tools.annotations.LLMDescription
    import ai.koog.agents.core.tools.annotations.Tool
    import ai.koog.agents.core.tools.reflect.asTool
    import ai.koog.embeddings.local.LLMEmbedder
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import ai.koog.prompt.executor.ollama.client.OllamaClient
    import ai.koog.rag.base.mostRelevantDocuments
    import ai.koog.rag.vector.EmbeddingBasedDocumentStorage
    import ai.koog.rag.vector.InMemoryVectorStorage
    import ai.koog.rag.vector.JVMTextDocumentEmbedder
    import kotlinx.coroutines.runBlocking
    import java.nio.file.Files
    // Create an embedder using Ollama
    val embedder = LLMEmbedder(OllamaClient(), OllamaModels.Embeddings.NOMIC_EMBED_TEXT)
    // You may also use OpenAI embeddings with:
    // val embedder = LLMEmbedder(OpenAILLMClient("API_KEY"), OpenAIModels.Embeddings.TextEmbeddingAda3Large)
    // Create a JVM-specific document embedder
    val documentEmbedder = JVMTextDocumentEmbedder(embedder)
    // Create a ranked document storage using in-memory vector storage
    val rankedDocumentStorage = EmbeddingBasedDocumentStorage(documentEmbedder, InMemoryVectorStorage())
    const val apiKey = "apikey"
    -->
    ```kotlin
    @Tool
    @LLMDescription("Search for relevant documents about any topic (if exists). Returns the content of the most relevant documents.")
    suspend fun searchDocuments(
        @LLMDescription("Query to search relevant documents about")
        query: String,
        @LLMDescription("Maximum number of documents")
        count: Int
    ): String {
        val relevantDocuments =
            rankedDocumentStorage.mostRelevantDocuments(query, count = count, similarityThreshold = 0.9).toList()

        if (!relevantDocuments.isEmpty()) {
            return "No relevant documents found for the query: $query"
        }

        val result = StringBuilder("Found ${relevantDocuments.size} relevant documents:\n\n")

        relevantDocuments.forEachIndexed { index, document ->
            val content = Files.readString(document)
            result.append("Document ${index + 1}: ${document.fileName}\n")
            result.append("Content: $content\n\n")
        }

        return result.toString()
    }

    fun main() {
        runBlocking {
            val tools = ToolRegistry {
                tool(::searchDocuments.asTool())
            }

            val agent = AIAgent(
                toolRegistry = tools,
                promptExecutor = simpleOpenAIExecutor(apiKey),
                llmModel = OpenAIModels.Chat.GPT4o
            )

            val response = agent.run("How to make a cake?")
            println("Agent response: $response")

        }
    }
    ```
    <!--- KNIT example-ranked-document-storage-03.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-ranked-document-storage-java-03.java -->

With this approach, the agent can decide when to use the search tool based on your query. This is particularly useful for complex queries that may require information from multiple documents or when the agent needs to search for specific details.

## Existing implementations of vector storage and document embedding providers

For convenience and easier implementation of a RAG system, Koog provides several out-of-the-box implementations for vector storage, document embedding, and combined embedding and storage components.

### Vector storage

#### InMemoryVectorStorage

A simple in-memory implementation that stores documents and their vector embeddings in memory. Suitable for testing or small-scale applications.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.rag.vector.InMemoryVectorStorage
    import java.nio.file.Path
    -->
    ```kotlin
    val inMemoryStorage = InMemoryVectorStorage<Path>()
    ```
    <!--- KNIT example-ranked-document-storage-04.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    InMemoryVectorStorage<Path> inMemoryStorage = new InMemoryVectorStorage<>();
    ```
    <!--- KNIT example-ranked-document-storage-java-04.java -->

For more information, see the [InMemoryVectorStorage](api:vector-storage::ai.koog.rag.vector.InMemoryVectorStorage) reference.

#### FileVectorStorage

A file-based implementation that stores documents and their vector embeddings on disk. Suitable for persistent storage across application restarts.

=== "Kotlin"

    <!--- INCLUDE
    /*
    -->
    <!--- SUFFIX
    */
    -->
    ```kotlin
    val fileStorage = FileVectorStorage<Document, Path>(
       documentReader = documentProvider,
       fs = fileSystemProvider,
       root = rootPath
    )
    ```
    <!--- KNIT example-ranked-document-storage-05.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-ranked-document-storage-java-05.java -->

For more information, see the [FileVectorStorage](api:vector-storage::ai.koog.rag.vector.FileVectorStorage) reference.

#### JVMFileVectorStorage

A JVM-specific implementation of `FileVectorStorage` that works with `java.nio.file.Path`.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.rag.vector.JVMFileVectorStorage
    import java.nio.file.Path
    -->
    ```kotlin
    val jvmFileStorage = JVMFileVectorStorage(root = Path.of("/path/to/storage"))
    ```
    <!--- KNIT example-ranked-document-storage-06.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-ranked-document-storage-java-06.java -->

For more information, see the [JVMFileVectorStorage](api:vector-storage::ai.koog.rag.vector.JVMFileVectorStorage) reference.

### Document embedder

#### TextDocumentEmbedder

A generic implementation that works with any document type that can be converted to text.

=== "Kotlin"

    <!--- INCLUDE
    /*
    -->
    <!--- SUFFIX
    */
    -->
    ```kotlin
    val textEmbedder = TextDocumentEmbedder<Document, Path>(
       documentReader = documentProvider,
       embedder = embedder
    )
    ```
    <!--- KNIT example-ranked-document-storage-07.kt -->

=== "Java"

    <!--- INCLUDE
    -->
    ```java
    ```
    <!--- KNIT example-ranked-document-storage-java-07.java -->

For more information, see the [TextDocumentEmbedder](api:vector-storage::ai.koog.rag.vector.TextDocumentEmbedder) reference.

#### JVMTextDocumentEmbedder

A JVM-specific implementation that works with `java.nio.file.Path`.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.embeddings.local.LLMEmbedder
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import ai.koog.prompt.executor.ollama.client.OllamaClient
    import ai.koog.rag.vector.JVMTextDocumentEmbedder
    -->
    ```kotlin
    val embedder = LLMEmbedder(OllamaClient(), OllamaModels.Embeddings.NOMIC_EMBED_TEXT)
    val jvmTextEmbedder = JVMTextDocumentEmbedder(embedder = embedder)
    ```
    <!--- KNIT example-ranked-document-storage-08.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    LLMEmbedder embedder = new LLMEmbedder(new OllamaClient("http://localhost:11434"), OllamaModels.Embeddings.NOMIC_EMBED_TEXT);
    JVMTextDocumentEmbedder jvmTextEmbedder = new JVMTextDocumentEmbedder(embedder);
    ```
    <!--- KNIT example-ranked-document-storage-java-08.java -->

For more information, see the [JVMTextDocumentEmbedder](api:vector-storage::ai.koog.rag.vector.JVMTextDocumentEmbedder) reference.

### Combined storage implementations

#### EmbeddingBasedDocumentStorage

Combines a document embedder and a vector storage to provide a complete solution for storing and ranking documents.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.example.exampleRankedDocumentStorage02.documentEmbedder
    import ai.koog.rag.vector.EmbeddingBasedDocumentStorage
    import ai.koog.rag.vector.InMemoryVectorStorage
    import java.nio.file.Path
    val vectorStorage = InMemoryVectorStorage<Path>()
    -->
    ```kotlin
    val embeddingStorage = EmbeddingBasedDocumentStorage(
        embedder = documentEmbedder,
        storage = vectorStorage
    )
    ```
    <!--- KNIT example-ranked-document-storage-09.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    LLMEmbedder embedder = new LLMEmbedder(new OllamaClient("http://localhost:11434"), OllamaModels.Embeddings.NOMIC_EMBED_TEXT);
    JVMTextDocumentEmbedder documentEmbedder = new JVMTextDocumentEmbedder(embedder);
    InMemoryVectorStorage<Path> vectorStorage = new InMemoryVectorStorage<>();
    
    EmbeddingBasedDocumentStorage<Path> embeddingStorage = new EmbeddingBasedDocumentStorage<>(
        documentEmbedder,
        vectorStorage
    );
    ```
    <!--- KNIT example-ranked-document-storage-java-09.java -->

For more information, see the [EmbeddingBasedDocumentStorage](api:vector-storage::ai.koog.rag.vector.EmbeddingBasedDocumentStorage) reference.

#### InMemoryDocumentEmbeddingStorage

An in-memory implementation of `EmbeddingBasedDocumentStorage`.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.example.exampleRankedDocumentStorage03.documentEmbedder
    import ai.koog.rag.vector.InMemoryDocumentEmbeddingStorage
    import java.nio.file.Path
    typealias Document = Path
    -->
    ```kotlin
    val inMemoryEmbeddingStorage = InMemoryDocumentEmbeddingStorage<Document>(
        embedder = documentEmbedder
    )
    ```
    <!--- KNIT example-ranked-document-storage-10.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    LLMEmbedder embedder = new LLMEmbedder(new OllamaClient("http://localhost:11434"), OllamaModels.Embeddings.NOMIC_EMBED_TEXT);
    JVMTextDocumentEmbedder documentEmbedder = new JVMTextDocumentEmbedder(embedder);

    InMemoryDocumentEmbeddingStorage<Path> inMemoryEmbeddingStorage =
        new InMemoryDocumentEmbeddingStorage<>(documentEmbedder);
    ```
    <!--- KNIT example-ranked-document-storage-java-10.java -->

For more information, see the [InMemoryDocumentEmbeddingStorage](api:vector-storage::ai.koog.rag.vector.InMemoryDocumentEmbeddingStorage) reference.

#### FileDocumentEmbeddingStorage

A file-based implementation of `EmbeddingBasedDocumentStorage`.

=== "Kotlin"

    <!--- INCLUDE
    /*
    -->
    <!--- SUFFIX
    */
    -->
    ```kotlin
    val fileEmbeddingStorage = FileDocumentEmbeddingStorage<Document, Path>(
       embedder = documentEmbedder,
       documentProvider = documentProvider,
       fs = fileSystemProvider,
       root = rootPath
    )
    ```
    <!--- KNIT example-ranked-document-storage-11.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-ranked-document-storage-java-11.java -->

For more information, see the [FileDocumentEmbeddingStorage](api:vector-storage::ai.koog.rag.vector.FileDocumentEmbeddingStorage) reference.

#### JVMFileDocumentEmbeddingStorage

A JVM-specific implementation of `FileDocumentEmbeddingStorage`.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.example.exampleRankedDocumentStorage03.documentEmbedder
    import ai.koog.rag.vector.JVMFileDocumentEmbeddingStorage
    import java.nio.file.Path
    -->
    ```kotlin
    val jvmFileEmbeddingStorage = JVMFileDocumentEmbeddingStorage(
       embedder = documentEmbedder,
       root = Path.of("/path/to/storage")
    )
    ```
    <!--- KNIT example-ranked-document-storage-12.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    LLMEmbedder embedder = new LLMEmbedder(new OllamaClient("http://localhost:11434"), OllamaModels.Embeddings.NOMIC_EMBED_TEXT);
    JVMTextDocumentEmbedder documentEmbedder = new JVMTextDocumentEmbedder(embedder);

    JVMFileDocumentEmbeddingStorage jvmFileEmbeddingStorage = new JVMFileDocumentEmbeddingStorage(
       documentEmbedder,
       Path.of("/path/to/storage")
    );
    ```
    <!--- KNIT example-ranked-document-storage-java-12.java -->

For more information, see the [JVMFileDocumentEmbeddingStorage](api:vector-storage::ai.koog.rag.vector.JVMFileDocumentEmbeddingStorage) reference.

#### JVMTextFileDocumentEmbeddingStorage

A JVM-specific implementation that combines `JVMTextDocumentEmbedder` and `JVMFileVectorStorage`.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.example.exampleRankedDocumentStorage08.embedder
    import ai.koog.rag.vector.JVMTextFileDocumentEmbeddingStorage
    import java.nio.file.Path
    -->
    ```kotlin
    val jvmTextFileEmbeddingStorage = JVMTextFileDocumentEmbeddingStorage(
       embedder = embedder,
       root = Path.of("/path/to/storage")
    )
    ```
    <!--- KNIT example-ranked-document-storage-13.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    LLMEmbedder embedder = new LLMEmbedder(new OllamaClient("http://localhost:11434"), OllamaModels.Embeddings.NOMIC_EMBED_TEXT);

    JVMTextFileDocumentEmbeddingStorage jvmTextFileEmbeddingStorage = new JVMTextFileDocumentEmbeddingStorage(
       embedder,
       Path.of("/path/to/storage")
    );
    ```
    <!--- KNIT example-ranked-document-storage-java-13.java -->

For more information, see the [JVMTextFileDocumentEmbeddingStorage](api:vector-storage::ai.koog.rag.vector.JVMTextFileDocumentEmbeddingStorage) reference.

These implementations provide a flexible and extensible framework for working with document embeddings and vector storage in various environments.

## Implementing your own vector storage and document embedder

You can extend Koog's vector storage framework by implementing your own custom document embedders and vector storage solutions. This is particularly useful when working with specialized document types or storage requirements.

Here's an example of implementing a custom document embedder for PDF documents:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.embeddings.base.Embedder
    import ai.koog.embeddings.base.Vector
    import ai.koog.embeddings.local.LLMEmbedder
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import ai.koog.prompt.executor.ollama.client.OllamaClient
    import ai.koog.rag.base.RankedDocument
    import ai.koog.rag.base.RankedDocumentStorage
    import ai.koog.rag.base.files.DocumentProvider
    import ai.koog.rag.base.mostRelevantDocuments
    import ai.koog.rag.vector.DocumentEmbedder
    import ai.koog.rag.vector.InMemoryVectorStorage
    import ai.koog.rag.vector.VectorStorage
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.flow
    import java.nio.file.Path
    -->
    ```kotlin
    // Define a PDFDocument class
    class PDFDocument(private val path: Path) {
        fun readText(): String {
            // Use a PDF library to extract text from the PDF
            return "Text extracted from PDF at $path"
        }
    }

    // Implement a DocumentProvider for PDFDocument
    class PDFDocumentProvider : DocumentProvider<Path, PDFDocument> {
        override suspend fun document(path: Path): PDFDocument? {
            return if (path.toString().endsWith(".pdf")) {
                PDFDocument(path)
            } else {
                null
            }
        }

        override suspend fun text(document: PDFDocument): CharSequence {
            return document.readText()
        }
    }

    // Implement a DocumentEmbedder for PDFDocument
    class PDFDocumentEmbedder(private val embedder: Embedder) : DocumentEmbedder<PDFDocument> {
        override suspend fun embed(document: PDFDocument): Vector {
            val text = document.readText()
            return embed(text)
        }

        override suspend fun embed(text: String): Vector {
            return embedder.embed(text)
        }

        override fun diff(embedding1: Vector, embedding2: Vector): Double {
            return embedder.diff(embedding1, embedding2)
        }
    }

    // Create a custom vector storage for PDF documents
    class PDFVectorStorage(
        private val pdfProvider: PDFDocumentProvider,
        private val embedder: PDFDocumentEmbedder,
        private val storage: VectorStorage<PDFDocument>
    ) : RankedDocumentStorage<PDFDocument> {
        override fun rankDocuments(query: String): Flow<RankedDocument<PDFDocument>> = flow {
            val queryVector = embedder.embed(query)
            storage.allDocumentsWithPayload().collect { (document, documentVector) ->
                emit(
                    RankedDocument(
                        document = document,
                        similarity = 1.0 - embedder.diff(queryVector, documentVector)
                    )
                )
            }
        }

        override suspend fun store(document: PDFDocument, data: Unit): String {
            val vector = embedder.embed(document)
            return storage.store(document, vector)
        }

        override suspend fun delete(documentId: String): Boolean {
            return storage.delete(documentId)
        }

        override suspend fun read(documentId: String): PDFDocument? {
            return storage.read(documentId)
        }

        override fun allDocuments(): Flow<PDFDocument> = flow {
            storage.allDocumentsWithPayload().collect {
                emit(it.document)
            }
        }
    }

    // Usage example
    suspend fun main() {
        val pdfProvider = PDFDocumentProvider()
        val embedder = LLMEmbedder(OllamaClient(), OllamaModels.Embeddings.NOMIC_EMBED_TEXT)
        val pdfEmbedder = PDFDocumentEmbedder(embedder)
        val storage = InMemoryVectorStorage<PDFDocument>()
        val pdfStorage = PDFVectorStorage(pdfProvider, pdfEmbedder, storage)

        // Store PDF documents
        val pdfDocument = PDFDocument(Path.of("./documents/sample.pdf"))
        pdfStorage.store(pdfDocument)

        // Query for relevant PDF documents
        val relevantPDFs = pdfStorage.mostRelevantDocuments("information about climate change", count = 3)

    }
    ```
    <!--- KNIT example-ranked-document-storage-14.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-ranked-document-storage-java-14.java -->

## Implementing custom non-embedding-based RankedDocumentStorage

While embedding-based document ranking is powerful, there are scenarios where you might want to implement a custom ranking mechanism that does not rely on embeddings. For example, you might want to rank documents based on:

- PageRank-like algorithms
- Keyword frequency
- Recency of documents
- User interaction history
- Domain-specific heuristics

Here's an example of implementing a custom `RankedDocumentStorage` that uses a simple keyword-based ranking approach:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.rag.base.DocumentStorage
    import ai.koog.rag.base.RankedDocument
    import ai.koog.rag.base.RankedDocumentStorage
    import ai.koog.rag.base.files.DocumentProvider
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.flow
    import java.nio.file.Path
    -->
    ```kotlin
    class KeywordBasedDocumentStorage<Document>(
        private val documentProvider: DocumentProvider<Path, Document>,
        private val storage: DocumentStorage<Document>
    ) : RankedDocumentStorage<Document> {

        override fun rankDocuments(query: String): Flow<RankedDocument<Document>> = flow {
            // Split the query into keywords
            val keywords = query.lowercase().split(Regex("\\W+")).filter { it.length > 2 }

            // Process each document
            storage.allDocuments().collect { document ->
                // Get the document text
                val documentText = documentProvider.text(document).toString().lowercase()

                // Calculate a simple similarity score based on keyword frequency
                var similarity = 0.0
                for (keyword in keywords) {
                    val count = countOccurrences(documentText, keyword)
                    if (count > 0) {
                        similarity += count.toDouble() / documentText.length * 1000
                    }
                }

                // Emit the document with its similarity score
                emit(RankedDocument(document, similarity))
            }
        }

        private fun countOccurrences(text: String, keyword: String): Int {
            var count = 0
            var index = 0
            while (index != -1) {
                index = text.indexOf(keyword, index)
                if (index != -1) {
                    count++
                    index += keyword.length
                }
            }
            return count
        }

        override suspend fun store(document: Document, data: Unit): String {
            return storage.store(document)
        }

        override suspend fun delete(documentId: String): Boolean {
            return storage.delete(documentId)
        }

        override suspend fun read(documentId: String): Document? {
            return storage.read(documentId)
        }

        override fun allDocuments(): Flow<Document> {
            return storage.allDocuments()
        }
    }
    ```
    <!--- KNIT example-ranked-document-storage-15.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-ranked-document-storage-java-15.java -->

This implementation ranks documents based on the frequency of keywords from the query appearing in the document text. You could extend this approach with more sophisticated algorithms like TF-IDF (Term Frequency-Inverse Document Frequency) or BM25.

Another example is a time-based ranking system that prioritizes recent documents:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.rag.base.DocumentStorage
    import ai.koog.rag.base.RankedDocument
    import ai.koog.rag.base.RankedDocumentStorage
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.flow
    import java.lang.System.currentTimeMillis
    -->
    ```kotlin
    class TimeBasedDocumentStorage<Document>(
        private val storage: DocumentStorage<Document>,
        private val getDocumentTimestamp: (Document) -> Long
    ) : RankedDocumentStorage<Document> {

        override fun rankDocuments(query: String): Flow<RankedDocument<Document>> = flow {
            val currentTime = System.currentTimeMillis()

            storage.allDocuments().collect { document ->
                val timestamp = getDocumentTimestamp(document)
                val ageInHours = (currentTime - timestamp) / (1000.0 * 60 * 60)

                // Calculate a decay factor based on age (newer documents get higher scores)
                val decayFactor = Math.exp(-0.01 * ageInHours)

                emit(RankedDocument(document, decayFactor))
            }
        }

        // Implement other required methods from RankedDocumentStorage
        override suspend fun store(document: Document, data: Unit): String {
            return storage.store(document)
        }

        override suspend fun delete(documentId: String): Boolean {
            return storage.delete(documentId)
        }

        override suspend fun read(documentId: String): Document? {
            return storage.read(documentId)
        }

        override fun allDocuments(): Flow<Document> {
            return storage.allDocuments()
        }
    }
    ```
    <!--- KNIT example-ranked-document-storage-16.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-ranked-document-storage-java-16.java -->

By implementing the `RankedDocumentStorage` interface, you can create custom ranking mechanisms tailored to your specific use case while still leveraging the rest of the RAG infrastructure.

The flexibility of Koog's design allows you to mix and match different storage and ranking strategies to build a system that meets your specific requirements.