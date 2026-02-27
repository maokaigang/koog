package ai.koog.agents.core.agent.cli;

import ai.koog.cli.transport.ProcessCliTransport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Java tests for AIAgentCliStrategy builder and factory methods.
 */
public class AIAgentCliStrategyJavaTest {

    @Test
    public void testFluentClaudeBuilder() {
        // Fluent builder is the most robust way for complex configurations in Java
        AIAgentCliStrategy<String, CliAIAgentResponse> strategy = AIAgentCliStrategy.builder("fluent-claude")
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
        // Fluent builder for Codex
        AIAgentCliStrategy<String, CliAIAgentResponse> strategy = AIAgentCliStrategy.builder("fluent-codex")
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
        // Test simplest Claude factory overload (2 parameters)
        AIAgentCliStrategy<String, CliAIAgentResponse> strategy = AIAgentCliStrategy.claude(
            "test-claude",
            ProcessCliTransport.defaultTransport()
        );
        assertNotNull(strategy);
        assertEquals("test-claude", strategy.getName());
    }

    @Test
    public void testCodexFactoryOverloads() {
        // Test simplest Codex factory overload (2 parameters)
        AIAgentCliStrategy<String, CliAIAgentResponse> strategy = AIAgentCliStrategy.codex(
            "test-codex",
            ProcessCliTransport.defaultTransport()
        );
        assertNotNull(strategy);
        assertEquals("test-codex", strategy.getName());
    }

    @Test
    public void testDockerTransportFactory() {
        // Test docker factory method
        ProcessCliTransport transport = ProcessCliTransport.dockerTransport("test-image");
        assertNotNull(transport);
    }
}
