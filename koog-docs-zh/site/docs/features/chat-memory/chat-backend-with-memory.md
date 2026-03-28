<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:01:33+00:00", "source_path": "features/chat-memory/chat-backend-with-memory.md", "source_sha256": "ee0285043891b0b36d576eb6ae8084f4906ffc643a8d63d448893d5a3501d90c", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 带记忆功能的聊天后端 { #chat-backend-with-memory }

`ChatMemory` 功能的一个常见模式是后端服务代表客户端管理智能体交互。每个 HTTP 请求都携带一个会话 ID，智能体加载匹配的对话历史记录，生成并返回响应，然后存储更新后的聊天历史记录，为下一次交互做好准备。

```kotlin
// --- Controller ---

@RestController
class ChatController(private val agentService: ChatAgentService) {
    @PostMapping("/chat")
    suspend fun chat(@RequestBody request: ChatRequest): ChatResponse {
        val reply = agentService.chat(request.sessionId, request.message)
        return ChatResponse(reply)
    }
}

// --- Service ---

@Service
class ChatAgentService(private val executor: SingleLLMPromptExecutor) {
    private val toolRegistry = ToolRegistry {
        // register your tools here
    }

    private val agent = AIAgent(
        promptExecutor = executor,
        llmModel = OpenAIModels.Chat.GPT4oMini,
        systemPrompt = "You are a helpful assistant.",
        toolRegistry = toolRegistry,
    ) {
        install(ChatMemory) {
            chatHistoryProvider = MyDatabaseProvider() // persistent storage
            windowSize(50)
        }
    }

    suspend fun chat(sessionId: String, message: String): String {
        return agent.run(message, sessionId)
    }
}
```

有关在 Spring Boot 中设置 Koog 的完整指南，请参阅 [Spring Boot 集成指南](../../spring-boot.md)。