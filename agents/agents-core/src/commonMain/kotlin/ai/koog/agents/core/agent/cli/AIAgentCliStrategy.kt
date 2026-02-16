package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.context.AIAgentCliContext
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.cli.transport.CliEvent
import ai.koog.cli.transport.CliNotFoundException
import ai.koog.cli.transport.CliTransport
import ai.koog.cli.transport.CliUnavailable
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.Structure
import ai.koog.prompt.structure.json.JsonStructure
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.serializer
import kotlin.jvm.JvmName
import kotlin.time.Duration

/**
 * Configuration for [AIAgentCliStrategy].
 */
public interface AIAgentCliStrategyConfig<Input, Output> {
    /** CLI transport for executing commands. */
    public val transport: CliTransport

    /** Binary name of the CLI tool. */
    public val binary: String

    /** Working directory for command execution. */
    public val workspace: String

    /** Environment variables for command execution. */
    public val env: Map<String, String>

    /** Execution timeout. */
    public val timeout: Duration?

    /** Generates command-line flags based on LLM model and system messages. */
    public fun flags(model: LLModel, systemMessages: List<Message.System>): List<String>

    /** Generates the request string from context and input. */
    public fun generateRequest(context: AIAgentCliContext, input: Input): String

    /** Extracts the output from CLI event lines. */
    public fun extractOutput(events: List<CliEvent.Line>): Output
}

/**
 * Strategy for executing AI agents using a command-line interface (CLI).
 */
