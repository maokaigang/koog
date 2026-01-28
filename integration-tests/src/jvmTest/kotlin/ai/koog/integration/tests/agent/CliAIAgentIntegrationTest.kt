package ai.koog.integration.tests.agent

import ai.koog.agents.cli.CliAIAgent
import ai.koog.agents.cli.claude.ClaudeCodeAgent
import ai.koog.agents.cli.claude.ClaudePermissionMode
import ai.koog.agents.cli.codex.CodexAgent
import ai.koog.agents.cli.transport.CliTransport
import ai.koog.agents.cli.transport.DockerCliTransport
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.testing.tools.MockExecutor
import ai.koog.integration.tests.utils.TestCredentials.readTestAnthropicKeyFromEnv
import ai.koog.integration.tests.utils.TestCredentials.readTestOpenAIKeyFromEnv
import ai.koog.prompt.llm.OllamaModels
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
    data class StructuredResult(val message: String)

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
            .build(serializer<StructuredResult>())

        testAgent(agent)
    }

    @Test
    fun integration_testCliAgentInGraphs() = runTest {
        val claudePlanMode = ClaudeCodeAgent(
            transport = dockerTransport,
            permissionMode = ClaudePermissionMode.Plan
        )
        val codex = CodexAgent(transport = dockerTransport)
        val claudeStructuredResult = ClaudeCodeAgent<StructuredResult>(transport = dockerTransport)

        val strategy = strategy<String, StructuredResult>("test-strategy") {
            val generatePlan by claudePlanMode.asNode().transform { it!! }
            val solveTask by codex.asNode().transform { it!! }
            val returnResult by claudeStructuredResult.asNode().transform { it!! }

            nodeStart then generatePlan then solveTask then returnResult then nodeFinish
        }

        val agent = AIAgent(
            promptExecutor = MockExecutor.builder().build(),
            agentConfig = AIAgentConfig.withSystemPrompt(
                "", OllamaModels.Meta.LLAMA_3_2, maxAgentIterations = 10,
            ),
            strategy = strategy
        )

        agent.run("Write a hello_world.py script").shouldNotBeNull()
    }
}
