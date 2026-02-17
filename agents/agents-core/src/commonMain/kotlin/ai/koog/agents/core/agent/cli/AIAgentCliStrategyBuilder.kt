package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.context.AIAgentCliContext
import ai.koog.cli.transport.CliTransport
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.Structure
import kotlin.time.Duration

/**
 * Builder for [AIAgentCliStrategy].
 *
 * Example usage:
 * ```
 * // Custom config
 * val strategy = AIAgentCliStrategy(name = "myAgent")
 *     .withConfig(myCustomConfig)
 *
 * // Claude
 * val claudeStrategy = AIAgentCliStrategy(name = "myAgent")
 *     .claude()
 *     .transport(myTransport)
 *     .apiKey("my-key")
 *     .permissionMode(ClaudePermissionMode.AcceptEdits)
 *     .build()
 *
 * // Claude in structured mode
 * val claudeStrategy = AIAgentCliStrategy(name = "myAgent")
 *     .claude()
 *     .transport(myTransport)
 *     .structure(myStructure)
 *
 * // Codex
 * val codexStrategy = AIAgentCliStrategy(name = "myAgent")
 *     .codex()
 *     .transport(myTransport)
 *     .sandbox(CodexSandboxMode.WorkspaceWrite)
 *     .build()
 * ```
 */
public class AIAgentCliStrategyBuilder(private val name: String) {

    /**
     * Configures the strategy to use Claude CLI.
     */
    public fun claude(): ClaudeStrategyBuilder = ClaudeStrategyBuilder(name)

    /**
     * Configures the strategy to use Codex CLI.
     */
    public fun codex(): CodexStrategyBuilder = CodexStrategyBuilder(name)

    /**
     * Builds a strategy with custom configuration.
     */
    public fun <Input, Output> withConfig(
        config: AIAgentCliStrategyConfig<Input, Output>
    ): AIAgentCliStrategy<Input, Output> = AIAgentCliStrategy(name, config)

    /**
     * Low-level builder for Claude CLI strategy configuration.
     * This is the base builder that can be configured to build either a regular or structured strategy.
     */
    public class ClaudeStrategyBuilder internal constructor(private val name: String) {
        private var transport: CliTransport? = null
        private var apiKey: String? = null
        private var permissionMode: ClaudePermissionMode? = null
        private var additionalFlags: List<String> = emptyList()
        private var workspace: String = "."
        private var timeout: Duration? = null
        private var generateRequestFn: (AIAgentCliContext, String) -> String = { _, input -> input }

        /**
         * Sets the CLI transport.
         */
        public fun transport(transport: CliTransport): ClaudeStrategyBuilder = apply {
            this.transport = transport
        }

        /**
         * Sets the API key.
         */
        public fun apiKey(apiKey: String?): ClaudeStrategyBuilder = apply {
            this.apiKey = apiKey
        }

        /**
         * Sets the permission mode.
         */
        public fun permissionMode(mode: ClaudePermissionMode): ClaudeStrategyBuilder = apply {
            this.permissionMode = mode
        }

        /**
         * Sets additional flags.
         */
        public fun additionalFlags(flags: List<String>): ClaudeStrategyBuilder = apply {
            this.additionalFlags = flags
        }

        /**
         * Sets the workspace directory.
         */
        public fun workspace(workspace: String): ClaudeStrategyBuilder = apply {
            this.workspace = workspace
        }

        /**
         * Sets the execution timeout.
         */
        public fun timeout(timeout: Duration): ClaudeStrategyBuilder = apply {
            this.timeout = timeout
        }

        /**
         * Sets the request generation function.
         */
        public fun <Input> generateRequest(
            fn: (AIAgentCliContext, Input) -> String
        ): ClaudeStrategyBuilder = apply {
            @Suppress("UNCHECKED_CAST")
            this.generateRequestFn = fn as (AIAgentCliContext, String) -> String
        }

