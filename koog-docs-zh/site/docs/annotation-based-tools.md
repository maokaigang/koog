<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:56:35+00:00", "source_path": "annotation-based-tools.md", "source_sha256": "c3704182e567f6a777972a92f31751fedc8f3bb279e0f3fcd4791b9e70391eba", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 基于注解的工具 { #annotation-based-tools }

基于注解的工具提供了一种声明式的方法，将函数和方法暴露给大型语言模型（LLMs）作为工具，适用于 Kotlin 和 Java。
通过使用注解，您可以将任何函数或方法转换为 LLMs 能够理解和使用的工具。

当您需要将现有功能暴露给 Kotlin 或 Java 中的 LLMs，而无需手动实现工具描述时，这种方法非常有用。

!!! note
    基于注解的工具仅支持 JVM，不适用于其他平台。如需跨平台支持，请使用[基于类的工具 API](class-based-tools.md)。

## 关键注解 { #key-annotations }

要在项目中使用基于注解的工具，您需要了解以下关键注解：

| 注解               | 描述                                                                 |
|--------------------|----------------------------------------------------------------------|
| `@Tool`           | 标记应作为工具暴露给 LLMs 的函数。                         |
| `@LLMDescription` | 提供关于工具及其组件的描述性信息。                                       |

## @Tool 注解 { #tool-annotation }

