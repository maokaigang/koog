<!-- koog-zh-meta: {"last_synced_at": "2026-03-29T03:04:26+00:00", "source_path": "tools/tool-descriptor-schemer.md", "source_sha256": "99c32801f678f49899bc08c56cdb4f33449d92a237e520c377c896ae1440ec08", "source_tag": "0.7.3", "translation_status": "changed"} -->
# ToolDescriptorSchemer { #tooldescriptorschemer }

`ToolDescriptorSchemer` 是一个扩展点，用于将 `ToolDescriptor` 转换为与特定 LLM 提供程序兼容的 JSON Schema 对象。它可以在 Kotlin 和 Java 中实现。

关键点：

- 位置：`ai.koog.agents.core.tools.serialization.ToolDescriptorSchemer`
- 契约：单个函数 `scheme(toolDescriptor: ToolDescriptor): JsonObject` 或 `generate(ToolDescriptor toolDescriptor): JsonObject`（Java）
- 提供的实现：
  - `OpenAICompatibleToolDescriptorSchemer` — 生成与 OpenAI 风格函数/工具定义兼容的架构。
  - `OllamaToolDescriptorSchemer` — 生成与 Ollama 工具 JSON 兼容的架构。

```kotlin
// Interface
interface ToolDescriptorSchemaGenerator {
fun generate(toolDescriptor: ToolDescriptor): JsonObject
}
```

## 为何使用它 { #why-use-it }

如果你想为 Kotlin 或 Java 中现有或新的 LLM 提供程序提供自定义架构，请实现此接口，以将 Koog 的 `ToolDescriptor` 转换为预期的 JSON Schema 格式。

## 实现示例 { #implementation-example }

以下是在 Kotlin 和 Java 中的最小化自定义实现，仅渲染参数类型的子集，以说明如何接入 SPI。实际实现应涵盖所有 `ToolParameterType`（String、Integer、Float、Boolean、Null、Enum、List、Object、AnyOf）。

=== "Kotlin"

    ```kotlin

    class MinimalSchemer : ToolDescriptorSchemaGenerator {
        override fun generate(toolDescriptor: ToolDescriptor): JsonObject = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                (toolDescriptor.requiredParameters + toolDescriptor.optionalParameters).forEach { p ->
                    put(p.name, buildJsonObject {
                        put("description", p.description)
                        when (val t = p.type) {
                            ToolParameterType.String -> put("type", "string")
                            ToolParameterType.Integer -> put("type", "integer")
                            is ToolParameterType.Enum -> {
                                put("type", "string")
                                putJsonArray("enum") { t.entries.forEach { add(JsonPrimitive(it)) } }
                            }
                            else -> put("type", "string") // fallback for brevity
                        }
                    })
                }
            }
            putJsonArray("required") { toolDescriptor.requiredParameters.forEach { add(JsonPrimitive(it.name)) } }
        }
    }
    ```

=== "Java"

    ```java
    public static class MinimalSchemer extends OpenAICompatibleToolDescriptorSchemaGenerator {
        @Override
        public JsonObject generate(ToolDescriptor toolDescriptor) {
            Map<String, JsonElement> root = new LinkedHashMap<>();
            root.put("type", JsonPrimitive("object"));

            // properties
            Map<String, JsonElement> props = new LinkedHashMap<>();
            for (ToolParameterDescriptor p : concat(toolDescriptor.getRequiredParameters(), toolDescriptor.getOptionalParameters())) {
                Map<String, JsonElement> prop = new LinkedHashMap<>();
                prop.put("description", JsonPrimitive(p.getDescription()));

                ToolParameterType t = p.getType();
                if (t == ToolParameterType.String.INSTANCE) {
                    prop.put("type", JsonPrimitive("string"));
                } else if (t == ToolParameterType.Integer.INSTANCE) {
                    prop.put("type", JsonPrimitive("integer"));
                } else if (t instanceof ToolParameterType.Enum) {
                    prop.put("type", JsonPrimitive("string"));
                    String[] entries = ((ToolParameterType.Enum) t).getEntries();
                    List<JsonElement> enumVals = new ArrayList<>();
                    for (String e : entries) enumVals.add(JsonPrimitive(e));
                    prop.put("enum", new JsonArray(enumVals));
                } else {
                    prop.put("type", JsonPrimitive("string")); // fallback for brevity
                }

                props.put(p.getName(), new JsonObject(prop));
            }
            root.put("properties", new JsonObject(props));

            // required array
            List<JsonElement> required = new ArrayList<>();
            for (ToolParameterDescriptor p : toolDescriptor.getRequiredParameters()) {
                required.add(JsonPrimitive(p.getName()));
            }
            root.put("required", new JsonArray(required));

            return new JsonObject(root);
        }

        private static List<ToolParameterDescriptor> concat(List<ToolParameterDescriptor> a, List<ToolParameterDescriptor> b) {
            List<ToolParameterDescriptor> res = new ArrayList<>(a.size() + b.size());
            res.addAll(a);
            res.addAll(b);
            return res;
        }
    }
    ```

## 与客户一起使用 { #using-with-a-client }

通常，您不需要直接调用策划者。 Koog 客户端接受 `ToolDescriptor` 对象列表，并在序列化对提供者的请求时在内部应用正确的方案。

下面的示例定义了一个简单的工具并将其传递给 OpenAI 客户端。客户端将在后台使用 `OpenAICompatibleToolDescriptorSchemer` 来构建 JSON 模式。

=== "Kotlin"

    ```kotlin
    val client = OpenAILLMClient(apiKey = System.getenv("OPENAI_API_KEY"), toolsConverter = MinimalSchemer())

    val getUserTool = ToolDescriptor(
        name = "get_user",
        description = "Returns user profile by id",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "id",
                description = "User id",
                type = ToolParameterType.String
            )
        )
    )

    val prompt = Prompt.build(id = "p1") { user("Hello") }
    val responses = runBlocking {
        client.execute(
            prompt = prompt,
            model = OpenAIModels.Chat.GPT4o,
            tools = listOf(getUserTool)
        )
    }
    ```

=== "Java"

    ```java
    // Custom schemer extending the OpenAI-compatible one is Kotlin-only in the docs; for Java example we reuse MinimalSchemer from above.
    OpenAILLMClient client = new OpenAILLMClient(System.getenv("OPENAI_API_KEY"), new OpenAIClientSettings(), null, null, new OpenAICompatibleToolDescriptorSchemaGenerator());

    ToolDescriptor getUserTool = new ToolDescriptor(
        "get_user",
        "Returns user profile by id",
        Collections.singletonList(new ToolParameterDescriptor(
            "id",
            "User id",
            ToolParameterType.String.INSTANCE
        )),
        Collections.emptyList()
    );

    Prompt prompt = Prompt.builder("p1")
        .user("Hello")
        .build();

    List<Message.Response> responses = client.execute(prompt, OpenAIModels.Chat.GPT4o, java.util.List.of(getUserTool));
    ```

如果您需要直接访问生成的模式（用于调试或自定义传输），您可以实例化特定于提供者的方案并自行序列化 JSON：

=== "Kotlin"

    ```kotlin
    val json = Json { prettyPrint = true }
    val schema = OpenAICompatibleToolDescriptorSchemaGenerator().generate(getUserTool())
    ```

=== "Java"

    ```java
    ```
