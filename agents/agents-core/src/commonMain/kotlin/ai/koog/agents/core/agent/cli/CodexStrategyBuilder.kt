package ai.koog.agents.core.agent.cli

import ai.koog.cli.transport.CliTransport
import kotlin.time.Duration

/**
 * Base class for Codex strategy builders.
 */
public abstract class CodexStrategyBuilderBase<Self : CodexStrategyBuilderBase<Self>> internal constructor(
    internal val name: String,
    internal var transport: CliTransport? = null,
    internal var apiKey: String? = null,
    internal var sandbox: CodexSandboxMode? = null,
    internal var askForApproval: CodexApprovalPolicy? = null,
    internal var additionalFlags: List<String> = emptyList(),
    internal var workspace: String = ".",
    internal var timeout: Duration? = null
) {

    internal abstract fun self(): Self

    /**
     * Sets the CLI transport.
     */
    public fun transport(transport: CliTransport): Self = self().apply {
        this.transport = transport
    }

    /**
     * Sets the API key.
     */
    public fun apiKey(apiKey: String?): Self = self().apply {
        this.apiKey = apiKey
    }

    /**
     * Sets the sandbox mode.
     */
    public fun sandbox(sandbox: CodexSandboxMode): Self = self().apply {
        this.sandbox = sandbox
    }

    /**
     * Sets the approval policy.
     */
    public fun askForApproval(policy: CodexApprovalPolicy): Self = self().apply {
        this.askForApproval = policy
    }

    /**
     * Sets additional flags.
     */
    public fun additionalFlags(flags: List<String>): Self = self().apply {
        this.additionalFlags = flags
    }

    /**
     * Sets the workspace directory.
     */
    public fun workspace(workspace: String): Self = self().apply {
        this.workspace = workspace
    }

    /**
     * Sets the execution timeout.
     */
    public fun timeout(timeout: Duration): Self = self().apply {
        this.timeout = timeout
    }
}

/**
 * Builder for Codex CLI strategy.
 */
public class CodexStrategyBuilder internal constructor(
    name: String,
) : CodexStrategyBuilderBase<CodexStrategyBuilder>(name) {
    override fun self(): CodexStrategyBuilder = this

    /**
     * Sets the request generation function.
     */
    public fun <Input> generateRequest(
        generateRequest: GenerateRequest<Input>
    ): CodexStrategyGenericInputBuilder<Input> = CodexStrategyGenericInputBuilder(
        name = name,
        transport = transport,
        apiKey = apiKey,
        sandbox = sandbox,
        askForApproval = askForApproval,
        additionalFlags = additionalFlags,
        workspace = workspace,
        timeout = timeout,
        generateRequest = generateRequest
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
public class CodexStrategyGenericInputBuilder<Input> internal constructor(
    name: String,
    transport: CliTransport?,
    apiKey: String?,
    sandbox: CodexSandboxMode?,
    askForApproval: CodexApprovalPolicy?,
    additionalFlags: List<String>,
    workspace: String,
    timeout: Duration?,
    private val generateRequest: GenerateRequest<Input>,
) : CodexStrategyBuilderBase<CodexStrategyGenericInputBuilder<Input>>(
    name = name,
    transport = transport,
    apiKey = apiKey,
    sandbox = sandbox,
    askForApproval = askForApproval,
    additionalFlags = additionalFlags,
    workspace = workspace,
    timeout = timeout,
) {
    override fun self(): CodexStrategyGenericInputBuilder<Input> = this

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
        generateRequest = generateRequest
    )
}
