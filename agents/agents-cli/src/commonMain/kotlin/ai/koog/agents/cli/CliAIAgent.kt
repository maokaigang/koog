package ai.koog.agents.cli

import ai.koog.agents.cli.transport.CliAvailable
import ai.koog.agents.cli.transport.CliTransport
import ai.koog.agents.cli.transport.CliUnavailable
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgentState
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.typeOf
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Base helper: runs a process and exposes stdout/stderr as a Flow of AgentEvents.
 *
 * @param binary The name or path of the binary to execute.
 * @param transport The transport mechanism to use for executing the agent process.
 * @param commandOptions Additional CLI options to pass to the agent.
 * @param env Additional environment variables to set for the agent process.
 * @param workspace The working directory for the agent process.
 * @param timeout The maximum duration to wait for the agent process to complete.
 * @param name The name of the agent.
 */
public abstract class CliAIAgent<Result>(
    private val binary: String,
    private val transport: CliTransport,
    private val commandOptions: List<String> = emptyList(),
    private val env: Map<String, String> = emptyMap(),
    private val workspace: String = ".",
    private val timeout: Duration? = null,
    private val name: String = binary
) : AIAgent<String, Result?>() {

    /**
     * Extracts the result of the agent run.
     *
     * This method processes a sequence of generated agent events and returns the agent execution result.
     *
     * @param events a list of events of type [AgentEvent], streamed by the agent cli
     * @return a [Result] object representing the extracted result, or null if no result received
     */
    protected abstract fun extractResult(events: List<AgentEvent>): Result?

    @OptIn(ExperimentalUuidApi::class)
    override val id: String = Uuid.random().toString()

    // TODO(): implement these overrides properly
    // note: it might require refactoring the config and the state logic

    override val agentConfig: AIAgentConfig
        get() = throw UnsupportedOperationException()

    override suspend fun getState(): AIAgentState<Result?> = throw UnsupportedOperationException()

    override suspend fun close() {
        // No-op by default
    }

    @OptIn(InternalAgentsApi::class)
    override suspend fun run(agentInput: String): Result? {
        connect()

        logger.info { "Starting agent '$name' with binary '$binary' in workspace '$workspace'" }

        val processEvents = transport.execute(
            command = listOf(binary) + commandOptions + agentInput,
            workspace = workspace,
            env = env,
            timeout = timeout
        ).onEach {
            logEvent(it)
        }.toList()

        val agentEvents = processEvents.filterIsInstance<AgentEvent>()

        val result = extractResult(agentEvents)

        val exitCode = processEvents.filterIsInstance<CliAIAgentEvent.Exit>().firstOrNull()?.exitCode ?: -1

        logger.info { "Agent '$name' finished with exit code $exitCode" }
        logger.info { "Agent '$name' result: $result" }

        return result
    }

    /**
     * Transforms this agent into a node that can be used in a graph strategy.
     */
    public fun asNode(name: String? = null): AIAgentNodeDelegate<String, Result?> =
        AIAgentNodeDelegate<String, Result?>(
            name = name,
            inputType = typeOf<String>(),
            outputType = typeOf<Any?>(),
            execute = { input -> run(input) }
        )

    private fun connect() {
        when (val availability = transport.checkAvailability(binary)) {
            is CliAvailable -> {
                logger.info { "Connected to agent '$name' (version: ${availability.version ?: "unknown"})" }
            }

            is CliUnavailable -> {
                throw CliNotFoundException(
                    "Agent '$name' CLI '$binary' is not available: ${availability.reason}",
                    availability.cause
                )
            }
        }
    }

    protected companion object {
        private val logger = KotlinLogging.logger { }

        private fun logEvent(event: CliAIAgentEvent) {
            logger.info {
                when (event) {
                    is CliAIAgentEvent.Started -> "Agent Started"
                    is AgentEvent.Stdout -> "[STDOUT] ${event.content}"
                    is AgentEvent.Stderr -> "[STDERR] ${event.content}"
                    is CliAIAgentEvent.Exit -> "Agent Exited (code: ${event.exitCode})"
                    is CliAIAgentEvent.Failed -> "Agent Failed: ${event.message}"
                }
            }
        }

        // json utils

        /**
         * Json configuration for cli agent implementations
         */
        public val json: Json = Json { ignoreUnknownKeys = true }

        /**
         * Converts a list of agent events to a list of JSON objects from stdout
         */
        public fun toJsonStdoutEvents(events: List<AgentEvent>): List<JsonObject> =
            events
                .filterIsInstance<AgentEvent.Stdout>()
                .mapNotNull {
                    runCatching {
                        json.decodeFromString<JsonObject>(it.content).jsonObject
                    }.getOrNull()
                }

        /**
         * Converts a JSON primitive to a string
         */
        public val JsonElement.stringVal: String?
            get() = (this as? JsonPrimitive)?.contentOrNull
    }
}
