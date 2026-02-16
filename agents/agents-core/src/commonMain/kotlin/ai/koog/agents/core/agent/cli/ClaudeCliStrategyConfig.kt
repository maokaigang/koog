package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.cli.JsonUtils.boolVal
import ai.koog.agents.core.agent.cli.JsonUtils.doubleVal
import ai.koog.agents.core.agent.cli.JsonUtils.intVal
import ai.koog.agents.core.agent.cli.JsonUtils.stringVal
import ai.koog.agents.core.agent.cli.JsonUtils.toJsonStdoutEvents
import ai.koog.agents.core.agent.context.AIAgentCliContext
import ai.koog.cli.transport.CliEvent
import ai.koog.cli.transport.CliException
import ai.koog.cli.transport.CliTransport
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.Structure
import ai.koog.prompt.structure.json.generator.JsonSchemaConsts
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration

/**
 * Claude Code permission mode.
 */
public enum class ClaudePermissionMode(public val value: String) {
    /**
     * Automatically accept all edits.
     */
    AcceptEdits("acceptEdits"),

    /**
     * Bypass all permission checks.
     */
    BypassPermissions("bypassPermissions"),

    /**
     * Default permission mode.
     */
    Default("default"),

    /**
     * Delegate permissions to the parent agent.
     */
    Delegate("delegate"),

    /**
     * Do not ask for permissions.
     */
    DontAsk("dontAsk"),

    /**
     * Plan mode: only show planned actions without executing them.
     */
    Plan("plan")
}

/**
 * Helper functions for Claude CLI agent
 */
public object ClaudeCliHelper {

    /**
     * Generates flags for Claude cli in autonomous mode.
     */
    public fun flags(
        model: LLModel,
        systemMessages: List<Message.System>,
        permissionMode: ClaudePermissionMode?,
        additionalFlags: List<String>
    ): List<String> =
        buildList {
            add("-p")
            add("--output-format")
            add("stream-json")
            add("--verbose")

            if (model.provider == LLMProvider.Anthropic) {
                add("--model")
                add(model.id)
            }

            systemMessages.forEach { systemMessage ->
                add("--append-system-prompt")
                add(systemMessage.content)
            }

            permissionMode?.let {
                add("--permission-mode")
                add(it.value)
            }

            addAll(additionalFlags)
        }

    /**
     * Generates flags for Claude cli in structured mode.
     */
    public fun structuredFlags(
        model: LLModel,
        systemMessages: List<Message.System>,
        permissionMode: ClaudePermissionMode?,
        additionalFlags: List<String>,
        structure: Structure<*, LLMParams.Schema.JSON>
    ): List<String> =
        flags(model, systemMessages, permissionMode, additionalFlags) + listOf(
            "--json-schema",
            extractClaudeSchema(structure.schema)
        )

    /**
     * Generates environment with provided API key.
     */
    public fun env(apiKey: String?): Map<String, String> = buildMap {
        apiKey?.let { put("ANTHROPIC_API_KEY", it) }
    }

    /**
     * Extracts output from Claude CLI events.
     */
    public fun extractOutput(events: List<CliEvent.Line>): CliAIAgentResponse {
        val jsonEvents = toJsonStdoutEvents(events)

        val resultEvent = jsonEvents
            .lastOrNull { it["type"]?.stringVal == "result" }
            ?: throw CliException("No result event found")

        val content = resultEvent["result"]
            ?.stringVal
            ?: throw CliException("No result found in result event")

        val isError = resultEvent["is_error"]?.boolVal ?: false

        val usageObject = resultEvent["usage"]?.jsonObject

        val usage = CliAgentUsage(
            inputTokens = usageObject?.get("input_tokens")?.intVal,
            outputTokens = usageObject?.get("output_tokens")?.intVal,
            buildJsonObject {
                put("cacheCreationInputTokens", usageObject?.get("cache_creation_input_tokens")?.intVal)
                put("cacheReadInputTokens", usageObject?.get("cache_read_input_tokens")?.intVal)
                put("totalCostUsd", resultEvent["total_cost_usd"]?.doubleVal)
            }
        )

        return CliAIAgentResponse(
            content = content,
            isError = isError,
            usage = usage
        )
    }

