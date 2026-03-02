package ai.koog.cli.transport

import java.io.File

/**
 * A volume mapping for Docker.
 * @property hostPath Path on the host machine.
 * @property containerPath Path inside the container.
 * @property readOnly Whether the volume should be mounted as read-only.
 */
public class DockerVolume @JvmOverloads constructor(
    public val hostPath: File,
    public val containerPath: String,
    public val readOnly: Boolean = false,
) {
    override fun toString(): String = buildString {
        append(hostPath.absolutePath)
        append(":")
        append(containerPath)
        if (readOnly) append(":ro")
    }
}

/**
 * Executes CLI commands inside a Docker container.
 *
 * @property imageName The Docker image to use.
 * @property volumes List of volume mappings.
 */
public class DockerCliTransport @JvmOverloads constructor(
    private val imageName: String,
    private val volumes: List<DockerVolume> = emptyList(),
) : ProcessCliTransport() {

    override fun buildCommand(
        command: List<String>,
        workspace: String,
        env: Map<String, String>
    ): List<String> = buildList {
        add("docker")
        add("run")
        add("--rm")

        // Environment variables
        env.forEach { (key, value) ->
            add("-e")
            add("$key=$value")
        }

        // Workspace volume
        val absoluteWorkspace = File(workspace).absolutePath
        val dockerWorkspace = if (isWindows) "C:\\workspace" else "/workspace"

        add("-v")
        add("$absoluteWorkspace:$dockerWorkspace")
        add("-w")
        add(dockerWorkspace)

        // Additional volumes
        volumes.forEach {
            add("-v")
            add(it.toString())
        }

        add(imageName)
        addAll(command)
    }

    private companion object {
        private val isWindows = System.getProperty("os.name").lowercase().contains("win")
    }
}
