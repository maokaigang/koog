package ai.koog.agents.cli

import ai.koog.agents.cli.transport.CliTransport
import java.io.File
import kotlin.time.Duration

/**
 * Base configuration for agent processes.
 */
public abstract class CliAIAgentConfig(
    public val binary: String,
    public val transport: CliTransport,
    public val workspace: File,
    public val timeout: Duration?
)
