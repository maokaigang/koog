package ai.koog.cli.transport

import kotlin.time.Duration

/**
 * Base class for cli-agent-related exceptions.
 */
public open class CliException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Exception indicating that a CLI agent binary was not found.
 */
public class CliNotFoundException(message: String, cause: Throwable? = null) : CliException(message, cause)

/**
 * Exception indicating that a CLI agent run timed out.
 */
public class CliTimeoutException(message: String, public val timeout: Duration, cause: Throwable? = null) :
    CliException(message, cause)
