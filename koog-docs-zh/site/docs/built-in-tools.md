<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:53:20+00:00", "source_path": "built-in-tools.md", "source_sha256": "91cb9a0f1301b26a4e97658ba776c667798ca39e5f666f8bfd60e25e34ba9367", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 内置工具 { #built-in-tools }

Koog 框架提供了内置工具，用于处理代理与用户交互的常见场景，涵盖 Kotlin 和 Java。

以下内置工具可用：

| 工具              | <div style="width:115px">名称</div> | 描述                                                                                                              |
|-------------------|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| SayToUser         | `__say_to_user__`                   | 允许代理向用户发送消息。该工具会将代理消息以 `Agent says: ` 前缀打印到控制台。    |
| AskUser           | `__ask_user__`                      | 允许代理向用户请求输入。该工具会将代理消息打印到控制台并等待用户响应。           |
| ExitTool          | `__exit__`                          | 允许代理结束对话并终止会话。                                                        |
| ReadFileTool      | `__read_file__`                     | 读取文本文件，支持可选的行范围选择。使用基于0的行索引返回带元数据的格式化内容。 |
| EditFileTool      | `__edit_file__`                     | 在文件中进行单一、有针对性的文本替换；也可创建新文件或完全替换内容。                |
| ListDirectoryTool | `__list_directory__`                | 以分层树结构列出目录内容，支持深度控制和通配符过滤。                          |
| WriteFileTool     | `__write_file__`                    | 将文本内容写入文件（必要时会创建父目录）。                                                   |

## 注册内置工具 { #registering-built-in-tools }

与任何其他工具一样，内置工具必须添加到工具注册表中才能供代理使用。示例如下：

```kotlin
// Create a tool registry with all built-in tools
val toolRegistry = ToolRegistry {
    tool(SayToUser)
    tool(AskUser)
    tool(ExitTool)
    tool(ReadFileTool(JVMFileSystemProvider.ReadOnly))
    tool(ListDirectoryTool(JVMFileSystemProvider.ReadOnly))
    tool(WriteFileTool(JVMFileSystemProvider.ReadWrite))
}

// Pass the registry when creating an agent
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(apiToken),
    systemPrompt = "You are a helpful assistant.",
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry
)

```

通过在 Kotlin 和 Java 的同一注册表中组合内置工具和自定义工具，您可以为代理创建全面的能力集。
要了解更多关于自定义工具的信息，请参阅[基于注解的工具](annotation-based-tools.md)和[基于类的工具](class-based-tools.md)。