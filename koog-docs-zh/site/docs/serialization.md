<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:08:16+00:00", "source_path": "serialization.md", "source_sha256": "e06a373ee639b5cc062f9f24b3d967f5244add6c74ef8813406d4dc66d1bd7e6", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 序列化 { #serialization }

## 简介 { #introduction }

Koog 使用一个轻量级、与库无关的序列化层，将工具参数和结果与 JSON 相互转换。
该层位于代理运行时与底层序列化库之间，因此您可以更换库而无需更改任何工具或代理代码。

除了工具之外，序列化层还被代理功能（如**持久化**）用于序列化和反序列化节点输入和输出。

默认情况下，Koog 使用 `KotlinxSerializer`（基于 kotlinx-serialization）。 在 JVM 上，您也可以切换到 `JacksonSerializer`（基于 jackson-databind）。

## `JSONSerializer` 接口 { #the-jsonserializer-interface }

`JSONSerializer` 是位于 `serialization-core` 中的核心抽象。
该接口有四个主要方法（编码/解码到字符串和 `JSONElement`），
以及两个用于在 `JSONElement` 和字符串之间转换的便捷方法：

- `encodeToString` / `decodeFromString` — 将类型化值序列化到/从 JSON 字符串。
- `encodeToJSONElement` / `decodeFromJSONElement` — 将类型化值序列化到/从 `JSONElement` 树。
- `encodeJSONElementToString` / `decodeJSONElementFromString` — 在 `JSONElement` 与其字符串形式之间转换。

以下示例展示了所有关键操作：

=== "Kotlin"

    ```kotlin
    @Serializable
    data class User(val name: String, val age: Int)

    val serializer: JSONSerializer = KotlinxSerializer()

    // Encode a data class to a JSON string
    val json: String = serializer.encodeToString(User("Alice", 30), typeToken<User>())

    // Decode a JSON string back to a data class
    val user: User = serializer.decodeFromString(json, typeToken<User>())

    // Encode to a JSONElement tree
    val element: JSONElement = serializer.encodeToJSONElement(user, typeToken<User>())

    // Decode from a JSONElement tree
    val userFromElement: User = serializer.decodeFromJSONElement(element, typeToken<User>())

    // Convert between JSONElement and a raw JSON string
    val jsonString = """{"key": "value"}"""
    val jsonElement: JSONElement = serializer.decodeJSONElementFromString(jsonString)
    val backToString: String = serializer.encodeJSONElementToString(jsonElement)
    ```

=== "Java"

    ```java
    // Jackson-serializable class
    record User(
        @JsonProperty("name") String name,
        @JsonProperty("age") int age
    ) {}

    var serializer = new JacksonSerializer();

    // Encode a data class to a JSON string
    String json = serializer.encodeToString(new User("Alice", 30), TypeToken.of(User.class));

    // Decode a JSON string back to a data class
    User user = serializer.decodeFromString(json, TypeToken.of(User.class));

    // Encode to a JSONElement tree
    JSONElement element = serializer.encodeToJSONElement(user, TypeToken.of(User.class));

    // Decode from a JSONElement tree
    User userFromElement = serializer.decodeFromJSONElement(element, TypeToken.of(User.class));

    // Convert between JSONElement and a raw JSON string
    String jsonString = "{\"key\": \"value\"}";
    JSONElement jsonElement = serializer.decodeJSONElementFromString(jsonString);
    String backToString = serializer.encodeJSONElementToString(jsonElement);
    ```

## 类型标记 { #type-tokens }

`TypeToken` 是 Koog 在运行时传递类型信息的方式。

=== "Kotlin"

    ```kotlin
    data class MyClass(val value: String)

    // Inline reified — preferred in Kotlin
    val tokenReified = typeToken<MyClass>()

    // From a KClass (when no reified type parameter is available)
    val tokenKClass = typeToken(MyClass::class)

    // Generic type — preserves type arguments at runtime
    val tokenGeneric = typeToken<List<String>>()
    ```

=== "Java"

    ```java
    record MyClass(
        String value
    ) {}

    // Simple class
    TypeToken tokenClass = TypeToken.of(MyClass.class);

    // Generic type — use TypeCapture to preserve type arguments
    TypeToken tokenGeneric = TypeToken.of(new TypeCapture<List<String>>() {});
    ```

