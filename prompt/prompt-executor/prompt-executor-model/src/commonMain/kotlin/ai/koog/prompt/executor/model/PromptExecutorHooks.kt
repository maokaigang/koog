package ai.koog.prompt.executor.model

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.LLMChoice
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame

/**
 * Immutable execution arguments propagated through executor hooks.
 */
public sealed interface ExecutionIntent {
    public val prompt: Prompt
    public val tools: List<ToolDescriptor>
    public val model: LLModel
}

/**
 * Initial execution arguments built directly from the corresponding [PromptExecutor] API method call.
 *
 * This intent is passed to:
 * - [PromptExecutorHooks.onModelChoiceFailed]
 * - [PromptExecutorHooks.beforeClientCall]
 * - [PromptExecutorHooks.beforeStreamingStart]
 */
public class InitialExecutionIntent(
    override val prompt: Prompt,
    override val tools: List<ToolDescriptor> = emptyList(),
    override val model: LLModel
) : ExecutionIntent

/**
 * Optional argument overrides returned from pre-call hooks.
 *
 * Current supported override:
 * - [UseDifferentPrompt]
 *
 * Precedence in [combineWith]:
 * - `this` is treated as outer override.
 * - [nestedOverrides] is treated as nested override.
 * - Nested override wins on conflicts.
 */
public sealed interface ExecutionArgOverrides {
    public fun combineWith(nestedOverrides: ExecutionArgOverrides): ExecutionArgOverrides

    public object NoOverrides : ExecutionArgOverrides {
        override fun combineWith(nestedOverrides: ExecutionArgOverrides): ExecutionArgOverrides {
            when (nestedOverrides) {
                is NoOverrides -> return this
                is UseDifferentPrompt -> return nestedOverrides
            }
        }
    }

    public data class UseDifferentPrompt(val prompt: Prompt) : ExecutionArgOverrides {
        override fun combineWith(nestedOverrides: ExecutionArgOverrides): ExecutionArgOverrides {
            when (nestedOverrides) {
                is NoOverrides -> return this
                is UseDifferentPrompt -> return nestedOverrides
            }
        }
    }
}

/**
 * Final execution arguments after applying [ExecutionArgOverrides] to [InitialExecutionIntent].
 *
 * This intent is passed to terminal hooks:
 * - [PromptExecutorHooks.onCompleted]
 * - [PromptExecutorHooks.onMultipleChoicesCompleted]
 * - [PromptExecutorHooks.onModerationCompleted]
 * - [PromptExecutorHooks.onCallFailed]
 * - [PromptExecutorHooks.onStreamingFrame]
 * - [PromptExecutorHooks.onStreamingFailed]
 * - [PromptExecutorHooks.onStreamingCompleted]
 */
public class ResolvedExecutionIntent private constructor(
    override val prompt: Prompt,
    override val tools: List<ToolDescriptor> = emptyList(),
    override val model: LLModel
) : ExecutionIntent {

    public constructor(
        initialExecutionIntent: InitialExecutionIntent,
        executionArgOverrides: ExecutionArgOverrides
    ) : this(
        prompt = when (executionArgOverrides) {
            ExecutionArgOverrides.NoOverrides -> initialExecutionIntent.prompt
            is ExecutionArgOverrides.UseDifferentPrompt -> executionArgOverrides.prompt
        },
        tools = initialExecutionIntent.tools,
        model = initialExecutionIntent.model
    )
}

/**
 * Executor lifecycle hooks emitted by executors.
 *
 * Hooks are observability-oriented and should not suppress exception propagation.
 *
 * Typical order for a successful non-streaming call:
 * 1. [beforeClientCall] which can override some execution arguments (see [ExecutionArgOverrides])
 * 2. Completion hook:
 *      - [onCompleted] (for [PromptExecutor.execute])
 *      - [onMultipleChoicesCompleted] (for [PromptExecutor.executeMultipleChoices])
 *      - [onModerationCompleted] (for [PromptExecutor.moderate])
 *
 * Typical order for a successful streaming call ([PromptExecutor.executeStreaming]):
 * 1. [beforeStreamingStart] which can override some execution arguments (see [ExecutionArgOverrides])
 * 2. [onStreamingFrame] for each frame
 * 3. [onStreamingCompleted]
 *
 * Failure branches:
 * - [onModelChoiceFailed] when model cannot be selected, happens before [beforeClientCall]
 * - [onCallFailed] for non-streaming failures after model was selected
 * - [onStreamingFailed] for streaming failures after model was selected.
 */
