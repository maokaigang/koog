package ai.koog.agents.core.agent.cli;

import ai.koog.agents.core.agent.context.AIAgentCliContext;
import ai.koog.cli.transport.ProcessCliTransport;
import kotlin.jvm.JvmClassMappingKt;
import kotlinx.serialization.Serializable;
import org.junit.jupiter.api.Test;
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
        var strategy = AIAgentCliStrategy.builder("fluent-claude")
            .claude()
            .transport(ProcessCliTransport.defaultTransport())
            .apiKey("fluent-key")
            .permissionMode(ClaudePermissionMode.Plan)
            .workspace("/tmp")
            .build();

        assertNotNull(strategy);
        assertEquals("fluent-claude", strategy.getName());
    }

    @Test
    public void testFluentCodexBuilder() {
        var strategy = AIAgentCliStrategy.builder("fluent-codex")
            .codex()
            .transport(ProcessCliTransport.defaultTransport())
            .apiKey("codex-key")
            .sandbox(CodexSandboxMode.ReadOnly)
            .askForApproval(CodexApprovalPolicy.Never)
            .build();

        assertNotNull(strategy);
        assertEquals("fluent-codex", strategy.getName());
    }

    @Test
    public void testClaudeFactoryOverloads() {
        var strategy = AIAgentCliStrategy.claude(
            "test-claude",
            ProcessCliTransport.defaultTransport()
        );
        assertNotNull(strategy);
        assertEquals("test-claude", strategy.getName());
    }

    @Test
    public void testCodexFactoryOverloads() {
        var strategy = AIAgentCliStrategy.codex(
            "test-codex",
            ProcessCliTransport.defaultTransport()
        );
        assertNotNull(strategy);
        assertEquals("test-codex", strategy.getName());
    }

    @Test
    public void testClaudeWithCustomInput() {
        var strategy = AIAgentCliStrategy.builder("claude-custom-input")
            .claude()
            .transport(ProcessCliTransport.defaultTransport())
            .generateRequest(this::generateRequest)
            .build();

        assertNotNull(strategy);
        assertEquals("claude-custom-input", strategy.getName());
    }

    @Test
    public void testCodexWithCustomInput() {
        var strategy = AIAgentCliStrategy.builder("codex-custom-input")
            .codex()
            .transport(ProcessCliTransport.defaultTransport())
            .generateRequest(this::generateRequest)
            .build();

        assertNotNull(strategy);
        assertEquals("codex-custom-input", strategy.getName());
    }

    @Test
    public void testClaudeWithStructuredOutput() {
        var strategy = AIAgentCliStrategy.builder("claude-structured")
            .claude()
            .transport(ProcessCliTransport.defaultTransport())
            .structure(JvmClassMappingKt.getKotlinClass(TestOutput.class))
            .build();

        assertNotNull(strategy);
        assertEquals("claude-structured", strategy.getName());
    }
}