        /**
         * Configures structured output with the provided structure.
         */
        public fun <Output> structure(
            structure: Structure<Output, LLMParams.Schema.JSON>
        ): ClaudeStructuredStrategyBuilder<String, Output> = ClaudeStructuredStrategyBuilder(
            name = name,
            transport = transport,
            apiKey = apiKey,
            structure = structure,
            permissionMode = permissionMode,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            generateRequestFn = generateRequestFn
        )

        /**
         * Builds the Claude strategy.
         */
        public fun build(): AIAgentCliStrategy<String, CliAIAgentResponse> = AIAgentCliStrategy.claude<String>(
            name = name,
            transport = requireNotNull(transport) { "Transport is required" },
            apiKey = apiKey,
            permissionMode = permissionMode,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            generateRequest = generateRequestFn
        )
    }

    /**
     * Builder for Claude CLI strategy with structured output.
     */
    public class ClaudeStructuredStrategyBuilder<Input, Output> internal constructor(
        private val name: String,
        private var transport: CliTransport?,
        private var apiKey: String?,
        private var structure: Structure<Output, LLMParams.Schema.JSON>,
        private var permissionMode: ClaudePermissionMode?,
        private var additionalFlags: List<String>,
        private var workspace: String,
        private var timeout: Duration?,
        private var generateRequestFn: (AIAgentCliContext, Input) -> String
    ) {
        /**
         * Sets the CLI transport.
         */
        public fun transport(transport: CliTransport): ClaudeStructuredStrategyBuilder<Input, Output> = apply {
            this.transport = transport
        }

        /**
         * Sets the API key.
         */
        public fun apiKey(apiKey: String?): ClaudeStructuredStrategyBuilder<Input, Output> = apply {
            this.apiKey = apiKey
        }

        /**
         * Sets the permission mode.
         */
        public fun permissionMode(mode: ClaudePermissionMode): ClaudeStructuredStrategyBuilder<Input, Output> = apply {
            this.permissionMode = mode
        }

        /**
         * Sets additional flags.
         */
        public fun additionalFlags(flags: List<String>): ClaudeStructuredStrategyBuilder<Input, Output> = apply {
            this.additionalFlags = flags
        }

        /**
         * Sets the workspace directory.
         */
        public fun workspace(workspace: String): ClaudeStructuredStrategyBuilder<Input, Output> = apply {
            this.workspace = workspace
        }

        /**
         * Sets the execution timeout.
         */
        public fun timeout(timeout: Duration): ClaudeStructuredStrategyBuilder<Input, Output> = apply {
            this.timeout = timeout
        }

        /**
         * Sets the request generation function.
         */
        public fun <NewInput> generateRequest(
            fn: (AIAgentCliContext, NewInput) -> String
        ): ClaudeStructuredStrategyBuilder<NewInput, Output> = ClaudeStructuredStrategyBuilder(
            name = name,
            transport = transport,
            apiKey = apiKey,
            structure = structure,
            permissionMode = permissionMode,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            generateRequestFn = fn
        )

        /**
         * Builds the Claude strategy with structured output.
         */
        public fun build(): AIAgentCliStrategy<Input, CliAgentStructuredResponse<Output>> = AIAgentCliStrategy.claude(
            name = name,
            transport = requireNotNull(transport) { "Transport is required" },
            apiKey = apiKey,
            structure = structure,
            permissionMode = permissionMode,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            generateRequest = generateRequestFn
        )
    }

    /**
     * Builder for Codex CLI strategy configuration.
     */
    public class CodexStrategyBuilder internal constructor(private val name: String) {
        private var transport: CliTransport? = null
        private var apiKey: String? = null
        private var sandbox: CodexSandboxMode? = null
        private var askForApproval: CodexApprovalPolicy? = null
        private var additionalFlags: List<String> = emptyList()
        private var workspace: String = "."
        private var timeout: Duration? = null

        /**
         * Sets the CLI transport.
         */
        public fun transport(transport: CliTransport): CodexStrategyBuilder = apply {
            this.transport = transport
        }

        /**
         * Sets the API key.
         */
        public fun apiKey(apiKey: String?): CodexStrategyBuilder = apply {
            this.apiKey = apiKey
        }

