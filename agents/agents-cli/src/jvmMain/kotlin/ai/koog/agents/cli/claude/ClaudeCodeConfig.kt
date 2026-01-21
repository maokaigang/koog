package ai.koog.agents.cli.claude

import ai.koog.agents.cli.CliAIAgentConfig
import ai.koog.agents.cli.transport.CliTransport
import java.io.File
import kotlin.time.Duration

/**
 * Claude Code config.
 */
public class ClaudeCodeConfig(
    binary: String = "claude",
    transport: CliTransport = CliTransport.Default,
    workspace: File = File("."),
    timeout: Duration? = null,
    public val model: String? = null,
    public val verbose: Boolean = true,
    public val includePartialMessages: Boolean = false,
    public val apiKey: String? = null,
) : CliAIAgentConfig(binary, transport, workspace, timeout)
