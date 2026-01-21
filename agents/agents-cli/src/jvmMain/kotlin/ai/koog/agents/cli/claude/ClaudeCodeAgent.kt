package ai.koog.agents.cli.claude

import ai.koog.agents.cli.AgentEvent
import ai.koog.agents.cli.CliAIAgent

/**
 * Claude Code CLI wrapper.
 */
public class ClaudeCodeAgent(
    public val config: ClaudeCodeConfig = ClaudeCodeConfig(),
) : CliAIAgent<String>(config) {

    override val commandOptions: List<String> = buildList {
        add("-p")

        add("--output-format")
        add("stream-json")

        if (config.verbose) add("--verbose")

        if (config.includePartialMessages) add("--include-partial-messages")

        config.model?.let {
            add("--model")
            add(it)
        }
    }

    override fun buildEnvironment(): Map<String, String> {
        return buildMap {
            config.apiKey?.let { put("ANTHROPIC_API_KEY", it) }
        }
    }

    override fun extractResult(events: List<AgentEvent>): String? {
        val jsonEvents = toJsonStdoutEvents(events)

        val resultJson = jsonEvents.lastOrNull { it["type"]?.stringVal == "result" }
        val result = resultJson?.let { it["result"]?.stringVal }

        return result
    }
}
