package ai.koog.integration.tests.agent;

import ai.koog.agents.core.agent.AIAgent;
import ai.koog.agents.core.agent.CliAIAgent;
import ai.koog.agents.core.agent.cli.CliAIAgentResponse;
import ai.koog.agents.core.agent.cli.CliAgentStructuredResponse;
import ai.koog.agents.core.agent.cli.CliAgentUsage;
import ai.koog.agents.core.agent.context.AIAgentCliContext;
import ai.koog.cli.transport.ProcessCliTransport;
import ai.koog.integration.tests.base.KoogJavaTestBase;
import ai.koog.integration.tests.utils.StructuredResults;
import ai.koog.integration.tests.utils.TestCredentials;
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels;
import ai.koog.prompt.executor.clients.openai.OpenAIModels;
import kotlin.jvm.JvmClassMappingKt;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JavaCliAIAgentIntegrationTest extends KoogJavaTestBase {

    private static class TestInput {
        public String request;

        public TestInput(String request) {
            this.request = request;
        }
    }

    private static String generateRequest(AIAgentCliContext context, TestInput input) {
        return input.request;
    }

    private void testAgent(CliAIAgent<String, CliAIAgentResponse> agent) {
        var response = agent.run("echo 'hi'");
        assertResponse(response);
    }

    private void assertResponse(CliAIAgentResponse response) {
        assertResponse(response, "hi");
    }

    private void assertResponse(CliAIAgentResponse response, String expectedContent) {
        assertNotNull(response);
        assertFalse(response.isError(), "Run should be successful");
        String content = response.getContent();
        assertTrue(content.toLowerCase().contains(expectedContent.toLowerCase()),
            "Response should contain '" + expectedContent + "'");

        CliAgentUsage usage = response.getUsage();
        assertNotNull(usage.getInputTokens(), "Usage should contain input tokens");
        assertNotNull(usage.getOutputTokens(), "Usage should contain output tokens");
    }

    private <T> void assertStructuredResponse(CliAgentStructuredResponse<T> response) {
        assertNotNull(response);
        assertNotNull(response.getResult());
        assertNotNull(response.getResponse());
        assertFalse(response.getResponse().isError(), "Run should be successful");
    }

    @Test
    public void integration_testCodex() {
        var apiKey = TestCredentials.INSTANCE.readTestOpenAIKeyFromEnv();
        var agent = AIAgent.builder()
            .llmModel(OpenAIModels.Chat.GPT4o)
            .systemPrompt("please follow the instructions of the user")
            .cliStrategy("codex", builder ->
                builder.codex()
                    .transport(ProcessCliTransport.defaultTransport())
                    .apiKey(apiKey)
                    .build()
            )
            .build();

        testAgent(agent);
    }

    @Test
    public void integration_testClaude() {
        var apiKey = TestCredentials.INSTANCE.readTestAnthropicKeyFromEnv();
        var agent = AIAgent.builder()
            .llmModel(AnthropicModels.Sonnet_4_5)
            .systemPrompt("please follow the instructions of the user")
            .cliStrategy("claude", builder ->
                builder.claude()
                    .transport(ProcessCliTransport.defaultTransport())
                    .apiKey(apiKey)
                    .build()
            )
            .build();

        testAgent(agent);
    }

    @Test
    public void integration_testClaudeStructuredOutput() {
        var apiKey = TestCredentials.INSTANCE.readTestAnthropicKeyFromEnv();
        var agent = AIAgent.builder()
            .llmModel(AnthropicModels.Sonnet_4_5)
            .systemPrompt("please follow the instructions of the user")
            .cliStrategy("claude-structured", builder ->
                builder.claude()
                    .transport(ProcessCliTransport.defaultTransport())
                    .apiKey(apiKey)
                    .structure(JvmClassMappingKt.getKotlinClass(StructuredResults.CalculationResult.class))
                    .build()
            )
            .build();

        var response = agent.run("what's 1 + 1?");
        assertStructuredResponse(response);
        assertEquals(2, response.getResult().getResult());
    }

    @Test
    public void integration_testClaudeCustomInput() {
        var apiKey = TestCredentials.INSTANCE.readTestAnthropicKeyFromEnv();
        var agent = AIAgent.builder()
            .llmModel(AnthropicModels.Sonnet_4_5)
            .systemPrompt("please follow the instructions of the user")
            .cliStrategy("claude-custom", builder ->
                builder.claude()
                    .transport(ProcessCliTransport.defaultTransport())
                    .apiKey(apiKey)
                    .generateRequest(JavaCliAIAgentIntegrationTest::generateRequest)
                    .build()
            )
            .build();

        var response = agent.run(new TestInput("echo 'hi'"));
        assertResponse(response);
    }

    @Test
    public void integration_testClaudeCustomInputStructuredOutput() {
        var apiKey = TestCredentials.INSTANCE.readTestAnthropicKeyFromEnv();
        var agent = AIAgent.builder()
            .llmModel(AnthropicModels.Sonnet_4_5)
            .systemPrompt("please follow the instructions of the user")
            .cliStrategy("claude-custom-structured", builder ->
                builder.claude()
                    .transport(ProcessCliTransport.defaultTransport())
                    .apiKey(apiKey)
                    .generateRequest(JavaCliAIAgentIntegrationTest::generateRequest)
                    .structure(JvmClassMappingKt.getKotlinClass(StructuredResults.CalculationResult.class))
                    .build()
            )
            .build();

        var response = agent.run(new TestInput("what's 1 + 1?"));
        assertStructuredResponse(response);
        assertEquals(2, response.getResult().getResult());
    }

    @Test
    public void integration_testCodexCustomInput() {
        var apiKey = TestCredentials.INSTANCE.readTestOpenAIKeyFromEnv();
        var agent = AIAgent.builder()
            .llmModel(OpenAIModels.Chat.GPT4o)
            .systemPrompt("please follow the instructions of the user")
            .cliStrategy("codex-custom", builder ->
                builder.codex()
                    .transport(ProcessCliTransport.defaultTransport())
                    .apiKey((String) apiKey)
                    .generateRequest(JavaCliAIAgentIntegrationTest::generateRequest)
                    .build()
            )
            .build();

        var response = agent.run(new TestInput("echo 'hi'"));
        assertResponse(response);
    }
}
