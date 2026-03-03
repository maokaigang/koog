package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.context.AIAgentCliContext
import ai.koog.cli.transport.ProcessCliTransport
import ai.koog.prompt.structure.json.JsonStructure
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

class AIAgentCliStrategyTest {

    private class TestInput(val request: String)

    private fun generateRequest(context: AIAgentCliContext, input: TestInput): String = input.request

    @Test
    fun testClaudeBasic() {
        val strategy = AIAgentCliStrategy.claude(
            name = "claude-test",
            transport = ProcessCliTransport.Default,
            apiKey = "test-key"
        )
        assertNotNull(strategy)
        assertEquals("claude-test", strategy.name)
    }

    @Test
    fun testClaudeGeneric() {
        val strategy = AIAgentCliStrategy.claude<TestInput>(
            name = "claude-generic",
            transport = ProcessCliTransport.Default,
            apiKey = "test-key",
            generateRequest = ::generateRequest
        )
        assertNotNull(strategy)
        assertEquals("claude-generic", strategy.name)
    }

    @Test
    fun testClaudeStructured() {
        val structure = JsonStructure.create<TestOutput>()
        val strategy = AIAgentCliStrategy.claude(
            name = "claude-structured",
            transport = ProcessCliTransport.Default,
            apiKey = "test-key",
            structure = structure,
            generateRequest = ::generateRequest
        )
        assertNotNull(strategy)
        assertEquals("claude-structured", strategy.name)
    }

    @Test
    fun testClaudeReified() {
        val strategy = AIAgentCliStrategy.claude<TestInput, TestOutput>(
            name = "claude-reified",
            transport = ProcessCliTransport.Default,
            apiKey = "test-key",
            generateRequest = ::generateRequest
        )
        assertNotNull(strategy)
        assertEquals("claude-reified", strategy.name)
    }

    @Test
    fun testCodexBasic() {
        val strategy = AIAgentCliStrategy.codex(
            name = "codex-test",
            transport = ProcessCliTransport.Default,
            apiKey = "test-key"
        )
        assertNotNull(strategy)
        assertEquals("codex-test", strategy.name)
    }

    @Test
    fun testCodexGeneric() {
        val strategy = AIAgentCliStrategy.codex(
            name = "codex-generic",
            transport = ProcessCliTransport.Default,
            apiKey = "test-key",
            generateRequest = ::generateRequest
        )
        assertNotNull(strategy)
        assertEquals("codex-generic", strategy.name)
    }

    @Test
    fun testClaudeFullParams() {
        val strategy = AIAgentCliStrategy.claude(
            name = "claude-full",
            transport = ProcessCliTransport.Default,
            apiKey = "test-key",
            permissionMode = ClaudePermissionMode.AcceptEdits,
            additionalFlags = listOf("--flag"),
            workspace = "/tmp",
            timeout = 30.seconds
        )
        assertNotNull(strategy)
        assertEquals("claude-full", strategy.name)
    }

    @Test
    fun testCodexFullParams() {
        val strategy = AIAgentCliStrategy.codex(
            name = "codex-full",
            transport = ProcessCliTransport.Default,
            apiKey = "test-key",
            sandbox = CodexSandboxMode.WorkspaceWrite,
            askForApproval = CodexApprovalPolicy.Untrusted,
            additionalFlags = listOf("--flag"),
            workspace = "/tmp",
            timeout = 30.seconds
        )
        assertNotNull(strategy)
        assertEquals("codex-full", strategy.name)
    }

    @Test
    fun testClaudeBuilder() {
        val strategy = AIAgentCliStrategy.builder("claude-builder")
            .claude()
            .transport(ProcessCliTransport.Default)
            .apiKey("test-key")
            .permissionMode(ClaudePermissionMode.AcceptEdits)
            .additionalFlags(listOf("--builder"))
            .workspace("/builder")
            .timeout(10.seconds)
            .build()

        assertNotNull(strategy)
        assertEquals("claude-builder", strategy.name)
    }

    @Test
    fun testClaudeStructuredBuilder() {
        val strategy = AIAgentCliStrategy.builder("claude-structured-builder")
            .claude()
            .transport(ProcessCliTransport.Default)
            .structure(JsonStructure.create<TestOutput>())
            .apiKey("test-key")
            .build()

        assertNotNull(strategy)
        assertEquals("claude-structured-builder", strategy.name)
    }

    @Test
    fun testCodexBuilder() {
        val strategy = AIAgentCliStrategy.builder("codex-builder")
            .codex()
            .transport(ProcessCliTransport.Default)
            .apiKey("test-key")
            .sandbox(CodexSandboxMode.ReadOnly)
            .askForApproval(CodexApprovalPolicy.Never)
            .build()

        assertNotNull(strategy)
        assertEquals("codex-builder", strategy.name)
    }

    @Test
    fun testCodexGenericBuilder() {
        val strategy = AIAgentCliStrategy.builder("codex-generic-builder")
            .codex()
            .transport(ProcessCliTransport.Default)
            .generateRequest<TestInput> { _, input -> input.request }
            .build()

        assertNotNull(strategy)
        assertEquals("codex-generic-builder", strategy.name)
    }
}
