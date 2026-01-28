package ai.koog.agents.cli.transport

import ai.koog.agents.cli.CliAIAgentEvent
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Interface for cli transport implementations.
 */
public interface CliTransport {
    /**
     * Checks if the required cli binary is available.
     */
    public fun checkAvailability(binary: String): CliAvailability

    /**
     * Executes the cli command and returns a Flow of AgentEvents.
     */
    public fun execute(
        command: List<String>,
        workspace: String,
        env: Map<String, String> = emptyMap(),
        timeout: Duration? = null
    ): Flow<CliAIAgentEvent>
}
