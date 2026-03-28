<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:19:45+00:00", "source_path": "tools/tool-descriptor-schemer.md", "source_sha256": "99c32801f678f49899bc08c56cdb4f33449d92a237e520c377c896ae1440ec08", "source_tag": "0.7.3", "translation_status": "changed"} -->
# ToolDescriptorSchemer { #tooldescriptorschemer }

`ToolDescriptorSchemer` 是一个扩展点，用于将 `ToolDescriptor` 转换为与特定 LLM 提供程序兼容的 JSON Schema 对象。它可以在 Kotlin 和 Java 中实现。

关键点：

- 位置：`ai.koog.agents.core.tools.serialization.ToolDescriptorSchemer`
- 契约：单个函数 `scheme(toolDescriptor: ToolDescriptor): JsonObject` 或 `generate(ToolDescriptor toolDescriptor): JsonObject`（Java）
- 提供的实现：
  - `OpenAICompatibleToolDescriptorSchemer` — 生成与 OpenAI 风格函数/工具定义兼容的架构。
  - `OllamaToolDescriptorSchemer` — 生成与 Ollama 工具 JSON 兼容的架构。

<!--- INCLUDE
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import kotlinx.serialization.json.JsonObject
-->
```kotlin
// Interface
interface ToolDescriptorSchemaGenerator {
fun generate(toolDescriptor: ToolDescriptor): JsonObject
}
```
<!--- KNIT example-tool-descriptor-schemer-01.kt -->


## 为何使用它 { #why-use-it }

如果你想为 Kotlin 或 Java 中现有或新的 LLM 提供程序提供自定义架构，请实现此接口，以将 Koog 的 `ToolDescriptor` 转换为预期的 JSON Schema 格式。

## 实现示例 { #implementation-example }

以下是在 Kotlin 和 Java 中的最小化自定义实现，仅渲染参数类型的子集，以说明如何接入 SPI。实际实现应涵盖所有 `ToolParameterType`（String、Integer、Float、Boolean、Null、Enum、List、Object、AnyOf）。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.tools.ToolDescriptor
    import ai.koog.agents.core.tools.ToolParameterDescriptor
    import ai.koog.agents.core.tools.ToolParameterType
    import ai.koog.agents.core.tools.serialization.ToolDescriptorSchemaGenerator
    import kotlinx.serialization.json.JsonPrimitive
    import kotlinx.serialization.json.JsonObject
    import kotlinx.serialization.json.buildJsonObject
    import kotlinx.serialization.json.put
    import kotlinx.serialization.json.putJsonArray
    import kotlinx.serialization.json.putJsonObject
    -->
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
    <!--- KNIT example-tool-descriptor-schemer-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
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
    <!--- KNIT example-tool-descriptor-schemer-java-01.java -->

## Using with a client

Typically, you do not need to call a schemer directly. Koog clients accept a list of `ToolDescriptor` objects and apply the correct schemer internally when serializing requests for the provider.

The example below defines a simple tool and passes it to the OpenAI client. The client will use `OpenAICompatibleToolDescriptorSchemer` under the hood to build the JSON schema.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
    import ai.koog.agents.core.tools.ToolDescriptor
    import ai.koog.agents.core.tools.ToolParameterDescriptor
    import ai.koog.agents.core.tools.ToolParameterType
    import ai.koog.prompt.dsl.Prompt
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.clients.openai.base.OpenAICompatibleToolDescriptorSchemaGenerator
    import kotlinx.serialization.json.JsonPrimitive
    import kotlinx.serialization.json.JsonObject
    import kotlinx.serialization.json.buildJsonObject
    import kotlinx.serialization.json.put
    import kotlinx.serialization.json.putJsonArray
    import kotlinx.serialization.json.putJsonObject
    import kotlinx.coroutines.runBlocking
    class MinimalSchemer : OpenAICompatibleToolDescriptorSchemaGenerator() {
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
    -->
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
    <!--- KNIT example-tool-descriptor-schemer-03.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
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
    <!--- KNIT example-tool-descriptor-schemer-java-02.java -->

If you need direct access to the produced schema (for debugging or for a custom transport), you can instantiate the provider‑specific schemer and serialize the JSON yourself:

=== "Kotlin"

    <!--- INCLUDE
    import kotlinx.serialization.json.Json
    import ai.koog.prompt.executor.clients.openai.base.OpenAICompatibleToolDescriptorSchemaGenerator
    import ai.koog.agents.core.tools.ToolDescriptor
    import ai.koog.agents.core.tools.ToolParameterDescriptor
    import ai.koog.agents.core.tools.ToolParameterType
    fun getUserTool(): ToolDescriptor {
        return ToolDescriptor(
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
    }
    -->
    
    ```kotlin
    val json = Json { prettyPrint = true }
    val schema = OpenAICompatibleToolDescriptorSchemaGenerator().generate(getUserTool())
    ```
    <!--- KNIT example-tool-descriptor-schemer-04.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ```
    <!--- KNIT example-tool-descriptor-schemer-java-03.java -->