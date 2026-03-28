<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:06:26+00:00", "source_path": "prompts/prompt-creation/index.md", "source_sha256": "6b3a1f1e8fd9770a64464e625ab9acbdd6f1d8a3180362b08fc1c89f2cc3fc1b", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 创建提示 { #creating-prompts }

Koog 提供了一种结构化的方式来创建提示，可以控制消息类型、顺序和内容：

* 对于 **Kotlin** 用户，通过类型安全的 Kotlin DSL。
* 对于 **Java** 用户，通过流畅的构建器 API。

## 基本结构 { #basic-structure }

Kotlin 中的 `prompt()` 函数或 Java 中的 `Prompt.builder()` 创建一个具有唯一 ID 和消息列表的 Prompt 对象：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    -->

    ```kotlin
    val prompt = prompt("unique_prompt_id") {
        // 消息列表
    }
    ```
    <!--- KNIT example-creating-prompts-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    Prompt prompt = Prompt.builder("unique_prompt_id")
        // 消息列表
        .build();
    ```
    <!--- KNIT example-creating-prompts-java-01.java -->

## 消息类型 { #message-types }

Kotlin DSL 和 Java 构建器 API 支持以下类型的消息，每种消息对应对话中的特定角色：

- **系统消息**：为 LLM 提供上下文、指令和约束，定义其行为。
- **用户消息**：表示用户输入。
- **助手消息**：表示 LLM 的响应，用于少样本学习或继续对话。
- **工具消息**：表示工具调用及其结果。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    -->

    ```kotlin
    val prompt = prompt("unique_prompt_id") {
        // 添加系统消息以设置上下文
        system("You are a helpful assistant with access to tools.")
        // 添加用户消息
        user("What is 5 + 3 ?")
        // 添加助手消息
        assistant("The result is 8.")
    }
    ```
    <!--- KNIT example-creating-prompts-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    Prompt prompt = Prompt.builder("unique_prompt_id")
        // 添加系统消息以设置上下文
        .system("You are a helpful assistant with access to tools.")
        // 添加用户消息
        .user("What is 5 + 3 ?")
        // 添加助手消息
        .assistant("The result is 8.")
        .build();
    ```
    <!--- KNIT example-creating-prompts-java-02.java -->

### 系统消息 { #system-message }

系统消息定义了 LLM 的行为，并为整个对话设置上下文。它可以指定模型的角色、语气，提供响应指南和约束，以及提供响应示例。

要创建系统消息，请将字符串作为参数提供给 `system()` Kotlin 函数或 Java 方法：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    -->

    ```kotlin
    val prompt = prompt("system_message") {
        system("You are a helpful assistant that explains technical concepts.")
    }
    ```
    <!--- KNIT example-creating-prompts-03.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    Prompt prompt = Prompt.builder("system_message")
        .system("You are a helpful assistant that explains technical concepts.")
        .build();
    ```
    <!--- KNIT example-creating-prompts-java-03.java -->

### 用户消息 { #user-messages }

用户消息表示来自用户的输入。要创建用户消息，请将字符串作为参数提供给 `user()` Kotlin 函数或 Java 方法：=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    -->

    ```kotlin
    val prompt = prompt("user_message") {
        system("You are a helpful assistant.")
        user("What is Koog?")
    }
    ```
    <!--- KNIT example-creating-prompts-04.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    Prompt prompt = Prompt.builder("user_message")
        .system("You are a helpful assistant.")
        .user("What is Koog?")
        .build();
    ```
    <!--- KNIT example-creating-prompts-java-04.java -->


大多数用户消息包含纯文本，但也可以包含多模态内容，例如图像、音频、视频和文档。
有关详细信息和示例，请参阅[多模态内容](multimodal-content.md)。

### 助手消息 { #assistant-messages }

助手消息代表一次LLM响应，可用于未来类似交互中的少样本学习、继续对话或展示期望的输出结构。

