<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:53:05+00:00", "source_path": "agents/planner-agents/goap-agents.md", "source_sha256": "1988cd49ab9eca6ea3b9ab0018767f4c80fa94b079063f66acfac0da0949fe04", "source_tag": "0.7.3", "translation_status": "changed"} -->
# GOAP 智能体 { #goap-agents }

GOAP 是一种算法规划方法，它使用 [A* 搜索] 来寻找满足目标条件同时最小化总成本的最优动作序列。
与使用 LLM 生成计划的 [LLM 规划器](llm-based-planners.md) 不同，
GOAP 智能体基于预定义的目标和动作，通过算法发现动作序列。

GOAP 规划器围绕三个核心概念工作：

- **状态**：表示世界的当前状态。
- **动作**：定义可以执行的操作，包括前提条件、效果（信念）、成本和执行逻辑。
- **目标**：定义目标条件、启发式成本函数和价值函数。

??? note "先决条件"

    --8<-- "quickstart-snippets.md:prerequisites"

    --8<-- "quickstart-snippets.md:dependencies"

    --8<-- "quickstart-snippets.md:api-key"

    本页示例假设您已设置 `OPENAI_API_KEY` 环境变量。

在 Koog 中，您可以通过声明式地指定目标和动作，使用 DSL 来定义 GOAP 智能体。

要创建 GOAP 智能体，您需要：

