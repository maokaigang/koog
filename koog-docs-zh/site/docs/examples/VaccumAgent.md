<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:00:55+00:00", "source_path": "examples/VaccumAgent.md", "source_sha256": "4b89dccfcdbdcb991fd69ee7e559c4da7ee27e6d067a8f452d5e3ba158e2e6f4", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 构建一个简单的吸尘器智能体 { #build-a-simple-vacuum-cleaner-agent }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/VaccumAgent.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/VaccumAgent.ipynb
){ .md-button }

在本笔记本中，我们将探索如何使用新的 Kotlin 智能体框架实现一个基本反射智能体。
我们的示例将是经典的“吸尘器世界”问题——
一个包含两个位置（可以是干净或脏污）的简单环境，以及一个需要清洁它们的智能体。

首先，让我们理解我们的环境模型：

```kotlin
import kotlin.random.Random

/**
 * Represents a simple vacuum world with two locations (A and B).
 *
 * The environment tracks:
 * - The current location of the vacuum agent ('A' or 'B')
 * - The cleanliness status of each location (true = dirty, false = clean)
 */
class VacuumEnv {
    var location: Char = 'A'
        private set

    private val status = mutableMapOf(
        'A' to Random.nextBoolean(),
        'B' to Random.nextBoolean()
    )

    fun percept(): Pair<Char, Boolean> = location to status.getValue(location)

    fun clean(): String {
        status[location] = false
        return "cleaned"
    }

    fun moveLeft(): String {
        location = 'A'
        return "move to A"
    }

    fun moveRight(): String {
        location = 'B'
        return "move to B"
    }

    fun isClean(): Boolean = status.values.all { it }

    fun worldLayout(): String = "${status.keys}"

    override fun toString(): String = "location=$location, dirtyA=${status['A']}, dirtyB=${status['B']}"
}
```

VacuumEnv 类模拟了我们简单的世界：
- 两个位置由字符 'A' 和 'B' 表示
- 每个位置可以是干净或脏污（随机初始化）
- 智能体在任何给定时间可以位于任一位置
- 智能体可以感知其当前位置以及该位置是否脏污
- 智能体可以采取行动：移动到特定位置或清洁当前位置

## 为吸尘器智能体创建工具 { #creating-tools-for-vacuum-agent }
现在，让我们定义我们的 AI 智能体将用于与环境交互的工具：

```kotlin
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

/**
 * Provides tools for the LLM agent to control the vacuum robot.
 * All methods either mutate or read from the VacuumEnv passed to the constructor.
 */
@LLMDescription("Tools for controlling a two-cell vacuum world")
class VacuumTools(private val env: VacuumEnv) : ToolSet {

    @Tool
    @LLMDescription("Returns current location and whether it is dirty")
    fun sense(): String {
        val (loc, dirty) = env.percept()
        return "location=$loc, dirty=$dirty, locations=${env.worldLayout()}"
    }

    @Tool
    @LLMDescription("Cleans the current cell")
    fun clean(): String = env.clean()

    @Tool
    @LLMDescription("Moves the agent to cell A")
    fun moveLeft(): String = env.moveLeft()

    @Tool
    @LLMDescription("Moves the agent to cell B")
    fun moveRight(): String = env.moveRight()
}
```

`VacuumTools` 类在我们的 LLM 智能体与环境之间创建了一个接口：

- 它实现了来自 Kotlin AI 智能体框架的 `ToolSet`
- 每个工具都使用 `@Tool` 进行注解，并为 LLM 提供描述
- 这些工具允许智能体感知其环境并采取行动
- 每个方法返回一个描述行动结果的字符串

## 设置智能体 { #setting-up-the-agent }
接下来，我们将配置并创建我们的 AI 智能体：

```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.agents.ext.tool.AskUser
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.params.LLMParams

val env = VacuumEnv()
val apiToken = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY environment variable not set")
val executor = simpleOpenAIExecutor(apiToken = apiToken)

val toolRegistry = ToolRegistry {
    tool(SayToUser)
    tool(AskUser)
    tools(VacuumTools(env).asTools())
}

val systemVacuumPrompt = """
    You are a reflex vacuum-cleaner agent living in a two-cell world labelled A and B.
    Your goal: make both cells clean, using the provided tools.
    First, call sense() to inspect where you are. Then decide: if dirty → clean(); else moveLeft()/moveRight().
    Continue until both cells are clean, then tell the user "done".
    Use sayToUser to inform the user about each step.
""".trimIndent()

val agentConfig = AIAgentConfig(
    prompt = prompt("chat", params = LLMParams(temperature = 1.0)) {
        system(systemVacuumPrompt)
    },
    model = OpenAIModels.Chat.GPT4o,
    maxAgentIterations = 50,
)

val agent = AIAgent(
    promptExecutor = executor,
    strategy = chatAgentStrategy(),
    agentConfig = agentConfig,
    toolRegistry = toolRegistry
)
```

在此设置中：

1. 我们创建环境的一个实例
2. 我们建立与 OpenAI 的 GPT-4o 模型的连接
3. 我们注册智能体可以使用的工具
4. 我们定义一个系统提示，为智能体提供其目标和行为规则
5. 我们使用带有聊天策略的 `AIAgent` 构造函数创建智能体

## 运行智能体 { #running-the-agent }

最后，让我们运行我们的智能体：

```kotlin
import kotlinx.coroutines.runBlocking

runBlocking {
    agent.run("Start cleaning, please")
}
```

    智能体说：当前在单元格 A。它已经是干净的。
    智能体说：已移动到单元格 B。它已经是干净的。

当我们运行此代码时：

1. 智能体收到开始清洁的初始提示
2. 它使用其工具来感知环境并做出决策
3. 它继续清洁直到两个单元格都干净
4. 在整个过程中，它不断向用户通报其正在执行的操作

```kotlin
// Finally we can validate that the work is finished by printing the env state

env
```

    location=B, dirtyA=false, dirtyB=false