## `JSONElement` — 库无关的 JSON 树 { #jsonelement-library-agnostic-json-tree }

`JSONElement` 是 JSON 数据的一种中性中间表示。它的存在使得序列化器、工具和代理内部实现无需依赖特定库中的 JSON 类型。

### 层级结构 { #hierarchy }

```
JSONElement
├── JSONObject   – key-value pairs  (entries: Map<String, JSONElement>)
├── JSONArray    – ordered list      (elements: List<JSONElement>)
└── JSONPrimitive
    ├── JSONLiteral  – string, number, or boolean
    └── JSONNull     – JSON null singleton
```

### 库类型的转换 { #conversion-to-and-from-library-types }

每个序列化集成都提供了扩展函数，允许您在 `JSONElement` 和库自身的动态 JSON 类型之间进行转换。当您已经拥有 `JsonElement`、`JsonNode` 等对象，并希望将其传递给 Koog（或反之）时，这非常有用，无需经过完整的编码/解码流程。下方为每个受支持的库提供了示例。

### 构建与读取元素 { #building-and-reading-elements }

=== "Kotlin"

    ```kotlin
    val obj = JSONObject(
        mapOf(
            "name" to JSONPrimitive("Alice"),
            "age" to JSONPrimitive(30),
            "active" to JSONPrimitive(true),
        )
    )

    val arr = JSONArray(listOf(JSONPrimitive(1), JSONPrimitive(2), JSONPrimitive(3)))

    // Reading values from an object
    val nameContent: String = (obj.entries["name"] as JSONPrimitive).content  // "Alice"
    val age: Int? = (obj.entries["age"] as JSONPrimitive).intOrNull // 30
    ```

=== "Java"

    ```java
    JSONObject obj = new JSONObject(
        Map.of(
            "name", JSONPrimitive.of("Alice"),
            "age", JSONPrimitive.of(30),
            "active", JSONPrimitive.of(true)
        )
    );

    JSONArray arr = new JSONArray(List.of(JSONPrimitive.of(1), JSONPrimitive.of(2), JSONPrimitive.of(3)));

    // Reading values from an object
    String nameContent = ((JSONPrimitive) obj.getEntries().get("name")).getContent();  // "Alice"
    Integer age = ((JSONPrimitive) obj.getEntries().get("age")).getIntOrNull(); // 30
    ```

## 支持的序列化器 { #supported-serializers }

### `KotlinxSerializer` (default)

- **模块**：`ai.koog:serialization-core`（通过 `ai.koog:agents-core` 间接包含）
- **支持库**: kotlinx-serialization

=== "Kotlin"

    ```kotlin
    // Default instance — uses Json.Default
    val defaultSerializer = KotlinxSerializer()

    // Custom Json configuration
    val customSerializer = KotlinxSerializer(
        json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    )
    ```

您也可以在 Koog 的 `JSONElement` 与 kotlinx-serialization 的 `JsonElement` 之间进行转换。

=== "Kotlin"

    ```kotlin
    val koogJson: JSONElement = JSONObject(
        mapOf(
            "key" to JSONPrimitive("value")
        )
    )

    // Convert to kotlinx-serialization dynamic JSON instance
    val kotlinxJson: JsonElement = koogJson.toKotlinxJsonElement()

    // Convert to Koog dynamic JSON instance
    val koogJsonConverted: JSONElement = kotlinxJson.toKoogJSONElement()
    ```

### `JacksonSerializer` (仅限JVM) { #jacksonserializer-jvm-only }

- **模块**：`ai.koog:serialization-jackson`（独立依赖）
- **支持库**：jackson-databind

将依赖项添加到您的 `build.gradle.kts`：

```kts
dependencies {
    implementation("ai.koog:serialization-jackson:<version>")
}
```

然后创建序列化器：

=== "Kotlin"

    ```kotlin
    // Default instance — uses a fresh ObjectMapper with JSONElementModule pre-registered
    val defaultSerializer = JacksonSerializer()

    // Custom ObjectMapper configuration
    val customSerializer = JacksonSerializer(
        objectMapper = ObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    )
    ```

