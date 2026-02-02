package ai.koog.agents.cli.codex

import ai.koog.agents.cli.AgentEvent
import ai.koog.agents.cli.CliAIAgent
import ai.koog.agents.cli.CliAIAgentResponse
import ai.koog.agents.cli.CliAgentUsage
import ai.koog.agents.cli.transport.CliTransport
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
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
 * @param additionalFlags Additional CLI options to pass to the agent.
 * @param transport The transport mechanism to use for executing the agent process.
 */
public class CodexAgent(
    transport: CliTransport,
    apiKey: String? = null,
    model: String? = null,
    systemPrompt: String? = null,
    sandbox: CodexSandboxMode? = null,
    askForApproval: CodexApprovalPolicy? = null,
    additionalFlags: List<String> = emptyList(),
    workspace: String = ".",
    timeout: Duration? = null,
) : CliAIAgent(
    binary = "codex",
    commandFlags = buildList {
        add("exec")

        add("--json")

        add("--skip-git-repo-check")

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

        addAll(additionalFlags)
    },
    env = buildMap { apiKey?.let { put("OPENAI_API_KEY", it) } },
    transport = transport,
    workspace = workspace,
    timeout = timeout
) {
    // Codex cli currently supports the structured output only via a file with json
    // It is not possible to pass the schema as a string
    // TODO(): support structured output when codex allows passing schema as a string

    override fun extractResponse(events: List<AgentEvent>): CliAIAgentResponse {
        val jsonEvents = toJsonStdoutEvents(events)

        val errorEvent = jsonEvents.lastOrNull { it["type"]?.stringVal == "turn.failed" }
        val resultIsError = errorEvent != null

        val content = if (resultIsError) {
            errorEvent["error"]?.jsonObject?.get("message")?.stringVal
        } else {
            toJsonStdoutEvents(events)
                .filter { it["type"]?.stringVal == "item.completed" }
                .mapNotNull { it["item"] as? JsonObject }
                .maxByOrNull { it["id"]?.stringVal?.drop(5)?.toInt() ?: -1 }
                ?.get("text")?.stringVal
        }

        val usageObject = jsonEvents
            .lastOrNull { it["type"]?.stringVal == "turn.completed" }
            ?.get("usage")
            ?.jsonObject

        val usage = CliAgentUsage(
            usageObject?.get("input_tokens")?.intVal,
            usageObject?.get("output_tokens")?.intVal,
            buildJsonObject {
                put("cachedInputTokens", usageObject?.get("cached_input_tokens")?.intVal)
            }
        )

        return CliAIAgentResponse(
            content = content ?: "Failed to extract message content",
            isError = resultIsError || content == null,
            usage = usage,
        )
    }
}
