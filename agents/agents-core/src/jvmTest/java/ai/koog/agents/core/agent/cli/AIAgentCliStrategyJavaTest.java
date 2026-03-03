package ai.koog.agents.core.agent.cli;

import ai.koog.agents.core.agent.context.AIAgentCliContext;
import ai.koog.cli.transport.CliTransport;
import ai.koog.cli.transport.ProcessCliTransport;
import ai.koog.prompt.structure.json.JsonStructure;
import kotlinx.serialization.KSerializer;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Java tests for AIAgentCliStrategy builder and factory methods.
 */
public class AIAgentCliStrategyJavaTest {

    private static class TestInput {
        public final String request;

        public TestInput(String request) {
            this.request = request;
        }
    }

    private String generateRequest(AIAgentCliContext context, TestInput input) {
        return input.request;
    }

    @Test
    public void testFluentClaudeBuilder() {
        AIAgentCliStrategy<String, CliAIAgentResponse> strategy = AIAgentCliStrategy.builder((String) "fluent-claude")
            .claude()
            .transport((CliTransport) ProcessCliTransport.defaultTransport())
            .apiKey("fluent-key")
            .permissionMode(ClaudePermissionMode.Plan)
            .workspace("/tmp")
            .build();

        assertNotNull(strategy);
        assertEquals("fluent-claude", strategy.getName());
    }

    @Test
    public void testFluentCodexBuilder() {
        AIAgentCliStrategy<String, CliAIAgentResponse> strategy = AIAgentCliStrategy.builder((String) "fluent-codex")
            .codex()
            .transport((CliTransport) ProcessCliTransport.defaultTransport())
            .apiKey("codex-key")
            .sandbox(CodexSandboxMode.ReadOnly)
            .askForApproval(CodexApprovalPolicy.Never)
            .build();

        assertNotNull(strategy);
        assertEquals("fluent-codex", strategy.getName());
    }

    @Test
    public void testClaudeFactoryOverloads() {
        AIAgentCliStrategy<String, CliAIAgentResponse> strategy = AIAgentCliStrategy.claude(
            (String) "test-claude",
            (CliTransport) ProcessCliTransport.defaultTransport()
        );
        assertNotNull(strategy);
        assertEquals("test-claude", strategy.getName());
    }

    @Test
    public void testCodexFactoryOverloads() {
        AIAgentCliStrategy<String, CliAIAgentResponse> strategy = AIAgentCliStrategy.codex(
            (String) "test-codex",
            (CliTransport) ProcessCliTransport.defaultTransport()
        );
        assertNotNull(strategy);
        assertEquals("test-codex", strategy.getName());
    }

    @Test
    public void testClaudeWithCustomInput() {
        AIAgentCliStrategy<TestInput, CliAIAgentResponse> strategy = AIAgentCliStrategy.builder("claude-custom-input")
            .claude()
            .transport(ProcessCliTransport.defaultTransport())
            .generateRequest(this::generateRequest)
            .build();

        assertNotNull(strategy);
        assertEquals("claude-custom-input", strategy.getName());
    }

    @Test
    public void testCodexWithCustomInput() {
        AIAgentCliStrategy<TestInput, CliAIAgentResponse> strategy = AIAgentCliStrategy.builder("codex-custom-input")
            .codex()
            .transport(ProcessCliTransport.defaultTransport())
            .generateRequest(this::generateRequest)
            .build();

        assertNotNull(strategy);
        assertEquals("codex-custom-input", strategy.getName());
    }

    public static class TestOutput {
        public String result;
    }

    @Test
    public void testClaudeWithStructuredOutput() {
        // In Java, we need to provide the serializer explicitly.
        // For testing purposes, we can use a mock or a simple serializer if available.
        // Since we are mostly testing the builder/factory, we can try to get a serializer for a simple class.
        // However, Kotlin's `serializer()` is often a static method on the companion object or a generated class.

        // Let's assume we can get a serializer for String or some other simple type if TestOutput is hard.
        KSerializer<String> stringSerializer = kotlinx.serialization.SerializersKt.serializer(String.class);
        JsonStructure<String> structure = JsonStructure.Companion.create(
            "StringStructure",
            stringSerializer,
            JsonStructure.Companion.getDefaultJson(),
            ai.koog.prompt.structure.json.generator.StandardJsonSchemaGenerator.Default,
            Collections.emptyMap(),
            Collections.emptySet(),
            Collections.emptyList(),
            JsonStructure.Companion::defaultDefinitionPrompt
        );

        AIAgentCliStrategy<String, CliAgentStructuredResponse<String>> strategy = AIAgentCliStrategy.builder("claude-structured")
            .claude()
            .transport(ProcessCliTransport.defaultTransport())
            .structure(structure)
            .build();

        assertNotNull(strategy);
        assertEquals("claude-structured", strategy.getName());
    }
}
