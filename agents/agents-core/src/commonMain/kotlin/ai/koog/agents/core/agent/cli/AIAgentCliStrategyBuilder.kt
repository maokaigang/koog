package ai.koog.agents.core.agent.cli

/**
 * Builder for [AIAgentCliStrategy].
 *
 * Example usage:
 * ```
 * // Custom config
 * val strategy = AIAgentCliStrategy(name = "myAgent")
 *     .withConfig(myCustomConfig)
 *
 * // Claude
 * val claudeStrategy = AIAgentCliStrategy(name = "myAgent")
 *     .claude()
 *     .transport(myTransport)
 *     .apiKey("my-key")
 *     .permissionMode(ClaudePermissionMode.AcceptEdits)
 *     .build()
 *
 * // Claude in structured mode
 * val claudeStrategy = AIAgentCliStrategy(name = "myAgent")
 *     .claude()
 *     .transport(myTransport)
 *     .structure(myStructure)
 *
 * // Codex
 * val codexStrategy = AIAgentCliStrategy(name = "myAgent")
 *     .codex()
 *     .transport(myTransport)
 *     .sandbox(CodexSandboxMode.WorkspaceWrite)
 *     .build()
 * ```
 */
public class AIAgentCliStrategyBuilder(private val name: String) {

    /**
     * Configures the strategy to use Claude CLI.
     */
    public fun claude(): ClaudeStrategyBuilder = ClaudeStrategyBuilder(name)

    /**
     * Configures the strategy to use Codex CLI.
     */
    public fun codex(): CodexStrategyBuilder = CodexStrategyBuilder(name)

    /**
     * Builds a strategy with custom configuration.
     */
    public fun <Input, Output> withConfig(
        config: AIAgentCliStrategyConfig<Input, Output>
    ): AIAgentCliStrategy<Input, Output> = AIAgentCliStrategy(name, config)
}
