package ai.koog.agents.cli.claude

import ai.koog.agents.cli.AgentEvent
import ai.koog.agents.cli.CliAIAgent
import ai.koog.agents.cli.transport.CliTransport
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
 *
 * @param apiKey The Anthropic API key.
 * @param model The model to use.
 * @param systemPrompt The system prompt to use.
 * @param permissionMode The permission mode to use.
 * @param workspace The working directory for the agent.
 * @param timeout The maximum duration to wait for the agent process to complete.
 * @param additionalOptions Additional CLI options to pass to the agent.
 * @param transport The transport mechanism to use for executing the agent process.
 */
public class ClaudeCodeAgent(
    apiKey: String? = null,
    model: String? = null,
    systemPrompt: String? = null,
    permissionMode: ClaudePermissionMode? = null,
    additionalOptions: List<String> = emptyList(),
    workspace: File = File("."),
    timeout: Duration? = null,
    transport: CliTransport = CliTransport.Default,
) : CliAIAgent<String>(
    binary = "claude",
    commandOptions = buildList {
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
    },
    env = buildMap { apiKey?.let { put("CLAUDE_API_KEY", it) } },
    transport = transport,
    workspace = workspace,
    timeout = timeout
) {

    override fun extractResult(events: List<AgentEvent>): String? {
        val jsonEvents = toJsonStdoutEvents(events)

        val resultJson = jsonEvents.lastOrNull { it["type"]?.stringVal == "result" }
        val result = resultJson?.let { it["result"]?.stringVal }

        return result
    }

    /**
     * Companion object for static builder api.
     */
    public companion object {
        /**
         * Creates a new [ClaudeCodeAgentBuilder].
         */
        @JvmStatic
        public fun builder(): ClaudeCodeAgentBuilder = ClaudeCodeAgentBuilder()
    }
}
