# Module cli-transport

Wrapper for external cli process run and management.

### Overview

The agents-cli module provides a foundation for integrating third-party AI agents that operate through a Command Line
Interface (CLI). It allows you to execute these agents within your Kotlin application, capturing their output (
stdout/stderr) and events in a structured way.

Key features include:

- `CliTransport` abstraction 

JVM-only implementations:
- Local process execution
- Docker-based execution.

### Using in your project

To use the cli-transport module in your project, add the following dependency:

```kotlin
dependencies {
    implementation("ai.koog.agents:cli-transport:$version")
}
```

### Transports

The module supports different ways to run the CLI process:

- `CliTransport.Default`: Runs the binary as a local process.
- `DockerCliTransport`: Runs the binary inside a Docker container (useful for sandboxing).
