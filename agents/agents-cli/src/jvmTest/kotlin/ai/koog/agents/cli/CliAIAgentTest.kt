package ai.koog.agents.cli

import ai.koog.agents.cli.transport.CliAvailability
import ai.koog.agents.cli.transport.CliAvailable
import ai.koog.agents.cli.transport.CliTransport
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration

class CliAIAgentTest {

    private class TestAgentConfig(
        binary: String,
        transport: CliTransport = CliTransport.Default,
        workspace: File = File("."),
        timeout: Duration? = null
    ) : CliAIAgentConfig(binary, transport, workspace, timeout)

    private class TestCliAIAgent(
        config: CliAIAgentConfig,
        private val env: Map<String, String> = emptyMap(),
        override val commandOptions: List<String> = emptyList()
    ) : CliAIAgent<String>(config) {
        override fun buildEnvironment(): Map<String, String> = env
        override fun extractResult(events: List<AgentEvent>): String {
            return events.filterIsInstance<AgentEvent.Stdout>().joinToString("\n") { it.content }
        }
    }

    // A transport that always says Available to skip the --version check
    private object AlwaysAvailableNativeTransport : CliTransport by CliTransport.Default {
        override fun checkAvailability(binary: String): CliAvailability {
            return CliAvailable("test-version")
        }
    }

    @Test
    fun testConnectAvailable() = runTest {
        val config = TestAgentConfig("java")
        val agent = TestCliAIAgent(config)
        agent.run("-version")
    }

    @Test
    fun testConnectUnavailable() = runTest {
        val config = TestAgentConfig("non-existent-binary-12345")
        val agent = TestCliAIAgent(config)

        shouldThrow<CliNotFoundException> {
            agent.run("input")
        }
    }

    @Test
    fun testRunExecution() = runTest {
        val config = TestAgentConfig("echo", AlwaysAvailableNativeTransport)
        val agent = TestCliAIAgent(config)

        val result = agent.run("hello")
        result shouldBe "hello"
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun testRunExecutionWithEnv() = runTest {
        val agentEnv = mapOf("KEY" to "VALUE")
        val config = TestAgentConfig("sh", AlwaysAvailableNativeTransport)
        val agent = TestCliAIAgent(config, env = agentEnv, commandOptions = listOf("-c"))

        val result = agent.run("echo \$KEY")
        result shouldBe "VALUE"
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun testRunExecutionStderr() = runTest {
        val config = TestAgentConfig("sh", AlwaysAvailableNativeTransport)
        val agent = TestCliAIAgent(config, commandOptions = listOf("-c"))

        val result = agent.run("echo 'error message' >&2")
        result shouldBe ""
    }
}
