package ai.koog.cli.transport

/**
 * Events reporting signals from a running cli tool.
 */
public sealed interface CliEvent {

    /**
     * Regular line of text output from the CLI.
     */
    public sealed class Line(public val content: String) : CliEvent

    /**
     * Reports a line from stdout
     */
    public class Stdout(content: String) : Line(content)

    /**
     * Reports a line from stderr
     */
    public class Stderr(content: String) : Line(content)

    /**
     * Reports the exit status of the CLI execution.
     */
    public class Exit(public val code: Int) : CliEvent

    /**
     * Reports a failure during cli execution.
     */
    public class Failed(public val message: String?) : CliEvent
}


