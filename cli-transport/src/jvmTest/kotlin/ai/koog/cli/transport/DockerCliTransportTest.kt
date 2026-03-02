package ai.koog.cli.transport

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test

class DockerCliTransportTest {
    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    private val imageName = if (isWindows) {
        "mcr.microsoft.com/windows/nanoserver:ltsc2022"
    } else {
        "alpine:latest"
    }

    @Test
    fun testDockerVolumeMapping() = runTest {
        val tmpDir = Files.createTempDirectory("docker-test")
        val testFile = tmpDir.resolve("test.txt").toFile()
        testFile.writeText("volume-test-content")

        val containerPath = if (isWindows) "C:/mnt/test" else "/mnt/test"
        val command = if (isWindows) {
            listOf("cmd", "/c", "type", "C:\\mnt\\test\\test.txt")
        } else {
            listOf("cat", "/mnt/test/test.txt")
        }

        try {
            val transport = DockerCliTransport(
                imageName = imageName,
                volumes = listOf(DockerVolume(tmpDir.toFile(), containerPath))
            )

            val events = transport.execute(
                command = command,
                workspace = "."
            ).toList()

            events.filterIsInstance<CliEvent.Stdout>()
                .firstOrNull()
                .shouldNotBeNull()
                .content.trim().shouldBe("volume-test-content")
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testDockerWorkspaceMapping() = runTest {
        val tmpDir = Files.createTempDirectory("workspace-test")
        val testFile = tmpDir.resolve("workspace.txt").toFile()
        testFile.writeText("workspace-content")

        val command = if (isWindows) {
            listOf("cmd", "/c", "type", "workspace.txt")
        } else {
            listOf("cat", "workspace.txt")
        }

        try {
            val transport = DockerCliTransport(imageName)
            val events = transport.execute(
                command = command,
                workspace = tmpDir.toAbsolutePath().toString()
            ).toList()

            events.filterIsInstance<CliEvent.Stdout>()
                .firstOrNull()
                .shouldNotBeNull()
                .content.trim().shouldBe("workspace-content")
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testDockerEnvVars() = runTest {
        val transport = DockerCliTransport(imageName)
        val env = mapOf("DOCKER_VAR" to "docker-value")
        val command = if (isWindows) {
            listOf("cmd", "/c", "echo %DOCKER_VAR%")
        } else {
            listOf("sh", "-c", "echo \$DOCKER_VAR")
        }
        val events = transport.execute(
            command = command,
            workspace = ".",
            env = env
        ).toList()

        events.filterIsInstance<CliEvent.Stdout>()
            .firstOrNull()
            .shouldNotBeNull()
            .content.trim().shouldBe("docker-value")
    }
}
