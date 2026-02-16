package ai.koog.agents.core.agent.cli

import ai.koog.cli.transport.CliEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Contains helper functions for working with JSONs
 */
public object JsonUtils {

    /**
     * Json configuration for cli agent implementations
     */
    public val json: Json = Json { ignoreUnknownKeys = true }

    /**
     * Converts a list of agent events to a list of JSON objects from stdout
     */
    public fun toJsonStdoutEvents(events: List<CliEvent.Line>): List<JsonObject> =
        events
            .mapNotNull {
                runCatching {
                    json.decodeFromString<JsonObject>(it.content).jsonObject
                }.getOrNull()
            }

    /**
     * Converts a JSON primitive to a string
     */
    public val JsonElement.stringVal: String?
        get() = (this as? JsonPrimitive)?.contentOrNull

    /**
     * Converts a JSON primitive to an integer
     */
    public val JsonElement.intVal: Int?
        get() = (this as? JsonPrimitive)?.intOrNull

    /**
     * Converted a JSON primitive to a double
     */
    public val JsonElement.doubleVal: Double?
        get() = (this as? JsonPrimitive)?.doubleOrNull

    /**
     * Converts a JSON primitive to a boolean
     */
    public val JsonElement.boolVal: Boolean?
        get() = (this as? JsonPrimitive)?.booleanOrNull
}
