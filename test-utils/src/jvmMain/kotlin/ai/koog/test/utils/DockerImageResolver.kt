package ai.koog.test.utils

import org.testcontainers.DockerClientFactory
import java.util.concurrent.TimeUnit

/**
 * Utility to resolve and ensure the availability of Docker images used in tests.
 */
public object DockerImageResolver {
    private val client by lazy { DockerClientFactory.instance().client() }

    /**
     * Resolves the default image name for the current OS and ensures it is available locally.
     * @return The resolved image name.
     */
    public fun resolveAndEnsureCliImage(): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val image = if (isWindows) {
            "mcr.microsoft.com/windows/nanoserver:ltsc2022"
        } else {
            "alpine:latest"
        }
        ensureImage(image)
        return image
    }

    /**
     * Ensures that the specified image is available locally, pulling it if necessary.
     */
    public fun ensureImage(imageName: String) {
        try {
            val localImages = client.listImagesCmd().withImageNameFilter(imageName).exec()
            if (localImages.isEmpty()) {
                client.pullImageCmd(imageName)
                    .start()
                    .awaitCompletion(5, TimeUnit.MINUTES)
            }
        } catch (e: Exception) {
            System.err.println("[DEBUG_LOG] Failed to ensure image $imageName: ${e.message}")
        }
    }
}
