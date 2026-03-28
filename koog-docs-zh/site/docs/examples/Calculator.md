<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:00:12+00:00", "source_path": "examples/Calculator.md", "source_sha256": "d23f5672fd11b7bd0172d24f355feaa9be4df6489af11a855266f9e4ce9d30f2", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 使用 Koog 构建工具调用计算器智能体 { #building-a-tool-calling-calculator-agent-with-koog }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/Calculator.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/Calculator.ipynb
){ .md-button }

在这个迷你教程中，我们将构建一个由 **Koog** 工具调用驱动的计算器智能体。
你将学习如何：
- 为算术设计小型、纯粹的**工具**
- 使用 Koog 的多调用策略编排**并行**工具调用
- 添加轻量级**事件日志**以提高透明度
- 使用 OpenAI 运行（可选 Ollama）

我们将保持 API 整洁且符合 Kotlin 的惯用风格，返回可预测的结果并优雅地处理边界情况（如除零错误）。

## 环境设置 { #setup }

我们假设你处于一个已安装 Koog 的 Kotlin Notebook 环境中。
提供一个 LLM 执行器

```kotlin
%useLatestDescriptors
%use koog


val OPENAI_API_KEY = System.getenv("OPENAI_API_KEY")
    ?: error("Please set the OPENAI_API_KEY environment variable")

val executor = simpleOpenAIExecutor(OPENAI_API_KEY)
```

## 计算器工具 { #calculator-tools }

工具是小型、纯粹的函数，具有清晰的契约。
我们将使用 `Double` 以获得更好的精度，并保持输出格式一致。

```kotlin
import ai.koog.agents.core.tools.annotations.Tool

// Format helper: integers render cleanly, decimals keep reasonable precision.
private fun Double.pretty(): String =
    if (abs(this % 1.0) < 1e-9) this.toLong().toString() else "%.10g".format(this)

@LLMDescription("Tools for basic calculator operations")
class CalculatorTools : ToolSet {

    @Tool
    @LLMDescription("Adds two numbers and returns the sum as text.")
    fun plus(
        @LLMDescription("First addend.") a: Double,
        @LLMDescription("Second addend.") b: Double
    ): String = (a + b).pretty()

    @Tool
    @LLMDescription("Subtracts the second number from the first and returns the difference as text.")
    fun minus(
        @LLMDescription("Minuend.") a: Double,
        @LLMDescription("Subtrahend.") b: Double
    ): String = (a - b).pretty()

    @Tool
    @LLMDescription("Multiplies two numbers and returns the product as text.")
    fun multiply(
        @LLMDescription("First factor.") a: Double,
        @LLMDescription("Second factor.") b: Double
    ): String = (a * b).pretty()

    @Tool
    @LLMDescription("Divides the first number by the second and returns the quotient as text. Returns an error message on division by zero.")
    fun divide(
        @LLMDescription("Dividend.") a: Double,
        @LLMDescription("Divisor (must not be zero).") b: Double
    ): String = if (abs(b) < 1e-12) {
        "ERROR: Division by zero"
    } else {
        (a / b).pretty()
    }
}
```

## 工具注册 { #tool-registry }

公开我们的工具（以及两个用于交互/日志记录的内置工具）。

```kotlin
val toolRegistry = ToolRegistry {
    tool(AskUser)   // enables explicit user clarification when needed
    tool(SayToUser) // allows the agent to present the final message to the user
    tools(CalculatorTools())
}
```

## 策略：多工具调用（带可选压缩） { #strategy-multiple-tool-calls-with-optional-compression }

此策略允许 LLM **一次提议多个工具调用**（例如 `plus`、`minus`、`multiply`、`divide`），然后返回结果。
如果令牌使用量增长过大，我们会在继续之前**压缩**工具结果的历史记录。