public interface PromptExecutorHooks {

    /**
     * Called when model resolution fails before the client call starts.
     *
     * Should be invoked by: [PromptExecutor.execute], [PromptExecutor.executeStreaming],
     * [PromptExecutor.executeMultipleChoices], [PromptExecutor.moderate].
     */
    public suspend fun onModelChoiceFailed(
        intent: InitialExecutionIntent,
        error: Throwable
    ) {
    }

    /**
     * Called right before the real non-streaming client call with the effective execution model.
     * Non-streaming equivalent of [beforeStreamingStart]
     *
     * Should be invoked by: [PromptExecutor.execute], [PromptExecutor.executeMultipleChoices], [PromptExecutor.moderate].
     */
    public suspend fun beforeClientCall(
        intent: InitialExecutionIntent,
        effectiveModel: LLModel
    ): ExecutionArgOverrides = ExecutionArgOverrides.NoOverrides

    /**
     * Called when a regular (non-streaming) execution completes successfully.
     *
     * Should be invoked by: [PromptExecutor.execute].
     */
    public suspend fun onCompleted(
        intent: ResolvedExecutionIntent,
        effectiveModel: LLModel,
        responses: List<Message.Response>
    ) {
    }

    /**
     * Called when a multiple-choices execution completes successfully.
     *
     * Should be invoked by: [PromptExecutor.executeMultipleChoices].
     */
    public suspend fun onMultipleChoicesCompleted(
        intent: ResolvedExecutionIntent,
        effectiveModel: LLModel,
        choices: List<LLMChoice>
    ) {
    }

    /**
     * Called when moderation completes successfully.
     *
     * Should be invoked by: [PromptExecutor.moderate].
     */
    public suspend fun onModerationCompleted(
        intent: ResolvedExecutionIntent,
        effectiveModel: LLModel,
        result: ModerationResult
    ) {
    }

    /**
     * Called when execution fails after model selection was successful.
     *
     * Should be invoked by: [PromptExecutor.execute], [PromptExecutor.executeMultipleChoices], [PromptExecutor.moderate].
     */
    public suspend fun onCallFailed(
        intent: ResolvedExecutionIntent,
        effectiveModel: LLModel,
        error: Throwable
    ) {
    }

    /**
     * Called right before streaming starts with the effective model.
     * Streaming equivalent of [beforeClientCall].
     *
     * Should be invoked by: [PromptExecutor.executeStreaming].
     */
    public suspend fun beforeStreamingStart(
        intent: InitialExecutionIntent,
        effectiveModel: LLModel
    ): ExecutionArgOverrides = ExecutionArgOverrides.NoOverrides

    /**
     * Called for each streaming frame.
     *
     * Should be invoked by: [PromptExecutor.executeStreaming].
     */
    public suspend fun onStreamingFrame(
        intent: ResolvedExecutionIntent,
        effectiveModel: LLModel,
        frame: StreamFrame
    ) {
    }

    /**
     * Called when streaming fails after model selection was successful.
     *
     * Should be invoked by: [PromptExecutor.executeStreaming].
     */
    public suspend fun onStreamingFailed(
        intent: ResolvedExecutionIntent,
        effectiveModel: LLModel,
        error: Throwable
    ) {
    }

    /**
     * Called when streaming completes successfully.
     *
     * Should be invoked by: [PromptExecutor.executeStreaming].
     */
    public suspend fun onStreamingCompleted(intent: ResolvedExecutionIntent, effectiveModel: LLModel) {}
}
