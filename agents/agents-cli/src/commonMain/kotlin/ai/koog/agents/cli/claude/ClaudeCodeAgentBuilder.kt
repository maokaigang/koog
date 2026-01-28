package ai.koog.agents.cli.claude

import ai.koog.agents.cli.transport.CliTransport
import kotlinx.serialization.KSerializer
import kotlin.time.Duration

/**
 * Builder for [ClaudeCodeAgent].
 */
public open class ClaudeCodeAgentBuilder {
    protected var transport: CliTransport? = null
    protected var apiKey: String? = null
    protected var model: String? = null
    protected var systemPrompt: String? = null
    protected var permissionMode: ClaudePermissionMode? = null
    protected var additionalOptions: List<String> = emptyList()
    protected var workspace: String = "."
    protected var timeout: Duration? = null

    /**
     * Sets the Anthropic API key.
     */
    public fun apiKey(apiKey: String?): ClaudeCodeAgentBuilder =
        apply { this.apiKey = apiKey }

    /**
     * Sets the model to use.
     */
    public fun model(model: String?): ClaudeCodeAgentBuilder =
        apply { this.model = model }

    /**
     * Sets the system prompt to use.
     */
    public fun systemPrompt(systemPrompt: String?): ClaudeCodeAgentBuilder =
        apply { this.systemPrompt = systemPrompt }

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
    public fun workspace(workspace: String): ClaudeCodeAgentBuilder =
        apply { this.workspace = workspace }

    /**
     * Sets the maximum duration to wait for the agent process to complete.
     */
    public fun timeout(timeout: Duration?): ClaudeCodeAgentBuilder =
        apply { this.timeout = timeout }

    /**
     * Sets the transport mechanism to use for executing the agent process.
     */
    public fun transport(transport: CliTransport): ClaudeCodeAgentBuilder =
        apply { this.transport = transport }

    /**
     * Builds a new [ClaudeCodeAgent] instance.
     */
    public fun build(): ClaudeCodeAgent<String> =
        ClaudeCodeAgent.invoke(
            transport = requireNotNull(transport) { "Transport must be set" },
            apiKey = apiKey,
            model = model,
            systemPrompt = systemPrompt,
            permissionMode = permissionMode,
            additionalOptions = additionalOptions,
            workspace = workspace,
            timeout = timeout
        )

    /**
     * Builds a new [ClaudeCodeAgent] instance with structured output given by [serializer].
     */
    public fun <Result> build(serializer: KSerializer<Result>): ClaudeCodeAgent<Result> =
        ClaudeCodeAgent.invoke(
            transport = requireNotNull(transport) { "Transport must be set" },
            serializer = serializer,
            apiKey = apiKey,
            model = model,
            systemPrompt = systemPrompt,
            permissionMode = permissionMode,
            additionalOptions = additionalOptions,
            workspace = workspace,
            timeout = timeout
        )
}
