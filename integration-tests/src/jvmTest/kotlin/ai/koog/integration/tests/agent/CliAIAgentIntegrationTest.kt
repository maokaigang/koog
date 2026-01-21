package ai.koog.integration.tests.agent

import ai.koog.agents.cli.CliAIAgent
import ai.koog.agents.cli.claude.ClaudeCodeAgent
import ai.koog.agents.cli.claude.ClaudeCodeConfig
import ai.koog.agents.cli.codex.CodexAgent
import ai.koog.agents.cli.codex.CodexConfig
import ai.koog.agents.cli.transport.CliTransport
import ai.koog.agents.cli.transport.DockerCliTransport
import ai.koog.integration.tests.utils.TestCredentials.readTestAnthropicKeyFromEnv
import ai.koog.integration.tests.utils.TestCredentials.readTestOpenAIKeyFromEnv
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.Test

class CliAIAgentIntegrationTest : AIAgentTestBase() {
    companion object {
        private const val IMAGE_NAME = "cli-agents"
        private val dockerTransport = DockerCliTransport(IMAGE_NAME)

        @JvmStatic
        fun defaultTransports(): Stream<CliTransport> = Stream.of(
            CliTransport.Default,
            dockerTransport
        )

        private suspend fun testAgent(agent: CliAIAgent<*>) {
            agent.run("echo 'hi'").shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource("defaultTransports")
    fun integration_testCodexAgent(transport: CliTransport) = runTest {
        val agent = CodexAgent(
            CodexConfig(
                transport = transport,
                apiKey = readTestOpenAIKeyFromEnv()
            )
        )
        testAgent(agent)
    }

    @ParameterizedTest
    @MethodSource("defaultTransports")
    fun integration_testClaudeAgent(transport: CliTransport) = runTest {
        val agent = ClaudeCodeAgent(
            ClaudeCodeConfig(
                transport = transport,
                apiKey = readTestAnthropicKeyFromEnv()
            )
        )
        testAgent(agent)
    }

    @Test
    fun integration_testCodexNoKey() = runTest {
        val agent = CodexAgent(CodexConfig(transport = dockerTransport))
        testAgent(agent)
    }

    @Test
    fun integration_testClaudeCodeNoKey() = runTest {
        val agent = ClaudeCodeAgent(ClaudeCodeConfig(transport = dockerTransport))
        testAgent(agent)
    }
}
