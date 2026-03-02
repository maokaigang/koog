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
    /**
     * A volume mapping for Docker.
     */
    public fun toMountArg(): String = buildString {
        append("type=bind,source=")
        val path = hostPath.absolutePath
        append(path)
        append(",target=")
        append(containerPath)
        if (readOnly) append(",readonly")
    }
}

private fun MutableList<String>.mount(volume: DockerVolume) {
    add("--mount")
    add(volume.toMountArg())
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
        val dockerWorkspace = if (isWindows) "C:\\workspace" else "/workspace"
        val workspaceVolume = DockerVolume(File(workspace), dockerWorkspace)

        mount(workspaceVolume)
        add("-w")
        add(dockerWorkspace)

        // Additional volumes
        volumes.forEach(::mount)

        add(imageName)
        addAll(command)
    }

    private companion object {
        private val isWindows = System.getProperty("os.name").lowercase().contains("win")
    }
}
