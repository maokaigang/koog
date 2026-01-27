package ai.koog.agents.cli.claude

import ai.koog.agents.cli.transport.CliTransport
import java.io.File
import kotlin.time.Duration

/**
 * Builder for [ClaudeCodeAgent].
 */
public class ClaudeCodeAgentBuilder {
    private var apiKey: String? = null
    private var model: String? = null
    private var systemPrompt: String? = null
    private var permissionMode: ClaudePermissionMode? = null
    private var additionalOptions: List<String> = emptyList()
    private var workspace: File = File(".")
    private var timeout: Duration? = null
    private var transport: CliTransport = CliTransport.Default

    /**
     * Sets the Anthropic API key.
     */
    public fun apiKey(apiKey: String?): ClaudeCodeAgentBuilder = apply { this.apiKey = apiKey }

    /**
     * Sets the model to use.
     */
    public fun model(model: String?): ClaudeCodeAgentBuilder = apply { this.model = model }

    /**
     * Sets the system prompt to use.
     */
    public fun systemPrompt(systemPrompt: String?): ClaudeCodeAgentBuilder = apply { this.systemPrompt = systemPrompt }

    /**
     * Sets the permission mode to use.
     */
    public fun permissionMode(permissionMode: ClaudePermissionMode?): ClaudeCodeAgentBuilder =
        apply { this.permissionMode = permissionMode }

    /**
     * Sets additional CLI options to pass to the agent.
     */
    public fun additionalOptions(additionalOptions: List<String>): ClaudeCodeAgentBuilder =
        apply { this.additionalOptions = additionalOptions }

    /**
     * Sets the working directory for the agent.
     */
    public fun workspace(workspace: File): ClaudeCodeAgentBuilder = apply { this.workspace = workspace }

    /**
     * Sets the maximum duration to wait for the agent process to complete.
     */
    public fun timeout(timeout: Duration?): ClaudeCodeAgentBuilder = apply { this.timeout = timeout }

    /**
     * Sets the transport mechanism to use for executing the agent process.
     */
    public fun transport(transport: CliTransport): ClaudeCodeAgentBuilder = apply { this.transport = transport }

    /**
     * Builds a new [ClaudeCodeAgent] instance.
     */
    public fun build(): ClaudeCodeAgent = ClaudeCodeAgent(
        apiKey = apiKey,
        model = model,
        systemPrompt = systemPrompt,
        permissionMode = permissionMode,
        additionalOptions = additionalOptions,
        workspace = workspace,
        timeout = timeout,
        transport = transport
    )
}
