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

    ```kotlin
    // Define a state for content creation
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

    // Create GOAP planner with LLM-powered actions
    val planner = AIAgentPlannerStrategy.goap("content-planner", ::ContentState) {
        // Define actions with preconditions and beliefs
        action(
            name = "Create outline",
            precondition = { state -> !state.hasOutline },
            belief = { state -> state.copy(hasOutline = true, outline = "Outline") },
            cost = { 1.0 }
        ) { ctx, state ->
            // Use LLM to create the outline
            val response = ctx.llm.writeSession {
                appendPrompt {
                    user("Create a detailed outline for an article about: ${state.topic}")
                }
                requestLLM()
            }
            state.copy(hasOutline = true, outline = response.content)
        }

        action(
            name = "Write draft",
            precondition = { state -> state.hasOutline && !state.hasDraft },
            belief = { state -> state.copy(hasDraft = true, draft = "Draft") },
            cost = { 2.0 }
        ) { ctx, state ->
            // Use LLM to write the draft
            val response = ctx.llm.writeSession {
                appendPrompt {
                    user("Write an article based on this outline:\n${state.outline}")
                }
                requestLLM()
            }
            state.copy(hasDraft = true, draft = response.content)
        }

        action(
            name = "Review content",
            precondition = { state -> state.hasDraft && !state.hasReview },
            belief = { state -> state.copy(hasReview = true) },
            cost = { 1.0 }
        ) { ctx, state ->
            // Use LLM to review the draft
            val response = ctx.llm.writeSession {
                appendPrompt {
                    user("Review this article and suggest improvements:\n${state.draft}")
                }
                requestLLM()
            }
            println("Review feedback: ${response.content}")
            state.copy(hasReview = true)
        }

        action(
            name = "Publish",
            precondition = { state -> state.hasReview && !state.isPublished },
            belief = { state -> state.copy(isPublished = true) },
            cost = { 1.0 }
        ) { ctx, state ->
            println("Publishing article...")
            state.copy(isPublished = true)
        }

        // Define the goal with a completion condition
        goal(
            name = "Published article",
            description = "Complete and publish the article",
            condition = { state -> state.isPublished }
        )
    }

    // Create and run the agent
    val agentConfig = AIAgentConfig(
        prompt = prompt("writer") {
            system("You are a professional content writer.")
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
        val result = agent.run("The Future of AI in Software Development")
        println("Final state: $result")
    }
    ```

=== "Java"

    ```java
    // Define a state for content creation
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

        var strategy = AIAgentPlannerStrategy.builder("content-planner")
            .goap(ContentState::new)
            .action("Create outline", builder -> builder
                .precondition(state -> !state.hasOutline)
                .belief(state -> state.copy(true, "Outline", false, "", false, false))
                .cost(state -> 1.0)
                .execute((context, state) -> {
                    String response = context.llm().writeSession(session -> {
                        session.appendPrompt(prompt -> {
                            prompt.user("Create a detailed outline for an article about: " + state.topic);
                            return null;
                        });
                        return session.requestLLM().getContent();
                    });
                    return state.copy(true, response, state.hasDraft, state.draft,
                                    state.hasReview, state.isPublished);
                })
            )
            .action("Write draft", builder -> builder
                .precondition(state -> state.hasOutline && !state.hasDraft)
                .belief(state -> state.copy(state.hasOutline, state.outline, true, "Draft", false, false))
                .cost(state -> 2.0)
                .execute((context, state) -> {
                    String response = context.llm().writeSession(session -> {
                        session.appendPrompt(prompt -> {
                            prompt.user("Write an article based on this outline:\n" + state.outline);
                            return null;
                        });
                        return session.requestLLM().getContent();
                    });
                    return state.copy(state.hasOutline, state.outline, true, response,
                                    state.hasReview, state.isPublished);
                })
            )
            .action("Review content", builder -> builder
                .precondition(state -> state.hasDraft && !state.hasReview)
                .belief(state -> state.copy(state.hasOutline, state.outline, state.hasDraft,
                                           state.draft, true, false))
                .cost(state -> 1.0)
                .execute((context, state) -> {
                    String response = context.llm().writeSession(session -> {
                        session.appendPrompt(prompt -> {
                            prompt.user("Review this article and suggest improvements:\n" + state.draft);
                            return null;
                        });
                        return session.requestLLM().getContent();
                    });
                    System.out.println("Review feedback: " + response);
                    return state.copy(state.hasOutline, state.outline, state.hasDraft,
                                    state.draft, true, state.isPublished);
                })
            )
            .action("Publish", builder -> builder
                .precondition(state -> state.hasReview && !state.isPublished)
                .belief(state -> state.copy(state.hasOutline, state.outline, state.hasDraft,
                                           state.draft, state.hasReview, true))
                .cost(state -> 1.0)
                .execute((context, state) -> {
                    System.out.println("Publishing article...");
                    return state.copy(state.hasOutline, state.outline, state.hasDraft,
                                    state.draft, state.hasReview, true);
                })
            )
            .goal("Published article", builder -> builder
                .description("Complete and publish the article")
                .condition(state -> state.isPublished)
            )
            .build();

        var agent = AIAgent.builder()
            .plannerStrategy(strategy)
            .promptExecutor(promptExecutor)
            .llmModel(OpenAIModels.Chat.GPT4o)
            .systemPrompt("You are a professional content writer.")
            .maxIterations(20)
            .build();

        String result = agent.run("The Future of AI in Software Development");
        System.out.println("Final state: " + result);
    }
    ```

