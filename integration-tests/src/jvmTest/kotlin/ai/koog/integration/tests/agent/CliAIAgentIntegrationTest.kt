package ai.koog.integration.tests.agent

import ai.koog.agents.cli.CliAIAgent
import ai.koog.agents.cli.claude.ClaudeCodeAgent
import ai.koog.agents.cli.codex.CodexAgent
import ai.koog.agents.cli.transport.CliTransport
import ai.koog.agents.cli.transport.DockerCliTransport
import ai.koog.integration.tests.utils.TestCredentials.readTestAnthropicKeyFromEnv
import ai.koog.integration.tests.utils.TestCredentials.readTestOpenAIKeyFromEnv
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
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

    @Serializable
    data class TestResult(val message: String)

    @ParameterizedTest
    @MethodSource("defaultTransports")
    fun integration_testCodexAgent(transport: CliTransport) = runTest {
        val agent = CodexAgent(
            apiKey = readTestOpenAIKeyFromEnv(),
            transport = transport
        )
        testAgent(agent)
    }

    @ParameterizedTest
    @MethodSource("defaultTransports")
    fun integration_testClaudeAgent(transport: CliTransport) = runTest {
        val agent = ClaudeCodeAgent(
            apiKey = readTestAnthropicKeyFromEnv(),
            transport = transport
        )
        testAgent(agent)
    }

    @Test
    fun integration_testCodexNoKey() = runTest {
        val agent = CodexAgent(transport = dockerTransport)
        testAgent(agent)
    }

    @Test
    fun integration_testClaudeCodeNoKey() = runTest {
        val agent = ClaudeCodeAgent(transport = dockerTransport)
        testAgent(agent)
    }

    @Test
    fun integration_testClaudeCodeBuilder() = runTest {
        val agent = ClaudeCodeAgent.builder()
            .apiKey(readTestAnthropicKeyFromEnv())
            .transport(CliTransport.Default)
            .build()
        agent.shouldNotBeNull()
    }

    @Test
    fun integration_testCodexBuilder() = runTest {
        val agent = CodexAgent.builder()
            .apiKey(readTestOpenAIKeyFromEnv())
            .transport(CliTransport.Default)
            .build()
        agent.shouldNotBeNull()
    }

    @Test
    fun integration_testClaudeCodeStructuredOutput() = runTest {
        val agent = ClaudeCodeAgent.builder()
            .apiKey(readTestAnthropicKeyFromEnv())
            .transport(CliTransport.Default)
            .build(serializer<TestResult>())

        testAgent(agent)
    }
}
