<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:53:26+00:00", "source_path": "class-based-tools.md", "source_sha256": "b0a4fc034e0651eb5f43e630ecb9155cee45256df98e540a8b93a19cdac1e3ad", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 基于类的工具 { #class-based-tools }

本节介绍 API，专为需要更高灵活性和定制行为的场景设计。
通过 Kotlin 中的这种方法，您可以完全控制工具，包括其参数、元数据、执行逻辑以及注册和调用方式。而在 Java 中，工具使用基于注解的方法创建，并通过反射进行注册。

这种控制级别非常适合创建扩展基础用例的复杂工具，使其能够无缝集成到智能体会话和工作流中。

本页描述如何在 Kotlin 和 Java 中实现工具、通过注册表管理工具、调用工具，以及在基于节点的智能体架构中使用工具。

!!! note
    API 是 Kotlin 的多平台框架。Java 工具使用基于注解的方法实现，并通过反射注册。这使您可以在 Kotlin 的不同平台中使用相同的工具，而 Java 则提供完整的 JVM 互操作性。

## 工具实现 { #tool-implementation }

Koog 框架提供以下实现工具的方法：

对于 Kotlin：

* 使用所有工具的基类 `Tool`。当您需要返回非文本结果或需要完全控制工具行为时，应使用此类。
* 使用扩展了基类 `Tool` 的 `SimpleTool` 类，它简化了返回文本结果的工具创建。当工具仅需返回文本时，应使用此方法。

这两种方法使用相同的核心组件，但在实现方式和返回结果上有所不同。

对于 Java：

* 使用基于注解的方法（`@Tool` 和 `@LLMDescription`）配合反射注册。这是实现 Java 互操作性的推荐方法，因为由于挂起函数的限制，不支持从 Java 子类化 Kotlin 的 `Tool` 或 `SimpleTool`。

### 工具类（Kotlin） { #tool-class-kotlin }

[`Tool<Args, Result>`](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool/index.html) 抽象类是 Kotlin 中创建工具的基类。
它允许您创建接受特定参数类型（`Args`）并返回各种类型结果（`Result`）的工具。

