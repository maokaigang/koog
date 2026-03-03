package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.context.AIAgentCliContext
import ai.koog.cli.transport.CliTransport
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.Structure
import ai.koog.prompt.structure.json.JsonStructure
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * Base class for building a Claude strategy.
 */
public abstract class ClaudeStrategyBuilderBase<Self : ClaudeStrategyBuilderBase<Self>> internal constructor(
    internal val name: String,
    internal var transport: CliTransport? = null,
    internal var apiKey: String? = null,
    internal var permissionMode: ClaudePermissionMode? = null,
    internal var additionalFlags: List<String> = emptyList(),
    internal var workspace: String = ".",
    internal var timeout: Duration? = null
) {

    internal abstract fun self(): Self

    /**
     * Sets the CLI transport.
     */
    public fun transport(transport: CliTransport): Self = self().apply {
        this.transport = transport
    }

    /**
     * Sets the API key.
     */
    public fun apiKey(apiKey: String?): Self = self().apply {
        this.apiKey = apiKey
    }

    /**
     * Sets the permission mode.
     */
    public fun permissionMode(mode: ClaudePermissionMode): Self = self().apply {
        this.permissionMode = mode
    }

    /**
     * Sets additional flags.
     */
    public fun additionalFlags(flags: List<String>): Self = self().apply {
        this.additionalFlags = flags
    }

    /**
     * Sets the workspace directory.
     */
    public fun workspace(workspace: String): Self = self().apply {
        this.workspace = workspace
    }

    /**
     * Sets the execution timeout.
     */
    public fun timeout(timeout: Duration): Self = self().apply {
        this.timeout = timeout
    }
}

/**
 * Default builder for Claude strategy with string input and response.
 */
public class ClaudeStrategyBuilder internal constructor(
    name: String,
) : ClaudeStrategyBuilderBase<ClaudeStrategyBuilder>(name) {
    override fun self(): ClaudeStrategyBuilder = this

    /**
     * Sets the structured output.
     */
    @OptIn(InternalSerializationApi::class)
    public fun <Output : Any> structure(
        outputClass: KClass<Output>
    ): ClaudeStrategyStructuredOutputBuilder<Output> {
        return structure(JsonStructure.create(serializer = outputClass.serializer()))
    }

    /**
     * Sets the structured output.
     */
    public fun <Output> structure(
        structure: Structure<Output, LLMParams.Schema.JSON>
    ): ClaudeStrategyStructuredOutputBuilder<Output> = ClaudeStrategyStructuredOutputBuilder(
        name = name,
        transport = transport,
        apiKey = apiKey,
        permissionMode = permissionMode,
        additionalFlags = additionalFlags,
        workspace = workspace,
        timeout = timeout,
        structure = structure
    )

    /**
     * Configures a custom request generator for the strategy.
     */
    public fun <Input> generateRequest(
        generateRequest: GenerateRequest<Input>
    ): ClaudeStrategyGenericInputBuilder<Input> = ClaudeStrategyGenericInputBuilder(
        name = name,
        transport = transport,
        apiKey = apiKey,
        permissionMode = permissionMode,
        additionalFlags = additionalFlags,
        workspace = workspace,
        timeout = timeout,
        generateRequest = generateRequest
    )

    /**
     * Builds the Claude strategy.
     */
    public fun build(): AIAgentCliStrategy<String, CliAIAgentResponse> = AIAgentCliStrategy.claude(
        name = name,
        transport = requireNotNull(transport) { "Transport is required" },
        apiKey = apiKey,
        permissionMode = permissionMode,
        additionalFlags = additionalFlags,
        workspace = workspace,
        timeout = timeout,
    )
}

/**
 * Builder for Claude strategy with a generic input type.
 */