要创建助手消息，请将字符串作为参数传递给`assistant()` Kotlin函数或Java方法：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    -->

    ```kotlin
    val prompt = prompt("article_review") {
        system("Evaluate the article.")

        // 示例 1
        user("The article is clear and easy to understand.")
        assistant("positive")

        // 示例 2
        user("The article is hard to read but it's clear and useful.")
        assistant("neutral")

        // 示例 3
        user("The article is confusing and misleading.")
        assistant("negative")

        // 待分类的新输入
        user("The article is interesting and helpful.")
    }
    ```
    <!--- KNIT example-creating-prompts-05.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    Prompt prompt = Prompt.builder("article_review")
        .system("Evaluate the article.")

        // 示例 1
        .user("The article is clear and easy to understand.")
        .assistant("positive")

        // 示例 2
        .user("The article is hard to read but it's clear and useful.")
        .assistant("neutral")

        // 示例 3
        .user("The article is confusing and misleading.")
        .assistant("negative")

        // 待分类的新输入
        .user("The article is interesting and helpful.")
        .build();
    ```
    <!--- KNIT example-creating-prompts-java-05.java -->


### 工具消息 { #tool-messages }

工具消息代表一次工具调用及其结果，可用于预填充工具调用的历史记录。

!!! tip
    LLM在执行过程中会生成工具调用。
    预填充这些调用有助于少样本学习或演示工具的预期使用方式。

要创建工具消息，请在Kotlin中调用`tool()`函数，或在Java中调用`toolCall()`和`toolResult()`方法：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    -->

    ```kotlin
    val prompt = prompt("calculator_example") {
        system("You are a helpful assistant with access to tools.")
        user("What is 5 + 3?")
        tool {
            // 工具调用
            call(
                id = "calculator_tool_id",
                tool = "calculator",
                content = """{"operation": "add", "a": 5, "b": 3}"""
            )

```            // 工具结果
            result(
                id = "calculator_tool_id",
                tool = "calculator",
                content = "8"
            )
        }

        // LLM 基于工具结果的响应
        assistant("5 + 3 的结果是 8。")
        user("4 + 5 等于多少？")
    }
    ```
    <!--- KNIT example-creating-prompts-06.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    Prompt prompt = Prompt.builder("calculator_example")
        .system("你是一个可以使用工具的助手。")
        .user("5 + 3 等于多少？")
        // 工具调用
        .toolCall("calculator_tool_id", "calculator", "{\"operation\": \"add\", \"a\": 5, \"b\": 3}")
        // 工具结果
        .toolResult("calculator_tool_id", "calculator", "8")
        // LLM 基于工具结果的响应    
        .assistant("5 + 3 的结果是 8。")
        .user("4 + 5 等于多少？")
        .build();
    ```
    <!--- KNIT example-creating-prompts-java-06.java -->


## 文本消息构建器 { #text-message-builders }

!!! warning
    文本消息构建器仅在 Kotlin 中可用。

构建 `system()`、`user()` 或 `assistant()` 消息时，可以使用辅助的[文本构建函数](api:prompt-model::ai.koog.prompt.text.TextContentBuilder)来实现丰富的文本格式化。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    -->

    ```kotlin
    val prompt = prompt("text_example") {
        user {
            +"请审阅以下代码片段："
            +"fun greet(name: String) = println(\"Hello, \$name!\")"

            // 段落分隔
            br()
            text("请在解释中包含：")

            // 缩进内容
            padding("  ") {
                +"1. 该函数的功能。"
                +"2. 字符串插值的工作原理。"
            }
        }
    }
    ```
    <!--- KNIT example-creating-prompts-07.kt -->

你也可以使用 [Markdown](api:prompt-markdown::ai.koog.prompt.markdown.markdown) 和 [XML](api:prompt-xml::ai.koog.prompt.xml.xml) 构建器来添加相应格式的内容。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.markdown.markdown
    import ai.koog.prompt.xml.xml
    -->

    ```kotlin
    val prompt = prompt("markdown_xml_example") {
        // 一个 Markdown 格式的用户消息
        user {
            markdown {
                h2("请使用以下标准评估文章：")
                bulleted {
                    item { +"清晰度和可读性" }
                    item { +"信息的准确性" }
                    item { +"对读者的实用性" }
                }
            }
        }
        // 一个 XML 格式的助手消息
        assistant {
            xml {
                xmlDeclaration()
                tag("review") {
                    tag("clarity") { text("positive") }
                    tag("accuracy") { text("neutral") }
                    tag("usefulness") { text("positive") }
                }
            }
        }
    }
    ```
    <!--- KNIT example-creating-prompts-08.kt -->

!!! tip
    你可以将文本构建函数与 XML 和 Markdown 构建器混合使用。

## 提示参数 { #prompt-parameters }

可以通过配置控制 LLM 行为的参数来自定义提示。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.params.LLMParams
    import ai.koog.prompt.params.LLMParams.ToolChoice
    -->```kotlin
val prompt = prompt(
    id = "custom_params",
    params = LLMParams(
        temperature = 0.7,
        numberOfChoices = 1,
        toolChoice = LLMParams.ToolChoice.Auto
    )
) {
    system("你是一位创意写作助手。")
    user("写一首关于冬天的歌。")
}
```
<!--- KNIT example-creating-prompts-09.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    // 首先创建参数
    LLMParams params = new LLMParams(
        0.7,                    // temperature
        null,                   // maxTokens
        1,                      // numberOfChoices
        null,                   // speculation
        null,                   // schema
        LLMParams.ToolChoice.Auto.INSTANCE, // toolChoice
        null,                   // user
        null                    // additionalProperties
    );

    Prompt prompt = Prompt.builder("custom_params")
        .system("你是一位创意写作助手。")
        .user("写一首关于冬天的歌。")
        .build();
        
    // 将参数应用到已构建的提示词
    prompt = prompt.withParams(params);
    ```
    <!--- KNIT example-creating-prompts-java-07.java -->

支持以下参数：

- `temperature`：控制模型响应的随机性。
- `toolChoice`：控制模型的工具调用行为。
- `numberOfChoices`：请求多个备选响应。
- `schema`：定义模型响应格式的结构。
- `maxTokens`：限制响应中的令牌数量。
- `speculation`：提供关于预期响应格式的提示（仅特定模型支持）。

更多信息，请参阅 [LLM 参数](../../llm-parameters.md)。

## 扩展现有提示词 { #extending-existing-prompts }

您可以通过在 Kotlin 中调用 `prompt()` 函数，或在 Java 中调用 `Prompt.builder()`，并将现有提示词作为参数传入，来扩展现有提示词：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    -->

    ```kotlin
    val basePrompt = prompt("base") {
        system("你是一位乐于助人的助手。")
        user("你好！")
        assistant("嗨！我能帮你什么吗？")
    }

    val extendedPrompt = prompt(basePrompt) {
        user("天气怎么样？")
    }
    ```
    <!--- KNIT example-creating-prompts-10.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    Prompt basePrompt = Prompt.builder("base")
        .system("你是一位乐于助人的助手。")
        .user("你好！")
        .assistant("嗨！我能帮你什么吗？")
        .build();

    Prompt extendedPrompt = Prompt.builder(String.valueOf(basePrompt))
        .user("天气怎么样？")
        .build();
    ```
    <!--- KNIT example-creating-prompts-java-08.java -->

这将创建一个新的提示词，其中包含来自 `basePrompt` 的所有消息以及新的用户消息。

## 后续步骤 { #next-steps }

- 学习如何处理[多模态内容](multimodal-content.md)。
- 如果您使用单一的 LLM 提供商，请使用 [LLM 客户端](../llm-clients.md) 运行提示词。
- 如果您使用多个 LLM 提供商，请使用[提示词执行器](../prompt-executors.md) 运行提示词。