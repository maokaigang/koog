package ai.koog.cli.transport

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertTrue

class ProcessCliTransportTest {
    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    @Test
    fun testCheckAvailability() {
        val availability = ProcessCliTransport.Default.checkAvailability("java")

        availability.shouldBeInstanceOf<CliAvailable>()
        availability.version
            .shouldNotBeNull()
            .shouldNotBeEmpty()
    }

    @Test
    fun testExecuteEcho() = runTest {
        val echoCommand = if (isWindows) {
            listOf("cmd", "/c", "echo hello world")
        } else {
            listOf("echo", "hello world")
        }

        val events = ProcessCliTransport.Default.execute(
            command = echoCommand,
            workspace = "."
        ).toList()

        events.filterIsInstance<CliEvent.Stdout>()
            .firstOrNull()
            .shouldNotBeNull()
            .content
            .trim()
            .trim('"')
            .shouldBe("hello world")

        events.last()
            .shouldBeInstanceOf<CliEvent.Exit>()
            .code.shouldBe(0)
    }

    @Test
    fun testExecuteInvalidCommand() = runTest {
        val invalidCommand = if (isWindows) {
            listOf("cmd", "/c", "non-existent-command-12345")
        } else {
            listOf("non-existent-command-12345")
        }

        assertThrows<Exception> {
            val events = ProcessCliTransport.Default.execute(
                command = invalidCommand,
                workspace = "."
            ).toList()

            val exitEvent = events.filterIsInstance<CliEvent.Exit>().firstOrNull()
            if (exitEvent?.code == 127 || (isWindows && exitEvent?.code == 1)) {
                throw Exception("Command not found")
            }
        }
    }

    @Test
    fun testExecuteWithEnv() = runTest {
        val env = mapOf("TEST_VAR" to "test-value")

        val command = if (isWindows) {
            listOf("cmd", "/c", "echo %TEST_VAR%")
        } else {
            listOf("sh", "-c", "echo \$TEST_VAR")
        }

        val events = ProcessCliTransport.Default.execute(
            command = command,
            workspace = ".",
            env = env
        ).toList()

        events
            .filterIsInstance<CliEvent.Stdout>()
            .firstOrNull()
            .shouldNotBeNull()
            .content.shouldBe("test-value")
    }

    @Test
    fun testExecuteStderr() = runTest {
        val command = if (isWindows) {
            listOf("cmd", "/c", "echo error message 1>&2")
        } else {
            listOf("sh", "-c", "echo 'error message' >&2")
        }

        val events = ProcessCliTransport.Default.execute(
            command = command,
            workspace = "."
        ).toList()

        assertTrue(
            events
                .filterIsInstance<CliEvent.Stderr>()
                .any { it.content.contains("error message") },
            "error message should be captured from the stderr"
        )
    }
}
