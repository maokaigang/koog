package ai.koog.integration.tests.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.asNode
import ai.koog.agents.core.agent.cli.AIAgentCliStrategy
import ai.koog.agents.core.agent.cli.ClaudePermissionMode
import ai.koog.agents.core.agent.cli.CliAIAgentResponse
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.testing.tools.MockExecutor
import ai.koog.cli.transport.DockerCliTransport
import ai.koog.cli.transport.ProcessCliTransport
import ai.koog.integration.tests.utils.RetryUtils.withRetry
import ai.koog.integration.tests.utils.TestCredentials.readTestAnthropicKeyFromEnv
import ai.koog.integration.tests.utils.TestCredentials.readTestOpenAIKeyFromEnv
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLModel
import ai.koog.test.utils.DockerAvailableCondition
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class CliAIAgentIntegrationTest : AIAgentTestBase() {
    companion object {
        private const val IMAGE_NAME = "cli-agents"
        private val dockerTransport = DockerCliTransport(IMAGE_NAME)

        private suspend fun testAgent(agent: AIAgent<String, CliAIAgentResponse>) {
            assertResponse(agent.run("echo 'hi'"))
        }

        private fun assertResponse(response: CliAIAgentResponse) {
            assertFalse(response.isError, "Run should be successful")
            assertContains(response.content, "hi", ignoreCase = true, "Response should contain 'hi'")

            val usage = response.usage

            assertNotNull(usage.inputTokens, "Usage should contain input tokens")
            assertNotNull(usage.outputTokens, "Usage should contain output tokens")
        }
    }

    @Serializable
    data class TestInput(val request: String)

    @Serializable
    data class StructuredResult(val message: String)

    private fun buildConfig(model: LLModel? = null): AIAgentConfig =
        AIAgentConfig(
            prompt("") { system("please follow the instructions of the user") },
            model ?: OllamaModels.Meta.LLAMA_3_2,
            maxAgentIterations = 10,
        )

    @Test
    fun integration_testCodex() = runTest {
        val agent = AIAgent(
            agentConfig = buildConfig(model = OpenAIModels.Chat.GPT4o),
            strategy = AIAgentCliStrategy.codex(
                name = "codex",
                apiKey = readTestOpenAIKeyFromEnv(),
                transport = ProcessCliTransport.Default
            )
        )

        testAgent(agent)
    }

    @Test
    @ExtendWith(DockerAvailableCondition::class)
    fun integration_testCodexDocker() = runTest {
        val agent = AIAgent(
            agentConfig = buildConfig(model = OpenAIModels.Chat.GPT4o),
            strategy = AIAgentCliStrategy.codex(
                name = "codex",
                apiKey = readTestOpenAIKeyFromEnv(),
                transport = ProcessCliTransport.Default
            )
        )

        testAgent(agent)
    }

    // it could fail locally if you are logged in to codex
    @Test
    fun integration_testCodexNoKey() = runTest {
        val agent = AIAgent(
            agentConfig = buildConfig(),
            strategy = AIAgentCliStrategy.codex(
                name = "codex",
                transport = ProcessCliTransport.Default
            )
        )

        assertTrue(agent.run("Hi!").isError, "Response should be an error")
    }

    @Test
    @ExtendWith(DockerAvailableCondition::class)
    fun integration_testCodexNoKeyDocker() = runTest {
        val agent = AIAgent(
            agentConfig = buildConfig(),
            strategy = AIAgentCliStrategy.codex(
                name = "codex",
                transport = dockerTransport
            )
        )

        assertTrue(agent.run("Hi!").isError, "Response should be an error")
    }

    @Test
    fun integration_testClaude() = runTest {
        val agent = AIAgent(
            agentConfig = buildConfig(model = AnthropicModels.Sonnet_4_5),
            strategy = AIAgentCliStrategy.claude(
                name = "claude",
                apiKey = readTestAnthropicKeyFromEnv(),
                transport = ProcessCliTransport.Default
            )
        )

        testAgent(agent)
    }

    @Test
    @ExtendWith(DockerAvailableCondition::class)
    fun integration_testClaudeDocker() = runTest {
        val agent = AIAgent(
            agentConfig = buildConfig(model = AnthropicModels.Sonnet_4_5),
            strategy = AIAgentCliStrategy.claude(
                name = "claude",
                apiKey = readTestAnthropicKeyFromEnv(),
                transport = dockerTransport
            )
        )

        testAgent(agent)
    }

    // it could fail locally if you are logged in to claude
    @Test
    fun integration_testClaudeCodeNoKey() = runTest {
        val agent = AIAgent(
            agentConfig = buildConfig(),
            strategy = AIAgentCliStrategy.claude(
                name = "claude",
                transport = ProcessCliTransport.Default
            )
        )

        assertTrue(agent.run("Hi!").isError, "Response should be an error")
    }

    @Test
    @ExtendWith(DockerAvailableCondition::class)
    fun integration_testClaudeCodeNoKeyDocker() = runTest {
        val agent = AIAgent(
            agentConfig = buildConfig(),
            strategy = AIAgentCliStrategy.claude(
                name = "claude",
                transport = dockerTransport
            )
        )

        assertTrue(agent.run("Hi!").isError, "Response should be an error")
    }

    @Test
    fun integration_testClaudeCodeStructuredOutput() = runTest {
        val agent = AIAgent(
            agentConfig = buildConfig(),
            strategy = AIAgentCliStrategy.claude<String, StructuredResult>(
                name = "claude",
                apiKey = readTestAnthropicKeyFromEnv(),
                transport = ProcessCliTransport.Default
            )
        )

        assertResponse(agent.run("echo 'hi'").response)
    }

    @Test
    fun integration_testClaudeCustomInput() = runTest {
        val agent = AIAgent(
            agentConfig = buildConfig(model = AnthropicModels.Sonnet_4_5),
            strategy = AIAgentCliStrategy.claude<TestInput>(
                name = "claude-custom",
                apiKey = readTestAnthropicKeyFromEnv(),
                transport = ProcessCliTransport.Default,
                generateRequest = { _, input -> input.request }
            )
        )

        val response = agent.run(TestInput("echo 'hi'"))
        assertResponse(response)
    }

    @Test
    fun integration_testCodexCustomInput() = runTest {
        val agent = AIAgent(
            agentConfig = buildConfig(model = OpenAIModels.Chat.GPT4o),
            strategy = AIAgentCliStrategy.codex<TestInput>(
                name = "codex-custom",
                apiKey = readTestOpenAIKeyFromEnv(),
                transport = ProcessCliTransport.Default,
                generateRequest = { _, input -> input.request }
            )
        )

        val response = agent.run(TestInput("echo 'hi'"))
        assertResponse(response)
    }

    @Test
    fun integration_testClaudeCustomInputStructuredOutput() = runTest {
        val agent = AIAgent(
            agentConfig = buildConfig(model = AnthropicModels.Sonnet_4_5),
            strategy = AIAgentCliStrategy.claude<TestInput, StructuredResult>(
                name = "claude-structured-custom",
                apiKey = readTestAnthropicKeyFromEnv(),
                transport = ProcessCliTransport.Default,
                generateRequest = { _, input -> input.request }
            )
        )

        val response = agent.run(TestInput("echo '{\"message\": \"hi\"}'"))
        assertResponse(response.response)
    }

    @Test
    fun integration_testCliAgentInGraphs() = runTest(timeout = 180.seconds) {
        withRetry {
            val claudeApiKey = readTestAnthropicKeyFromEnv()
            val codexApiKey = readTestOpenAIKeyFromEnv()

            val claudePlanMode = AIAgent(
                agentConfig = buildConfig(),
                strategy = AIAgentCliStrategy.claude(
                    name = "claude-plan",
                    apiKey = claudeApiKey,
                    transport = ProcessCliTransport.Default,
                    permissionMode = ClaudePermissionMode.Plan
                )
            )

            val codex = AIAgent(
                agentConfig = buildConfig(),
                strategy = AIAgentCliStrategy.codex(
                    name = "codex",
                    apiKey = codexApiKey,
                    transport = ProcessCliTransport.Default
                )
            )

            val claudeStructured = AIAgent(
                agentConfig = buildConfig(),
                strategy = AIAgentCliStrategy.claude<String, StructuredResult>(
                    name = "claude-structured",
                    apiKey = claudeApiKey,
                    transport = ProcessCliTransport.Default,
                )
            )

            val strategy = strategy("test-strategy") {
                val generatePlan by claudePlanMode.asNode().transform { it.content }
                val solveTask by codex.asNode().transform { it.content }
                val returnResult by claudeStructured.asNode()

                nodeStart then generatePlan then solveTask then returnResult then nodeFinish
            }

            val agent = AIAgent(
                promptExecutor = MockExecutor.builder().build(),
                agentConfig = buildConfig(),
                strategy = strategy
            )

            assertResponse(agent.run("Write a python script printing 'hi'").response)
        }
    }
}
