package ai.koog.cli.transport

import ai.koog.test.utils.DockerImageResolver
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertTrue

class ProcessCliTransportTest {
    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    companion object {
        private val imageName by lazy { DockerImageResolver.resolveAndEnsureCliImage() }

        @JvmStatic
        fun transportProvider(): Stream<CliTransport> = Stream.builder<CliTransport>()
            .add(ProcessCliTransport.Default)
            .apply {
                // mac runners on CI do not have docker
                // TODO(): need to refactor, so that the test runs locally on mac
                if (!System.getProperty("os.name").lowercase().contains("mac")) {
                    add(ProcessCliTransport.dockerTransport(imageName))
                }
            }
            .build()
    }

    @Test
    fun testCheckAvailability() {
        val availability = ProcessCliTransport.Default.checkAvailability("java")

        availability.shouldBeInstanceOf<CliAvailable>()
        availability.version
            .shouldNotBeNull()
            .shouldNotBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("transportProvider")
    fun testExecuteEcho(transport: CliTransport) = runTest {
        val echoCommand = if (isWindows) {
            listOf("cmd", "/c", "echo", "hello world")
        } else {
            listOf("echo", "hello world")
        }

        val events = transport.execute(
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

    @ParameterizedTest
    @MethodSource("transportProvider")
    fun testExecuteInvalidCommand(transport: CliTransport) = runTest {
        val invalidCommand = if (isWindows && transport is DockerCliTransport) {
            listOf("cmd", "/c", "non-existent-command-12345")
        } else {
            listOf("non-existent-command-12345")
        }

        assertThrows<Exception> {
            val events = transport.execute(
                command = invalidCommand,
                workspace = "."
            ).toList()

            val exitEvent = events.filterIsInstance<CliEvent.Exit>().firstOrNull()
            if (exitEvent?.code == 127 || (isWindows && exitEvent?.code == 1)) {
                throw Exception("Command not found")
            }
        }
    }

    @ParameterizedTest
    @MethodSource("transportProvider")
    fun testExecuteWithEnv(transport: CliTransport) = runTest {
        val env = mapOf("TEST_VAR" to "test-value")

        val command = if (isWindows) {
            listOf("cmd", "/c", "echo %TEST_VAR%")
        } else {
            listOf("sh", "-c", "echo \$TEST_VAR")
        }

        val events = transport.execute(
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

    @ParameterizedTest
    @MethodSource("transportProvider")
    fun testExecuteStderr(transport: CliTransport) = runTest {
        val command = if (isWindows) {
            listOf("cmd", "/c", "echo error message 1>&2")
        } else {
            listOf("sh", "-c", "echo 'error message' >&2")
        }

        val events = transport.execute(
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

    @ParameterizedTest
    @MethodSource("transportProvider")
    fun testExecuteWithWorkspacePathUnix(transport: CliTransport) = runTest {
        val tmpDir = createTempDirectory("koog-test")
        try {
            val events = transport.execute(
                command = listOf("pwd"),
                workspace = tmpDir.toAbsolutePath().toString()
            ).toList()

            val actualPath = events.filterIsInstance<CliEvent.Stdout>()
                .firstOrNull()
                .shouldNotBeNull()
                .content
                .trim()

            val expectedPath = when (transport) {
                is DockerCliTransport -> "/workspace"
                is ProcessCliTransport.Default -> tmpDir.toRealPath().toString()
                else -> error("Unknown transport type")
            }

            actualPath shouldBe expectedPath
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun testExecuteWithWorkspacePathWindows() = runTest {
        val tmpDir = createTempDirectory("koog-test")
        try {
            val events = ProcessCliTransport.Default.execute(
                command = listOf("cmd", "/c", "cd"),
                workspace = tmpDir.toAbsolutePath().toString()
            ).toList()

            val actualPath = events.filterIsInstance<CliEvent.Stdout>()
                .firstOrNull()
                .shouldNotBeNull()
                .content
                .trim()
                .let(Path::of)
                .toRealPath()
                .toString()

            val expectedPath = tmpDir.toRealPath().toString()

            actualPath shouldBe expectedPath
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }
}
