package ai.koog.agents.cli.codex

import ai.koog.agents.cli.AgentEvent
import ai.koog.agents.cli.CliAIAgent
import kotlinx.serialization.json.jsonObject

/**
 * OpenAI Codex CLI wrapper.
 */
public class CodexAgent(
    public val config: CodexConfig = CodexConfig(),
) : CliAIAgent<String>(config) {

    override val commandOptions: List<String> = buildList {
        add("exec")

        add("--json")

        if (config.skipGitRepoCheck) add("--skip-git-repo-check")

        add("--sandbox")
        add(config.sandboxMode.value)

        config.model?.let {
            add("--model")
            add(it)
        }

        add("-o")
        add("/dev/stdout")
    }

    override fun buildEnvironment(): Map<String, String> {
        return buildMap {
            config.apiKey?.let { put("CODEX_API_KEY", it) }
        }
    }

    override fun extractResult(events: List<AgentEvent>): String? {
        val jsonEvents = toJsonStdoutEvents(events)

        val error = jsonEvents
            .lastOrNull { it["type"]?.stringVal == "turn.failed" }
            ?.let { it["error"]?.jsonObject?.get("message")?.stringVal }
        val result = error ?: events.filterIsInstance<AgentEvent.Stdout>().lastOrNull()?.content

        return result
    }
}
