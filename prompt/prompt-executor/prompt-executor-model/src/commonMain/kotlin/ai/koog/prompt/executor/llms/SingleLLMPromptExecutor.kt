package ai.koog.prompt.executor.llms

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.model.ExecutionArgOverrides.NoOverrides
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
import kotlinx.coroutines.flow.flow

/**
 * Executes prompts using a direct client for communication with large language model (LLM) providers.
 *
 * This class provides functionality to execute prompts with optional tools and retrieve either a list of responses
 * or a streaming flow of response chunks from the LLM provider. It delegates the actual LLM interaction to the provided
 * implementation of `LLMClient`.
 *
 * @constructor Creates an instance of `LLMPromptExecutor`.
 * @param llmClient The client used for direct communication with the LLM provider.
 */
@Deprecated(
    "Please use MultiLLMPromptExecutor instead",
    replaceWith = ReplaceWith("MultiLLMPromptExecutor", "ai.koog.prompt.executor.llms.MultiLLMPromptExecutor")
)
public open class SingleLLMPromptExecutor(
    private val llmClient: LLMClient,
) : PromptExecutor() {
    private companion object {
        private val logger = KotlinLogging.logger("ai.koog.prompt.executor.llms.LLMPromptExecutor")
    }

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
        hooks: PromptExecutorHooks?
    ): List<Message.Response> {
        logger.debug { "Executing prompt: $prompt with tools: $tools and model: $model" }
        val initialExecutionIntent = InitialExecutionIntent(prompt, tools, model)

        val effectiveModel = initialExecutionIntent.model
        val overrides = hooks?.beforeClientCall(initialExecutionIntent, effectiveModel) ?: NoOverrides
        val finalIntent = ResolvedExecutionIntent(initialExecutionIntent, overrides)

        val response = try {
            llmClient.execute(finalIntent.prompt, effectiveModel, finalIntent.tools)
        } catch (error: Throwable) {
            hooks?.onCallFailed(finalIntent, effectiveModel, error)
            throw error
        }

        hooks?.onCompleted(finalIntent, effectiveModel, response)
        logger.debug { "Response: $response" }

        return response
    }

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
        hooks: PromptExecutorHooks?
    ): Flow<StreamFrame> = flow {
        logger.debug { "Executing streaming prompt: $prompt with tools: $tools and model: $model" }
        val initialExecutionIntent = InitialExecutionIntent(prompt, tools, model)

        val effectiveModel = initialExecutionIntent.model
        val overrides = hooks?.beforeStreamingStart(initialExecutionIntent, effectiveModel) ?: NoOverrides
        val finalIntent = ResolvedExecutionIntent(initialExecutionIntent, overrides)

        try {
            llmClient.executeStreaming(finalIntent.prompt, effectiveModel, finalIntent.tools).collect { frame ->
                hooks?.onStreamingFrame(finalIntent, effectiveModel, frame)
                emit(frame)
            }
            hooks?.onStreamingCompleted(finalIntent, effectiveModel)
        } catch (error: Throwable) {
            hooks?.onStreamingFailed(finalIntent, effectiveModel, error)
            throw error
        }
    }

    override suspend fun executeMultipleChoices(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
        hooks: PromptExecutorHooks?
    ): List<LLMChoice> {
        logger.debug { "Executing prompt: $prompt with tools: $tools and model: $model" }
        val initialExecutionIntent = InitialExecutionIntent(prompt, tools, model)

        val effectiveModel = initialExecutionIntent.model
        val overrides = hooks?.beforeClientCall(initialExecutionIntent, effectiveModel) ?: NoOverrides
        val finalIntent = ResolvedExecutionIntent(initialExecutionIntent, overrides)

        val choices = try {
            llmClient.executeMultipleChoices(finalIntent.prompt, effectiveModel, finalIntent.tools)
        } catch (error: Throwable) {
            hooks?.onCallFailed(finalIntent, effectiveModel, error)
            throw error
        }

        hooks?.onMultipleChoicesCompleted(finalIntent, effectiveModel, choices)
        logger.debug { "Choices: $choices" }
        return choices
    }

    override suspend fun moderate(
        prompt: Prompt,
        model: LLModel,
        hooks: PromptExecutorHooks?
    ): ModerationResult {
        val initialExecutionIntent = InitialExecutionIntent(prompt = prompt, model = model)

        val effectiveModel = initialExecutionIntent.model
        val overrides = hooks?.beforeClientCall(initialExecutionIntent, effectiveModel) ?: NoOverrides
        val finalIntent = ResolvedExecutionIntent(initialExecutionIntent, overrides)

        val result = try {
            llmClient.moderate(finalIntent.prompt, effectiveModel)
        } catch (error: Throwable) {
            hooks?.onCallFailed(finalIntent, effectiveModel, error)
            throw error
        }
        hooks?.onModerationCompleted(finalIntent, effectiveModel, result)
        return result
    }

    override suspend fun models(): List<LLModel> = llmClient.models()

    override fun getStandardJsonSchemaGenerator(model: LLModel): StandardJsonSchemaGenerator {
        return llmClient.getStandardJsonSchemaGenerator()
    }

    override fun getBasicJsonSchemaGenerator(model: LLModel): BasicJsonSchemaGenerator {
        return llmClient.getBasicJsonSchemaGenerator()
    }

    override fun close() {
        llmClient.close()
    }
}
