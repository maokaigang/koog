package ai.koog.agents.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents the usage information from a CLI agent.
 *
 * @param inputTokens The number of tokens used in the input.
 * @param outputTokens The number of tokens generated in the output.
 * @param additionalInfo Additional information about the agent token usage.
 */
@Serializable
public data class CliAgentUsage(
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val additionalInfo: JsonObject? = null
)

/**
 * Represents the response from a CLI agent.
 *
 * @param content The full content (e.g., stdout) of the agent execution.
 * @param usage Usage information about the agent execution.
 * @param metadata Additional meta information about the agent execution.
 */
@Serializable
public data class CliAIAgentResponse(
    val content: String,
    val isError: Boolean,
    val usage: CliAgentUsage = CliAgentUsage(),
    val metadata: JsonObject? = null
)

/**
 * Represents a container for structured data from a CLI agent.
 *
 * @param T The type of the structured data.
 * @property result The parsed structured data.
 * @property response The original response from which the result was parsed.
 */
@Serializable
public data class CliAgentStructuredResponse<out T>(
    val result: T,
    val response: CliAIAgentResponse
)
