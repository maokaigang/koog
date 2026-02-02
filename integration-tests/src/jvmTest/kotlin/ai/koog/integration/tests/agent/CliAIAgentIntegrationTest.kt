package ai.koog.integration.tests.agent

import ai.koog.agents.cli.CliAIAgentResponse
import ai.koog.agents.cli.claude.ClaudeCodeAgent
import ai.koog.agents.cli.claude.ClaudePermissionMode
import ai.koog.agents.cli.codex.CodexAgent
import ai.koog.agents.cli.transport.DockerCliTransport
import ai.koog.agents.cli.transport.ProcessCliTransport
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.testing.tools.MockExecutor
import ai.koog.integration.tests.utils.TestCredentials.readTestAnthropicKeyFromEnv
import ai.koog.integration.tests.utils.TestCredentials.readTestOpenAIKeyFromEnv
import ai.koog.prompt.llm.OllamaModels
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test

class CliAIAgentIntegrationTest : AIAgentTestBase() {
    companion object {
        private const val IMAGE_NAME = "cli-agents"
        private val dockerTransport = DockerCliTransport(IMAGE_NAME)

        private suspend fun testAgent(agent: AIAgent<String, CliAIAgentResponse>) {
            val response = agent.run("echo 'hi'")

            response.isError.shouldBeFalse()
            response.content.contains("hi", ignoreCase = true).shouldBeTrue()

            val usage = response.usage

            usage.inputTokens.shouldNotBeNull()
            usage.outputTokens.shouldNotBeNull()
        }
    }

    @Serializable
    data class StructuredResult(val message: String)

    @Test
    fun integration_testCodexWithDefaultTransport() = runTest {
        val agent = CodexAgent(
            apiKey = readTestOpenAIKeyFromEnv(),
            transport = ProcessCliTransport.Default
        )
        testAgent(agent)
    }

    @Test
    fun integration_testCodexNoKey() = runTest {
        val agent = CodexAgent(transport = dockerTransport)
        agent.run("Hi!").isError.shouldBeTrue()
    }

    @Test
    fun integration_testClaudeCodeNoKey() = runTest {
        val agent = ClaudeCodeAgent(transport = dockerTransport)
        agent.run("Hi!").isError.shouldBeTrue()
    }

    @Test
    fun integration_testClaudeCodeInvoke() = runTest {
        val agent = ClaudeCodeAgent(
            apiKey = readTestAnthropicKeyFromEnv(),
            transport = ProcessCliTransport.Default
        )

        testAgent(agent)
    }

    @Test
    fun integration_testCodexInvoke() = runTest {
        val agent = CodexAgent(
            apiKey = readTestOpenAIKeyFromEnv(),
            transport = ProcessCliTransport.Default
        )

        testAgent(agent)
    }

    @Test
    fun integration_testClaudeCodeStructuredOutput() = runTest {
        val agent = ClaudeCodeAgent(
            apiKey = readTestAnthropicKeyFromEnv(),
            transport = dockerTransport
        )

        agent.runStructured<StructuredResult>("echo 'hi'")
    }

    @Test
    fun integration_testCliAgentInGraphs() = runTest {
        val claudeApiKey = readTestAnthropicKeyFromEnv()
        val codexApiKey = readTestOpenAIKeyFromEnv()

        val claudePlanMode = ClaudeCodeAgent(
            apiKey = claudeApiKey,
            transport = dockerTransport,
            permissionMode = ClaudePermissionMode.Plan
        )

        val codex = CodexAgent(
            apiKey = codexApiKey,
            transport = dockerTransport
        )

        val claude = ClaudeCodeAgent(
            apiKey = claudeApiKey,
            transport = dockerTransport
        )

        val strategy = strategy<String, StructuredResult>("test-strategy") {
            val generatePlan by claudePlanMode.asNode().transform { it.content }
            val solveTask by codex.asNode().transform { it.content }
            val returnResult by claude.asNodeStructured<StructuredResult>().transform { it.result }

            nodeStart then generatePlan then solveTask then returnResult then nodeFinish
        }

        val agent = AIAgent(
            promptExecutor = MockExecutor.builder().build(),
            agentConfig = AIAgentConfig.withSystemPrompt(
                "",
                OllamaModels.Meta.LLAMA_3_2,
                maxAgentIterations = 10,
            ),
            strategy = strategy
        )

        agent.run("Write a hello_world.py script").shouldNotBeNull()
    }
}
