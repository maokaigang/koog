package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.cli.JsonUtils.intVal
import ai.koog.agents.core.agent.cli.JsonUtils.stringVal
import ai.koog.agents.core.agent.cli.JsonUtils.toJsonStdoutEvents
import ai.koog.agents.core.agent.context.AIAgentCliContext
import ai.koog.cli.transport.CliEvent
import ai.koog.cli.transport.CliTransport
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
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
 * Configuration for Codex CLI agent.
 */
public class CodexCliStrategyConfig<Input>(
    override val transport: CliTransport,
    public val apiKey: String? = null,
    public val sandbox: CodexSandboxMode? = null,
    public val askForApproval: CodexApprovalPolicy? = null,
    public val additionalFlags: List<String> = emptyList(),
    override val workspace: String = ".",
    override val timeout: Duration? = null,
    private val generateRequest: GenerateRequest<Input>
) : AIAgentCliStrategyConfig<Input, CliAIAgentResponse> {
    override val binary: String = "codex"

    override val env: Map<String, String> = buildMap {
        apiKey?.let { put("CODEX_API_KEY", it) }
    }

    override fun flags(model: LLModel, systemMessages: List<Message.System>): List<String> =
        buildList {
            add("exec")
            add("--json")
            add("--skip-git-repo-check")

            if (model.provider == LLMProvider.OpenAI) {
                add("--model")
                add(model.id)
            }

            // note: codex does not support system messages, so we are not using them here

            sandbox?.let {
                add("--sandbox")
                add(it.value)
            }

            askForApproval?.let {
                add("--ask-for-approval")
                add(it.value)
            }

            addAll(additionalFlags)
        }

    override fun generateRequest(context: AIAgentCliContext, input: Input): String =
        generateRequest.generateRequest(context, input)

    override fun extractOutput(events: List<CliEvent.Line>): CliAIAgentResponse {
        val jsonEvents = toJsonStdoutEvents(events)

        val errorEvent = jsonEvents.lastOrNull { it["type"]?.stringVal == "turn.failed" }
        val resultIsError = errorEvent != null

        val content = if (resultIsError) {
            errorEvent["error"]?.jsonObject?.get("message")?.stringVal
        } else {
            jsonEvents
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
            inputTokens = usageObject?.get("input_tokens")?.intVal,
            outputTokens = usageObject?.get("output_tokens")?.intVal,
            additionalInfo = buildJsonObject {
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