```kotlin
import ai.koog.agents.core.environment.ReceivedToolResult

object CalculatorStrategy {
    private const val MAX_TOKENS_THRESHOLD = 1000

    val strategy = strategy<String, String>("test") {
        val callLLM by nodeLLMRequestMultiple()
        val executeTools by nodeExecuteMultipleTools(parallelTools = true)
        val sendToolResults by nodeLLMSendMultipleToolResults()
        val compressHistory by nodeLLMCompressHistory<List<ReceivedToolResult>>()

        edge(nodeStart forwardTo callLLM)

        // If the assistant produced a final answer, finish.
        edge((callLLM forwardTo nodeFinish) transformed { it.first() } onAssistantMessage { true })

        // Otherwise, run the tools LLM requested (possibly several in parallel).
        edge((callLLM forwardTo executeTools) onMultipleToolCalls { true })

        // If we’re getting large, compress past tool results before continuing.
        edge(
            (executeTools forwardTo compressHistory)
                onCondition { llm.readSession { prompt.latestTokenUsage > MAX_TOKENS_THRESHOLD } }
        )
        edge(compressHistory forwardTo sendToolResults)

        // Normal path: send tool results back to the LLM.
        edge(
            (executeTools forwardTo sendToolResults)
                onCondition { llm.readSession { prompt.latestTokenUsage <= MAX_TOKENS_THRESHOLD } }
        )

        // LLM might request more tools after seeing results.
        edge((sendToolResults forwardTo executeTools) onMultipleToolCalls { true })

        // Or it can produce the final answer.
        edge((sendToolResults forwardTo nodeFinish) transformed { it.first() } onAssistantMessage { true })
    }
}
```

## 智能体配置 { #agent-configuration }

一个最小化、以工具为导向的提示效果很好。保持较低的温度以确保数学运算的确定性。

```kotlin
val agentConfig = AIAgentConfig(
    prompt = prompt("calculator") {
        system("You are a calculator. Always use the provided tools for arithmetic.")
    },
    model = OpenAIModels.Chat.GPT4o,
    maxAgentIterations = 50
)
```

```kotlin
import ai.koog.agents.features.eventHandler.feature.handleEvents

val agent = AIAgent(
    promptExecutor = executor,
    strategy = CalculatorStrategy.strategy,
    agentConfig = agentConfig,
    toolRegistry = toolRegistry
) {
    handleEvents {
        onToolCallStarting { e ->
            println("Tool called: ${e.tool.name}, args=${e.toolArgs}")
        }
        onAgentExecutionFailed { e ->
            println("Agent error: ${e.throwable.message}")
        }
        onAgentCompleted { e ->
            println("Final result: ${e.result}")
        }
    }
}
```

## 尝试运行 { #try-it }

智能体应将表达式分解为并行工具调用，并返回格式整洁的结果。

