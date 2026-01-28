package ai.koog.agents.cli.codex

import ai.koog.agents.cli.AgentEvent
import ai.koog.agents.cli.CliAIAgent
import ai.koog.agents.cli.transport.CliTransport
import kotlinx.serialization.json.jsonObject
import kotlin.jvm.JvmStatic
import kotlin.time.Duration

/**
 * Codex sandbox mode.
 */
public enum class CodexSandboxMode(public val value: String) {
    /**
     * Read-only access to the filesystem.
     */
    ReadOnly("read-only"),

    /**
     * Write access to the workspace directory.
     */
    WorkspaceWrite("workspace-write"),

    /**
     * Full access to the system.
     */
    DangerFullAccess("danger-full-access")
}

/**
 * Codex approval policy.
 */
public enum class CodexApprovalPolicy(public val value: String) {
    /**
     * Ask for approval for all untrusted commands.
     */
    Untrusted("untrusted"),

    /**
     * Ask for approval only if a command fails.
     */
    OnFailure("on-failure"),

    /**
     * The model decides when to ask for approval.
     */
    OnRequest("on-request"),

    /**
     * Never ask for approval.
     */
    Never("never")
}

/**
 * OpenAI Codex CLI wrapper.
 *
 * @param apiKey The OpenAI API key.
 * @param model The model to use.
 * @param systemPrompt The system prompt to use.
 * @param sandbox The sandbox mode to use.
 * @param askForApproval The approval policy to use.
 * @param workspace The working directory for the agent.
 * @param timeout The maximum duration to wait for the agent process to complete.
 * @param additionalOptions Additional CLI options to pass to the agent.
 * @param transport The transport mechanism to use for executing the agent process.
 */
public class CodexAgent(
    transport: CliTransport,
    apiKey: String? = null,
    model: String? = null,
    systemPrompt: String? = null,
    sandbox: CodexSandboxMode? = null,
    askForApproval: CodexApprovalPolicy? = null,
    additionalOptions: List<String> = emptyList(),
    workspace: String = ".",
    timeout: Duration? = null,
) : CliAIAgent<String>(
    binary = "codex",
    commandOptions = buildList {
        add("exec")

        add("--json")

        add("--skip-git-repo-check")

        add("-o")
        add("/dev/stdout")

        model?.let {
            add("--model")
            add(it)
        }

        systemPrompt?.let {
            add("-c")
            add("system_prompt=\"$it\"")
        }

        sandbox?.let {
            add("--sandbox")
            add(it.value)
        }

        askForApproval?.let {
            add("--ask-for-approval")
            add(it.value)
        }

        addAll(additionalOptions)
    },
    env = buildMap { apiKey?.let { put("OPENAI_API_KEY", it) } },
    transport = transport,
    workspace = workspace,
    timeout = timeout
) {

    override fun extractResult(events: List<AgentEvent>): String? {
        val jsonEvents = toJsonStdoutEvents(events)

        val error = jsonEvents
            .lastOrNull { it["type"]?.stringVal == "turn.failed" }
            ?.let { it["error"]?.jsonObject?.get("message")?.stringVal }
        val result = error ?: events.filterIsInstance<AgentEvent.Stdout>().lastOrNull()?.content

        return result
    }

    /**
     * Companion object for static builder api.
     */
    public companion object {
        /**
         * Creates a new [CodexAgentBuilder].
         */
        @JvmStatic
        public fun builder(): CodexAgentBuilder = CodexAgentBuilder()
    }
}
