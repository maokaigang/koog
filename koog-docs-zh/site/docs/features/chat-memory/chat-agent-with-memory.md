<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:59:31+00:00", "source_path": "features/chat-memory/chat-agent-with-memory.md", "source_sha256": "c8ba945bb113e19bd19bd4e7b64e3b88cbc8c08cb6ed83ebbe0abfa1f832b2c1", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 构建具有记忆功能的聊天助手 { #build-a-chat-agent-with-memory }

本指南演示如何利用 [ChatMemory](index.md) 功能创建一个能够跨多次交互记住历史对话内容的命令行聊天应用。

该 CLI 应用执行以下循环：

- 从控制台读取用户输入
- 若输入不是 `/bye` 且非空，则使用用户输入和指定会话 ID 运行助手
- 助手首先加载该会话 ID 对应的历史对话记录，并将消息与用户输入一同加入提示词
- 助手执行 LLM 交互
- 在运行结束返回响应前，助手将完整对话历史存储至指定会话 ID 下，并限制仅保留最新的 20 条消息
- 应用随后输出助手的响应

流程示意图如下：

```mermaid
graph TB
    subgraph agent [Agent with chat memory]
        load[Load chat history]
        save[Save chat history]
        llm([LLM interaction])
        
        load --> llm --> save
    end
    
    start((Start))
    read[Read input]
    print[Print response]
    exit((Exit))
    
    start --> read
    read --"/bye"--> exit
    read --"empty"--> read
    read --"User input"--> agent
    agent --"Agent response"--> print --> read
```

## 代码实现 { #code }

??? note "前置准备"

    --8<-- "quickstart-snippets.md:prerequisites"

    添加主 [Koog 助手包](https://central.sonatype.com/artifact/ai.koog/koog-agents/)
    及 [聊天记忆功能包](https://mvnrepository.com/artifact/ai.koog/agents-features-memory)
    作为依赖项：

    === "Gradle (Kotlin)"
    
        ```kotlin title="build.gradle.kts"
        dependencies {
            implementation("ai.koog:koog-agents:0.7.0")
            implementation("ai.koog:agents-features-memory:0.7.0")
        }
        ```
    
    === "Gradle (Groovy)"
    
        ```groovy title="build.gradle"
        dependencies {
            implementation 'ai.koog:koog-agents:0.7.0'
            implementation 'ai.koog:agents-features-memory:0.7.0'
        }
        ```
    
    === "Maven"
    
        ```xml title="pom.xml"
        <dependency>
            <groupId>ai.koog</groupId>
            <artifactId>koog-agents-jvm</artifactId>
            <version>0.7.0</version>
        </dependency>
        <dependency>
            <groupId>ai.koog</groupId>
            <artifactId>agents-features-memory-jvm</artifactId>
            <version>0.7.0</version>
        </dependency>
        ```

    --8<-- "quickstart-snippets.md:api-key"

    Examples on this page assume that you have set the `OPENAI_API_KEY` environment variable.

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.chatMemory.feature.ChatMemory
    import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    -->
    ```kotlin
    suspend fun main() {
        val sessionId = "my-conversation"

        simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")).use { executor ->
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = OpenAIModels.Chat.GPT5_2,
                systemPrompt = "You are a helpful assistant."
            ) {
                install(ChatMemory) {
                    windowSize(20) // keep only the last 20 messages
                }
            }

            while (true) {
                print("You: ")
                val input = readln().trim()
                if (input == "/bye") break
                if (input.isEmpty()) continue

                val reply = agent.run(input, sessionId)
                println("Assistant: $reply\n")
            }
        }
    }
    ```

=== "Java"

    ```java
    public class ExampleChatAgentOpenAI {
        public static void main(String[] args) {
            String sessionId = "my-conversation";
    
            try (var executor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY"))) {
                AIAgent<String, String> agent = AIAgent.builder()
                        .promptExecutor(executor)
                        .llmModel(OpenAIModels.Chat.GPT5_2)
                        .systemPrompt("You are a helpful assistant.")
                        .install(ChatMemory.Feature, config -> {
                            config.windowSize(20); // keep only the last 20 messages
                        })
                        .build();
    
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    System.out.print("You: ");
                    String input = scanner.nextLine().trim();
                    if (input.equals("/bye")) break;
                    if (input.isEmpty()) continue;
    
                    String reply = agent.run(input, sessionId);
                    System.out.println("Assistant: " + reply + "\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    ```

## Implementation details

The second argument to `agent.run()` is the [session ID](index.md#session-ids)
used to identify and differentiate between ongoing conversations.
In our example, it is constant because there is only one conversation at a time.
In a real application, you can have a separate unique ID for conversations related to the same user, for example.

The agent uses the default [history provider](index.md#history-providers)
that stores the conversation history in memory.
This means that the history is lost when the application exits.
In a real application, you should implement a custom history provider
to persistently store the history in a database or a file.

The `windowSize(20)` [preprocessor](index.md#preprocessors) ensures a limited context size:
the agent stores only up to 20 most recent messages.
Without this, the prompt size can grow beyond the context limit.

## Example session

```
You: My name is Alice.
Assistant: Nice to meet you, Alice! How can I help you today?

You: What's my favorite color? It's blue.
Assistant: Got it — your favorite color is blue!

You: What's my name?
Assistant: Your name is Alice!
```

Even though each interaction is a separate agent run, the agent correctly answers "Your name is Alice!"
because the `ChatMemory` feature loaded earlier exchanges before processing the third message.