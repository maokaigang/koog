<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:57:21+00:00", "source_path": "examples/BedrockAgent.md", "source_sha256": "329e8d8b5833239a7b07885d506efeace8a97c7adf3f88ca457b3b7961ca8601", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 使用 AWS Bedrock 和 Koog 框架构建 AI 智能体 { #building-ai-agents-with-aws-bedrock-and-koog-framework }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/BedrockAgent.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/BedrockAgent.ipynb
){ .md-button }

欢迎来到这份关于使用 Koog 框架结合 AWS Bedrock 集成来创建智能 AI 智能体的综合指南。在本笔记本中，我们将逐步构建一个能够通过自然语言命令控制简单开关设备的功能性智能体。

## 你将学到什么 { #what-you-ll-learn }

- 如何使用 Kotlin 注解为 AI 智能体定义自定义工具
- 为 LLM 驱动的智能体设置 AWS Bedrock 集成
- 创建工具注册表并将其连接到智能体
- 构建能够理解和执行命令的交互式智能体

## 先决条件 { #prerequisites }

- 具有适当权限的 AWS Bedrock 访问权限
- 已配置的 AWS 凭证（访问密钥和秘密密钥）
- 对 Kotlin 协程的基本理解

让我们开始构建我们的第一个 Bedrock 驱动的 AI 智能体！


```kotlin
%useLatestDescriptors
// %use koog
```


```kotlin
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

// Simple state-holding device that our agent will control
class Switch {
    private var state: Boolean = false

    fun switch(on: Boolean) {
        state = on
    }

    fun isOn(): Boolean {
        return state
    }
}

/**
 * ToolSet implementation that exposes switch operations to the AI agent.
 *
 * Key concepts:
 * - @Tool annotation marks methods as callable by the agent
 * - @LLMDescription provides natural language descriptions for the LLM
 * - ToolSet interface allows grouping related tools together
 */
class SwitchTools(val switch: Switch) : ToolSet {

    @Tool
    @LLMDescription("Switches the state of the switch to on or off")
    fun switchState(state: Boolean): String {
        switch.switch(state)
        return "Switch turned ${if (state) "on" else "off"} successfully"
    }

    @Tool
    @LLMDescription("Returns the current state of the switch (on or off)")
    fun getCurrentState(): String {
        return "Switch is currently ${if (switch.isOn()) "on" else "off"}"
    }
}
```


```kotlin
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools

// Create our switch instance
val switch = Switch()

// Build the tool registry with our switch tools
val toolRegistry = ToolRegistry {
    // Convert our ToolSet to individual tools and register them
    tools(SwitchTools(switch).asTools())
}

println("✅ Tool registry created with ${toolRegistry.tools.size} tools:")
toolRegistry.tools.forEach { tool ->
    println("  - ${tool.name}")
}
```

    ✅ 工具注册表已创建，包含 2 个工具：
      - getCurrentState
      - switchState



```kotlin
import ai.koog.prompt.executor.clients.bedrock.BedrockClientSettings
import ai.koog.prompt.executor.clients.bedrock.BedrockRegions

val region = BedrockRegions.US_WEST_2.regionCode
val maxRetries = 3

// Configure Bedrock client settings
val bedrockSettings = BedrockClientSettings(
    region = region, // Choose your preferred AWS region
    maxRetries = maxRetries // Number of retry attempts for failed requests
)

println("🌐 Bedrock configured for region: $region")
println("🔄 Max retries set to: $maxRetries")
```

    🌐 Bedrock 已配置区域：us-west-2
    🔄 最大重试次数设置为：3



```kotlin
import ai.koog.prompt.executor.llms.all.simpleBedrockExecutor

// Create the Bedrock LLM executor with credentials from environment
val executor = simpleBedrockExecutor(
    awsAccessKeyId = System.getenv("AWS_BEDROCK_ACCESS_KEY")
        ?: throw IllegalStateException("AWS_BEDROCK_ACCESS_KEY environment variable not set"),
    awsSecretAccessKey = System.getenv("AWS_BEDROCK_SECRET_ACCESS_KEY")
        ?: throw IllegalStateException("AWS_BEDROCK_SECRET_ACCESS_KEY environment variable not set"),
    settings = bedrockSettings
)

println("🔐 Bedrock executor initialized successfully")
println("💡 Pro tip: Set AWS_BEDROCK_ACCESS_KEY and AWS_BEDROCK_SECRET_ACCESS_KEY environment variables")
```

    🔐 Bedrock 执行器初始化成功
    💡 专业提示：设置 AWS_BEDROCK_ACCESS_KEY 和 AWS_BEDROCK_SECRET_ACCESS_KEY 环境变量



