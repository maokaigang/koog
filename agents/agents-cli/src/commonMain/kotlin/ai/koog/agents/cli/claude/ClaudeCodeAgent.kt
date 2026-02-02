package ai.koog.agents.cli.claude

import ai.koog.agents.cli.AgentEvent
import ai.koog.agents.cli.CliAIAgentResponse
import ai.koog.agents.cli.CliAIAgentStructured
import ai.koog.agents.cli.CliAgentException
import ai.koog.agents.cli.CliAgentStructuredResponse
import ai.koog.agents.cli.CliAgentUsage
import ai.koog.agents.cli.transport.CliTransport
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
 * Claude Code CLI wrapper.
 */
public class ClaudeCodeAgent(
    transport: CliTransport,
    apiKey: String? = null,
    model: String? = null,
    systemPrompt: String? = null,
    permissionMode: ClaudePermissionMode? = null,
    additionalFlags: List<String> = emptyList(),
    workspace: String = ".",
    timeout: Duration? = null,
) : CliAIAgentStructured(
    binary = "claude",
    commandFlags = buildList {
        add("-p")

        add("--output-format")
        add("stream-json")

        add("--verbose")

        model?.let {
            add("--model")
            add(it)
        }

        systemPrompt?.let {
            add("--system-prompt")
            add(it)
        }

        permissionMode?.let {
            add("--permission-mode")
            add(it.value)
        }

        addAll(additionalFlags)
    },
    env = buildMap {
        apiKey?.let { put("ANTHROPIC_API_KEY", it) }
    },
    transport = transport,
    workspace = workspace,
    timeout = timeout
) {

    override fun extractResponse(events: List<AgentEvent>): CliAIAgentResponse {
        val jsonEvents = toJsonStdoutEvents(events)

        val resultEvent = jsonEvents
            .lastOrNull { it["type"]?.stringVal == "result" }
            ?: throw CliAgentException("No result event found")

        val content = resultEvent["result"]
            ?.stringVal
            ?: throw CliAgentException("No result found in result event")

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

    override fun <T> structuredOutputFlags(structure: Structure<T, LLMParams.Schema.JSON>): List<String> {
        return listOf("--json-schema", extractClaudeSchema(structure.schema))
    }

    override fun <T> extractStructuredResponse(
        events: List<AgentEvent>,
        structure: Structure<T, *>
    ): CliAgentStructuredResponse<T> {
        val response = extractResponse(events)
        val resultString = toJsonStdoutEvents(events)
            .lastOrNull { it["type"]?.stringVal == "result" }
            ?.get("structured_output")
            ?.toString()
            ?: throw CliAgentException("No structured output found")
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