public class AIAgentCliStrategy<Input, Output>(
    override val name: String,
    private val config: AIAgentCliStrategyConfig<Input, Output>
) : AIAgentStrategy<Input, Output, AIAgentCliContext> {

    override suspend fun execute(context: AIAgentCliContext, input: Input): Output {
        connect()

        val model = context.config.model
        val systemMessages = context.config.prompt.messages.filterIsInstance<Message.System>()

        val command = listOf(config.binary) + config.flags(model, systemMessages) + config.generateRequest(context, input)
        logger.info { "Executing CLI command: ${command.joinToString(" ")}" }

        val events = config.transport.execute(
            command = command,
            workspace = config.workspace,
            env = config.env,
            timeout = config.timeout
        )
            .onEach { logEvent(it) }
            .filterIsInstance<CliEvent.Line>()
            .toList()

        val result = config.extractOutput(events)

        return result
    }

    private fun connect() {
        val availability = config.transport.checkAvailability(config.binary)
        if (availability is CliUnavailable) {
            throw CliNotFoundException(
                "CLI '${config.binary}' is not available: ${availability.reason}",
                availability.cause
            )
        }
    }

    /**
     * Json utils for cli agent implementations
     */
    public companion object {

        // claude constructors

        /**
         * Creates a new instance of [AIAgentCliStrategy] using Claude.
         */
        public fun claude(
            name: String,
            transport: CliTransport,
            apiKey: String? = null,
            permissionMode: ClaudePermissionMode? = null,
            additionalFlags: List<String> = emptyList(),
            workspace: String = ".",
            timeout: Duration? = null,
        ): AIAgentCliStrategy<String, CliAIAgentResponse> = claude<String>(
            name = name,
            transport = transport,
            apiKey = apiKey,
            permissionMode = permissionMode,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            generateRequest = { _, input -> input },
        )

        /**
         * Creates a new instance of [AIAgentCliStrategy] using Claude.
         */
        @JvmName("claudeGeneric")
        public fun <Input> claude(
            name: String,
            transport: CliTransport,
            apiKey: String? = null,
            permissionMode: ClaudePermissionMode? = null,
            additionalFlags: List<String> = emptyList(),
            workspace: String = ".",
            timeout: Duration? = null,
            generateRequest: (AIAgentCliContext, Input) -> String,
        ): AIAgentCliStrategy<Input, CliAIAgentResponse> = AIAgentCliStrategy(
            name = name,
            config = ClaudeCliStrategyConfig(
                transport = transport,
                apiKey = apiKey,
                permissionMode = permissionMode,
                additionalFlags = additionalFlags,
                workspace = workspace,
                timeout = timeout,
                generateRequestFn = generateRequest
            )
        )

        /**
         * Creates a new instance of [AIAgentCliStrategy] in structured output mode.
         */
        @JvmName("claudeStructuredGeneric")
        public fun <Input, Output> claude(
            name: String,
            transport: CliTransport,
            apiKey: String? = null,
            structure: Structure<Output, LLMParams.Schema.JSON>,
            permissionMode: ClaudePermissionMode? = null,
            additionalFlags: List<String> = emptyList(),
            workspace: String = ".",
            timeout: Duration? = null,
            generateRequest: (AIAgentCliContext, Input) -> String = { _, input -> input.toString() },
        ): AIAgentCliStrategy<Input, CliAgentStructuredResponse<Output>> = AIAgentCliStrategy(
            name = name,
            config = ClaudeCliStrategyStructuredConfig(
                transport = transport,
                apiKey = apiKey,
                structure = structure,
                permissionMode = permissionMode,
                additionalFlags = additionalFlags,
                workspace = workspace,
                timeout = timeout,
                generateRequestFn = generateRequest
            )
        )

        /**
         * Creates a new instance of [AIAgentCliStrategy] in structured output mode.
         */
        public inline fun <Input, reified Output> claude(
            name: String,
            transport: CliTransport,
            apiKey: String? = null,
            permissionMode: ClaudePermissionMode? = null,
            additionalFlags: List<String> = emptyList(),
            workspace: String = ".",
            timeout: Duration? = null,
            noinline generateRequest: (AIAgentCliContext, Input) -> String = { _, input -> input.toString() },
        ): AIAgentCliStrategy<Input, CliAgentStructuredResponse<Output>> = claude(
            name = name,
            transport = transport,
            apiKey = apiKey,
            structure = JsonStructure.create(serializer = serializer<Output>()),
            permissionMode = permissionMode,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            generateRequest = generateRequest
        )

        // codex constructors

        /**
         * Creates a new instance of [AIAgentCliStrategy] using Codex.
         */
        public fun codex(
            name: String,
            transport: CliTransport,
            apiKey: String? = null,
            sandbox: CodexSandboxMode? = null,
            askForApproval: CodexApprovalPolicy? = null,
            additionalFlags: List<String> = emptyList(),
            workspace: String = ".",
            timeout: Duration? = null,
        ): AIAgentCliStrategy<String, CliAIAgentResponse> = codex(
            name = name,
            transport = transport,
            apiKey = apiKey,
            sandbox = sandbox,
            askForApproval = askForApproval,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            generateRequest = { _, input -> input },
        )

        /**
         * Creates a new instance of [AIAgentCliStrategy] using Codex.
         */
        public fun <Input> codex(
            name: String,
            transport: CliTransport,
            apiKey: String? = null,
            sandbox: CodexSandboxMode? = null,
            askForApproval: CodexApprovalPolicy? = null,
            additionalFlags: List<String> = emptyList(),
            workspace: String = ".",
            timeout: Duration? = null,
            generateRequest: (AIAgentCliContext, Input) -> String,
        ): AIAgentCliStrategy<Input, CliAIAgentResponse> = AIAgentCliStrategy(
            name = name,
            config = CodexCliStrategyConfig(
                transport = transport,
                apiKey = apiKey,
                sandbox = sandbox,
                askForApproval = askForApproval,
                additionalFlags = additionalFlags,
                workspace = workspace,
                timeout = timeout,
                generateRequestFn = generateRequest
            )
        )

        private val logger = KotlinLogging.logger {}

        /**
         * Logs the cli agent events
         */
        private fun logEvent(event: CliEvent) {
            logger.info {
                when (event) {
                    is CliEvent.Stdout -> "[STDOUT] ${event.content}"
                    is CliEvent.Stderr -> "[STDERR] ${event.content}"
                    is CliEvent.Exit -> "Agent Exited (code: ${event.code})"
                    is CliEvent.Failed -> "Agent Failed: ${event.message}"
                }
            }
        }
    }
}
