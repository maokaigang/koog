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

 每个工具包含以下组件： | <div style="width:110px">组件</div> | 描述 |
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

在实现你的工具后，你需要将其添加到工具注册表，然后与智能体配合使用。详情请参阅[工具注册表](tools-overview.md#tool-registry)。

更多详情，请参阅[API 参考](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool/index.html)。

### SimpleTool 类（Kotlin） { #simpletool-class-kotlin }

[`SimpleTool<Args>`](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-simple-tool/index.html) 抽象类继承自 `Tool<Args, ToolResult.Text>`，简化了返回文本结果的工具创建过程。

每个简单工具包含以下组成部分：

| <div style="width:110px">组件</div> | 描述 |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Args` | 定义自定义工具所需参数的可序列化数据类。 |
| `argsSerializer` | 用于定义工具参数序列化方式的重写变量。另请参阅[参数序列化器](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool/args-serializer.html)。 |
| `descriptor` | 用于指定工具元数据的重写变量：<br/>- `name`<br/>- `description`<br/>- `requiredParameters`（默认为空）<br/> - `optionalParameters`（默认为空）<br/> 另请参阅 [描述符](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-tool/descriptor.html)。 |
| `doExecute()` | 描述工具主要执行动作的重写函数。它接收类型为`Args`的参数，并返回一个`String`。另请参阅[执行()](https://api.koog.ai/agents/agents-tools/ai.koog.agents.core.tools/-simple-tool/do-execute.html)。 |

!!! note "Java 实现"
    在Java中，等效的方法是使用基于注解的方式，这些方法返回`String`。框架会自动处理文本结果的包装。更多细节请参见下方的[基于注解的方法](#annotation-based-methods-java)。

!!! tip
    确保你的工具具有清晰的描述和定义明确的参数名称，以便LLM能够更容易理解并正确使用它们。在Kotlin中，使用`descriptor`和构造函数参数；在Java中，使用`@Tool`和`@LLMDescription`注解。

#### 使用示例 { #usage-example }

以下是使用`SimpleTool`在Kotlin中实现自定义工具的示例：

=== "Kotlin"

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

### 基于注解的方法（Java） { #annotation-based-methods-java }

要在Java中实现工具，无需子类化`Tool`或`SimpleTool`，而应使用基于注解的方法配合`@Tool`和`@LLMDescription`。Koog会通过反射自动处理序列化与注册。如需了解具体实现方式，请参阅下方Java示例。

#### 使用示例 { #usage-examples }

这是在Java中实现工具的一个示例，相当于在Kotlin中使用`Tool`类。

=== "Java"

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

以下是Java中工具实现的一个示例，相当于在Kotlin中使用`SimpleTool`类。此示例实现了一个返回文本结果的简单工具。

=== "Java"

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

### 以自定义格式向 LLM 发送工具结果 { #sending-tool-result-to-llm-in-custom-format }

For Kotlin:

如果您对发送至LLM的JSON结果不满意（在某些情况下，若工具输出以Markdown格式结构化，LLM可能表现更佳），则需遵循以下步骤：

1. 实现 `ToolResult.TextSerializable` 接口，并重写 `textForLLM()` 方法
2. 使用`ToolResultUtils.toTextSerializer<T>()`覆盖`resultSerializer`

For Java:

从你的注解方法中直接返回格式化文本（例如 Markdown）作为 `String`。框架会自动处理。

#### Example

以下是一个示例，展示了在 Kotlin 和 Java 中自定义格式化输出的效果：

=== "Kotlin"

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

=== "Java"

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

在 Kotlin 或 Java 中实现你的工具后，你需要将其添加到工具注册表中，然后通过代理来使用它。详情请参阅 [工具注册表](tools-overview.md#tool-registry)。