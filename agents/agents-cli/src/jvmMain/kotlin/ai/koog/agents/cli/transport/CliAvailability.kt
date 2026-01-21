package ai.koog.agents.cli.transport

/**
 * Represents the availability of a Cli tool.
 */
public sealed interface CliAvailability

/**
 * Indicates that the tool is available.
 */
public class CliAvailable(public val version: String?) : CliAvailability

/**
 * Indicates that the tool is unavailable.
 */
public class CliUnavailable(
    public val reason: String,
    public val cause: Throwable? = null,
) : CliAvailability
