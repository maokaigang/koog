package ai.koog.agents.core.feature

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.ExecutionArgOverrides
import ai.koog.prompt.executor.model.ExecutionIntent
import ai.koog.prompt.executor.model.InitialExecutionIntent
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.model.PromptExecutorHooks
import ai.koog.prompt.executor.model.ResolvedExecutionIntent
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.LLMChoice
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.structure.json.generator.BasicJsonSchemaGenerator
import ai.koog.prompt.structure.json.generator.StandardJsonSchemaGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A wrapper around [ai.koog.prompt.executor.model.PromptExecutor] that allows for adding internal functionality to the executor
 * to catch and log events related to LLM calls.
 *
 * @property executor The [ai.koog.prompt.executor.model.PromptExecutor] to wrap;
 * @property context The [AIAgentContext] associated with the agent that is executing the prompt.
 */
@InternalAgentsApi
public class ContextualPromptExecutor(
    private val executor: PromptExecutor,
    private val context: AIAgentContext,
) : PromptExecutor() {

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
        hooks: PromptExecutorHooks?
    ): List<Message.Response> {
        return executor.execute(
            prompt = prompt,
            model = model,
            tools = tools,
            hooks = ContextualPromptExecutorHooks(eventId = eventId(), outerHooks = hooks)
        )
    }

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
        hooks: PromptExecutorHooks?
    ): Flow<StreamFrame> {
        return executor.executeStreaming(
            prompt = prompt,
            model = model,
            tools = tools,
            hooks = ContextualPromptExecutorHooks(eventId = eventId(), outerHooks = hooks)
        )
    }

    // TODO: Add Pipeline interceptors for this method. Without them features cannot modify prompts before calls to LLMs.
    override suspend fun executeMultipleChoices(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
        hooks: PromptExecutorHooks?
    ): List<LLMChoice> =
        executor.executeMultipleChoices(
            prompt = prompt,
            model = model,
            tools = tools,
            hooks = ContextualPromptExecutorHooks(eventId(), outerHooks = hooks, isMultipleChoices = true)
        )

    override suspend fun moderate(
        prompt: Prompt,
        model: LLModel,
        hooks: PromptExecutorHooks?
    ): ModerationResult {
        return executor.moderate(
            prompt = prompt,
            model = model,
            hooks = ContextualPromptExecutorHooks(eventId = eventId(), outerHooks = hooks)
        )
    }

    override suspend fun models(): List<LLModel> = executor.models()

    override fun getStandardJsonSchemaGenerator(model: LLModel): StandardJsonSchemaGenerator {
        return executor.getStandardJsonSchemaGenerator(model)
    }

    override fun getBasicJsonSchemaGenerator(model: LLModel): BasicJsonSchemaGenerator {
        return executor.getBasicJsonSchemaGenerator(model)
    }

    override fun close() {
        executor.close()
    }

    private fun eventId(): String {
        @OptIn(ExperimentalUuidApi::class)
        return Uuid.random().toString()
    }

    private inner class ContextualPromptExecutorHooks(
        private val eventId: String,
        private val outerHooks: PromptExecutorHooks?,
        private val isMultipleChoices: Boolean = false, // TODO: Remove when pipeline interceptors for multiple choices are added.
    ) : PromptExecutorHooks {

        override suspend fun onModelChoiceFailed(intent: InitialExecutionIntent, error: Throwable) {
            logger.debug {
                "Failed to choose model for LLM call (event id: $eventId, prompt: ${intent.prompt}, tools: [${intent.tools.joinToString { it.name }}]," +
                    " requested model: ${intent.model.id}, error: $error)"
            }
            outerHooks?.onModelChoiceFailed(intent, error)
        }

        override suspend fun beforeClientCall(
            intent: InitialExecutionIntent,
            effectiveModel: LLModel
        ): ExecutionArgOverrides {
            logger.debug {
                "Starting LLM call (event id: $eventId, prompt: ${intent.prompt}, tools: [${intent.tools.joinToString { it.name }}]," +
                    " requested model: ${intent.model.id}, effective model: ${effectiveModel.id})"
            }

            if (isMultipleChoices) {
                return outerHooks?.beforeClientCall(intent, effectiveModel) ?: ExecutionArgOverrides.NoOverrides
            } else {
                val promptBeforeInterceptors = context.llm.prompt

                context.pipeline.onLLMCallStarting(
                    eventId = eventId,
                    executionInfo = context.executionInfo,
                    runId = context.runId,
                    prompt = intent.prompt,
                    model = effectiveModel,
                    tools = intent.tools,
                    context = context
                )

                val outerOverrides = outerHooks?.beforeClientCall(intent, effectiveModel)
                return potentialPromptOverride(promptBeforeInterceptors, intent, outerOverrides)
            }
        }

        override suspend fun onCompleted(
            intent: ResolvedExecutionIntent,
            effectiveModel: LLModel,
            responses: List<Message.Response>
        ) {
            logger.trace { "Finished LLM call (event id: $eventId) with responses: [${responses.joinToString { "${it.role}: ${it.content}" }}]" }
            context.pipeline.onLLMCallCompleted(
                eventId = eventId,
                executionInfo = context.executionInfo,
                runId = context.runId,
                prompt = intent.prompt,
                model = effectiveModel,
                tools = intent.tools,
                responses = responses,
                moderationResponse = null,
                context = context
            )

            outerHooks?.onCompleted(intent, effectiveModel, responses)
        }

        override suspend fun onModerationCompleted(
            intent: ResolvedExecutionIntent,
            effectiveModel: LLModel,
            result: ModerationResult
        ) {
            logger.trace { "Finished moderation LLM request (event id: $eventId) with response: $result" }
            context.pipeline.onLLMCallCompleted(
                eventId = eventId,
                executionInfo = context.executionInfo,
                runId = context.runId,
                prompt = intent.prompt,
                model = effectiveModel,
                tools = emptyList(),
                responses = emptyList(),
                moderationResponse = result,
                context = context
            )

            outerHooks?.onModerationCompleted(intent, effectiveModel, result)
        }

        // TODO: Add Pipeline interceptors for this method. Without them features cannot modify prompts before calls to LLMs.
        override suspend fun onMultipleChoicesCompleted(
            intent: ResolvedExecutionIntent,
            effectiveModel: LLModel,
            choices: List<LLMChoice>
        ) {
            logger.debug {
                val messageBuilder = StringBuilder().appendLine("Finished LLM call with LLM Choice response:")
                choices.forEachIndexed { index, response ->
                    messageBuilder.appendLine("- Response #$index")
                    response.forEach { message ->
                        messageBuilder.appendLine("  -- [${message.role}] ${message.content}")
                    }
                }

                "Finished LLM call with responses: $messageBuilder"
            }

            outerHooks?.onMultipleChoicesCompleted(intent, effectiveModel, choices)
        }

        override suspend fun beforeStreamingStart(
            intent: InitialExecutionIntent,
            effectiveModel: LLModel
        ): ExecutionArgOverrides {
            logger.debug {
                "Executing LLM streaming call (event id: $eventId, prompt: ${intent.prompt}, tools: [${intent.tools.joinToString { it.name }}]," +
                    " requested model: ${intent.model.id}, effective model: ${effectiveModel.id})"
            }
            val promptBeforeInterceptors = context.llm.prompt

            context.pipeline.onLLMStreamingStarting(
                eventId = eventId,
                executionInfo = context.executionInfo,
                runId = context.runId,
                prompt = intent.prompt,
                model = effectiveModel,
                tools = intent.tools,
                context = context
            )

            val outerOverrides = outerHooks?.beforeStreamingStart(intent, effectiveModel)
            return potentialPromptOverride(promptBeforeInterceptors, intent, outerOverrides)
        }

        override suspend fun onStreamingFrame(
            intent: ResolvedExecutionIntent,
            effectiveModel: LLModel,
            frame: StreamFrame
        ) {
            logger.trace { "Received frame from LLM streaming call (event id: $eventId): $frame" }
            context.pipeline.onLLMStreamingFrameReceived(
                eventId = eventId,
                executionInfo = context.executionInfo,
                runId = context.runId,
                prompt = intent.prompt,
                model = effectiveModel,
                streamFrame = frame,
                context = context
            )

            outerHooks?.onStreamingFrame(intent, effectiveModel, frame)
        }

        override suspend fun onStreamingFailed(
            intent: ResolvedExecutionIntent,
            effectiveModel: LLModel,
            error: Throwable
        ) {
            logger.debug(error) { "Error in LLM streaming call (event id: $eventId): $error" }
            context.pipeline.onLLMStreamingFailed(
                eventId = eventId,
                executionInfo = context.executionInfo,
                runId = context.runId,
                prompt = intent.prompt,
                model = effectiveModel,
                throwable = error,
                context = context
            )

            outerHooks?.onStreamingFailed(intent, effectiveModel, error)
        }

        override suspend fun onStreamingCompleted(intent: ResolvedExecutionIntent, effectiveModel: LLModel) {
            logger.debug { "Finished LLM streaming call (event id: $eventId)" }
            context.pipeline.onLLMStreamingCompleted(
                eventId = eventId,
                executionInfo = context.executionInfo,
                runId = context.runId,
                prompt = intent.prompt,
                model = effectiveModel,
                tools = intent.tools,
                context = context
            )

            outerHooks?.onStreamingCompleted(intent, effectiveModel)
        }

        override suspend fun onCallFailed(intent: ResolvedExecutionIntent, effectiveModel: LLModel, error: Throwable) {
            outerHooks?.onCallFailed(intent, effectiveModel, error)
        }

        private fun potentialPromptOverride(
            promptBeforeInterceptors: Prompt,
            intent: ExecutionIntent,
            outerOverrides: ExecutionArgOverrides?
        ): ExecutionArgOverrides {
            val nestedOverrides = if (promptBeforeInterceptors !== context.llm.prompt) {
                logger.debug { "Executing LLM call with modified prompt (event id: $eventId, prompt: ${context.llm.prompt}, tools: [${intent.tools.joinToString { it.name }}])" }
                ExecutionArgOverrides.UseDifferentPrompt(context.llm.prompt)
            } else {
                logger.debug { "Executing LLM call prompt (event id: $eventId, prompt: ${context.llm.prompt}, tools: [${intent.tools.joinToString { it.name }}])" }
                ExecutionArgOverrides.NoOverrides
            }

            return when (outerOverrides) {
                null -> nestedOverrides
                else -> outerOverrides.combineWith(nestedOverrides)
            }
        }
    }
}