`@Tool` 注解用于标记应作为工具暴露给 LLMs 的函数（Kotlin）或方法（Java）。
通过反射从实现 `ToolSet` 接口的对象中收集带有 `@Tool` 注解的函数和方法。详情请参阅[实现 ToolSet 接口](#1-implement-the-toolset-interface)。

### 定义 { #definition }

```kotlin
@Target(AnnotationTarget.FUNCTION)
public annotation class Tool(val customName: String = "")
```

### 参数 { #parameters }

| <div style="width:100px">名称</div> | 是否必需 | 描述                                                                               |
|-------------------------------------|----------|------------------------------------------------------------------------------------|
| `customName`                        | 否       | 指定工具的自定义名称。如果未提供，则使用函数名称。                                   |

### 用法 { #usage }

要将函数或方法标记为工具，请在实现 `ToolSet` 接口的类中对该函数或方法应用 `@Tool` 注解：

=== "Kotlin"

    ```kotlin
    class MyToolSet : ToolSet {
        @Tool
        fun myTool(): String {
            // 工具实现
            return "Result"
        }

        @Tool(customName = "customToolName")
        fun anotherTool(): String {
            // 工具实现
            return "Result"
        }
    }
    ```

=== "Java"

    ```java
    public class MyToolSet implements ToolSet {
        @Tool
        public String myTool() {
            // 工具实现
            return "Result";
        }

        @Tool(customName = "customToolName")
        public String anotherTool() {
            // 工具实现
            return "Result";
        }
    }
    ```

## @LLMDescription 注解 { #llmdescription-annotation }

`@LLMDescription` 注解为代码元素（类、函数、方法、参数等）向 LLM 提供描述性信息。这有助于 LLM 理解这些元素的用途和用法。

### 定义 { #definition }

```kotlin
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION
)
public annotation class LLMDescription(val description: String)
```

### 参数 { #parameters }

| 名称          | 是否必需 | 描述                                     |
|---------------|----------|------------------------------------------|
| `description` | 是       | 描述被注解元素的字符串。                 |

### 用法 { #usage }

`@LLMDescription` 注解可以应用在不同层级。例如：

* 函数层级：

=== "Kotlin"

    ```kotlin
    @Tool
    @LLMDescription("Performs a specific operation and returns the result")
    fun myTool(): String {
        // Function implementation
        return "Result"
    }
    ```

=== "Java"

    ```java
    @Tool
    @LLMDescription(description = "Performs a specific operation and returns the result")
    public String myTool() {
        // Function implementation
        return "Result";
    }
    ```

* 参数层级：

=== "Kotlin"

    ```kotlin
    @Tool
    @LLMDescription("Processes input data")
    fun processTool(
        @LLMDescription("The input data to process")
        input: String,

        @LLMDescription("Optional configuration parameters")
        config: String = ""
    ): String {
        // Function implementation
        return "Processed: $input with config: $config"
    }
    ```

=== "Java"

    ```java
    @Tool
    @LLMDescription(description = "Processes input data")
    public String processTool(
            @LLMDescription(description = "The input data to process") String input,
            @LLMDescription(description = "Optional configuration parameters") String config
    ) {
        // Function implementation
        return "Processed: " + input + " with config: " + config;
    }
    ```

## 创建工具 { #creating-a-tool }

### 1. 实现 ToolSet 接口 { #1-implement-the-toolset-interface }

创建一个实现 `ToolSet` 接口的类。
此接口将您的类标记为工具的容器。

=== "Kotlin"

    ```kotlin
    class MyFirstToolSet : ToolSet {
        // Tools will go here
    }
    ```

=== "Java"

    ```java
    public class MyFirstToolSet implements ToolSet {
        // Tools will go here
    }
    ```

### 2. 添加工具函数 { #2-add-tool-functions }

向您的类中添加函数或方法，并使用 `@Tool` 注解将它们暴露为工具：

=== "Kotlin"

    ```kotlin
    class MyFirstToolSet : ToolSet {
        @Tool
        fun getWeather(location: String): String {
            // In a real implementation, you would call a weather API
            return "The weather in $location is sunny and 72°F"
        }
    }
    ```

=== "Java"

    ```java
    public class MyFirstToolSet implements ToolSet {
        @Tool
        public String getWeather(String location) {
            // 在实际实现中，您将调用天气 API
            return "The weather in " + location + " is sunny and 72°F";
        }
    }
    ```

### 3. 添加描述 { #3-add-descriptions }

添加 `@LLMDescription` 注解，为 LLM 提供上下文：

=== "Kotlin"

    ```kotlin
    @LLMDescription("Tools for getting weather information")
    class MyFirstToolSet : ToolSet {
        @Tool
        @LLMDescription("Get the current weather for a location")
        fun getWeather(
            @LLMDescription("The city and state/country")
            location: String
        ): String {
            // 在实际实现中，您将调用天气 API
            return "The weather in $location is sunny and 72°F"
        }
    }
    ```

=== "Java"

    ```java
    @LLMDescription(description = "Tools for getting weather information")
    public class MyFirstToolSet implements ToolSet {
        @Tool
        @LLMDescription(description = "Get the current weather for a location")
        public String getWeather(
                @LLMDescription(description = "The city and state/country") String location
        ) {
            // 在实际实现中，您将调用天气 API
            return "The weather in " + location + " is sunny and 72°F";
        }
    }
    ```

### 4. 与智能体一起使用您的工具 { #4-use-your-tools-with-an-agent }

现在您可以将您的工具与智能体一起使用：

=== "Kotlin"

    ```kotlin
    fun main() {
        runBlocking {
            // 创建您的工具集
            val weatherTools = MyFirstToolSet()

            // 使用您的工具创建一个智能体

            val agent = AIAgent(
                promptExecutor = simpleOpenAIExecutor(apiToken),
                systemPrompt = "Provide weather information for a given location.",
                llmModel = OpenAIModels.Chat.GPT4o,
                toolRegistry = ToolRegistry {
                    tools(weatherTools)
                }
            )

            // 智能体现在可以使用您的天气工具了
            agent.run("What's the weather like in New York?")
        }
    }
    ```

=== "Java"

    ```java
    String apiToken = System.getenv("OPENAI_API_KEY");

    // 创建您的工具集
     MyFirstToolSet weatherTools = new MyFirstToolSet();

    ToolRegistry toolRegistry = ToolRegistry.builder()
        .tools(weatherTools)
        .build();

    // 使用您的工具创建一个智能体
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
        .systemPrompt("Provide weather information for a given location.")
        .llmModel(OpenAIModels.Chat.GPT4o)
        .toolRegistry(toolRegistry)
        .build();

    // 智能体现在可以使用你的天气工具了
    String result = agent.run("纽约的天气怎么样？");
    System.out.println(result);
    ```

## 使用示例 { #usage-examples }

以下是一些工具注解的实际应用示例。

### 基础示例：开关控制器 { #basic-example-switch-controller }

此示例展示了一个用于控制开关的简单工具集：

=== "Kotlin"

    ```kotlin
    @LLMDescription("用于控制开关的工具")
    class SwitchTools(val switch: Switch) : ToolSet {
        @Tool
        @LLMDescription("切换开关的状态")
        fun switch(
            @LLMDescription("要设置的状态（true 为开，false 为关）")
            state: Boolean
        ): String {
            switch.switch(state)
            return "已切换到 ${if (state) "开" else "关"}"
        }

        @Tool
        @LLMDescription("返回开关的当前状态")
        fun switchState(): String {
            return "开关当前为 ${if (switch.isOn()) "开" else "关"}"
        }
    }
    ```

=== "Java"

    ```java
    public class Switch {
        private boolean state;

        public Switch(boolean state) {
            this.state = state;
        }

        // "switch" 是 Java 中的保留关键字，因此我们使用不同的方法名
        public void setState(boolean state) {
            this.state = state;
        }

        public boolean isOn() {
            return state;
        }
    }

    @LLMDescription(description = "用于控制开关的工具")
    public class SwitchTools implements ToolSet {
        private final Switch sw;

        public SwitchTools(Switch sw) {
            this.sw = sw;
        }

        @Tool
        @LLMDescription(description = "切换开关的状态")
        public String switchStateTo(
                @LLMDescription(description = "要设置的状态（true 为开，false 为关）") boolean state
        ) {
            sw.setState(state);
            return "已切换到 " + (state ? "开" : "关");
        }

        @Tool
        @LLMDescription(description = "返回开关的当前状态")
        public String switchState() {
            return "开关当前为 " + (sw.isOn() ? "开" : "关");
        }
    }
    ```

当 LLM 需要控制开关时，它可以从提供的描述中理解以下信息：

- 工具的用途和功能。
- 使用工具所需的参数。
- 每个参数可接受的值。
- 执行后期望的返回值。

### 高级示例：诊断工具 { #advanced-example-diagnostic-tools }

此示例展示了一个用于设备诊断的更复杂工具集：

=== "Kotlin"

    ```kotlin
    @LLMDescription("用于对设备执行诊断和故障排除的工具")
    class DiagnosticToolSet : ToolSet {
        @Tool
        @LLMDescription("在设备上运行诊断以检查其状态并识别任何问题")
        fun runDiagnostic(
            @LLMDescription("要诊断的设备的 ID")
            deviceId: String,```kotlin
        @Tool
        @LLMDescription("诊断的附加信息（可选）")
        additionalInfo: String = ""
    ): String {
        // 实现
        return "设备 $deviceId 的诊断结果"
    }

    @Tool
    @LLMDescription("分析错误代码以确定其含义和可能的解决方案")
    fun analyzeError(
        @LLMDescription("要分析的错误代码（例如 'E1001'）")
        errorCode: String
    ): String {
        // 实现
        return "错误代码 $errorCode 的分析结果"
    }
}
```

=== "Java"

    ```java
    @LLMDescription(description = "用于对设备执行诊断和故障排除的工具集")
    public class DiagnosticToolSet implements ToolSet {
        // 便捷重载（不作为工具公开）
        public String runDiagnostic(String deviceId) {
            return runDiagnostic(deviceId, "");
        }

        @Tool
        @LLMDescription(description = "对设备运行诊断以检查其状态并识别任何问题")
        public String runDiagnostic(
                @LLMDescription(description = "要诊断的设备ID") String deviceId,
                @LLMDescription(description = "诊断的附加信息（可选）") String additionalInfo
        ) {
            // 实现
            return "设备 " + deviceId + " 的诊断结果";
        }

        @Tool
        @LLMDescription(description = "分析错误代码以确定其含义和可能的解决方案")
        public String analyzeError(
                @LLMDescription(description = "要分析的错误代码（例如 'E1001'）") String errorCode
        ) {
            // 实现
            return "错误代码 " + errorCode + " 的分析结果";
        }
    }
    ```

## 最佳实践 { #best-practices }

* **提供清晰的描述**：编写清晰简洁的描述，说明工具、参数和返回值的用途与行为。
* **描述所有参数**：为所有参数添加 `@LLMDescription`，以帮助 LLM 理解每个参数的用途。
* **使用一致的命名**：对工具和参数采用一致的命名约定，使其更直观易懂。
* **分组相关工具**：将相关工具分组到同一个 `ToolSet` 实现中，并提供类级别的描述。
* **返回信息丰富的结果**：确保工具返回值能清晰提供操作结果的信息。
* **优雅地处理错误**：在工具中包含错误处理，并返回信息丰富的错误消息。
* **记录默认值**：当参数具有默认值（Kotlin）或重载（Java）时，请在描述中予以说明。
* **保持工具专注性**：每个工具应执行特定、定义明确的任务，避免尝试处理过多功能。

## 常见问题排查 { #troubleshooting-common-issues }

使用工具注解时，可能会遇到一些常见问题。

### 工具未被识别 { #tools-not-being-recognized }

如果智能体无法识别您的工具，请检查以下事项：

- 您的类实现了 `ToolSet` 接口。
- 所有工具函数或方法均使用 `@Tool` 进行注解。
- 工具函数或方法具有适当的返回类型（建议使用 `String` 以简化）。
- 您的工具已正确注册到智能体中。

### 工具描述不清晰 { #unclear-tool-descriptions }

如果 LLM 未能正确使用您的工具或误解其用途，请尝试以下方法：

- 尽可能使用基本参数类型（在 Kotlin 中使用 `String`、`Boolean`、`Int`，或在 Java 中使用 `String`、`boolean`、`int`）。
- 在参数描述中清晰说明期望的格式。
- 对于复杂类型，可考虑使用具有特定格式的 `String` 参数，并在工具中解析它们。
- 在参数描述中包含有效输入的示例。
- 注意 Java 不支持默认参数，请使用方法重载替代。

### 参数类型问题 { #parameter-type-issues }

如果 LLM 提供了错误的参数类型，请尝试以下方法：

- 尽可能使用简单参数类型（`String`、`Boolean`、`Int`）。
- 在参数描述中清晰说明期望的格式。
- 对于复杂类型，可考虑使用具有特定格式的 `String` 参数，并在工具中解析它们。
- 在参数描述中包含有效输入的示例。

### 性能问题 { #performance-issues }

如果您的工具导致性能问题，请尝试以下方法：

- 保持工具实现轻量化。
- 对于资源密集型操作，考虑实现异步处理。
- 在适当时缓存结果。
- 记录工具使用情况以识别瓶颈。
