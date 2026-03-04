package ai.koog.cli.transport

import ai.koog.agents.annotations.JavaAPI
import ai.koog.utils.io.SuitableForIO
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.time.Duration

/**
 * Base class for transports that execute a local [Process].
 */
public abstract class ProcessCliTransport : CliTransport {

    /**
     * Builds the [ProcessBuilder] for execution.
     */
    protected abstract fun buildCommand(
        command: List<String>,
        workspace: String = ".",
        env: Map<String, String> = emptyMap()
    ): List<String>

    override fun checkAvailability(binary: String): CliAvailability = try {
        val process = ProcessBuilder(buildCommand(listOf(binary, "--version")))
            .directory(File("."))
            .start()
        val reader = process.inputStream.bufferedReader()
        val version = reader.readLine()?.trim()
        val exitCode = process.waitFor()
        if (exitCode == 0) {
            CliAvailable(version)
        } else {
            CliUnavailable("Process exited with code $exitCode")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        CliUnavailable(reason = e.message ?: e.toString(), cause = e)
    }

    override fun execute(
        command: List<String>,
        workspace: String,
        env: Map<String, String>,
        timeout: Duration?
    ): Flow<CliEvent> {
        val fullCommand = buildCommand(command, workspace, env)
        logger.info { "Executing command: ${fullCommand.joinToString(" ")} in workspace: $workspace" }

        return channelFlow {
            val process = try {
                ProcessBuilder(fullCommand)
                    .directory(File(workspace))
                    .apply { environment().putAll(env) }
                    .redirectErrorStream(false)
                    .start()
                    .also { it.outputStream.close() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Failed to start process: ${e.message}" }
                val failed = CliEvent.Failed(e.message ?: e.toString())
                send(failed)
                close(e)
                return@channelFlow
            }

            val stdoutJob = launch(Dispatchers.SuitableForIO) {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { content ->
                        logger.debug { "Process stdout: $content" }
                        trySend(CliEvent.Stdout(content))
                    }
                }
            }

            val stderrJob = launch(Dispatchers.SuitableForIO) {
                process.errorStream.bufferedReader().useLines { lines ->
                    lines.forEach { content ->
                        logger.warn { "Process stderr: $content" }
                        trySend(CliEvent.Stderr(content))
                    }
                }
            }

            val waiter = launch(Dispatchers.SuitableForIO) {
                try {
                    val code = if (timeout != null) {
                        if (withTimeoutOrNull(timeout) { process.waitFor() } == null) {
                            process.destroyForcibly()
                            logger.error { "Execution timed out after $timeout" }
                            throw CliTimeoutException("Execution timed out after $timeout", timeout)
                        }
                        process.exitValue()
                    } else {
                        process.waitFor()
                    }

                    // Ensure all output is collected before finishing
                    stdoutJob.join()
                    stderrJob.join()

                    if (code != 0) {
                        logger.warn { "Process exited with non-zero code: $code" }
                    }

                    val exit = CliEvent.Exit(code)
                    trySend(exit)
                } catch (e: CliTimeoutException) {
                    trySend(CliEvent.Failed(e.message))
                } finally {
                    close()
                }
            }

            awaitClose {
                try {
                    process.destroy()
                } catch (_: Throwable) {
                }
                stdoutJob.cancel()
                stderrJob.cancel()
                waiter.cancel()
            }
        }.flowOn(Dispatchers.SuitableForIO)
    }

    /**
     * Default implementation of ProcessTransport.
     */
    public object Default : ProcessCliTransport() {
        private val isWindows = System.getProperty("os.name").lowercase().contains("win")

        override fun buildCommand(
            command: List<String>,
            workspace: String,
            env: Map<String, String>
        ): List<String> = if (isWindows) {
            listOf("cmd", "/c") + command
        } else {
            command
        }
    }

    /**
     * Companion object for the [ProcessCliTransport] class, providing utility methods for creating
     * specific types of `ProcessCliTransport` implementations.
     */
    public companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Returns the default [ProcessCliTransport] implementation.
         */
        @JavaAPI
        @JvmStatic
        public fun defaultTransport(): ProcessCliTransport = Default

        /**
         * Creates a [DockerCliTransport] with the specified image and optional volumes.
         */
        @JavaAPI
        @JvmStatic
        @JvmOverloads
        public fun dockerTransport(
            imageName: String,
            volumes: List<DockerVolume> = emptyList()
        ): ProcessCliTransport = DockerCliTransport(imageName, volumes)
    }
}
