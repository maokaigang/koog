package ai.koog.agents.cli.claude

import ai.koog.agents.cli.AgentEvent
import ai.koog.agents.cli.CliAIAgent
import ai.koog.agents.cli.transport.CliTransport
import ai.koog.prompt.structure.json.generator.JsonSchemaConsts
import ai.koog.prompt.structure.json.generator.StandardJsonSchemaGenerator
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import java.io.File
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
public class ClaudeCodeAgent<Result> internal constructor(
    commandOptions: List<String>,
    env: Map<String, String> = emptyMap(),
    transport: CliTransport,
    workspace: File,
    timeout: Duration?,
    private val isStructured: Boolean,
    private val decode: (String) -> Result?,
) : CliAIAgent<Result?>(
    binary = "claude",
    commandOptions = commandOptions,
    env = env,
    transport = transport,
    workspace = workspace,
    timeout = timeout
) {

    override fun extractResult(events: List<AgentEvent>): Result? {
        val jsonEvents = toJsonStdoutEvents(events)
        val resultEvent = jsonEvents.lastOrNull { it["type"]?.stringVal == "result" } ?: return null

        val resultString = if (isStructured) {
            resultEvent["structured_output"]?.toString()
        } else {
            resultEvent["result"]?.stringVal
        } ?: return null

        return decode(resultString)
    }

    /**
     * Companion object for static builder api and constructor overloads.
     */
    public companion object {
        /**
         * Creates a new [ClaudeCodeAgentBuilder].
         */
        @JvmStatic
        public fun builder(): ClaudeCodeAgentBuilder = ClaudeCodeAgentBuilder()

        /**
         * Creates a new [ClaudeCodeAgent] without structured output (returns [String]).
         */
        public operator fun invoke(
            apiKey: String? = null,
            model: String? = null,
            systemPrompt: String? = null,
            permissionMode: ClaudePermissionMode? = null,
            additionalOptions: List<String> = emptyList(),
            workspace: File = File("."),
            timeout: Duration? = null,
            transport: CliTransport = CliTransport.Default,
        ): ClaudeCodeAgent<String> {
            val commandOptions = getCommandOptions(
                model = model,
                systemPrompt = systemPrompt,
                permissionMode = permissionMode,
                additionalOptions = additionalOptions,
            )

            val env = buildMap {
                apiKey?.let { put("ANTHROPIC_API_KEY", it) }
            }

            return ClaudeCodeAgent(
                commandOptions = commandOptions,
                env = env,
                workspace = workspace,
                timeout = timeout,
                transport = transport,
                isStructured = false,
            ) { it }
        }

        /**
         * Creates a new [ClaudeCodeAgent] with structured output using the provided [serializer].
         */
        public operator fun <T> invoke(
            serializer: KSerializer<T>,
            apiKey: String? = null,
            model: String? = null,
            systemPrompt: String? = null,
            permissionMode: ClaudePermissionMode? = null,
            additionalOptions: List<String> = emptyList(),
            workspace: File = File("."),
            timeout: Duration? = null,
            transport: CliTransport = CliTransport.Default,
        ): ClaudeCodeAgent<T> {
            val commandOptions = getCommandOptions(
                model = model,
                systemPrompt = systemPrompt,
                permissionMode = permissionMode,
                additionalOptions = additionalOptions,
            ) + "--json-schema" + generateClaudeSchema(serializer)

            val env = buildMap {
                apiKey?.let { put("ANTHROPIC_API_KEY", it) }
            }

            return ClaudeCodeAgent(
                commandOptions = commandOptions,
                env = env,
                workspace = workspace,
                timeout = timeout,
                transport = transport,
                isStructured = true,
            ) { resultString ->
                runCatching {
                    json.decodeFromString(serializer, resultString)
                }.getOrNull()
            }
        }

        /**
         * Creates a new [ClaudeCodeAgent] with structured output using the reified type [T].
         */
        @JvmName("createReified")
        public inline operator fun <reified T> invoke(
            apiKey: String? = null,
            model: String? = null,
            systemPrompt: String? = null,
            permissionMode: ClaudePermissionMode? = null,
            additionalOptions: List<String> = emptyList(),
            workspace: File = File("."),
            timeout: Duration? = null,
            transport: CliTransport = CliTransport.Default,
        ): ClaudeCodeAgent<T> = invoke(
            serializer = serializer<T>(),
            apiKey = apiKey,
            model = model,
            systemPrompt = systemPrompt,
            permissionMode = permissionMode,
            additionalOptions = additionalOptions,
            workspace = workspace,
            timeout = timeout,
            transport = transport
        )

        /**
         * Generates a JSON schema for Claude Code CLI from the provided [serializer].
         */
        private fun generateClaudeSchema(serializer: KSerializer<*>): String {
            val schema = StandardJsonSchemaGenerator.generate(
                json = Json.Default,
                name = "output_schema",
                serializer = serializer,
                descriptionOverrides = emptyMap(),
                excludedProperties = emptySet()
            )

            val rootRef = schema.schema[JsonSchemaConsts.Keys.REF]?.jsonPrimitive?.content
            val rootDefKey = rootRef?.removePrefix(JsonSchemaConsts.Keys.REF_PREFIX)
            val rootType = rootDefKey?.let { schema.schema[JsonSchemaConsts.Keys.DEFS]?.jsonObject?.get(it) }

            require(rootType is JsonObject) { "Claude Code CLI requires a JSON object as the root type." }

            val updatedSchema = rootType.toMutableMap()
            val defs = schema.schema[JsonSchemaConsts.Keys.DEFS]
            if (defs != null) {
                updatedSchema[JsonSchemaConsts.Keys.DEFS] = defs
            }

            return JsonObject(updatedSchema).toString()
        }

        private fun getCommandOptions(
            model: String?,
            systemPrompt: String?,
            permissionMode: ClaudePermissionMode?,
            additionalOptions: List<String>,
        ) = buildList {
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

            addAll(additionalOptions)
        }
    }
}
