package ai.koog.agents.cli

import kotlin.time.Duration

/**
 * Base class for cli-agent-related exceptions.
 */
public open class CliAgentException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Exception indicating that a CLI agent binary was not found.
 */
public class CliNotFoundException(message: String, cause: Throwable? = null) : CliAgentException(message, cause)

/**
 * Exception indicating that a CLI agent run timed out.
 */
public class CliAgentTimeoutException(message: String, public val timeout: Duration, cause: Throwable? = null) :
    CliAgentException(message, cause)
