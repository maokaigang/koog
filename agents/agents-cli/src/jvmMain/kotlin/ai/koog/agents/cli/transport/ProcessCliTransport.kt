package ai.koog.agents.cli.transport

import ai.koog.agents.cli.AgentEvent
import ai.koog.agents.cli.CliAIAgentEvent
import ai.koog.agents.cli.CliAgentTimeoutException
import ai.koog.utils.io.SuitableForIO
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
        workspace: String,
        env: Map<String, String>
    ): List<String>

    override fun checkAvailability(binary: String): CliAvailability = try {
        val process = ProcessBuilder(binary, "--version")
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
    ): Flow<CliAIAgentEvent> {
        val command = buildCommand(command, workspace, env)

        return channelFlow {
            val process = try {
                ProcessBuilder(command)
                    .directory(File(workspace))
                    .apply { environment().putAll(env) }
                    .redirectErrorStream(false)
                    .start()
                    .also { it.outputStream.close() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val failed = CliAIAgentEvent.Failed(e.message ?: e.toString())
                send(failed)
                close(e)
                return@channelFlow
            }

            send(CliAIAgentEvent.Started)

            val stdoutJob = launch(Dispatchers.SuitableForIO) {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { content ->
                        trySend(AgentEvent.Stdout(content))
                    }
                }
            }

            val stderrJob = launch(Dispatchers.SuitableForIO) {
                process.errorStream.bufferedReader().useLines { lines ->
                    lines.forEach { content ->
                        trySend(AgentEvent.Stderr(content))
                    }
                }
            }

            val waiter = launch(Dispatchers.SuitableForIO) {
                try {
                    val code = if (timeout != null) {
                        if (withTimeoutOrNull(timeout) { process.waitFor() } == null) {
                            process.destroyForcibly()
                            throw CliAgentTimeoutException("Execution timed out after $timeout", timeout)
                        }
                        process.exitValue()
                    } else {
                        process.waitFor()
                    }

                    // Ensure all output is collected before finishing
                    stdoutJob.join()
                    stderrJob.join()

                    val exit = CliAIAgentEvent.Exit(code)
                    trySend(exit)
                } catch (e: CliAgentTimeoutException) {
                    trySend(CliAIAgentEvent.Failed(e.message))
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
        override fun buildCommand(
            command: List<String>,
            workspace: String,
            env: Map<String, String>
        ): List<String> = command
    }
}
