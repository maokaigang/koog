package ai.koog.agents.cli

import ai.koog.agents.cli.transport.CliTransport
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.Structure
import ai.koog.prompt.structure.json.JsonStructure
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf
import kotlin.time.Duration

/**
 * Extension of [CliAIAgent] with structured output support.
 *
 * @param binary The name or path of the binary to execute.
 * @param transport The transport mechanism to use for executing the agent process.
 * @param commandFlags Additional CLI flags to pass to the agent.
 * @param env Additional environment variables to set for the agent process.
 * @param workspace The working directory for the agent process.
 * @param timeout The maximum duration to wait for the agent process to complete.
 * @param name The name of the agent.
 */
public abstract class CliAIAgentStructured(
    binary: String,
    transport: CliTransport,
    commandFlags: List<String> = emptyList(),
    env: Map<String, String> = emptyMap(),
    workspace: String = ".",
    timeout: Duration? = null,
    name: String = binary
) : CliAIAgent(binary, transport, commandFlags, env, workspace, timeout, name) {

    /**
     * Returns the CLI flags for the structured request.
     *
     * @param structure The structure to use for the request.
     * @return A list of CLI flags for the structured request.
     */
    protected abstract fun <T> structuredOutputFlags(structure: Structure<T, LLMParams.Schema.JSON>): List<String>

    /**
     * Extracts the structured response from the agent run.
     *
     * @param events a list of events of type [AgentEvent], streamed by the agent cli
     * @param structure The structure used for the request.
     * @return a [CliAgentStructuredResponse] object containing the parsed result
     */
    protected abstract fun <T> extractStructuredResponse(
        events: List<AgentEvent>,
        structure: Structure<T, *>
    ): CliAgentStructuredResponse<T>

    /**
     * Runs the agent with structured output.
     *
     * @param agentInput The input to the agent.
     * @param structure The structure to use for the request.
     * @return a [CliAgentStructuredResponse] object containing the parsed result.
     */
    @OptIn(InternalAgentsApi::class)
    public suspend fun <T> runStructured(
        agentInput: String,
        structure: Structure<T, LLMParams.Schema.JSON>
    ): CliAgentStructuredResponse<T> {
        connect()

        logger.info { "Starting agent '$name' (structured) with binary '$binary' in workspace '$workspace'" }

        val processEvents = transport.execute(
            command = listOf(binary) + commandFlags + structuredOutputFlags(structure) + agentInput,
            workspace = workspace,
            env = env,
            timeout = timeout
        ).onEach {
            logEvent(it)
        }.toList()

        val result = extractStructuredResponse(processEvents.filterIsInstance<AgentEvent>(), structure)

        logger.info { "Agent '$name' (structured) finished" }
        logger.info { "Agent '$name' (structured) result: $result" }

        return result
    }

    /**
     * Runs the agent with structured output.
     *
     * @param agentInput The input to the agent.
     * @param serializer The serializer of the requested structure.
     * @return a [CliAgentStructuredResponse] object containing the parsed result.
     */
    public suspend fun <T> runStructured(
        agentInput: String,
        serializer: KSerializer<T>
    ): CliAgentStructuredResponse<T> {
        val structure = JsonStructure.create(serializer = serializer)
        return runStructured(agentInput, structure)
    }

    /**
     * Runs the agent with structured output.
     *
     * @param agentInput The input to the agent.
     * @param T The type of the requested structure.
     * @Return a [CliAgentStructuredResponse] object containing the parsed result.
     */
    public suspend inline fun <reified T> runStructured(
        agentInput: String,
    ): CliAgentStructuredResponse<T> {
        return runStructured(agentInput, serializer<T>())
    }

    /**
     * Transforms this agent into a node that can be used in a graph strategy.
     *
     * @param name Optional name of the node.
     * @param structure The structure to use for the request.
     */
    public fun <T> asNodeStructured(
        name: String? = null,
        structure: Structure<T, LLMParams.Schema.JSON>
    ): AIAgentNodeDelegate<String, CliAgentStructuredResponse<T>> =
        AIAgentNodeDelegate(
            name = name,
            inputType = typeOf<String>(),
            outputType = typeOf<CliAgentStructuredResponse<T>>(),
        ) { agentInput ->
            runStructured(agentInput, structure).also { response ->
                llm.writeSession {
                    appendPrompt {
                        assistant(response.response.content)
                    }
                }
            }
        }

    /**
     * Transforms this agent into a node that can be used in a graph strategy.
     *
     * @param name Optional name of the node.
     * @param serializer The serializer of the requested structure.
     */
    public fun <T> asNodeStructured(
        name: String? = null,
        serializer: KSerializer<T>
    ): AIAgentNodeDelegate<String, CliAgentStructuredResponse<T>> =
        asNodeStructured(name, JsonStructure.create(serializer = serializer))

    /**
     * Transforms this agent into a node that can be used in a graph strategy.
     *
     * @param name Optional name of the node.
     * @param T The type of the requested structure.
     */
    public inline fun <reified T> asNodeStructured(
        name: String? = null,
    ): AIAgentNodeDelegate<String, CliAgentStructuredResponse<T>> =
        asNodeStructured(name, serializer<T>())
}