## 自定义成本函数 { #custom-cost-functions }

由于 [A* 搜索] 会将成本作为寻找最优动作序列时的重要因素，
你可以为动作和目标定义自定义成本函数，以引导规划器：

=== "Kotlin"

    ```kotlin
    action(
        name = "Expensive operation",
        precondition = { true },
        belief = { state -> state.copy(operationDone = true) },
        cost = { state ->
            // Dynamic cost based on state
            if (state.hasOptimization) 1.0 else 10.0
        }
    ) { ctx, state ->
        // Execute action
        state.copy(operationDone = true)
    }
    ```

=== "Java"

    ```java
    .action("Expensive operation", builder -> builder
        .precondition(state -> true)
        .belief(state -> state.copy(true))
        .cost(state -> {
            // Dynamic cost based on state
            return state.hasOptimization ? 1.0 : 10.0;
        })
        .execute((context, state) -> {
            // Execute action
            return state.copy(true);
        })
    )
    ```

## 状态信念与实际执行的比较 { #state-beliefs-compared-to-actual-execution }

GOAP 会区分“信念”（乐观预测）与“实际执行”这两个概念：

- **规划信念**（`Belief`）：规划器认为将会发生什么，用于规划。
- **实际执行**（`Execution`）：实际发生了什么，用于更新真实状态。

这样一来，规划器可以基于预期结果制定计划，同时在执行阶段正确处理真实结果：

=== "Kotlin"

    ```kotlin
    action(
        name = "Attempt complex task",
        precondition = { state -> !state.taskComplete },
        belief = { state ->
            // Optimistic belief: task will succeed
            state.copy(taskComplete = true)
        },
        cost = { 5.0 }
    ) { ctx, state ->
        // Actual execution might fail or have different results
        val success = performComplexTask()
        state.copy(
            taskComplete = success,
            attempts = state.attempts + 1
        )
    }
    ```

=== "Java"

    ```java
    .action("Attempt complex task", builder -> builder
        .precondition(state -> !state.taskComplete)
        .belief(state -> {
            // Optimistic belief: task will succeed
            return state.copy(true, state.attempts);
        })
        .cost(state -> 5.0)
        .execute((context, state) -> {
            // Actual execution might fail or have different results
            boolean success = performComplexTask();
            return state.copy(success, state.attempts + 1);
        })
    )
    ```

[A* search]: https://en.wikipedia.org/wiki/A*_search_algorithm
