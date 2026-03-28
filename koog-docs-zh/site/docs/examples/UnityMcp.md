<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:58:07+00:00", "source_path": "examples/UnityMcp.md", "source_sha256": "743d3e7192e419d54d08df73869abbe2c1a1dbe782de72d8aefe8c0c829b5b6b", "source_tag": "0.7.3", "translation_status": "changed"} -->
# Unity + Koog：通过 Kotlin 智能体驱动你的游戏 { #unity-koog-drive-your-game-from-a-kotlin-agent }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/UnityMcp.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/UnityMcp.ipynb
){ .md-button }

本笔记本将引导你使用 Koog 和模型上下文协议（MCP）构建一个精通 Unity 的 AI 智能体。我们将连接到一个 Unity MCP 服务器，发现工具，通过 LLM 进行规划，并对你当前打开的场景执行操作。

> 先决条件
> - 已安装 Unity-MCP 服务器插件的 Unity 项目
> - JDK 17+
> - 在 OPENAI_API_KEY 环境变量中设置 OpenAI API 密钥



```kotlin
%useLatestDescriptors
%use koog

```


```kotlin
lateinit var process: Process

```

## 1) 提供你的 OpenAI API 密钥 { #1-provide-your-openai-api-key }
我们从 `OPENAI_API_KEY` 环境变量中读取 API 密钥，以便将密钥与笔记本内容分离。



```kotlin
val token = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY environment variable not set")
val executor = simpleOpenAIExecutor(token)
```

## 2) 配置 Unity 智能体 { #2-configure-the-unity-agent }
我们为 Unity 定义一个简洁的系统提示词和智能体设置。



```kotlin
val agentConfig = AIAgentConfig(
    prompt = prompt("cook_agent_system_prompt") {
        system {
            "You are a Unity assistant. You can execute different tasks by interacting with tools from the Unity engine."
        }
    },
    model = OpenAIModels.Chat.GPT4o,
    maxAgentIterations = 1000
)
```


```kotlin

```

## 3) 启动 Unity MCP 服务器 { #3-start-the-unity-mcp-server }
我们将从你的 Unity 项目目录启动 Unity MCP 服务器，并通过标准输入/输出进行连接。



```kotlin
// https://github.com/IvanMurzak/Unity-MCP
val pathToUnityProject = "path/to/unity/project"
val process = ProcessBuilder(
    "$pathToUnityProject/com.ivanmurzak.unity.mcp.server/bin~/Release/net9.0/com.IvanMurzak.Unity.MCP.Server",
    "60606"
).start()
```

## 4) 从 Koog 连接并运行智能体 { #4-connect-from-koog-and-run-the-agent }
我们从 Unity MCP 服务器发现工具，构建一个简单的“先规划后执行”策略，并运行一个仅使用工具来修改你当前打开场景的智能体。



```kotlin
import kotlinx.coroutines.runBlocking

runBlocking {
    // Create the ToolRegistry with tools from the MCP server
    val toolRegistry = McpToolRegistryProvider.fromTransport(
        transport = McpToolRegistryProvider.defaultStdioTransport(process)
    )

    toolRegistry.tools.forEach {
        println(it.name)
        println(it.descriptor)
    }

    val strategy = strategy<String, String>("unity_interaction") {
        val nodePlanIngredients by nodeLLMRequest(allowToolCalls = false)
        val interactionWithUnity by subgraphWithTask<String, String>(
            // work with plan
            tools = toolRegistry.tools,
        ) { input ->
            "Start interacting with Unity according to the plan: $input"
        }

        edge(
            nodeStart forwardTo nodePlanIngredients transformed {
                "Create detailed plan for " + agentInput + "" +
                    "using the following tools: ${toolRegistry.tools.joinToString("\n") {
                        it.name + "\ndescription:" + it.descriptor
                    }}"
            }
        )
        edge(nodePlanIngredients forwardTo interactionWithUnity onAssistantMessage { true })
        edge(interactionWithUnity forwardTo nodeFinish)
    }

    val agent = AIAgent(
        promptExecutor = executor,
        strategy = strategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry,
        installFeatures = {
            install(Tracing)

            install(EventHandler) {
                onAgentStarting { eventContext ->
                    println("OnAgentStarting first (strategy: ${strategy.name})")
                }

                onAgentStarting { eventContext ->
                    println("OnAgentStarting second (strategy: ${strategy.name})")
                }

                onAgentCompleted { eventContext ->
                    println(
                        "OnAgentCompleted (agent id: ${eventContext.agentId}, result: ${eventContext.result})"
                    )
                }
            }
        }
    )

    val result = agent.run(
        " extend current opened scene for the towerdefence game. " +
            "Add more placements for the towers, change the path for the enemies"
    )

    result
}
```

## 5) 关闭 MCP 进程 { #5-shut-down-the-mcp-process }
在运行结束时，务必清理外部的 Unity MCP 服务器进程。



```kotlin
// Shutdown the Unity MCP process
process.destroy()
```