1. 将状态定义为一个数据类，其属性代表与您目标相关的各个方面。
2. 使用 [goap()](https://api.koog.ai/agents/agents-planner/ai.koog.agents.planner.goap/goap.html) 函数创建一个 [GOAPPlanner](https://api.koog.ai/agents/agents-planner/ai.koog.agents.planner.goap/-g-o-a-p-planner/index.html) 实例。
    1. 使用 [action()](https://api.koog.ai/agents/agents-planner/ai.koog.agents.planner.goap/-g-o-a-p-planner-builder/action.html) 函数定义带有前提条件和信念的动作。
    2. 使用 [goal()](https://api.koog.ai/agents/agents-planner/ai.koog.agents.planner.goap/-g-o-a-p-planner-builder/goal.html) 函数定义带有完成条件的目标。
3. 使用 [AIAgentPlannerStrategy](https://api.koog.ai/agents/agents-planner/ai.koog.agents.planner/-a-i-agent-planner-strategy/index.html) 包装规划器，并将其传递给 [PlannerAIAgent](https://api.koog.ai/agents/agents-planner/ai.koog.agents.planner/-planner-a-i-agent/index.html) 构造函数。

!!! note

    规划器选择单个动作及其序列。
    每个动作都包含一个必须为真才能执行该动作的前提条件，
    以及一个定义预测结果的信念。
    有关信念的更多信息，请参阅 [状态信念与实际执行的比较](#state-beliefs-compared-to-actual-execution)。

在以下示例中，GOAP 负责创建文章（大纲 → 草稿 → 审阅 → 发布）的高层规划，
而 LLM 则在每个动作内执行实际的内容生成。

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent
    import ai.koog.agents.core.agent.config.AIAgentConfig
    import ai.koog.agents.planner.AIAgentPlannerStrategy
    import ai.koog.agents.planner.goap.GoapAgentState
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.executor.clients.openai.OpenAIModels
    import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
    -->
    ```kotlin
    // 为内容创作定义一个状态
    data class ContentState(
        val topic: String,
        val hasOutline: Boolean = false,
        val outline: String = "",
        val hasDraft: Boolean = false,
        val draft: String = "",
        val hasReview: Boolean = false,
        val isPublished: Boolean = false
    ): GoapAgentState<String, String>(topic) {
        override fun provideOutput(): String = draft
    }
    ```    // 使用 LLM 驱动的动作创建 GOAP 规划器
    val planner = AIAgentPlannerStrategy.goap("content-planner", ::ContentState) {
        // 定义带有前置条件和信念的动作
        action(
            name = "创建大纲",
            precondition = { state -> !state.hasOutline },
            belief = { state -> state.copy(hasOutline = true, outline = "大纲") },
            cost = { 1.0 }
        ) { ctx, state ->
            // 使用 LLM 创建大纲
            val response = ctx.llm.writeSession {
                appendPrompt {
                    user("为关于以下主题的文章创建详细大纲：${state.topic}")
                }
                requestLLM()
            }
            state.copy(hasOutline = true, outline = response.content)
        }

        action(
            name = "撰写草稿",
            precondition = { state -> state.hasOutline && !state.hasDraft },
            belief = { state -> state.copy(hasDraft = true, draft = "草稿") },
            cost = { 2.0 }
        ) { ctx, state ->
            // 使用 LLM 撰写草稿
            val response = ctx.llm.writeSession {
                appendPrompt {
                    user("基于以下大纲撰写文章：\n${state.outline}")
                }
                requestLLM()
            }
            state.copy(hasDraft = true, draft = response.content)
        }

        action(
            name = "审阅内容",
            precondition = { state -> state.hasDraft && !state.hasReview },
            belief = { state -> state.copy(hasReview = true) },
            cost = { 1.0 }
        ) { ctx, state ->
            // 使用 LLM 审阅草稿
            val response = ctx.llm.writeSession {
                appendPrompt {
                    user("审阅以下文章并提出改进建议：\n${state.draft}")
                }
                requestLLM()
            }
            println("审阅反馈：${response.content}")
            state.copy(hasReview = true)
        }

        action(
            name = "发布",
            precondition = { state -> state.hasReview && !state.isPublished },
            belief = { state -> state.copy(isPublished = true) },
            cost = { 1.0 }
        ) { ctx, state ->
            println("正在发布文章...")
            state.copy(isPublished = true)
        }

        // 定义带有完成条件的目标
        goal(
            name = "已发布文章",
            description = "完成并发布文章",
            condition = { state -> state.isPublished }
        )
    }

    // 创建并运行智能体
    val agentConfig = AIAgentConfig(
        prompt = prompt("writer") {
            system("你是一名专业的内容撰稿人。")
        },
        model = OpenAIModels.Chat.GPT4o,
        maxAgentIterations = 20
    )

    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
        strategy = planner,
        agentConfig = agentConfig
    )

    suspend fun main() {
        val result = agent.run("人工智能在软件开发中的未来")
        println("最终状态：$result")
    }
    ```
    <!--- KNIT example-goap-agents-01.kt -->

=== "Java"<!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.planner.AIAgentPlannerStrategy;
    import ai.koog.agents.planner.goap.GoapAgentState;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    class exampleGoapAgents01 {
    -->
<!--- SUFFIX
    }
    -->
```java
// 定义内容创作的状态
static class ContentState extends GoapAgentState<String, String> {
    public String topic;
    public boolean hasOutline = false;
    public String outline = "";
    public boolean hasDraft = false;
    public String draft = "";
    public boolean hasReview = false;
    public boolean isPublished = false;

    public ContentState(String topic) {
        super(topic);
        this.topic = topic;
    }

    public ContentState copy(boolean hasOutline, String outline, boolean hasDraft,
                             String draft, boolean hasReview, boolean isPublished) {
        ContentState state = new ContentState(topic);
        state.hasOutline = hasOutline;
        state.outline = outline;
        state.hasDraft = hasDraft;
        state.draft = draft;
        state.hasReview = hasReview;
        state.isPublished = isPublished;
        return state;
    }

    @Override
    public String provideOutput() {
        return draft;
    }
}

public static void main(String[] args) {
    var promptExecutor = PromptExecutor.builder()
        .openAI("OPENAI_API_KEY")
        .build();
```var strategy = AIAgentPlannerStrategy.builder("content-planner")
            .goap(ContentState::new)
            .action("创建大纲", builder -> builder
                .precondition(state -> !state.hasOutline)
                .belief(state -> state.copy(true, "大纲", false, "", false, false))
                .cost(state -> 1.0)
                .execute((context, state) -> {
                    String response = context.llm().writeSession(session -> {
                        session.appendPrompt(prompt -> {
                            prompt.user("为以下主题创建详细文章大纲：" + state.topic);
                            return null;
                        });
                        return session.requestLLM().getContent();
                    });
                    return state.copy(true, response, state.hasDraft, state.draft,
                                    state.hasReview, state.isPublished);
                })
            )
            .action("撰写草稿", builder -> builder
                .precondition(state -> state.hasOutline && !state.hasDraft)
                .belief(state -> state.copy(state.hasOutline, state.outline, true, "草稿", false, false))
                .cost(state -> 2.0)
                .execute((context, state) -> {
                    String response = context.llm().writeSession(session -> {
                        session.appendPrompt(prompt -> {
                            prompt.user("根据以下大纲撰写文章：\n" + state.outline);
                            return null;
                        });
                        return session.requestLLM().getContent();
                    });
                    return state.copy(state.hasOutline, state.outline, true, response,
                                    state.hasReview, state.isPublished);
                })
            )
            .action("内容审阅", builder -> builder
                .precondition(state -> state.hasDraft && !state.hasReview)
                .belief(state -> state.copy(state.hasOutline, state.outline, state.hasDraft,
                                           state.draft, true, false))
                .cost(state -> 1.0)
                .execute((context, state) -> {
                    String response = context.llm().writeSession(session -> {
                        session.appendPrompt(prompt -> {
                            prompt.user("审阅以下文章并提出改进建议：\n" + state.draft);
                            return null;
                        });
                        return session.requestLLM().getContent();
                    });
                    System.out.println("审阅反馈：" + response);
                    return state.copy(state.hasOutline, state.outline, state.hasDraft,
                                    state.draft, true, state.isPublished);
                })
            )
            .action("发布", builder -> builder
                .precondition(state -> state.hasReview && !state.isPublished)
                .belief(state -> state.copy(state.hasOutline, state.outline, state.hasDraft,
                                           state.draft, state.hasReview, true))
                .cost(state -> 1.0)
                .execute((context, state) -> {
                    System.out.println("正在发布文章...");
                    return state.copy(state.hasOutline, state.outline, state.hasDraft,
                                    state.draft, state.hasReview, true);
                })
            )
            .goal("已发布文章", builder -> builder
                .description("完成并发布文章")
                .condition(state -> state.isPublished)
            )
            .build();```kotlin
        var agent = AIAgent.builder()
            .plannerStrategy(strategy)
            .promptExecutor(promptExecutor)
            .llmModel(OpenAIModels.Chat.GPT4o)
            .systemPrompt("你是一名专业的内容写手。")
            .maxIterations(20)
            .build();

        String result = agent.run("人工智能在软件开发的未来");
        System.out.println("最终状态: " + result);
    }
    ```
    <!--- KNIT exampleGoapAgentsJava01.java -->
    

## 自定义成本函数 { #custom-cost-functions }

由于 [A* 搜索] 使用成本作为寻找最优操作序列的一个因素，你可以为操作和目标定义自定义成本函数来引导规划器：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.planner.goap.GoapAgentState
    import ai.koog.agents.planner.AIAgentPlannerStrategy
    data class MyState(
        val topic: String,
        val operationDone: Boolean = true,
        val hasOptimization: Boolean = true
    ): GoapAgentState<String, String>(topic) {
        override fun provideOutput(): String = ""
    }
    val planner = AIAgentPlannerStrategy.goap("content-planner", ::MyState) {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    action(
        name = "昂贵操作",
        precondition = { true },
        belief = { state -> state.copy(operationDone = true) },
        cost = { state ->
            // 基于状态的动态成本
            if (state.hasOptimization) 1.0 else 10.0
        }
    ) { ctx, state ->
        // 执行操作
        state.copy(operationDone = true)
    }
    ```
    <!--- KNIT example-goap-agents-02.kt -->

=== "Java"
    
    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.planner.AIAgentPlannerStrategy;
    import ai.koog.agents.planner.goap.GoapAgentState;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    class exampleGoapAgents02 {
        public static class MyState extends GoapAgentState<String, String> {
            public String topic;
            public boolean operationDone = false;
            public boolean hasOptimization = true;
            public MyState(String topic) {
                super(topic);
                this.topic = topic;
            }
            public MyState copy(boolean operationDone) {
                MyState state = new MyState(topic);
                state.operationDone = operationDone;
                state.hasOptimization = this.hasOptimization;
                return state;
            }
            @Override
            public String provideOutput() {
                return "";
            }
        }
        public static void main(String[] args) {
            var planner = AIAgentPlannerStrategy.builder("content-planner")
                .goap(MyState::new)
    -->
    <!--- SUFFIX
            .build();
        }
    }
    -->
    ```java
    .action("昂贵操作", builder -> builder
        .precondition(state -> true)
        .belief(state -> state.copy(true))
        .cost(state -> {
            // 基于状态的动态成本
            return state.hasOptimization ? 1.0 : 10.0;
        })
        .execute((context, state) -> {
            // 执行操作
            return state.copy(true);
        })
    )
    ```
    <!--- KNIT exampleGoapAgentsJava02.java -->

## 状态信念与实际执行的对比 { #state-beliefs-compared-to-actual-execution }

GOAP 区分了信念（乐观预测）和实际执行这两个概念：

- **信念**：规划器认为会发生什么，用于规划。
- **执行**：实际发生的情况，用于真实的状态更新。

这使得规划器能够基于预期结果制定计划，同时妥善处理实际结果：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.agents.planner.goap.GoapAgentState
    import ai.koog.agents.planner.AIAgentPlannerStrategy
    data class MyState(
        val topic: String,
        val taskComplete: Boolean = true,
        val attempts: Int = 0
    ): GoapAgentState<String, String>(topic) {
        override fun provideOutput(): String = ""
    }
    fun performComplexTask(): Boolean = true
    val planner = AIAgentPlannerStrategy.goap("content-planner", ::MyState) {
    -->
    <!--- SUFFIX
    }
    -->
    ```kotlin
    action(
        name = "尝试复杂任务",
        precondition = { state -> !state.taskComplete },
        belief = { state ->
            // 乐观信念：任务将会成功
            state.copy(taskComplete = true)
        },
        cost = { 5.0 }
    ) { ctx, state ->
        // 实际执行可能会失败或产生不同结果
        val success = performComplexTask()
        state.copy(
            taskComplete = success,
            attempts = state.attempts + 1
        )
    }
    ```
    <!--- KNIT example-goap-agents-03.kt -->

=== "Java"

    <!--- INCLUDE
    import ai.koog.agents.core.agent.AIAgent;
    import ai.koog.agents.planner.AIAgentPlannerStrategy;
    import ai.koog.agents.planner.goap.GoapAgentState;
    import ai.koog.prompt.executor.clients.openai.OpenAIModels;
    import ai.koog.prompt.executor.model.PromptExecutor;
    class exampleGoapAgents03 {
        public static class MyState extends GoapAgentState<String, String> {
            public String topic;
            public boolean taskComplete = false;
            public int attempts = 0;
            public MyState(String topic) {
                super(topic);
                this.topic = topic;
            }
            public MyState copy(boolean taskComplete, int attempts) {
                MyState state = new MyState(topic);
                state.taskComplete = taskComplete;
                state.attempts = attempts;
                return state;
            }
            @Override
            public String provideOutput() {
                return "";
            }
        }
        static boolean performComplexTask() {
            return true;
        }
        public static void main(String[] args) {
            var planner = AIAgentPlannerStrategy.builder("content-planner")
                .goap(MyState::new)
    -->
    <!--- SUFFIX
            .build();
        }
    }
    -->
    ```java
    .action("尝试复杂任务", builder -> builder
        .precondition(state -> !state.taskComplete)
        .belief(state -> {
            // 乐观信念：任务将会成功
            return state.copy(true, state.attempts);
        })
        .cost(state -> 5.0)
        .execute((context, state) -> {
            // 实际执行可能会失败或产生不同结果
            boolean success = performComplexTask();
            return state.copy(success, state.attempts + 1);
        })
    )
    ```
    <!--- KNIT exampleGoapAgentsJava03.java -->

[A* 搜索]: https://en.wikipedia.org/wiki/A*_search_algorithm