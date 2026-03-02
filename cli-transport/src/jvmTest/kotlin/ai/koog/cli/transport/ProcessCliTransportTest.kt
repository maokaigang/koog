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

class ProcessCliTransportTest {

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
        val events = transport.execute(
            command = listOf("echo", "hello world"),
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
        assertThrows<Exception> {
            val events = transport.execute(
                command = listOf("non-existent-command-12345"),
                workspace = "."
            ).toList()

            if (events.filterIsInstance<CliEvent.Exit>().first().code == 127) {
                throw Exception("Command not found")
            }
        }
    }

    @ParameterizedTest
    @MethodSource("transportProvider")
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun testExecuteWithEnv(transport: CliTransport) = runTest {
        val env = mapOf("TEST_VAR" to "test-value")

        val events = transport.execute(
            command = listOf("sh", "-c", "echo \$TEST_VAR"),
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
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun testExecuteStderr(transport: CliTransport) = runTest {
        // Redirect stdout to stderr
        val events = transport.execute(
            command = listOf("sh", "-c", "echo 'error message' >&2"),
            workspace = "."
        ).toList()

        events
            .filterIsInstance<CliEvent.Stderr>()
            .firstOrNull()
            .shouldNotBeNull()
            .content.shouldBe("error message")
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun testExecuteEchoWindows() = runTest {
        val events = ProcessCliTransport.Default.execute(
            command = listOf("cmd", "/c", "echo", "hello world"),
            workspace = "."
        ).toList()

        events.filterIsInstance<CliEvent.Stdout>()
            .firstOrNull()
            .shouldNotBeNull()
            .content.trim().shouldBe("hello world")

        events.last().shouldBeInstanceOf<CliEvent.Exit>().code shouldBe 0
    }

    @ParameterizedTest
    @MethodSource("transportProvider")
    @EnabledOnOs(OS.LINUX, OS.MAC)
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