        /**
         * Sets the sandbox mode.
         */
        public fun sandbox(sandbox: CodexSandboxMode): CodexStrategyBuilder = apply {
            this.sandbox = sandbox
        }

        /**
         * Sets the approval policy.
         */
        public fun askForApproval(policy: CodexApprovalPolicy): CodexStrategyBuilder = apply {
            this.askForApproval = policy
        }

        /**
         * Sets additional flags.
         */
        public fun additionalFlags(flags: List<String>): CodexStrategyBuilder = apply {
            this.additionalFlags = flags
        }

        /**
         * Sets the workspace directory.
         */
        public fun workspace(workspace: String): CodexStrategyBuilder = apply {
            this.workspace = workspace
        }

        /**
         * Sets the execution timeout.
         */
        public fun timeout(timeout: Duration): CodexStrategyBuilder = apply {
            this.timeout = timeout
        }

        /**
         * Sets the request generation function.
         */
        public fun <Input> generateRequest(
            fn: (AIAgentCliContext, Input) -> String
        ): CodexStrategyBuilderGeneric<Input> = CodexStrategyBuilderGeneric(
            name = name,
            transport = transport,
            apiKey = apiKey,
            sandbox = sandbox,
            askForApproval = askForApproval,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            generateRequestFn = fn
        )

        /**
         * Builds the Codex strategy.
         */
        public fun build(): AIAgentCliStrategy<String, CliAIAgentResponse> = AIAgentCliStrategy.codex(
            name = name,
            transport = requireNotNull(transport) { "Transport is required" },
            apiKey = apiKey,
            sandbox = sandbox,
            askForApproval = askForApproval,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout
        )
    }

    /**
     * Generic builder for Codex CLI strategy with custom input type.
     */
    public class CodexStrategyBuilderGeneric<Input> internal constructor(
        private val name: String,
        private var transport: CliTransport?,
        private var apiKey: String?,
        private var sandbox: CodexSandboxMode?,
        private var askForApproval: CodexApprovalPolicy?,
        private var additionalFlags: List<String>,
        private var workspace: String,
        private var timeout: Duration?,
        private var generateRequestFn: (AIAgentCliContext, Input) -> String
    ) {
        /**
         * Sets the CLI transport.
         */
        public fun transport(transport: CliTransport): CodexStrategyBuilderGeneric<Input> = apply {
            this.transport = transport
        }

        /**
         * Sets the API key.
         */
        public fun apiKey(apiKey: String?): CodexStrategyBuilderGeneric<Input> = apply {
            this.apiKey = apiKey
        }

        /**
         * Sets the sandbox mode.
         */
        public fun sandbox(sandbox: CodexSandboxMode): CodexStrategyBuilderGeneric<Input> = apply {
            this.sandbox = sandbox
        }

        /**
         * Sets the approval policy.
         */
        public fun askForApproval(policy: CodexApprovalPolicy): CodexStrategyBuilderGeneric<Input> = apply {
            this.askForApproval = policy
        }

        /**
         * Sets additional flags.
         */
        public fun additionalFlags(flags: List<String>): CodexStrategyBuilderGeneric<Input> = apply {
            this.additionalFlags = flags
        }

        /**
         * Sets the workspace directory.
         */
        public fun workspace(workspace: String): CodexStrategyBuilderGeneric<Input> = apply {
            this.workspace = workspace
        }

        /**
         * Sets the execution timeout.
         */
        public fun timeout(timeout: Duration): CodexStrategyBuilderGeneric<Input> = apply {
            this.timeout = timeout
        }

        /**
         * Builds the Codex strategy.
         */
        public fun build(): AIAgentCliStrategy<Input, CliAIAgentResponse> = AIAgentCliStrategy.codex(
            name = name,
            transport = requireNotNull(transport) { "Transport is required" },
            apiKey = apiKey,
            sandbox = sandbox,
            askForApproval = askForApproval,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            generateRequest = generateRequestFn
        )
    }
}