```kotlin
import kotlinx.coroutines.runBlocking

runBlocking {
    agent.run("(10 + 20) * (5 + 5) / (2 - 11)")
}
// Expected final value ≈ -33.333...
```工具调用：plus，参数=VarArgs(args={参数 #1 a 对应函数 Line_4_jupyter.CalculatorTools.plus(kotlin.Double, kotlin.Double): kotlin.String=10.0, 参数 #2 b 对应函数 Line_4_jupyter.CalculatorTools.plus(kotlin.Double, kotlin.Double): kotlin.String=20.0})
工具调用：plus，参数=VarArgs(args={参数 #1 a 对应函数 Line_4_jupyter.CalculatorTools.plus(kotlin.Double, kotlin.Double): kotlin.String=5.0, 参数 #2 b 对应函数 Line_4_jupyter.CalculatorTools.plus(kotlin.Double, kotlin.Double): kotlin.String=5.0})
工具调用：minus，参数=VarArgs(args={参数 #1 a 对应函数 Line_4_jupyter.CalculatorTools.minus(kotlin.Double, kotlin.Double): kotlin.String=2.0, 参数 #2 b 对应函数 Line_4_jupyter.CalculatorTools.minus(kotlin.Double, kotlin.Double): kotlin.String=11.0})
工具调用：multiply，参数=VarArgs(args={参数 #1 a 对应函数 Line_4_jupyter.CalculatorTools.multiply(kotlin.Double, kotlin.Double): kotlin.String=30.0, 参数 #2 b 对应函数 Line_4_jupyter.CalculatorTools.multiply(kotlin.Double, kotlin.Double): kotlin.String=10.0})
工具调用：divide，参数=VarArgs(args={参数 #1 a 对应函数 Line_4_jupyter.CalculatorTools.divide(kotlin.Double, kotlin.Double): kotlin.String=1.0, 参数 #2 b 对应函数 Line_4_jupyter.CalculatorTools.divide(kotlin.Double, kotlin.Double): kotlin.String=-9.0})
工具调用：divide，参数=VarArgs(args={参数 #1 a 对应函数 Line_4_jupyter.CalculatorTools.divide(kotlin.Double, kotlin.Double): kotlin.String=300.0, 参数 #2 b 对应函数 Line_4_jupyter.CalculatorTools.divide(kotlin.Double, kotlin.Double): kotlin.String=-9.0})
最终结果：表达式 \((10 + 20) * (5 + 5) / (2 - 11)\) 的结果约为 \(-33.33\)。





表达式 \((10 + 20) * (5 + 5) / (2 - 11)\) 的结果约为 \(-33.33\)。



## 尝试强制并行调用 { #try-forcing-parallel-calls }

要求模型一次性调用所有需要的工具。
你仍应看到正确的计划和稳定的执行。


```kotlin
runBlocking {
    agent.run("Use tools to calculate (10 + 20) * (5 + 5) / (2 - 11). Please call all the tools at once.")
}
```

工具调用：plus，参数=VarArgs(args={参数 #1 a 对应函数 Line_4_jupyter.CalculatorTools.plus(kotlin.Double, kotlin.Double): kotlin.String=10.0, 参数 #2 b 对应函数 Line_4_jupyter.CalculatorTools.plus(kotlin.Double, kotlin.Double): kotlin.String=20.0})
工具调用：plus，参数=VarArgs(args={参数 #1 a 对应函数 Line_4_jupyter.CalculatorTools.plus(kotlin.Double, kotlin.Double): kotlin.String=5.0, 参数 #2 b 对应函数 Line_4_jupyter.CalculatorTools.plus(kotlin.Double, kotlin.Double): kotlin.String=5.0})
工具调用：minus，参数=VarArgs(args={参数 #1 a 对应函数 Line_4_jupyter.CalculatorTools.minus(kotlin.Double, kotlin.Double): kotlin.String=2.0, 参数 #2 b 对应函数 Line_4_jupyter.CalculatorTools.minus(kotlin.Double, kotlin.Double): kotlin.String=11.0})
工具调用：multiply，参数=VarArgs(args={参数 #1 a 对应函数 Line_4_jupyter.CalculatorTools.multiply(kotlin.Double, kotlin.Double): kotlin.String=30.0, 参数 #2 b 对应函数 Line_4_jupyter.CalculatorTools.multiply(kotlin.Double, kotlin.Double): kotlin.String=10.0})
工具调用：divide，参数=VarArgs(args={参数 #1 a 对应函数 Line_4_jupyter.CalculatorTools.divide(kotlin.Double, kotlin.Double): kotlin.String=30.0, 参数 #2 b 对应函数 Line_4_jupyter.CalculatorTools.divide(kotlin.Double, kotlin.Double): kotlin.String=-9.0})
最终结果：\((10 + 20) * (5 + 5) / (2 - 11)\) 的结果约为 \(-3.33\)。





\((10 + 20) * (5 + 5) / (2 - 11)\) 的结果约为 \(-3.33\)。## 使用 Ollama 运行

如果您偏好本地推理，可以交换执行器和模型。

```kotlin
val ollamaExecutor: PromptExecutor = simpleOllamaAIExecutor()

val ollamaAgentConfig = AIAgentConfig(
    prompt = prompt("calculator", LLMParams(temperature = 0.0)) {
        system("You are a calculator. Always use the provided tools for arithmetic.")
    },
    model = OllamaModels.Meta.LLAMA_3_2,
    maxAgentIterations = 50
)


val ollamaAgent = AIAgent(
    promptExecutor = ollamaExecutor,
    strategy = CalculatorStrategy.strategy,
    agentConfig = ollamaAgentConfig,
    toolRegistry = toolRegistry
)

runBlocking {
    ollamaAgent.run("(10 + 20) * (5 + 5) / (2 - 11)")
}
```

    智能体说：表达式 (10 + 20) * (5 + 5) / (2 - 11) 的结果约为 -33.33。





    如果您还有更多问题或需要进一步帮助，请随时提问！