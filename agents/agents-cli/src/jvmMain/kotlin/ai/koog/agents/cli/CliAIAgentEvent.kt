package ai.koog.agents.cli

/**
 * Represents an event of an agent process.
 */
public sealed interface CliAIAgentEvent {
    /**
     * An event indicating that the agent process has started.
     */
    public object Started : CliAIAgentEvent

    /**
     * An event indicating that the agent process has exited.
     */
    public class Exit(public val exitCode: Int) : CliAIAgentEvent

    /**
     * An event indicating that the agent process has failed.
     */
    public class Failed(public val message: String?) : CliAIAgentEvent
}

/**
 * Represents an event of an agent from an agent cli.
 */
public sealed interface AgentEvent : CliAIAgentEvent {
    /**
     * The content of the event.
     */
    public val content: String

    /**
     * An event streamed from stdout.
     */
    public class Stdout(public override val content: String) : AgentEvent

    /**
     * An event streamed from stderr.
     */
    public class Stderr(public override val content: String) : AgentEvent
}