```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.bedrock.BedrockModels

val agent = AIAgent(
    executor = executor,
    llmModel = BedrockModels.AnthropicClaude35SonnetV2, // State-of-the-art reasoning model
    systemPrompt = """
        You are a helpful assistant that controls a switch device.

        You can:
        - Turn the switch on or off when requested
        - Check the current state of the switch
        - Explain what you're doing

        Always be clear about the switch's current state and confirm actions taken.
    """.trimIndent(),
    temperature = 0.1, // Low temperature for consistent, focused responses
    toolRegistry = toolRegistry
)

println("🤖 AI Agent created successfully!")
println("📋 System prompt configured")
println("🛠️  Tools available: ${toolRegistry.tools.size}")
println("🎯 Model: ${BedrockModels.AnthropicClaude35SonnetV2}")
println("🌡️  Temperature: 0.1 (focused responses)")
```

    🤖 AI 智能体创建成功！
    📋 系统提示已配置
    🛠️  可用工具：2
    🎯 模型：LLModel(provider=Bedrock, id=us.anthropic.claude-3-5-sonnet-20241022-v2:0, capabilities=[Temperature, Tools, ToolChoice, Image, Document, Completion], contextLength=200000, maxOutputTokens=8192)
    🌡️  温度：0.1（聚焦响应）



```kotlin
import kotlinx.coroutines.runBlocking

println("🎉 Bedrock Agent with Switch Tools - Ready to Go!")
println("💬 You can ask me to:")
println("   • Turn the switch on/off")
println("   • Check the current switch state")
println("   • Ask questions about the switch")
println()
println("💡 Example: 'Please turn on the switch' or 'What's the current state?'")
println("📝 Type your request:")

val input = readln()
println("\n🤖 Processing your request...")

runBlocking {
    val response = agent.run(input)
    println("\n✨ Agent response:")
    println(response)
}
```

    🎉 Bedrock 带开关工具的智能体 - 准备就绪！
    💬 你可以要求我：
       • 打开/关闭开关
       • 检查当前开关状态
       • 询问关于开关的问题
    
    💡 示例：'请打开开关' 或 '当前状态是什么？'
    📝 输入你的请求：



    执行被中断


## 刚才发生了什么？ 🎯 { #what-just-happened }

当你运行智能体时，幕后发生了以下神奇的过程：

1. **自然语言处理**：你的输入通过 Bedrock 发送给 Claude 3.5 Sonnet
2. **意图识别**：模型理解你想对开关做什么
3. **工具选择**：根据你的请求，智能体决定调用哪些工具
4. **动作执行**：在你的开关对象上调用适当的工具方法
5. **响应生成**：智能体用自然语言生成关于所发生事情的响应

这展示了 Koog 框架的核心力量——自然语言理解与程序化操作之间的无缝集成。

## 后续步骤与扩展 { #next-steps-extensions }

准备好进一步探索了吗？以下是一些可以尝试的想法：

### 🔧 增强工具 { #enhanced-tools }
```kotlin
@Tool
@LLMDescription("Sets a timer to automatically turn off the switch after specified seconds")
fun setAutoOffTimer(seconds: Int): String

@Tool
@LLMDescription("Gets the switch usage statistics and history")
fun getUsageStats(): String
```

### 🌐 多设备 { #multiple-devices }
```kotlin
class HomeAutomationTools : ToolSet {
    @Tool fun controlLight(room: String, on: Boolean): String
    @Tool fun setThermostat(temperature: Double): String
    @Tool fun lockDoor(doorName: String): String
}
```

### 🧠 记忆与上下文 { #memory-context }
```kotlin
val agent = AIAgent(
    executor = executor,
    // ... other config
    features = listOf(
        MemoryFeature(), // Remember past interactions
        LoggingFeature()  // Track all actions
    )
)
```

### 🔄 高级工作流 { #advanced-workflows }
```kotlin
// Multi-step workflows with conditional logic
@Tool
@LLMDescription("Executes evening routine: dims lights, locks doors, sets thermostat")
fun eveningRoutine(): String
```

## 关键要点✅ **工具即函数**：任何 Kotlin 函数都能成为智能体的能力 { #key-takeaways }
✅ **注解驱动行为**：@Tool 和 @LLMDescription 使函数可被发现
✅ **工具集组织能力**：将相关工具按逻辑分组
✅ **注册表即工具箱**：ToolRegistry 包含所有可用的智能体能力
✅ **智能体统筹一切**：AIAgent 将 LLM 智能与工具结合

Koog 框架让构建能够理解自然语言并执行实际操作的复杂 AI 智能体变得异常简单。从简单开始，然后根据需要添加更多工具和功能来扩展智能体的能力。

**祝您构建智能体愉快！** 🚀

## 测试智能体 { #testing-the-agent }

是时候看看我们的智能体如何行动了！现在智能体能够理解自然语言请求，并使用我们提供的工具来控制开关。

**尝试以下命令：**
- "打开开关"
- "当前状态是什么？"
- "请关闭开关"
- "开关是开还是关？"