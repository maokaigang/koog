package ai.koog.agents.cli.transport

import ai.koog.agents.cli.AgentEvent
import ai.koog.agents.cli.CliAIAgentEvent
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File
import kotlin.test.Test

class NativeCliTransportTest {

    private val workspace = File(".")

    @Test
    fun testCheckAvailability() {
        val availability = CliTransport.Default.checkAvailability("java")

        availability.shouldBeInstanceOf<CliAvailable>()
        availability.version
            .shouldNotBeNull()
            .shouldNotBeEmpty()
    }

    @Test
    fun testExecuteEcho() = runTest {
        val events = CliTransport.Default.execute(
            command = listOf("echo", "hello world"),
            workspace = workspace
        ).toList()

        events[0].shouldBeInstanceOf<CliAIAgentEvent.Started>()

        events[1]
            .shouldBeInstanceOf<AgentEvent.Stdout>()
            .content.shouldBe("hello world")

        events[2]
            .shouldBeInstanceOf<CliAIAgentEvent.Exit>()
            .exitCode.shouldBe(0)
    }

    @Test
    fun testExecuteInvalidCommand() = runTest {
        assertThrows<Exception> {
            CliTransport.Default.execute(
                command = listOf("non-existent-command-12345"),
                workspace = workspace
            ).toList()
        }
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun testExecuteWithEnv() = runTest {
        val env = mapOf("TEST_VAR" to "test-value")

        val events = CliTransport.Default.execute(
            command = listOf("sh", "-c", "echo \$TEST_VAR"),
            workspace = workspace,
            env = env
        ).toList()

        events
            .filterIsInstance<AgentEvent.Stdout>()
            .firstOrNull()
            .shouldNotBeNull()
            .content.shouldBe("test-value")
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun testExecuteStderr() = runTest {
        val workspace = File(".").absoluteFile

        // Redirect stdout to stderr
        val events = CliTransport.Default.execute(
            command = listOf("sh", "-c", "echo 'error message' >&2"),
            workspace = workspace
        ).toList()

        events
            .filterIsInstance<AgentEvent.Stderr>()
            .firstOrNull()
            .shouldNotBeNull()
            .content.shouldBe("error message")
    }
}