    /**
     * Extracts structured output from Claude CLI events.
     */
    public fun <T> extractStructuredOutput(
        events: List<CliEvent.Line>,
        structure: Structure<T, *>
    ): CliAgentStructuredResponse<T> {
        val response = extractOutput(events)
        val jsonEvents = toJsonStdoutEvents(events)
        val resultString = jsonEvents
            .lastOrNull { it["type"]?.stringVal == "result" }
            ?.get("structured_output")
            ?.toString()
            ?: throw CliException("No structured output found")
        val result = structure.parse(resultString)

        return CliAgentStructuredResponse(
            result = result,
            response = response.copy(content = resultString)
        )
    }

    /**
     * Extracts a JSON schema for Claude Code CLI from the provided [schema].
     */
    private fun extractClaudeSchema(schema: LLMParams.Schema.JSON): String {
        val jsonSchema = schema.schema

        val defs = jsonSchema[JsonSchemaConsts.Keys.DEFS]!!

        val rootType = jsonSchema[JsonSchemaConsts.Keys.REF]
            ?.stringVal
            ?.removePrefix(JsonSchemaConsts.Keys.REF_PREFIX)
            ?.let { defs.jsonObject[it] }

        require(rootType is JsonObject) { "Claude Code CLI requires a JSON object as the root type." }

        val updatedSchema = rootType.toMutableMap()
        updatedSchema[JsonSchemaConsts.Keys.DEFS] = defs

        return JsonObject(updatedSchema).toString()
    }
}

/**
 * Configuration for Claude CLI agent with structured output.
 */
public class ClaudeCliStrategyStructuredConfig<Input, Output>(
    override val transport: CliTransport,
    public val apiKey: String? = null,
    public val structure: Structure<Output, LLMParams.Schema.JSON>,
    public val permissionMode: ClaudePermissionMode? = null,
    public val additionalFlags: List<String> = emptyList(),
    override val workspace: String = ".",
    override val timeout: Duration? = null,
    private val generateRequestFn: (AIAgentCliContext, Input) -> String = { _, input -> input.toString() },
) : AIAgentCliStrategyConfig<Input, CliAgentStructuredResponse<Output>> {
    override val binary: String = "claude"
    override val env: Map<String, String> = ClaudeCliHelper.env(apiKey)

    override fun flags(model: LLModel, systemMessages: List<Message.System>): List<String> =
        ClaudeCliHelper.structuredFlags(model, systemMessages, permissionMode, additionalFlags, structure)

    override fun generateRequest(context: AIAgentCliContext, input: Input): String =
        generateRequestFn(context, input)

    override fun extractOutput(events: List<CliEvent.Line>): CliAgentStructuredResponse<Output> =
        ClaudeCliHelper.extractStructuredOutput(events, structure)
}

/**
 * Configuration for Claude CLI agent.
 */
public class ClaudeCliStrategyConfig<Input>(
    override val transport: CliTransport,
    public val apiKey: String? = null,
    public val permissionMode: ClaudePermissionMode? = null,
    public val additionalFlags: List<String> = emptyList(),
    override val workspace: String = ".",
    override val timeout: Duration? = null,
    private val generateRequestFn: (AIAgentCliContext, Input) -> String,
) : AIAgentCliStrategyConfig<Input, CliAIAgentResponse> {
    override val binary: String = "claude"
    override val env: Map<String, String> = ClaudeCliHelper.env(apiKey)

    override fun flags(model: LLModel, systemMessages: List<Message.System>): List<String> =
        ClaudeCliHelper.flags(model, systemMessages, permissionMode, additionalFlags)

    override fun generateRequest(context: AIAgentCliContext, input: Input): String =
        generateRequestFn(context, input)

    override fun extractOutput(events: List<CliEvent.Line>): CliAIAgentResponse =
        ClaudeCliHelper.extractOutput(events)
}
