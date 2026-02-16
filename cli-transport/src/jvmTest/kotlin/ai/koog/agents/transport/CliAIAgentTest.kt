package ai.koog.agents.transport

import ai.koog.cli.transport.CliAvailability
import ai.koog.cli.transport.CliAvailable
import ai.koog.cli.transport.CliTransport
import ai.koog.agents.core.agent.cli.CliAIAgentResponse
import ai.koog.cli.transport.CliNotFoundException
import ai.koog.cli.transport.ProcessCliTransport
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.test.Test

class CliAIAgentTest {

    private class TestCliAIAgent(
        binary: String,
        commandFlags: List<String> = emptyList(),
        env: Map<String, String> = emptyMap(),
        transport: CliTransport = AlwaysAvailableNativeTransport,
    ) : CliAIAgent(
        binary,
        transport,
        commandFlags,
        env,
    ) {
        override fun extractResponse(events: List<AgentEvent>): CliAIAgentResponse {
            val result = events.filterIsInstance<AgentEvent.Stdout>().joinToString("\n") { it.content }

            return decode(buildJsonObject {
                put("result", result)
                // Note: Exit code is no longer available in decodeResponse via AgentEvent list
                put("exitCode", 0)
            })
        }

        private fun decode(resultEvent: JsonObject): CliAIAgentResponse {
            val result = resultEvent["result"]?.stringVal ?: ""
            val exitCode = resultEvent["exitCode"]?.jsonPrimitive?.intOrNull ?: -1
            return CliAIAgentResponse(
                content = result,
                isError = exitCode != 0,
                metadata = buildJsonObject { put("exitCode", exitCode) }
            )
        }
    }

    // Transport that always says Available to skip the --version check
    private object AlwaysAvailableNativeTransport : CliTransport by ProcessCliTransport.Default {
        override fun checkAvailability(binary: String): CliAvailability {
            return CliAvailable("test-version")
        }
    }

    @Test
    fun testConnectAvailable() = runTest {
        val agent = TestCliAIAgent("java")
        agent.run("-version") shouldNotBeNull {}
    }

    @Test
    fun testConnectUnavailable() = runTest {
        val agent = TestCliAIAgent(
            "non-existent-binary-12345",
            transport = ProcessCliTransport.Default
        )

        shouldThrow<CliNotFoundException> {
            agent.run("input")
        }
    }

    @Test
    fun testRunExecution() = runTest {
        val agent = TestCliAIAgent(
            "echo",
        )

        val response = agent.run("hello")
        response.content shouldBe "hello"
        response.metadata?.get("exitCode")?.jsonPrimitive?.intOrNull shouldBe 0
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun testRunExecutionWithEnv() = runTest {
        val agent = TestCliAIAgent(
            "sh",
            commandFlags = listOf("-c"),
            env = mapOf("KEY" to "VALUE"),
        )

        val response = agent.run("echo \$KEY")
        response.content shouldBe "VALUE"
        response.metadata?.get("exitCode")?.jsonPrimitive?.intOrNull shouldBe 0
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun testRunExecutionStderr() = runTest {
        val agent = TestCliAIAgent(
            "sh",
            commandFlags = listOf("-c"),
        )

        val response = agent.run("echo 'error message' >&2")
        response.content shouldBe ""
        response.metadata?.get("exitCode")?.jsonPrimitive?.intOrNull shouldBe 0
    }
}
