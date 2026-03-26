package ai.koog.agents.core.agent

import ai.koog.agents.core.agent.cli.AIAgentCliStrategy
import ai.koog.agents.core.agent.cli.CliAIAgentResponse
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.cli.transport.ProcessCliTransport
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CliAgentBuilderTest {

    @Test
    fun testCliStrategyBuilder() {
        val strategy = AIAgentCliStrategy.codex(
            name = "test-codex",
            apiKey = "test-key",
            transport = ProcessCliTransport.Default
        )

        val agent = AIAgent.builder()
            .llmModel(OpenAIModels.Chat.GPT4o)
            .cliStrategy(strategy)
            .id("test-cli-agent")
            .systemPrompt("You are a CLI assistant")
            .maxIterations(10)
            .build()

        assertNotNull(agent)
        assertEquals("test-cli-agent", agent.id)
        assertEquals(10, agent.agentConfig.maxAgentIterations)
        assertEquals(OpenAIModels.Chat.GPT4o, agent.agentConfig.model)
    }

    @Test
    fun testCliStrategyByNameBuilder() {
        val agent = AIAgent.builder()
            .llmModel(OpenAIModels.Chat.GPT4o)
            .cliStrategy("codex") { builder ->
                builder.codex()
                    .apiKey("test-key")
                    .transport(ProcessCliTransport.Default)
                    .build()
            }
            .id("test-cli-agent-name")
            .build()

        assertNotNull(agent)
        assertEquals("test-cli-agent-name", agent.id)
    }
}
