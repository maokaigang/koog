package ai.koog.agents.cli.codex

import ai.koog.agents.cli.transport.CliTransport
import java.io.File
import kotlin.time.Duration

/**
 * Builder for [CodexAgent].
 */
public class CodexAgentBuilder {
    private var apiKey: String? = null
    private var model: String? = null
    private var systemPrompt: String? = null
    private var sandbox: CodexSandboxMode? = null
    private var askForApproval: CodexApprovalPolicy? = null
    private var additionalOptions: List<String> = emptyList()
    private var workspace: File = File(".")
    private var timeout: Duration? = null
    private var transport: CliTransport = CliTransport.Default

    /**
     * Sets the OpenAI API key.
     */
    public fun apiKey(apiKey: String?): CodexAgentBuilder = apply { this.apiKey = apiKey }

    /**
     * Sets the model to use.
     */
    public fun model(model: String?): CodexAgentBuilder = apply { this.model = model }

    /**
     * Sets the system prompt to use.
     */
    public fun systemPrompt(systemPrompt: String?): CodexAgentBuilder = apply { this.systemPrompt = systemPrompt }

    /**
     * Sets the sandbox mode to use.
     */
    public fun sandbox(sandbox: CodexSandboxMode?): CodexAgentBuilder = apply { this.sandbox = sandbox }

    /**
     * Sets the approval policy to use.
     */
    public fun askForApproval(askForApproval: CodexApprovalPolicy?): CodexAgentBuilder =
        apply { this.askForApproval = askForApproval }

    /**
     * Sets additional CLI options to pass to the agent.
     */
    public fun additionalOptions(additionalOptions: List<String>): CodexAgentBuilder =
        apply { this.additionalOptions = additionalOptions }

    /**
     * Sets the working directory for the agent.
     */
    public fun workspace(workspace: File): CodexAgentBuilder = apply { this.workspace = workspace }

    /**
     * Sets the maximum duration to wait for the agent process to complete.
     */
    public fun timeout(timeout: Duration?): CodexAgentBuilder = apply { this.timeout = timeout }

    /**
     * Sets the transport mechanism to use for executing the agent process.
     */
    public fun transport(transport: CliTransport): CodexAgentBuilder = apply { this.transport = transport }

    /**
     * Builds a new [CodexAgent] instance.
     */
    public fun build(): CodexAgent = CodexAgent(
        apiKey = apiKey,
        model = model,
        systemPrompt = systemPrompt,
        sandbox = sandbox,
        askForApproval = askForApproval,
        additionalOptions = additionalOptions,
        workspace = workspace,
        timeout = timeout,
        transport = transport
    )
}