=== "Java"

    ```java
    // Default instance — uses a fresh ObjectMapper with JSONElementModule pre-registered
    var defaultSerializer = new JacksonSerializer();

    // Custom ObjectMapper configuration
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    var customSerializer = new JacksonSerializer(objectMapper);
    ```

!!! note
    `JacksonSerializer` 会自动在它用于正确序列化/反序列化 `JSONElement` 类型的 `ObjectMapper` 上注册 `JSONElementModule`。

您也可以在 Koog 的 `JSONElement` 与 Jackson 的 `JsonNode` 之间进行转换。

=== "Kotlin"

    ```kotlin
    val koogJson: JSONElement = JSONObject(
        mapOf(
            "key" to JSONPrimitive("value")
        )
    )

    // Convert to Jackson dynamic JSON instance
    val jacksonJson: JsonNode = koogJson.toJacksonJsonNode()

    // Convert to Koog dynamic JSON instance
    val koogJsonConverted: JSONElement = jacksonJson.toKoogJSONElement()
    ```

=== "Java"

    ```java
    JSONElement koogJson = new JSONObject(
        Map.of(
            "key", JSONPrimitive.of("value")
        )
    );

    // Convert to Jackson dynamic JSON instance
    JsonNode jacksonJson = JacksonJSONElementMappers.toJacksonJsonNode(koogJson);

    // Convert to Koog dynamic JSON instance
    JSONElement koogJsonConverted = JacksonJSONElementMappers.toKoogJSONElement(jacksonJson);
    ```

## 在 `AIAgentConfig` 中配置序列化器 { #configuring-the-serializer-in-aiagentconfig }

=== "Kotlin"

    在构造 `AIAgentConfig` 时传入 `serializer` 参数。
    如果省略，则使用 `KotlinxSerializer`。

    ```kotlin
    val agentConfig = AIAgentConfig(
        prompt = prompt("assistant") {
            system("You are a helpful assistant.")
        },
        model = OpenAIModels.Chat.GPT4o,
        maxAgentIterations = 10,
        serializer = JacksonSerializer()
    )
    ```

=== "Java"

    在构造 `AIAgentConfig` 时传入 `serializer` 参数。
    如果省略，则使用 `JacksonSerializer`。

    ```java
    var agentConfig = AIAgentConfig.builder()
        .model(OpenAIModels.Chat.GPT4o)
        .prompt(
            Prompt.builder("assistant")
                .system("You are a helpful assistant")
                .build()
        )
        .maxAgentIterations(10)
        .serializer(new JacksonSerializer())
        .build();
    ```

## 工具如何与序列化器交互 { #how-tools-interact-with-the-serializer }

代理运行时会自动在每个 `Tool` 实例上调用以下方法。
在正常使用中，您无需自行调用它们。

- **`decodeArgs(rawArgs, serializer)`** (JSON → TArgs) — 将来自 LLM 的原始 JSON 参数反序列化为工具的强类型参数类。
- **`encodeArgs(args, serializer)`** (TArgs → JSON) — 将强类型参数序列化回 JSON（供某些代理功能使用）。
- **`decodeResult(rawResult, serializer)`** (JSON → TResult) — 反序列化存储的 JSON 结果。
- **`encodeResult(result, serializer)`** (TResult → JSON) — 将工具的结果序列化为 JSON。
- **`encodeResultToString(result, serializer)`** (TResult → String) — 将工具的结果序列化为发送给 LLM 的字符串。
  默认情况下，委托给 `encodeResult`。可以重写以自定义发送给 LLM 的结果格式。

这些方法在 `Tool` 上是 `open` 的，因此如果您需要为特定工具自定义序列化行为，可以重写它们。

## 功能如何使用序列化器 { #how-features-use-the-serializer }

序列化层不仅限于工具——某些代理功能也依赖它。

例如，**持久化** 使用在 `AIAgentConfig` 中配置的 `JSONSerializer` 来序列化和反序列化节点输入和输出，以创建检查点和恢复代理状态。这意味着流经持久化节点的任何类型都必须能够被配置的 `JSONSerializer` 序列化。

有关检查点创建和恢复的详细信息，请参阅 [代理持久化](features/agent-persistence.md)。
