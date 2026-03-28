<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:57:51+00:00", "source_path": "examples/Guesser.md", "source_sha256": "c954e262deae023106a537bd8b7c5ddd9aeaeacd3ff90f1dd2c61d5893a51ab0", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 使用 Koog 构建一个猜数字智能体 { #building-a-number-guessing-agent-with-koog }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/Guesser.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/Guesser.ipynb
){ .md-button }

让我们构建一个有趣的小型智能体，来猜出你心中所想的数字。我们将借助 Koog 的工具调用功能来提出有针对性的问题，并使用经典的二分查找策略逐步逼近答案。最终成果是一个可以直接放入文档的、符合惯用法的 Kotlin Notebook。

我们将保持代码简洁、流程清晰：只需几个小巧的工具、一个简短的提示词，以及一个交互式的 CLI 循环。

## 环境设置 { #setup }

本 Notebook 假设：
- 你在一个安装了 Koog 的 Kotlin Notebook 中运行。
- 环境变量 `OPENAI_API_KEY` 已设置。智能体将通过 `simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY"))` 使用它。

加载 Koog 内核：

```kotlin
%useLatestDescriptors
%use koog
```

## 工具：提出有针对性的问题 { #tools-asking-targeted-questions }

工具是 LLM 可以调用的、描述清晰的小型函数。我们将提供三个工具：
- `lessThan(value)`：“你的数字小于 value 吗？”
- `greaterThan(value)`：“你的数字大于 value 吗？”
- `proposeNumber(value)`：“你的数字等于 value 吗？”（当范围缩小时使用）

每个工具返回一个简单的 "YES"/"NO" 字符串。辅助函数 `ask` 实现了一个极简的 Y/n 循环并验证输入。通过 `@LLMDescription` 提供的描述有助于模型正确选择工具。

```kotlin
import ai.koog.agents.core.tools.annotations.Tool

class GuesserTool : ToolSet {

    @Tool
    @LLMDescription("Asks the user if his number is STRICTLY less than a given value.")
    fun lessThan(
        @LLMDescription("A value to compare the guessed number with.") value: Int
    ): String = ask("Is your number less than $value?", value)

    @Tool
    @LLMDescription("Asks the user if his number is STRICTLY greater than a given value.")
    fun greaterThan(
        @LLMDescription("A value to compare the guessed number with.") value: Int
    ): String = ask("Is your number greater than $value?", value)

    @Tool
    @LLMDescription("Asks the user if his number is EXACTLY equal to the given number. Only use this tool once you've narrowed down your answer.")
    fun proposeNumber(
        @LLMDescription("A value to compare the guessed number with.") value: Int
    ): String = ask("Is your number equal to $value?", value)

    fun ask(question: String, value: Int): String {
        print("$question [Y/n]: ")
        val input = readln()
        println(input)

        return when (input.lowercase()) {
            "", "y", "yes" -> "YES"
            "n", "no" -> "NO"
            else -> {
                println("Invalid input! Please, try again.")
                ask(question, value)
            }
        }
    }
}
```

## 工具注册 { #tool-registry }

将你的工具暴露给智能体。我们还添加了一个内置的 `SayToUser` 工具，以便智能体可以直接向用户显示消息。

```kotlin
val toolRegistry = ToolRegistry {
    tool(SayToUser)
    tools(GuesserTool())
}
```

## 智能体配置 { #agent-configuration }

我们只需要一个简短、面向工具的系统提示词。我们会建议采用二分查找策略，并保持 `temperature = 0.0` 以确保稳定、确定性的行为。这里我们使用 OpenAI 的推理模型 `GPT4oMini` 来进行清晰的规划。

```kotlin
val agent = AIAgent(
    executor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    llmModel = OpenAIModels.Chat.GPT4oMini,
    systemPrompt = """
            You are a number guessing agent. Your goal is to guess a number that the user is thinking of.
            
            Follow these steps:
            1. Start by asking the user to think of a number between 1 and 100.
            2. Use the less_than and greater_than tools to narrow down the range.
                a. If it's neither greater nor smaller, use the propose_number tool.
            3. Once you're confident about the number, use the propose_number tool to check if your guess is correct.
            4. If your guess is correct, congratulate the user. If not, continue guessing.
            
            Be efficient with your guessing strategy. A binary search approach works well.
        """.trimIndent(),
    temperature = 0.0,
    toolRegistry = toolRegistry
)
```

## 运行它 { #run-it }

- 想一个 1 到 100 之间的数字。
- 输入 `start` 开始。
- 用 `Y`/`Enter` 回答“是”或用 `n` 回答“否”来回应智能体的问题。智能体应该在大约 7 步内猜中你的数字。

```kotlin
import kotlinx.coroutines.runBlocking

println("Number Guessing Game started!")
println("Think of a number between 1 and 100, and I'll try to guess it.")
println("Type 'start' to begin the game.")

val initialMessage = readln()
runBlocking {
    agent.run(initialMessage)
}
```

## 工作原理 { #how-it-works }

- 智能体读取系统提示词并规划二分查找。
- 每次迭代时，它会调用你的一个工具：`lessThan`、`greaterThan`，或（当确定时）`proposeNumber`。
- 辅助函数 `ask` 收集你的 Y/n 输入，并向模型返回一个清晰的 "YES"/"NO" 信号。
- 当获得确认后，它会通过 `SayToUser` 向你表示祝贺。

## 扩展它 { #extend-it }

- 通过调整系统提示词来更改范围（例如 1..1000）。
- 添加一个 `between(low, high)` 工具来进一步减少调用次数。
- 在保持相同工具的同时，更换模型或执行器（例如使用 Ollama 执行器和本地模型）。
- 将猜测或结果持久化到存储中以供分析。

## 故障排除 { #troubleshooting }

- 缺少密钥：确保你的环境中已设置 `OPENAI_API_KEY`。
- 内核未找到：确保 `%useLatestDescriptors` 和 `%use koog` 已成功执行。
- 工具未调用：确认 `ToolRegistry` 包含 `GuesserTool()`，并且提示词中的名称与你的工具函数名称匹配。