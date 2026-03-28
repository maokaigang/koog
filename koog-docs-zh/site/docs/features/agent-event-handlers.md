<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:01:27+00:00", "source_path": "features/agent-event-handlers.md", "source_sha256": "7ddee9fa3ba60e35535a23ee976e75afebc59132331e65c76fea0a8aaa0a72d4", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 事件处理器 { #event-handlers }

您可以通过使用事件处理器来监控和响应智能体工作流中的特定事件，用于日志记录、测试、调试和扩展智能体行为。

## 功能概述 { #feature-overview }

EventHandler 功能允许您挂钩到各种智能体事件。它作为一种事件委托机制，具有以下作用：

- 管理 AI 智能体操作的生命周期。
- 提供用于监控和响应工作流不同阶段的钩子。
- 支持错误处理和恢复。
- 便于工具调用跟踪和结果处理。

<!--## Key components

The EventHandler entity consists of five main handler types:

- Initialization handler that executes at the initialization of an agent run
- Result handler that processes successful results from agent operations
- Error handler that handles exceptions and errors that occur during execution
- Tool call listener that notifies when a tool is about to be invoked
- Tool result listener that processes the results after a tool has been called-->

### 安装与配置 { #installation-and-configuration }

EventHandler 功能通过 `EventHandler` 类与智能体工作流集成，
该类提供了为不同智能体事件注册回调的方法，并可以作为功能安装在智能体配置中。详情请参阅 [API 参考](api:agents-features-event-handler::ai.koog.agents.features.eventHandler.feature.EventHandler)。

要为智能体安装该功能并配置事件处理器，请执行以下操作：

=== "Kotlin"

    ```kotlin
    handleEvents {
        // 处理工具调用
        onToolCallStarting { eventContext ->
            println("Tool called: ${eventContext.toolName} with args ${eventContext.toolArgs}")
        }
        // 处理智能体完成执行时触发的事件
        onAgentCompleted { eventContext ->
            println("Agent finished with result: ${eventContext.result}")
        }

        // 其他事件处理器
    }
    ```

=== "Java"

    ```java
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOllamaAIExecutor("http://localhost:11434"))
        .llmModel(OllamaModels.Meta.LLAMA_3_2)
        .install(EventHandler.Feature, cfg -> {
            // 处理工具调用
            cfg.onToolCallStarting(ctx -> {
                System.out.println("Tool called: " + ctx.getToolName() + " with args " + ctx.getToolArgs());
            });
            // 处理智能体完成执行时触发的事件
            cfg.onAgentCompleted(ctx -> {
                System.out.println("Agent finished with result: " + ctx.getResult());
            });
        })
        .build();
    ```

有关事件处理器配置的更多详细信息，请参阅 [API 参考](api:agents-features-event-handler::ai.koog.agents.features.eventHandler.feature.EventHandlerConfig)。

您也可以在创建智能体时使用 `handleEvents` 扩展函数来设置事件处理器。
此函数同样会安装事件处理器功能并为智能体配置事件处理器。示例如下：

=== "Kotlin"

    ```kotlin
    val agent = AIAgent(
        promptExecutor = simpleOllamaAIExecutor(),
        llmModel = OllamaModels.Meta.LLAMA_3_2,
    ){
        handleEvents {
            // 处理工具调用
            onToolCallStarting { eventContext ->
                println("Tool called: ${eventContext.toolName} with args ${eventContext.toolArgs}")
            }
            // 处理智能体完成执行时触发的事件
            onAgentCompleted { eventContext ->
                println("Agent finished with result: ${eventContext.result}")
            }

            // 其他事件处理器
        }
    }
    ```

=== "Java"

    ```java
    AIAgent<String, String> agent = AIAgent.builder()
        .promptExecutor(simpleOllamaAIExecutor("http://localhost:11434"))
        .llmModel(OllamaModels.Meta.LLAMA_3_2)
        .install(EventHandler.Feature, cfg -> {
            // 处理工具调用
            cfg.onToolCallStarting(ctx -> {
                System.out.println("工具调用: " + ctx.getToolName() + " 参数为 " + ctx.getToolArgs());
            });
            // 处理代理完成执行时触发的事件
            cfg.onAgentCompleted(ctx -> {
                System.out.println("代理执行完成，结果: " + ctx.getResult());
            });
        })
        .build();
    ```
