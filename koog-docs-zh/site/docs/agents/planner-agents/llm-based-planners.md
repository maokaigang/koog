<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:53:52+00:00", "source_path": "agents/planner-agents/llm-based-planners.md", "source_sha256": "c0f55f669bd9c63fc090c9c6e3e391fde62b55c8097bbf367bd0ca35780be75b", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 基于 LLM 的规划器 { #llm-based-planners }

基于 LLM 的规划器使用 LLM 来生成和评估计划。
它们基于字符串状态运行，并通过 LLM 请求执行步骤。
字符串状态意味着代理的状态是一个单一的字符串。
在每一步中，代理接受一个初始状态字符串，并返回最终状态字符串作为结果。

??? note "先决条件"

    --8<-- "quickstart-snippets.md:prerequisites"

    --8<-- "quickstart-snippets.md:dependencies"

    --8<-- "quickstart-snippets.md:api-key"

    本页示例假设您已设置 `OPENAI_API_KEY` 环境变量。

Koog 提供了两种简单的规划器：

- [SimpleLLMPlanner](https://api.koog.ai/agents/agents-planner/ai.koog.agents.planner.llm/-simple-l-l-m-planner/index.html)
  仅在开始时生成一次计划，然后按照计划执行直到完成。
  若要包含重新规划功能，请扩展 `SimpleLLMPlanner` 并重写 `assessPlan` 方法，
  以指示代理何时应重新规划。
- [SimpleLLMWithCriticPlanner](https://api.koog.ai/agents/agents-planner/ai.koog.agents.planner.llm/-simple-l-l-m-with-critic-planner/index.html)
  实现了 `assessPlan` 方法，该方法使用 LLM 通过 LLM 请求检查计划的有效性，
  并评估代理是否应重新规划。

以下示例展示了如何使用 `SimpleLLMPlanner` 创建一个简单的规划器代理：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.config.AIAgentConfig
    import ai.koog.agents.planner.AIAgentPlannerStrategy
    import ai.koog.agents.planner.PlannerAIAgent
    import ai.koog.agents.planner.llm.SimpleLLMPlanner
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    import kotlinx.coroutines.runBlocking
    -->
    ```kotlin
    // 创建规划器
    val planner = SimpleLLMPlanner()

    // 将其包装在规划器策略中
    val strategy = AIAgentPlannerStrategy(
        name = "simple-planner",
        planner = planner
    )

    // 配置代理
    val agentConfig = AIAgentConfig(
        prompt = prompt("planner") {
            system("You are a helpful planning assistant.")
        },
        model = OpenAIModels.Chat.GPT4o,
        maxAgentIterations = 50
    )

    // 创建规划器代理
    val agent = PlannerAIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
        strategy = strategy,
        agentConfig = agentConfig
    )

    suspend fun main() {
        // 运行代理执行任务
        val result = agent.run("Create a plan to organize a team meeting")
        println(result)
    }
    ```
    <!--- KNIT example-llm-based-planners-01.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.planner.AIAgentPlannerStrategy;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    class exampleLLMBasedPlanner01 {
        public static void main(String[] args) {
    -->
    <!--- SUFFIX
        }
    }
    -->
    ```java
    // 使用基于 LLM 的规划器创建规划器策略
    AIAgentPlannerStrategy<String, String, ?> strategy =
        AIAgentPlannerStrategy.builder("simple-planner")
            .llmBasedPlanner()
            .build();

    // 创建 OpenAI 执行器
    var promptExecutor = PromptExecutor.builder()
        .openAI("OPENAI_API_KEY")
        .build();

    // 使用 AIAgent 构建器创建规划器代理
    AIAgent<String, String> agent = AIAgent.builder()
        .plannerStrategy(strategy)
        .promptExecutor(promptExecutor)
        .llmModel(OpenAIModels.Chat.GPT4o)
        .systemPrompt("You are a helpful planning assistant.")
        .maxIterations(50)
        .build();

    // 运行代理执行任务
    String result = agent.run("Create a plan to organize a team meeting");
    System.out.println(result);
    ```
     <!--- KNIT exampleLLMBasedPlannerJava01.java -->

## 后续步骤 { #next-steps }

- 了解 [GOAP 代理](goap-agents.md)