package ai.koog.agents.cli

import ai.koog.agents.cli.transport.CliAvailability
import ai.koog.agents.cli.transport.CliAvailable
import ai.koog.agents.cli.transport.CliTransport
import ai.koog.agents.cli.transport.ProcessCliTransport
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.test.Test

class CliAIAgentTest {

    private class TestCliAIAgent(
        binary: String,
        commandOptions: List<String> = emptyList(),
        env: Map<String, String> = emptyMap(),
        transport: CliTransport = AlwaysAvailableNativeTransport,
    ) : CliAIAgent<String>(
        binary,
        transport,
        commandOptions,
        env,
    ) {
        override fun extractResult(events: List<AgentEvent>): String {
            return events.filterIsInstance<AgentEvent.Stdout>().joinToString("\n") { it.content }
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

        val result = agent.run("hello")
        result shouldBe "hello"
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun testRunExecutionWithEnv() = runTest {
        val agent = TestCliAIAgent(
            "sh",
            commandOptions = listOf("-c"),
            env = mapOf("KEY" to "VALUE"),
        )

        val result = agent.run("echo \$KEY")
        result shouldBe "VALUE"
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun testRunExecutionStderr() = runTest {
        val agent = TestCliAIAgent(
            "sh",
            commandOptions = listOf("-c"),
        )

        val result = agent.run("echo 'error message' >&2")
        result shouldBe ""
    }
}
