<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:50:58+00:00", "source_path": "agents/functional-agents.md", "source_sha256": "b48cc12e5b7d8b10d3d16d85b7be16da011d544bfd040f3aba85285b0ced95c6", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 函数式智能体 { #functional-agents }

通过函数式智能体，您可以将逻辑实现为一个处理用户输入、与LLM交互、在必要时调用工具并生成最终输出的函数。与[基于图的智能体](graph-based-agents.md)相比，这通常意味着更快的原型设计，但存在以下缺点：

- 不易可视化
- 无状态持久化

??? note "前置条件"

    --8<-- "quickstart-snippets.md:prerequisites"

    --8<-- "quickstart-snippets.md:dependencies"

    --8<-- "quickstart-snippets.md:api-key"

    本页示例假设您通过Ollama在本地运行 Llama 3.2。

本页描述如何实现函数式策略，以便为您的智能体快速原型化一些自定义逻辑。

## 创建最小函数式智能体 { #create-a-minimal-functional-agent }

要创建最小函数式智能体，请使用与[基础智能体](basic-agents.md)相同的[`AIAgent`](https://api.koog.ai/agents/agents-core/ai.koog.agents.core.agent/-a-i-agent/index.html)接口，并向其传递[`AIAgentFunctionalStrategy`](https://api.koog.ai/agents/agents-core/ai.koog.agents.core.agent/-a-i-agent-functional-strategy/index.html)的实例。您可以定义一个函数式策略，该策略接收输入并返回输出，进行一次LLM调用，然后从响应中返回助手消息的内容。

在Kotlin中，最便捷的方式是使用`functionalStrategy {...}` DSL方法。在Java中，您可以在`AIAgent`构建器上使用`functionalStrategy`方法。

=== "Kotlin"

    ```kotlin
    val strategy = functionalStrategy<String, String> { input ->
        val response = requestLLM(input)
        response.asAssistantMessage().content
    }

    val mathAgent = AIAgent(
        promptExecutor = simpleOllamaAIExecutor(),
        llmModel = OllamaModels.Meta.LLAMA_3_2,
        strategy = strategy
    )

    fun main() = runBlocking {
        val result = mathAgent.run("What is 12 × 9?")
        println(result)
    }
    ```

=== "Java"

    ```java
    AIAgent<String, String> mathAgent = AIAgent.builder()
        .promptExecutor(SimpleLLMExecutorsKt.simpleOllamaAIExecutor("http://localhost:11434"))
        .llmModel(OllamaModels.Meta.LLAMA_3_2)
        .functionalStrategy("mathStrategy", (AIAgentFunctionalContext context, String input) -> {
            Message.Response response = context.requestLLM(input);
            if (response instanceof Message.Assistant) {
                return ((Message.Assistant) response).getContent();
            }
            return "";
        })
        .build();

    String result = mathAgent.run("What is 12 × 9?");
    System.out.println(result);
    ```

代理可以生成以下输出：

```text
The answer to 12 × 9 is 108.
```

## 进行顺序 LLM 调用 { #make-sequential-llm-calls }

你可以扩展之前的策略，进行多次连续的LLM调用：

=== "Kotlin"

    ```kotlin
    val strategy = functionalStrategy<String, String> { input ->
        // The first LLM call produces an initial draft based on the user input
        val draft = requestLLM("Draft: $input").asAssistantMessage().content
        // The second LLM call improves the initial draft
        val improved = requestLLM("Improve and clarify.").asAssistantMessage().content
        // The final LLM call formats the improved text and returns the result
        requestLLM("Format the result as bold.").asAssistantMessage().content
    }
    ```

=== "Java"

    ```java
    AIAgent<String, String> mathAgent = AIAgent.builder()
        .promptExecutor(simpleOllamaAIExecutor("http://localhost:11434"))
        .systemPrompt("You are a precise math assistant.")
        .llmModel(OllamaModels.Meta.LLAMA_3_2)
        .functionalStrategy((AIAgentFunctionalContext context, String input) -> {
            // The first LLM call produces an initial draft based on the user input
            Message.Response draftResponse = context.requestLLM("Draft: " + input);
            String draft = "";
            if (draftResponse instanceof Message.Assistant) {
                draft = ((Message.Assistant) draftResponse).getContent();
            }

            // The second LLM call improves the initial draft
            Message.Response improvedResponse = context.requestLLM("Improve and clarify.");
            String improved = "";
            if (improvedResponse instanceof Message.Assistant) {
                improved = ((Message.Assistant) improvedResponse).getContent();
            }

            // The final LLM call formats the improved text and returns the result
            Message.Response finalResponse = context.requestLLM("Format the result as bold.");
            if (finalResponse instanceof Message.Assistant) {
                return ((Message.Assistant) finalResponse).getContent();
            }
            return "";
        })
        .build();
    ```

代理可以生成以下输出：

```text
To calculate the product of 12 and 9, we multiply these two numbers together.

12 × 9 = **108**
```

## 添加工具 { #add-tools }

在许多情况下，一个功能型代理需要完成特定任务，例如读写数据、调用API或执行其他确定性操作。在Koog中，您将这些能力作为[工具](../tools-overview.md)公开，并让LLM决定何时调用它们。

以下是您需要完成的任务：

1. 创建一个[基于注解的工具](../annotation-based-tools.md)。
2. 将其添加到工具注册表中，并将注册表传递给代理。
3. 确保代理策略能够识别LLM响应中的工具调用，执行所请求的工具，
   将结果发送回LLM，并重复此过程，直到没有剩余的工具调用。

=== "Kotlin"

    ```kotlin
    @LLMDescription("Tools for performing math operations")
    class MathTools : ToolSet {
        @Tool
        @LLMDescription("Multiplies two numbers and returns the result")
        fun multiply(a: Int, b: Int): Int {
            // This is not necessary, but it helps to see the tool call in the console output
            println("Multiplying $a and $b...")
            return a * b
        }
    }

    val toolRegistry = ToolRegistry {
        tool(MathTools()::multiply)
    }

    val strategy = functionalStrategy<String, String> { input ->
        // Send the user input to the LLM
        var responses = requestLLMMultiple(input)

        // Only loop while the LLM requests tools
        while (responses.containsToolCalls()) {
            // Extract tool calls from the response
            val pendingCalls = extractToolCalls(responses)
            // Execute the tools and return the results
            val results = executeMultipleTools(pendingCalls)
            // Send the tool results back to the LLM. The LLM may call more tools or return a final output
            responses = sendMultipleToolResults(results)
        }

        // When no tool calls remain, extract and return the assistant message content from the response
        responses.single().asAssistantMessage().content
    }

    val mathAgentWithTools = AIAgent(
        promptExecutor = simpleOllamaAIExecutor(),
        llmModel = OllamaModels.Meta.LLAMA_3_2,
        toolRegistry = toolRegistry,
        strategy = strategy
    )

    fun main() = runBlocking {
        val result = mathAgentWithTools.run("Multiply 3 by 4, then multiply the result by 5.")
        println(result)
    }
    ```

=== "Java"

    ```java
    @LLMDescription(description = "Tools for performing math operations")
    public static class MathTools implements ToolSet {
        @Tool
        @LLMDescription(description = "Multiplies two numbers and returns the result")
        public int multiply(int a, int b) {
            // This is not necessary, but it helps to see the tool call in the console output
            System.out.println("Multiplying " + a + " and " + b + "...");
            return a * b;
        }
    }

    public static void main(String[] args) {
        MathTools mathTools = new MathTools();
        ToolRegistry toolRegistry = ToolRegistry.builder()
            .tools(mathTools)
            .build();

        AIAgent<String, String> mathAgentWithTools = AIAgent.builder()
            .promptExecutor(SimpleLLMExecutorsKt.simpleOllamaAIExecutor("http://localhost:11434"))
            .llmModel(OllamaModels.Meta.LLAMA_3_2)
            .toolRegistry(toolRegistry)
            .functionalStrategy("mathWithTools", (AIAgentFunctionalContext context, String input) -> {
                // Send the user input to the LLM
                List<Message.Response> responses = context.requestLLMMultiple(input);

                // Only loop while the LLM requests tools
                while (context.containsToolCalls(responses)) {
                    // Extract tool calls from the response
                    List<Message.Tool.Call> pendingCalls = context.extractToolCalls(responses);
                    // Execute the tools and return the results
                    List<ReceivedToolResult> results = context.executeMultipleTools(pendingCalls, false);
                    // Send the tool results back to the LLM
                    responses = context.sendMultipleToolResults(results);
                }

                // Extract and return the assistant message content from the response
                Message.Response finalResponse = responses.get(0);
                if (finalResponse instanceof Message.Assistant) {
                    return ((Message.Assistant) finalResponse).getContent();
                }
                return "";
            })
            .build();

        String result = mathAgentWithTools.run("Multiply 3 by 4, then multiply the result by 5.");
        System.out.println(result);
    }
    ```

代理可以生成以下输出：

```text
Multiplying 3 and 4...
Multiplying 12 and 5...
The result of multiplying 3 by 4 is 12. Multiplying 12 by 5 gives us a final answer of 60.
```

## 下一步 { #next-steps }

- 了解如何创建[基于图的智能体](graph-based-agents.md)