每个工具包含以下组件：| <div style="width:110px">组件</div> | 描述                                                                                                                                                                                                                                                                                                                                                                                                                           |
|------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Args`                                   | 定义工具所需参数的可序列化数据类。                                                                                                                                                                                                                                                                                                                                                             |
| `Result`                                 | 工具返回结果的可序列化类型。若需以自定义格式呈现工具结果，请继承 [ToolResult.TextSerializable](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool-result/-text-serializable/index.html) 类并实现 `textForLLM(): String` 方法                                                                                                           |
| `argsSerializer`                         | 重写变量，用于定义工具参数的解析方式。另请参阅 [argsSerializer](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool/args-serializer.html)。                                                                                                                                                                                                                        |
| `resultSerializer`                       | 重写变量，用于定义工具结果的解析方式。另请参阅 [resultSerializer](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool/result-serializer.html)。若选择继承 [ToolResult.TextSerializable](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool-result/-text-serializable/index.html)，可考虑使用 `ToolResultUtils.toTextSerializer()` |
| `descriptor`                             | 重写变量，用于指定工具元数据：<br/>- `name`<br/>- `description`<br/>- `requiredParameters`（默认为空）<br/>- `optionalParameters`（默认为空）<br/>另请参阅 [descriptor](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool/descriptor.html)。                                                                                                                                |
| `execute()`                              | 实现工具逻辑的函数。接收类型为 `Args` 的参数，并返回类型为 `Result` 的结果。另请参阅 [execute()]()。                                                                                                                                                                                                                                                                                  |!!! note "Java 实现"
    在 Java 中，无需继承 `Tool<Args, Result>` 类，而是使用基于注解的方法，配合 `@Tool` 和 `@LLMDescription`。框架会通过反射自动处理序列化和注册。更多
    详细信息，请参阅下方的[基于注解的方法](#annotation-based-methods-java)。

!!! tip
    请确保您的工具具有清晰的描述和定义明确的参数名称，以便 LLM 能够更容易地理解并正确使用它们。在 Kotlin 中，使用 `descriptor` 属性；在 Java 中，使用 `@LLMDescription` 注解。

#### 使用示例 { #usage-example }

以下是一个使用 `Tool` 类实现自定义工具并返回数值结果的示例：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.tools.Tool
    import ai.koog.agents.core.tools.ToolDescriptor
    import ai.koog.agents.core.tools.ToolParameterDescriptor
    import ai.koog.agents.core.tools.ToolParameterType
    import ai.koog.serialization.typeToken
    import kotlinx.serialization.Serializable
    import ai.koog.agents.core.tools.annotations.LLMDescription
    -->
    ```kotlin
    // Implement a simple calculator tool that adds two digits
    object CalculatorTool : Tool<CalculatorTool.Args, Int>(
        argsType = typeToken<Args>(),
        resultType = typeToken<Int>(),
        name = "calculator",
        description = "A simple calculator that can add two digits (0-9)."
    ) {

        // Arguments for the calculator tool
        @Serializable
        data class Args(
            @property:LLMDescription("The first digit to add (0-9)")
            val digit1: Int,
            @property:LLMDescription("The second digit to add (0-9)")
            val digit2: Int
        ) {
            init {
                require(digit1 in 0..9) { "digit1 must be a single digit (0-9)" }
                require(digit2 in 0..9) { "digit2 must be a single digit (0-9)" }
            }
        }

        // Function to add two digits
        override suspend fun execute(args: Args): Int = args.digit1 + args.digit2
    }
    ```
    <!--- KNIT example-class-based-tools-01.kt -->

After implementing your tool, you need to add it to a tool registry and then use it with an agent. For details, see [Tool registry](tools-overview.md#tool-registry).

For more details, see [API reference](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool/index.html).

### SimpleTool class (Kotlin)

The [`SimpleTool<Args>`](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-simple-tool/index.html) abstract class extends `Tool<Args, ToolResult.Text>` and simplifies the creation of tools that return text results.

Each simple tool consists of the following components:

| <div style="width:110px">Component</div> | Description                                                                                                                                                                                                                                                                                              |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Args`                                   | The serializable data class that defines arguments required for the custom tool.                                                                                                                                                                                                                         |
| `argsSerializer`                         | The overridden variable that defines how the arguments for the tool are serialized. See also [argsSerializer](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool/args-serializer.html).                                                                                             |
| `descriptor`                             | The overridden variable that specifies tool metadata:<br/>- `name`<br/>- `description`<br/>- `requiredParameters` (empty by default)<br/> - `optionalParameters` (empty by default)<br/> See also [descriptor](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool/descriptor.html). |
| `doExecute()`                            | The overridden function that describes the main action performed by the tool. It takes arguments of type `Args` and returns a `String`. See also [doExecute()](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-simple-tool/do-execute.html).                                          |