public class ClaudeStrategyGenericInputBuilder<Input> internal constructor(
    name: String,
    transport: CliTransport?,
    apiKey: String?,
    permissionMode: ClaudePermissionMode?,
    additionalFlags: List<String>,
    workspace: String,
    timeout: Duration?,
    private val generateRequest: GenerateRequest<Input>
) : ClaudeStrategyBuilderBase<ClaudeStrategyGenericInputBuilder<Input>>(
    name = name,
    transport = transport,
    apiKey = apiKey,
    permissionMode = permissionMode,
    additionalFlags = additionalFlags,
    workspace = workspace,
    timeout = timeout,
) {
    override fun self(): ClaudeStrategyGenericInputBuilder<Input> = this

    /**
     * Sets the structured output.
     */
    @OptIn(InternalSerializationApi::class)
    public fun <Output : Any> structure(
        outputClass: KClass<Output>
    ): ClaudeStrategyGenericInputStructuredOutputBuilder<Input, Output> {
        return structure(JsonStructure.create(serializer = outputClass.serializer()))
    }

    /**
     * Sets the structured output.
     */
    public fun <Output> structure(
        structure: Structure<Output, LLMParams.Schema.JSON>
    ): ClaudeStrategyGenericInputStructuredOutputBuilder<Input, Output> =
        ClaudeStrategyGenericInputStructuredOutputBuilder(
            name = name,
            transport = transport,
            apiKey = apiKey,
            permissionMode = permissionMode,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            generateRequest = generateRequest,
            structure = structure
        )

    /**
     * Builds the Claude strategy with generic input.
     */
    public fun build(): AIAgentCliStrategy<Input, CliAIAgentResponse> = AIAgentCliStrategy.claude<Input>(
        name = name,
        transport = requireNotNull(transport) { "Transport is required" },
        apiKey = apiKey,
        permissionMode = permissionMode,
        additionalFlags = additionalFlags,
        workspace = workspace,
        timeout = timeout,
        generateRequest = generateRequest,
    )
}

/**
 * Builder for Claude strategy with structured output.
 */
public class ClaudeStrategyStructuredOutputBuilder<Output> internal constructor(
    name: String,
    transport: CliTransport?,
    apiKey: String?,
    permissionMode: ClaudePermissionMode?,
    additionalFlags: List<String>,
    workspace: String,
    timeout: Duration?,
    private val structure: Structure<Output, LLMParams.Schema.JSON>
) : ClaudeStrategyBuilderBase<ClaudeStrategyStructuredOutputBuilder<Output>>(
    name = name,
    transport = transport,
    apiKey = apiKey,
    permissionMode = permissionMode,
    additionalFlags = additionalFlags,
    workspace = workspace,
    timeout = timeout,
) {
    override fun self(): ClaudeStrategyStructuredOutputBuilder<Output> = this

    /**
     * Configures a custom request generator for the strategy.
     */
    public fun <Input> generateRequest(
        generateRequest: (AIAgentCliContext, Input) -> String
    ): ClaudeStrategyGenericInputStructuredOutputBuilder<Input, Output> = ClaudeStrategyGenericInputStructuredOutputBuilder(
        name = name,
        transport = transport,
        apiKey = apiKey,
        permissionMode = permissionMode,
        additionalFlags = additionalFlags,
        workspace = workspace,
        timeout = timeout,
        generateRequest = generateRequest,
        structure = structure
    )

    /**
     * Builds the Claude strategy with structured output.
     */
    public fun build(): AIAgentCliStrategy<String, CliAgentStructuredResponse<Output>> = AIAgentCliStrategy.claude(
        name = name,
        transport = requireNotNull(transport) { "Transport is required" },
        apiKey = apiKey,
        structure = structure,
        permissionMode = permissionMode,
        additionalFlags = additionalFlags,
        workspace = workspace,
        timeout = timeout,
    )
}

/**
 * Builder for Claude strategy with both generic input and structured output.
 */
public class ClaudeStrategyGenericInputStructuredOutputBuilder<Input, Output> internal constructor(
    name: String,
    transport: CliTransport?,
    apiKey: String?,
    permissionMode: ClaudePermissionMode?,
    additionalFlags: List<String>,
    workspace: String,
    timeout: Duration?,
    private val generateRequest: GenerateRequest<Input>,
    private val structure: Structure<Output, LLMParams.Schema.JSON>
) : ClaudeStrategyBuilderBase<ClaudeStrategyGenericInputStructuredOutputBuilder<Input, Output>>(
    name = name,
    transport = transport,
    apiKey = apiKey,
    permissionMode = permissionMode,
    additionalFlags = additionalFlags,
    workspace = workspace,
    timeout = timeout,
) {
    override fun self(): ClaudeStrategyGenericInputStructuredOutputBuilder<Input, Output> = this

    /**
     * Builds the Claude strategy with generic input and structured output.
     */
    public fun build(): AIAgentCliStrategy<Input, CliAgentStructuredResponse<Output>> = AIAgentCliStrategy.claude(
        name = name,
        transport = requireNotNull(transport) { "Transport is required" },
        apiKey = apiKey,
        structure = structure,
        permissionMode = permissionMode,
        additionalFlags = additionalFlags,
        workspace = workspace,
        timeout = timeout,
        generateRequest = generateRequest,
    )
}
