<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:07:25+00:00", "source_path": "parallel-node-execution.md", "source_sha256": "869a432c4e93c7a19ec520e566c09e0a63deb0c5a8368a937db7701034b1c317", "source_tag": "0.7.3", "translation_status": "changed"} -->
## 概述 { #overview }

并行节点执行功能允许您同时运行多个AI智能体节点，从而提升性能并支持复杂的工作流构建。该特性在以下场景中尤为实用：

- 通过不同模型或方法同时处理相同输入
- 并行执行多个独立操作
- 实现竞争性评估模式：生成多个解决方案后进行比对

## 核心组件 { #key-components }

Koog 中的并行节点执行包含以下所述的方法与数据结构。

### 方法 { #methods }

- `parallel()`：并行执行多个节点并收集其结果。

### 数据结构 { #data-structures }

- `ParallelResult`：表示并行节点执行的完成结果。
- `NodeExecutionResult`：包含节点执行的输出与上下文信息。

## 基础用法 { #basic-usage }

### 并行运行节点 { #running-nodes-in-parallel }

要启动节点的并行执行，请按以下格式使用 `parallel` 方法：

```kotlin
val nodeName by parallel<Input, Output>(
   firstNode, secondNode, thirdNode /* Add more nodes if needed */
) {
   // Merge strategy goes here, for example:
   selectByMax { it.length }
}
```

以下是一个实际示例，并行运行三个节点并选择长度最大的结果：

```kotlin
val calc by parallel<String, Int>(
   nodeCalcTokens, nodeCalcSymbols, nodeCalcWords,
) {
   selectByMax { it }
}
```

以上代码并行运行 `nodeCalcTokens`、`nodeCalcSymbols` 和 `nodeCalcWords` 节点，并返回具有最大值的那个结果。

### 合并策略 { #merge-strategies }

并行执行节点后，需要指定结果合并方式。Koog 提供以下合并策略：

- `selectBy()`：根据谓词函数选择结果。
- `selectByMax()`：根据比较函数选择最大值结果。
- `selectByIndex()`：根据选择函数返回的索引选择结果。
- `fold()`：使用操作函数将结果折叠为单个值。

#### selectBy { #selectby }

根据谓词函数选择结果：

```kotlin
val nodeSelectJoke by parallel<String, String>(
   nodeOpenAI, nodeAnthropicSonnet, nodeAnthropicOpus,
) {
   selectBy { it.contains("programmer") }
}
```

此操作选择第一个包含“programmer”单词的笑话。

#### selectByMax { #selectbymax }

根据比较函数选择最大值结果：

```kotlin
val nodeLongestJoke by parallel<String, String>(
   nodeOpenAI, nodeAnthropicSonnet, nodeAnthropicOpus,
) {
   selectByMax { it.length }
}
```

此操作选择长度最长的笑话。

#### selectByIndex { #selectbyindex }

根据选择函数返回的索引选择结果：

```kotlin
val nodeBestJoke by parallel<String, String>(
   nodeOpenAI, nodeAnthropicSonnet, nodeAnthropicOpus,
) {
   selectByIndex { jokes ->
      // Use another LLM to determine the best joke
      llm.writeSession {
         model = OpenAIModels.Chat.GPT4o
         appendPrompt {
            system("You are a comedy critic. Select the best joke.")
            user("Here are three jokes: ${jokes.joinToString("\n\n")}")
         }
         val response = requestLLMStructured<JokeRating>()
         response.getOrNull()!!.data.bestJokeIndex
      }
   }
}
```

此示例通过另一个 LLM 调用来确定最佳笑话的索引。

#### fold { #fold }

使用操作函数将结果折叠为单个值：

```kotlin
val nodeAllJokes by parallel<String, String>(
   nodeOpenAI, nodeAnthropicSonnet, nodeAnthropicOpus,
) {
   fold("Jokes:\n") { result, joke -> "$result\n$joke" }
}
```

此操作将所有笑话合并为单个字符串。

## 示例：最佳笑话智能体 { #example-best-joke-agent }

以下完整示例演示如何使用并行执行从不同 LLM 模型生成笑话并选择最佳结果：

```kotlin
val strategy = strategy("best-joke") {
   // Define nodes for different LLM models
   val nodeOpenAI by node<String, String> { topic ->
      llm.writeSession {
         model = OpenAIModels.Chat.GPT4o
         appendPrompt {
            system("You are a comedian. Generate a funny joke about the given topic.")
            user("Tell me a joke about $topic.")
         }
         val response = requestLLMWithoutTools()
         response.content
      }
   }

   val nodeAnthropicSonnet by node<String, String> { topic ->
      llm.writeSession {
         model = AnthropicModels.Sonnet_4_5
         appendPrompt {
            system("You are a comedian. Generate a funny joke about the given topic.")
            user("Tell me a joke about $topic.")
         }
         val response = requestLLMWithoutTools()
         response.content
      }
   }

   val nodeAnthropicOpus by node<String, String> { topic ->
      llm.writeSession {
         model = AnthropicModels.Opus_4_6
         appendPrompt {
            system("You are a comedian. Generate a funny joke about the given topic.")
            user("Tell me a joke about $topic.")
         }
         val response = requestLLMWithoutTools()
         response.content
      }
   }

   // Execute joke generation in parallel and select the best joke
   val nodeGenerateBestJoke by parallel(
      nodeOpenAI, nodeAnthropicSonnet, nodeAnthropicOpus,
   ) {
      selectByIndex { jokes ->
         // Another LLM (e.g., GPT4o) would find the funniest joke:
         llm.writeSession {
            model = OpenAIModels.Chat.GPT4o
            appendPrompt {
               prompt("best-joke-selector") {
                  system("You are a comedy critic. Give a critique for the given joke.")
                  user(
                     """
                            Here are three jokes about the same topic:

                            ${jokes.mapIndexed { index, joke -> "Joke $index:\n$joke" }.joinToString("\n\n")}

                            Select the best joke and explain why it's the best.
                            """.trimIndent()
                  )
               }
            }

            val response = requestLLMStructured<JokeRating>()
            val bestJoke = response.getOrNull()!!.data
            bestJoke.bestJokeIndex
         }
      }
   }

   // Connect the nodes
   nodeStart then nodeGenerateBestJoke then nodeFinish
}
```

## 最佳实践 { #best-practices }

1. **考虑资源限制**：并行执行节点时需注意资源使用情况，特别是在同时发起多个 LLM API 调用时。

2. **上下文管理**：每次并行执行都会创建分叉上下文。合并结果时，需选择保留哪个上下文，或如何合并来自不同执行的上下文。3. **针对您的使用场景进行优化**：
    - 对于竞争性评估（如笑话示例），使用 `selectByIndex` 来选择最佳结果
    - 对于寻找最大值，使用 `selectByMax`
    - 对于基于条件的筛选，使用 `selectBy`
    - 对于聚合操作，使用 `fold` 将所有结果合并为复合输出

## 性能考量 { #performance-considerations }

并行执行可以显著提升吞吐量，但也会带来一些开销：

- 每个并行节点都会创建一个新的协程
- 上下文的分叉与合并会增加一定的计算成本
- 大量并行执行时可能出现资源争用

为实现最佳性能，建议对以下类型的操作进行并行化：

- 彼此独立的任务
- 执行时间较长的操作
- 不共享可变状态的任务