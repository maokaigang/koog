package ai.koog.agents.cli.codex

import ai.koog.agents.cli.CliAIAgentConfig
import ai.koog.agents.cli.transport.CliTransport
import java.io.File
import kotlin.time.Duration

/**
 * Codex config.
 */
public class CodexConfig(
    binary: String = "codex",
    transport: CliTransport = CliTransport.Default,
    workspace: File = File("."),
    timeout: Duration? = null,
    public val model: String? = null,
    public val sandboxMode: SandboxMode = SandboxMode.WORKSPACE_WRITE,
    public val approvalPolicy: ApprovalPolicy = ApprovalPolicy.ALWAYS,
    public val skipGitRepoCheck: Boolean = true,
    public val apiKey: String? = null,
) : CliAIAgentConfig(binary, transport, workspace, timeout)

/**
 * Sandbox mode.
 */
public enum class SandboxMode(public val value: String) {
    WORKSPACE_WRITE("workspace-write"),
    WORKSPACE_READ("workspace-read"),
    NONE("none"),
}

/**
 * Approval policy.
 */
public enum class ApprovalPolicy(public val value: String) {
    ALWAYS("always"),
    NEVER("never"),
}
