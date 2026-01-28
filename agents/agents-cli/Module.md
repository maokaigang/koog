# Module agents-cli

Wrapper for running AI agents as external CLI processes.

### Overview

The agents-cli module provides a foundation for integrating third-party AI agents that operate through a Command Line
Interface (CLI). It allows you to execute these agents within your Kotlin application, capturing their output (
stdout/stderr) and events in a structured way.

Key features include:

- Base `CliAIAgent` for implementing custom CLI agent wrappers.
- `CliTransport` abstraction supporting local process execution and Docker-based execution.
- Real-time event streaming from CLI processes.
- Built-in wrappers for popular agents like `ClaudeCodeAgent` and `CodexAgent`.
- Automatic availability checks for required binaries.

### Using in your project

To use the agents-cli module in your project, add the following dependency:

```kotlin
dependencies {
    implementation("ai.koog.agents:agents-cli:$version")
}
```

> Note: you also need to make sure that the CLI agent binary is available on your system, or in a Docker container.

### Implementing a Custom CLI Agent

To implement your own CLI agent, extend the `CliAIAgent` class and implement the `extractResult` method:

```kotlin
class MyCustomCliAgent(
    config: MyConfig
) : CliAIAgent<String>(
    binary = "my-cli-tool",
    commandOptions = listOf("--some-flag"),
    workspace = config.workspace
) {
    override fun extractResult(events: List<AgentEvent>): String? {
        // Extract the final result from the captured stdout/stderr events
        return events.filterIsInstance<AgentEvent.Stdout>()
            .lastOrNull()?.content
    }
}
```

### Ready-to-use Agents

#### ClaudeCodeAgent

A wrapper for the Anthropic Claude CLI.

```kotlin
val agent = ClaudeCodeAgent.invoke(
    apiKey = "your-api-key",
    model = "claude-3-5-sonnet-latest",
    workspace = File("./my-project")
)

val result = agent.run("Refactor the login logic in AuthService.kt")
```

#### CodexAgent

A wrapper for the OpenAI Codex CLI.

```kotlin
val agent = CodexAgent(
    apiKey = "your-api-key",
    sandbox = CodexSandboxMode.WorkspaceWrite
)

val result = agent.run("Create a unit test for the MathUtils class")
```

### Transports

The module supports different ways to run the CLI process:

- `CliTransport.Default`: Runs the binary as a local process.
- `DockerCliTransport`: Runs the binary inside a Docker container (useful for sandboxing).

```kotlin
val agent = ClaudeCodeAgent(
    transport = DockerCliTransport(image = "anthropic/claude-code"),
    workspace = File("./sandbox")
)
```
