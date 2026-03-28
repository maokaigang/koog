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

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.agent.functionalStrategy
    import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import kotlinx.coroutines.runBlocking
    -->
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
    <!--- KNIT example-functional-agent-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
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
   <!--- KNIT example-functional-agent-java-01.java -->

智能体可能产生以下输出：

```text
The answer to 12 × 9 is 108.
```
<!--- KNIT example-functional-agent-01.txt -->

## 进行顺序LLM调用 { #make-sequential-llm-calls }

您可以扩展先前的策略以进行多次顺序LLM调用：

=== "Kotlin"<!--- INCLUDE
    import ai.koog.agents.core.agent.functionalStrategy
    -->
```kotlin
val strategy = functionalStrategy<String, String> { input ->
    // 第一个 LLM 调用基于用户输入生成初始草稿
    val draft = requestLLM("草稿: $input").asAssistantMessage().content
    // 第二个 LLM 调用改进初始草稿
    val improved = requestLLM("改进并澄清。").asAssistantMessage().content
    // 最后的 LLM 调用格式化改进后的文本并返回结果
    requestLLM("将结果格式化为粗体。").asAssistantMessage().content
}
```
<!--- KNIT example-functional-agent-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    AIAgent<String, String> mathAgent = AIAgent.builder()
        .promptExecutor(simpleOllamaAIExecutor("http://localhost:11434"))
        .systemPrompt("你是一个精确的数学助手。")
        .llmModel(OllamaModels.Meta.LLAMA_3_2)
        .functionalStrategy((AIAgentFunctionalContext context, String input) -> {
            // 第一个 LLM 调用基于用户输入生成初始草稿
            Message.Response draftResponse = context.requestLLM("草稿: " + input);
            String draft = "";
            if (draftResponse instanceof Message.Assistant) {
                draft = ((Message.Assistant) draftResponse).getContent();
            }

            // 第二个 LLM 调用改进初始草稿
            Message.Response improvedResponse = context.requestLLM("改进并澄清。");
            String improved = "";
            if (improvedResponse instanceof Message.Assistant) {
                improved = ((Message.Assistant) improvedResponse).getContent();
            }

            // 最后的 LLM 调用格式化改进后的文本并返回结果
            Message.Response finalResponse = context.requestLLM("将结果格式化为粗体。");
            if (finalResponse instanceof Message.Assistant) {
                return ((Message.Assistant) finalResponse).getContent();
            }
            return "";
        })
        .build();
    ```
    <!--- KNIT example-functional-agent-java-02.java -->

该代理可以生成以下输出：

```text
To calculate the product of 12 and 9, we multiply these two numbers together.

12 × 9 = **108**
```
<!--- KNIT example-functional-agent-02.txt -->

## 添加工具 { #add-tools }

在许多情况下，功能型代理需要完成特定任务，
例如读写数据、调用 API 或执行其他确定性操作。
在 Koog 中，你可以将此类能力作为[工具](../tools-overview.md)公开，并让 LLM 决定何时调用它们。

你需要执行以下操作：

1. 创建一个[基于注解的工具](../annotation-based-tools.md)。
2. 将其添加到工具注册表中，并将注册表传递给代理。
3. 确保代理策略能够识别 LLM 响应中的工具调用，执行请求的工具，
   将其结果发送回 LLM，并重复此过程直到没有剩余的工具调用。

=== "Kotlin"<!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.agent.functionalStrategy
    import ai.koog.agents.core.tools.ToolRegistry
    import ai.koog.agents.core.tools.annotations.LLMDescription
    import ai.koog.agents.core.tools.annotations.Tool
    import ai.koog.agents.core.tools.reflect.ToolSet
    import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
    import ai.koog.prompt.executor.ollama.client.OllamaModels
    import kotlinx.coroutines.runBlocking
    -->
```kotlin
@LLMDescription("用于执行数学运算的工具")
class MathTools : ToolSet {
    @Tool
    @LLMDescription("将两个数字相乘并返回结果")
    fun multiply(a: Int, b: Int): Int {
        // 这并非必需，但有助于在控制台输出中查看工具调用
        println("正在计算 $a 乘以 $b...")
        return a * b
    }
}

val toolRegistry = ToolRegistry {
    tool(MathTools()::multiply)
}

val strategy = functionalStrategy<String, String> { input ->
    // 将用户输入发送给 LLM
    var responses = requestLLMMultiple(input)

    // 仅当 LLM 请求工具时循环
    while (responses.containsToolCalls()) {
        // 从响应中提取工具调用
        val pendingCalls = extractToolCalls(responses)
        // 执行工具并返回结果
        val results = executeMultipleTools(pendingCalls)
        // 将工具结果发送回 LLM。LLM 可能调用更多工具或返回最终输出
        responses = sendMultipleToolResults(results)
    }

    // 当没有剩余工具调用时，从响应中提取并返回助手消息内容
    responses.single().asAssistantMessage().content
}

val mathAgentWithTools = AIAgent(
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2,
    toolRegistry = toolRegistry,
    strategy = strategy
)

fun main() = runBlocking {
    val result = mathAgentWithTools.run("将 3 乘以 4，再将结果乘以 5。")
    println(result)
}
```
<!--- KNIT example-functional-agent-03.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    @LLMDescription(description = "用于执行数学运算的工具")
    public static class MathTools implements ToolSet {
        @Tool
        @LLMDescription(description = "将两个数字相乘并返回结果")
        public int multiply(int a, int b) {
            // 这并非必需，但有助于在控制台输出中查看工具调用
            System.out.println("正在计算 " + a + " 乘以 " + b + "...");
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
                // 将用户输入发送给 LLM
                List<Message.Response> responses = context.requestLLMMultiple(input);```java
// 仅当 LLM 请求工具时循环执行
while (context.containsToolCalls(responses)) {
    // 从响应中提取工具调用
    List<Message.Tool.Call> pendingCalls = context.extractToolCalls(responses);
    // 执行工具并返回结果
    List<ReceivedToolResult> results = context.executeMultipleTools(pendingCalls, false);
    // 将工具结果发送回 LLM
    responses = context.sendMultipleToolResults(results);
}

// 从响应中提取并返回助手消息内容
Message.Response finalResponse = responses.get(0);
if (finalResponse instanceof Message.Assistant) {
    return ((Message.Assistant) finalResponse).getContent();
}
return "";
})
.build();

String result = mathAgentWithTools.run("将3乘以4，再将结果乘以5。");
System.out.println(result);
}
```
<!--- KNIT example-functional-agent-java-03.java -->

智能体可生成以下输出：

```text
Multiplying 3 and 4...
Multiplying 12 and 5...
The result of multiplying 3 by 4 is 12. Multiplying 12 by 5 gives us a final answer of 60.
```
<!--- KNIT example-functional-agent-03.txt -->

## 后续步骤 { #next-steps }

- 学习如何创建[基于图的智能体](graph-based-agents.md)