!!! note "Java Implementation"
    In Java, the equivalent approach is to use annotation-based methods that return `String`. The framework automatically handles the text result wrapping. For more details, see [Annotation-based methods](#annotation-based-methods-java) below.

!!! tip
    Ensure your tools have clear descriptions and well-defined parameter names to make it easier for the LLM to understand and use them properly. In Kotlin, use the `descriptor` and constructor parameters; in Java, use `@Tool` and `@LLMDescription` annotations.

#### Usage example 

Here is an example of a custom tool implementation using `SimpleTool` in Kotlin:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.tools.SimpleTool
    import ai.koog.agents.core.tools.annotations.LLMDescription
    import ai.koog.serialization.typeToken
    import kotlinx.serialization.Serializable
    -->
    ```kotlin
    // Create a tool that casts a string expression to a double value
    object CastToDoubleTool : SimpleTool<CastToDoubleTool.Args>(
        argsType = typeToken<Args>(),
        name = "cast_to_double",
        description = "casts the passed expression to double or returns 0.0 if the expression is not castable"
    ) {
        // Define tool arguments
        @Serializable
        data class Args(
            @property:LLMDescription("An expression to case to double")
            val expression: String,
            @property:LLMDescription("A comment on how to process the expression")
            val comment: String
        )

        // Function that executes the tool with the provided arguments
        override suspend fun execute(args: Args): String {
            return "Result: ${castToDouble(args.expression)}, " + "the comment was: ${args.comment}"
        }

        // Function to cast a string expression to a double value
        private fun castToDouble(expression: String): Double {
            return expression.toDoubleOrNull() ?: 0.0
        }
    }
    ```
    <!--- KNIT example-class-based-tools-02.kt -->

### Annotation-based methods (Java)

To implement tools in Java, instead of subclassing `Tool` or `SimpleTool`, use annotation-based methods with `@Tool` and
`@LLMDescription`. Koog handles serialization and registration automatically through reflection. To learn more about the
implementation, see Java examples below.

#### Usage examples

This is an example of a tool implementation in Java, equivalent to using the `Tool` class in Kotlin.

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // Java equivalent: implement the tool as a Java method and register it via ToolRegistry.builder().
    // This is the recommended Java interop path instead of subclassing the Kotlin Tool base class.
    public final class CalculatorTool {
        private CalculatorTool() {}
    
        @Tool(customName = "calculator")
        @LLMDescription(description = "A simple calculator that can add two digits (0-9).")
        public static int calculator(
                @LLMDescription(description = "The first digit to add (0-9)") int digit1,
                @LLMDescription(description = "The second digit to add (0-9)") int digit2
        ) {
            if (digit1 < 0 || digit1 > 9) throw new IllegalArgumentException("digit1 must be a single digit (0-9)");
            if (digit2 < 0 || digit2 > 9) throw new IllegalArgumentException("digit2 must be a single digit (0-9)");
            return digit1 + digit2;
        }
    
        public static ToolRegistry registry() throws NoSuchMethodException {
            return ToolRegistry.builder()
                .tool(CalculatorTool.class.getMethod("calculator", int.class, int.class))
                .build();
        }
    }
    // Note: Subclassing the Kotlin Tool<TArgs, TResult> and overriding a suspend execute(...) from Java is not supported.
    // The Java interop uses reflection-based registration of Java methods as tools.
    ```
    <!--- KNIT example-class-based-tools-java-01.java -->

Here is an example of a tool implementation in Java, equivalent to using the `SimpleTool` class in Kotlin. This example
implements a simple tool that returns a text result.

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // Java equivalent of SimpleTool: provide a Java method and register it as a tool.
    public final class CastToDoubleTool {
        private CastToDoubleTool() {}
    
        @Tool(customName = "cast_to_double")
        @LLMDescription(description = "casts the passed expression to double or returns 0.0 if the expression is not castable")
        public static String castToDouble(
                @LLMDescription(description = "An expression to case to double") String expression,
                @LLMDescription(description = "A comment on how to process the expression") String comment
        ) {
            double value;
            try {
                value = Double.parseDouble(expression);
            } catch (Exception e) {
                value = 0.0;
            }
            return "Result: " + value + ", the comment was: " + comment;
        }
    
        public static ToolRegistry registry() throws NoSuchMethodException {
            return ToolRegistry.builder()
                .tool(CastToDoubleTool.class.getMethod("castToDouble", String.class, String.class))
                .build();
        }
    }
    // Note: Extending Kotlin SimpleTool<TArgs> from Java is not required; registering a Java method is the idiomatic approach.
    ```
    <!--- KNIT example-class-based-tools-java-02.java -->

### Sending tool result to LLM in custom format

For Kotlin:

If you are not happy with JSON results sent to LLM (in some cases, LLMs can work better if tool output is structured as Markdown, for instance), you have to follow the following steps:

1. Implement `ToolResult.TextSerializable` interface, and override `textForLLM()` method
2. Override `resultSerializer` using `ToolResultUtils.toTextSerializer<T>()`

For Java:

Return formatted text (such as Markdown) directly as a `String` from your annotated method. The framework handles this automatically.

#### Example

Here is an example showing custom formatted output in both Kotlin and Java:

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.tools.Tool
    import ai.koog.agents.core.tools.ToolDescriptor
    import ai.koog.agents.core.tools.ToolParameterDescriptor
    import ai.koog.agents.core.tools.ToolParameterType
    import ai.koog.serialization.typeToken
    import kotlinx.serialization.Serializable
    import ai.koog.agents.core.tools.annotations.LLMDescription
    import ai.koog.prompt.markdown.markdown
    -->
    ```kotlin
    // A tool that edits file
    object EditFile : Tool<EditFile.Args, EditFile.Result>(
        argsType = typeToken<Args>(),
        resultType = typeToken<Result>(),
        name = "edit_file",
        description = "Edits the given file"
    ) {
        // Define tool arguments
        @Serializable
        public data class Args(
            val path: String,
            val original: String,
            val replacement: String
        )

        @Serializable
        public data class Result(
            private val patchApplyResult: PatchApplyResult
        ) {

            @Serializable
            public sealed interface PatchApplyResult {
                @Serializable
                public data class Success(val updatedContent: String) : PatchApplyResult

                @Serializable
                public sealed class Failure(public val reason: String) : PatchApplyResult
            }

            // Textual output (in Markdown format) that will be visible to the LLM after the tool finishes.
            fun textForLLM(): String = markdown {
                if (patchApplyResult is PatchApplyResult.Success) {
                    line {
                        bold("Successfully").text(" edited file (patch applied)")
                    }
                } else {
                    line {
                        text("File was ")
                            .bold("not")
                            .text(" modified (patch application failed: ${(patchApplyResult as PatchApplyResult.Failure).reason})")
                    }
                }
            }

            override fun toString(): String = textForLLM()
        }

        // Function that executes the tool with the provided arguments
        override suspend fun execute(args: Args): Result {
            return TODO("Implement file edit")
        }
    }
    ```
    <!--- KNIT example-class-based-tools-03.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    import ai.koog.agents.core.tools.ToolRegistry;
    import ai.koog.agents.core.tools.annotations.LLMDescription;
    import ai.koog.agents.core.tools.annotations.Tool;

    // Java equivalent: return Markdown text directly to the LLM from a Java method and register it as a tool.
    // This avoids needing a custom serializable Result type (which would require Kotlin serialization support).
    public final class EditFile {
        private EditFile() {}

        @Tool(customName = "edit_file")
        @LLMDescription(description = "Edits the given file")
        public static String editFile(
                String path,
                String original,
                String replacement
        ) {
            // TODO: Implement file edit logic; below is a placeholder illustrating Markdown output
            boolean success = false;
            if (success) {
                return "**Successfully** edited file (patch applied)";
            } else {
                return "File was **not** modified (patch application failed: reason)";
            }
        }

        public static ToolRegistry registry() throws NoSuchMethodException {
            return ToolRegistry.builder()
                .tool(EditFile.class.getMethod("editFile", String.class, String.class, String.class))
                .build();
        }
    }
    // Note: If you need a structured custom Result object from Java, you must expose a Kotlin @Serializable type
    // or another serializer-aware type. Returning String works out-of-the-box with Koog's Java interop.
    ```
    <!--- KNIT example-class-based-tools-java-03.java -->

After implementing your tool in Kotlin or Java, you need to add it to a tool registry and then use it with an agent.
For details, see [Tool registry](tools-overview.md#tool-registry).