<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:56:31+00:00", "source_path": "examples/Banking.md", "source_sha256": "83421e6a40dc2246f3a1c96fb0e17a1d5bde632dfab2f89223798a4acc8f6171", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 使用 Koog 构建 AI 银行助手 { #building-an-ai-banking-assistant-with-koog }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/Banking.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/Banking.ipynb
){ .md-button }

在本教程中，我们将使用 **Koog** 代理在 Kotlin 中构建一个小型银行助手。
您将学习如何：
- 定义领域模型和示例数据
- 为**转账**和**交易分析**提供面向能力的工具
- 对用户意图进行分类（转账与分析）
- 以两种风格编排调用：
  1) 图/子图策略
  2) “代理即工具”

最终，您将能够将自由格式的用户请求路由到正确的工具，并生成有用、可审计的响应。

## 设置与依赖 { #setup-dependencies }

我们将使用 Kotlin Notebook 内核。请确保您的 Koog 工件可从 Maven Central 解析，
并且您的 LLM 提供者密钥可通过 `OPENAI_API_KEY` 获取。

```kotlin
%useLatestDescriptors
%use datetime

// uncomment this for using koog from Maven Central
// %use koog
```

```kotlin
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor

val apiKey = System.getenv("OPENAI_API_KEY") ?: error("Please set OPENAI_API_KEY environment variable")
val openAIExecutor = simpleOpenAIExecutor(apiKey)
```

## 定义系统提示 { #defining-the-system-prompt }

精心设计的系统提示有助于 AI 理解其角色和约束。此提示将指导我们所有代理的行为。

```kotlin
val bankingAssistantSystemPrompt = """
    |You are a banking assistant interacting with a user (userId=123).
    |Your goal is to understand the user's request and determine whether it can be fulfilled using the available tools.
    |
    |If the task can be accomplished with the provided tools, proceed accordingly,
    |at the end of the conversation respond with: "Task completed successfully."
    |If the task cannot be performed with the tools available, respond with: "Can't perform the task."
""".trimMargin()
```

## 领域模型与示例数据 { #domain-model-sample-data }

首先，让我们定义领域模型和示例数据。我们将使用支持序列化的 Kotlin 数据类。

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val id: Int,
    val name: String,
    val surname: String? = null,
    val phoneNumber: String
)

val contactList = listOf(
    Contact(100, "Alice", "Smith", "+1 415 555 1234"),
    Contact(101, "Bob", "Johnson", "+49 151 23456789"),
    Contact(102, "Charlie", "Williams", "+36 20 123 4567"),
    Contact(103, "Daniel", "Anderson", "+46 70 123 45 67"),
    Contact(104, "Daniel", "Garcia", "+34 612 345 678"),
)

val contactById = contactList.associateBy(Contact::id)
```

## 工具：转账 { #tools-money-transfer }

工具应具备**纯粹性**和可预测性。

我们建模两个“软契约”：
- `chooseRecipient` 在检测到歧义时返回*候选对象*。
- `sendMoney` 支持 `confirmed` 标志。如果 `false`，它会要求代理向用户确认。

```kotlin
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

@LLMDescription("Tools for money transfer operations.")
class MoneyTransferTools : ToolSet {

    @Tool
    @LLMDescription(
        """
        Returns the list of contacts for the given user.
        The user in this demo is always userId=123.
        """
    )
    fun getContacts(
        @LLMDescription("The unique identifier of the user whose contact list is requested.") userId: Int
    ): String = buildString {
        contactList.forEach { c ->
            appendLine("${c.id}: ${c.name} ${c.surname ?: ""} (${c.phoneNumber})")
        }
    }.trimEnd()

    @Tool
    @LLMDescription("Returns the current balance (demo value).")
    fun getBalance(
        @LLMDescription("The unique identifier of the user.") userId: Int
    ): String = "Balance: 200.00 EUR"

    @Tool
    @LLMDescription("Returns the default user currency (demo value).")
    fun getDefaultCurrency(
        @LLMDescription("The unique identifier of the user.") userId: Int
    ): String = "EUR"

    @Tool
    @LLMDescription("Returns a demo FX rate between two ISO currencies (e.g. EUR→USD).")
    fun getExchangeRate(
        @LLMDescription("Base currency (e.g., EUR).") from: String,
        @LLMDescription("Target currency (e.g., USD).") to: String
    ): String = when (from.uppercase() to to.uppercase()) {
        "EUR" to "USD" -> "1.10"
        "EUR" to "GBP" -> "0.86"
        "GBP" to "EUR" -> "1.16"
        "USD" to "EUR" -> "0.90"
        else -> "No information about exchange rate available."
    }

    @Tool
    @LLMDescription(
        """
        Returns a ranked list of possible recipients for an ambiguous name.
        The agent should ask the user to pick one and then use the selected contact id.
        """
    )
    fun chooseRecipient(
        @LLMDescription("An ambiguous or partial contact name.") confusingRecipientName: String
    ): String {
        val matches = contactList.filter { c ->
            c.name.contains(confusingRecipientName, ignoreCase = true) ||
                (c.surname?.contains(confusingRecipientName, ignoreCase = true) ?: false)
        }
        if (matches.isEmpty()) {
            return "No candidates found for '$confusingRecipientName'. Use getContacts and ask the user to choose."
        }
        return matches.mapIndexed { idx, c ->
            "${idx + 1}. ${c.id}: ${c.name} ${c.surname ?: ""} (${c.phoneNumber})"
        }.joinToString("\n")
    }

    @Tool
    @LLMDescription(
        """
        Sends money from the user to a contact.
        If confirmed=false, return "REQUIRES_CONFIRMATION" with a human-readable summary.
        The agent should confirm with the user before retrying with confirmed=true.
        """
    )
    fun sendMoney(
        @LLMDescription("Sender user id.") senderId: Int,
        @LLMDescription("Amount in sender's default currency.") amount: Double,
        @LLMDescription("Recipient contact id.") recipientId: Int,
        @LLMDescription("Short purpose/description.") purpose: String,
        @LLMDescription("Whether the user already confirmed this transfer.") confirmed: Boolean = false
    ): String {
        val recipient = contactById[recipientId] ?: return "Invalid recipient."
        val summary = "Transfer €%.2f to %s %s (%s) for \"%s\"."
            .format(amount, recipient.name, recipient.surname ?: "", recipient.phoneNumber, purpose)

        if (!confirmed) {
            return "REQUIRES_CONFIRMATION: $summary"
        }

        // In a real system this is where you'd call a payment API.
        return "Money was sent. $summary"
    }
}
```

## 创建您的第一个代理 { #creating-your-first-agent }

现在让我们创建一个使用转账工具的代理。
代理将 LLM 与工具结合以完成任务。

```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgentService
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.ext.tool.AskUser
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import kotlinx.coroutines.runBlocking

val transferAgentService = AIAgentService(
    executor = openAIExecutor,
    llmModel = OpenAIModels.Chat.GPT4oMini,
    systemPrompt = bankingAssistantSystemPrompt,
    temperature = 0.0,  // Use deterministic responses for financial operations
    toolRegistry = ToolRegistry {
        tool(AskUser)
        tools(MoneyTransferTools().asTools())
    }
)

// Test the agent with various scenarios
println("Banking Assistant started")
val message = "Send 25 euros to Daniel for dinner at the restaurant."

// Other test messages you can try:
// - "Send 50 euros to Alice for the concert tickets"
// - "What's my current balance?"
// - "Transfer 100 euros to Bob for the shared vacation expenses"

runBlocking {
    val result = transferAgentService.createAgentAndRun(message)
    result
}
```

    银行助手已启动
    有两个名为 Daniel 的联系人。请确认您想转账给哪一位：
    1. Daniel Anderson (+46 70 123 45 67)
    2. Daniel Garcia (+34 612 345 678)
    请确认向 Daniel Garcia (+34 612 345 678) 转账 €25.00，用途为“餐厅晚餐”。





    任务成功完成。

## 添加交易分析 { #adding-transaction-analytics }

让我们通过交易分析工具扩展助手的能力。
首先，定义交易领域模型。

```kotlin
@Serializable
enum class TransactionCategory(val title: String) {
    FOOD_AND_DINING("Food & Dining"),
    SHOPPING("Shopping"),
    TRANSPORTATION("Transportation"),
    ENTERTAINMENT("Entertainment"),
    GROCERIES("Groceries"),
    HEALTH("Health"),
    UTILITIES("Utilities"),
    HOME_IMPROVEMENT("Home Improvement");

    companion object {
        fun fromString(value: String): TransactionCategory? =
            entries.find { it.title.equals(value, ignoreCase = true) }

        fun availableCategories(): String =
            entries.joinToString(", ") { it.title }
    }
}

@Serializable
data class Transaction(
    val merchant: String,
    val amount: Double,
    val category: TransactionCategory,
    val date: LocalDateTime
)
```

### 示例交易数据 { #sample-transaction-data }

```kotlin
val transactionAnalysisPrompt = """
Today is 2025-05-22.
Available categories for transactions: ${TransactionCategory.availableCategories()}
"""

val sampleTransactions = listOf(
    Transaction("Starbucks", 5.99, TransactionCategory.FOOD_AND_DINING, LocalDateTime(2025, 5, 22, 8, 30, 0, 0)),
    Transaction("Amazon", 129.99, TransactionCategory.SHOPPING, LocalDateTime(2025, 5, 22, 10, 15, 0, 0)),
    Transaction(
        "Shell Gas Station",
        45.50,
        TransactionCategory.TRANSPORTATION,
        LocalDateTime(2025, 5, 21, 18, 45, 0, 0)
    ),
    Transaction("Netflix", 15.99, TransactionCategory.ENTERTAINMENT, LocalDateTime(2025, 5, 21, 12, 0, 0, 0)),
    Transaction("AMC Theaters", 32.50, TransactionCategory.ENTERTAINMENT, LocalDateTime(2025, 5, 20, 19, 30, 0, 0)),
    Transaction("Whole Foods", 89.75, TransactionCategory.GROCERIES, LocalDateTime(2025, 5, 20, 16, 20, 0, 0)),
    Transaction("Target", 67.32, TransactionCategory.SHOPPING, LocalDateTime(2025, 5, 20, 14, 30, 0, 0)),
    Transaction("CVS Pharmacy", 23.45, TransactionCategory.HEALTH, LocalDateTime(2025, 5, 19, 11, 25, 0, 0)),
    Transaction("Subway", 12.49, TransactionCategory.FOOD_AND_DINING, LocalDateTime(2025, 5, 19, 13, 15, 0, 0)),
    Transaction("Spotify Premium", 9.99, TransactionCategory.ENTERTAINMENT, LocalDateTime(2025, 5, 19, 14, 15, 0, 0)),
    Transaction("AT&T", 85.00, TransactionCategory.UTILITIES, LocalDateTime(2025, 5, 18, 9, 0, 0, 0)),
    Transaction("Home Depot", 156.78, TransactionCategory.HOME_IMPROVEMENT, LocalDateTime(2025, 5, 18, 15, 45, 0, 0)),
    Transaction("Amazon", 129.99, TransactionCategory.SHOPPING, LocalDateTime(2025, 5, 17, 10, 15, 0, 0)),
    Transaction("Starbucks", 5.99, TransactionCategory.FOOD_AND_DINING, LocalDateTime(2025, 5, 17, 8, 30, 0, 0)),
    Transaction("Whole Foods", 89.75, TransactionCategory.GROCERIES, LocalDateTime(2025, 5, 16, 16, 20, 0, 0)),
    Transaction("CVS Pharmacy", 23.45, TransactionCategory.HEALTH, LocalDateTime(2025, 5, 15, 11, 25, 0, 0)),
    Transaction("AT&T", 85.00, TransactionCategory.UTILITIES, LocalDateTime(2025, 5, 14, 9, 0, 0, 0)),
    Transaction("Xbox Game Pass", 14.99, TransactionCategory.ENTERTAINMENT, LocalDateTime(2025, 5, 14, 16, 45, 0, 0)),
    Transaction("Aldi", 76.45, TransactionCategory.GROCERIES, LocalDateTime(2025, 5, 13, 17, 30, 0, 0)),
    Transaction("Chipotle", 15.75, TransactionCategory.FOOD_AND_DINING, LocalDateTime(2025, 5, 13, 12, 45, 0, 0)),
    Transaction("Best Buy", 299.99, TransactionCategory.SHOPPING, LocalDateTime(2025, 5, 12, 14, 20, 0, 0)),
    Transaction("Olive Garden", 89.50, TransactionCategory.FOOD_AND_DINING, LocalDateTime(2025, 5, 12, 19, 15, 0, 0)),
    Transaction("Whole Foods", 112.34, TransactionCategory.GROCERIES, LocalDateTime(2025, 5, 11, 10, 30, 0, 0)),
    Transaction("Old Navy", 45.99, TransactionCategory.SHOPPING, LocalDateTime(2025, 5, 11, 13, 45, 0, 0)),
    Transaction("Panera Bread", 18.25, TransactionCategory.FOOD_AND_DINING, LocalDateTime(2025, 5, 10, 11, 30, 0, 0)),
    Transaction("Costco", 245.67, TransactionCategory.GROCERIES, LocalDateTime(2025, 5, 10, 15, 20, 0, 0)),
    Transaction("Five Guys", 22.50, TransactionCategory.FOOD_AND_DINING, LocalDateTime(2025, 5, 9, 18, 30, 0, 0)),
    Transaction("Macy's", 156.78, TransactionCategory.SHOPPING, LocalDateTime(2025, 5, 9, 14, 15, 0, 0)),
    Transaction("Hulu Plus", 12.99, TransactionCategory.ENTERTAINMENT, LocalDateTime(2025, 5, 8, 20, 0, 0, 0)),
    Transaction("Whole Foods", 94.23, TransactionCategory.GROCERIES, LocalDateTime(2025, 5, 8, 16, 45, 0, 0)),
    Transaction("Texas Roadhouse", 78.90, TransactionCategory.FOOD_AND_DINING, LocalDateTime(2025, 5, 8, 19, 30, 0, 0)),
    Transaction("Walmart", 167.89, TransactionCategory.SHOPPING, LocalDateTime(2025, 5, 7, 11, 20, 0, 0)),
    Transaction("Chick-fil-A", 14.75, TransactionCategory.FOOD_AND_DINING, LocalDateTime(2025, 5, 7, 12, 30, 0, 0)),
    Transaction("Aldi", 82.45, TransactionCategory.GROCERIES, LocalDateTime(2025, 5, 6, 15, 45, 0, 0)),
    Transaction("TJ Maxx", 67.90, TransactionCategory.SHOPPING, LocalDateTime(2025, 5, 6, 13, 20, 0, 0)),
    Transaction("P.F. Chang's", 95.40, TransactionCategory.FOOD_AND_DINING, LocalDateTime(2025, 5, 5, 19, 15, 0, 0)),
    Transaction("Whole Foods", 78.34, TransactionCategory.GROCERIES, LocalDateTime(2025, 5, 4, 14, 30, 0, 0)),
    Transaction("H&M", 89.99, TransactionCategory.SHOPPING, LocalDateTime(2025, 5, 3, 16, 20, 0, 0)),
    Transaction("Red Lobster", 112.45, TransactionCategory.FOOD_AND_DINING, LocalDateTime(2025, 5, 2, 18, 45, 0, 0)),
    Transaction("Whole Foods", 67.23, TransactionCategory.GROCERIES, LocalDateTime(2025, 5, 2, 11, 30, 0, 0)),
    Transaction("Marshalls", 123.45, TransactionCategory.SHOPPING, LocalDateTime(2025, 5, 1, 15, 20, 0, 0)),
    Transaction(
        "Buffalo Wild Wings",
        45.67,
        TransactionCategory.FOOD_AND_DINING,
        LocalDateTime(2025, 5, 1, 19, 30, 0, 0)
    ),
    Transaction("Aldi", 145.78, TransactionCategory.GROCERIES, LocalDateTime(2025, 5, 1, 10, 15, 0, 0))
)
```

## 交易分析工具 { #transaction-analysis-tools }

```kotlin
@LLMDescription("Tools for analyzing transaction history")
class TransactionAnalysisTools : ToolSet {

    @Tool
    @LLMDescription(
        """
        Retrieves transactions filtered by userId, category, start date, and end date.
        All parameters are optional. If no parameters are provided, all transactions are returned.
        Dates should be in the format YYYY-MM-DD.
        """
    )
    fun getTransactions(
        @LLMDescription("The ID of the user whose transactions to retrieve.")
        userId: String? = null,
        @LLMDescription("The category to filter transactions by (e.g., 'Food & Dining').")
        category: String? = null,
        @LLMDescription("The start date to filter transactions by, in the format YYYY-MM-DD.")
        startDate: String? = null,
        @LLMDescription("The end date to filter transactions by, in the format YYYY-MM-DD.")
        endDate: String? = null
    ): String {
        var filteredTransactions = sampleTransactions

        // Validate userId (in production, this would query a real database)
        if (userId != null && userId != "123") {
            return "No transactions found for user $userId."
        }

        // Apply category filter
        category?.let { cat ->
            val categoryEnum = TransactionCategory.fromString(cat)
                ?: return "Invalid category: $cat. Available: ${TransactionCategory.availableCategories()}"
            filteredTransactions = filteredTransactions.filter { it.category == categoryEnum }
        }

        // Apply date range filters
        startDate?.let { date ->
            val startDateTime = parseDate(date, startOfDay = true)
            filteredTransactions = filteredTransactions.filter { it.date >= startDateTime }
        }

        endDate?.let { date ->
            val endDateTime = parseDate(date, startOfDay = false)
            filteredTransactions = filteredTransactions.filter { it.date <= endDateTime }
        }

        if (filteredTransactions.isEmpty()) {
            return "No transactions found matching the specified criteria."
        }

        return filteredTransactions.joinToString("\n") { transaction ->
            "${transaction.date}: ${transaction.merchant} - " +
                "$${transaction.amount} (${transaction.category.title})"
        }
    }

    @Tool
    @LLMDescription("Calculates the sum of an array of double numbers.")
    fun sumArray(
        @LLMDescription("Comma-separated list of double numbers to sum (e.g., '1.5,2.3,4.7').")
        numbers: String
    ): String {
        val numbersList = numbers.split(",")
            .mapNotNull { it.trim().toDoubleOrNull() }
        val sum = numbersList.sum()
        return "Sum: $%.2f".format(sum)
    }

    // Helper function to parse dates
    private fun parseDate(dateStr: String, startOfDay: Boolean): LocalDateTime {
        val parts = dateStr.split("-").map { it.toInt() }
        require(parts.size == 3) { "Invalid date format. Use YYYY-MM-DD" }

        return if (startOfDay) {
            LocalDateTime(parts[0], parts[1], parts[2], 0, 0, 0, 0)
        } else {
            LocalDateTime(parts[0], parts[1], parts[2], 23, 59, 59, 999999999)
        }
    }
}
```

```kotlin
val analysisAgentService = AIAgentService(
    executor = openAIExecutor,
    llmModel = OpenAIModels.Chat.GPT4oMini,
    systemPrompt = "$bankingAssistantSystemPrompt\n$transactionAnalysisPrompt",
    temperature = 0.0,
    toolRegistry = ToolRegistry {
        tools(TransactionAnalysisTools().asTools())
    }
)

println("Transaction Analysis Assistant started")
val analysisMessage = "How much have I spent on restaurants this month?"

// Other queries to try:
// - "What's my maximum check at a restaurant this month?"
// - "How much did I spend on groceries in the first week of May?"
// - "What's my total spending on entertainment in May?"
// - "Show me all transactions from last week"

runBlocking {
    val result = analysisAgentService.createAgentAndRun(analysisMessage)
    result
}
```

    交易分析助手已启动





    本月您在餐厅总共消费了 $517.64。
    
    任务成功完成。

## 使用图构建代理 { #building-an-agent-with-graph }

现在让我们将专用代理组合成一个图代理，以便将请求路由到适当的处理程序。

### 请求分类 { #request-classification }

首先，我们需要一种对传入请求进行分类的方法：

```kotlin
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("unused")
@SerialName("UserRequestType")
@Serializable
@LLMDescription("Type of user request: Transfer or Analytics")
enum class RequestType { Transfer, Analytics }

@Serializable
@LLMDescription("The bank request that was classified by the agent.")
data class ClassifiedBankRequest(
    @property:LLMDescription("Type of request: Transfer or Analytics")
    val requestType: RequestType,
    @property:LLMDescription("Actual request to be performed by the banking application")
    val userRequest: String
)

```

### 共享工具注册表 { #shared-tool-registry }

```kotlin
// Create a comprehensive tool registry for the multi-agent system
val toolRegistry = ToolRegistry {
    tool(AskUser)  // Allow agents to ask for clarification
    tools(MoneyTransferTools().asTools())
    tools(TransactionAnalysisTools().asTools())
}
```

## 代理策略 { #agent-strategy }

现在我们将创建一个编排多个节点的策略：

```kotlin
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.structure.StructureFixingParser

val strategy = strategy<String, String>("banking assistant") {

    // Subgraph for classifying user requests
    val classifyRequest by subgraph<String, ClassifiedBankRequest>(
        tools = listOf(AskUser)
    ) {
        // Use structured output to ensure proper classification
        val requestClassification by nodeLLMRequestStructured<ClassifiedBankRequest>(
            examples = listOf(
                ClassifiedBankRequest(
                    requestType = RequestType.Transfer,
                    userRequest = "Send 25 euros to Daniel for dinner at the restaurant."
                ),
                ClassifiedBankRequest(
                    requestType = RequestType.Analytics,
                    userRequest = "Provide transaction overview for the last month"
                )
            ),
            fixingParser = StructureFixingParser(
                model = OpenAIModels.Chat.GPT4oMini,
                retries = 2,
            )
        )

        val callLLM by nodeLLMRequest()
        val callAskUserTool by nodeExecuteTool()

        // Define the flow
        edge(nodeStart forwardTo requestClassification)

        edge(
            requestClassification forwardTo nodeFinish
                onCondition { it.isSuccess }
                transformed { it.getOrThrow().data }
        )

        edge(
            requestClassification forwardTo callLLM
                onCondition { it.isFailure }
                transformed { "Failed to understand the user's intent" }
        )

        edge(callLLM forwardTo callAskUserTool onToolCall { true })

        edge(
            callLLM forwardTo callLLM onAssistantMessage { true }
                transformed { "Please call `${AskUser.name}` tool instead of chatting" }
        )

        edge(callAskUserTool forwardTo requestClassification
            transformed { it.result.toString() })
    }

    // Subgraph for handling money transfers
    val transferMoney by subgraphWithTask<ClassifiedBankRequest, String>(
        tools = MoneyTransferTools().asTools() + AskUser,
        llmModel = OpenAIModels.Chat.GPT4o  // Use more capable model for transfers
    ) { request ->
        """
        $bankingAssistantSystemPrompt
        Specifically, you need to help with the following request:
        ${request.userRequest}
        """.trimIndent()
    }

    // Subgraph for transaction analysis
    val transactionAnalysis by subgraphWithTask<ClassifiedBankRequest, String>(
        tools = TransactionAnalysisTools().asTools() + AskUser,
    ) { request ->
        """
        $bankingAssistantSystemPrompt
        $transactionAnalysisPrompt
        Specifically, you need to help with the following request:
        ${request.userRequest}
        """.trimIndent()
    }

    // Connect the subgraphs
    edge(nodeStart forwardTo classifyRequest)

    edge(classifyRequest forwardTo transferMoney
        onCondition { it.requestType == RequestType.Transfer })

    edge(classifyRequest forwardTo transactionAnalysis
        onCondition { it.requestType == RequestType.Analytics })

    // Route results to finish node
    edge(transferMoney forwardTo nodeFinish)
    edge(transactionAnalysis forwardTo nodeFinish)
}
```

```kotlin
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.prompt.dsl.prompt

val agentConfig = AIAgentConfig(
    prompt = prompt(id = "banking assistant") {
        system("$bankingAssistantSystemPrompt\n$transactionAnalysisPrompt")
    },
    model = OpenAIModels.Chat.GPT4o,
    maxAgentIterations = 50  // Allow for complex multi-step operations
)

val agent = AIAgent<String, String>(
    promptExecutor = openAIExecutor,
    strategy = strategy,
    agentConfig = agentConfig,
    toolRegistry = toolRegistry,
)
```

## 运行图代理 { #run-graph-agent }

```kotlin
println("Banking Assistant started")
val testMessage = "Send 25 euros to Daniel for dinner at the restaurant."

// Test various scenarios:
// Transfer requests:
//   - "Send 50 euros to Alice for the concert tickets"
//   - "Transfer 100 to Bob for groceries"
//   - "What's my current balance?"
//
// Analytics requests:
//   - "How much have I spent on restaurants this month?"
//   - "What's my maximum check at a restaurant this month?"
//   - "How much did I spend on groceries in the first week of May?"
//   - "What's my total spending on entertainment in May?"

runBlocking {
    val result = agent.run(testMessage)
    "Result: $result"
}
```银行助手已启动  
我找到了多个名为Daniel的联系人。请选择正确的一位：  
1. Daniel Anderson (+46 70 123 45 67)  
2. Daniel Garcia (+34 612 345 678)  
请指定正确收件人的编号。  
请确认是否要继续向Daniel Garcia发送25欧元，用途为“餐厅晚餐”。





结果：任务成功完成。



## 智能体组合——将智能体作为工具使用 { #agent-composition-using-agents-as-tools }

Koog允许您在其他智能体中将智能体作为工具使用，从而实现强大的组合模式。


```kotlin
import ai.koog.agents.core.agent.createAgentTool
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType

val classifierAgent = AIAgent(
    executor = openAIExecutor,
    llmModel = OpenAIModels.Chat.GPT4oMini,
    toolRegistry = ToolRegistry {
        tool(AskUser)

        // Convert agents into tools
        tool(
            transferAgentService.createAgentTool(
                agentName = "transferMoney",
                agentDescription = "Transfers money and handles all related operations",
                inputDescriptor = ToolParameterDescriptor(
                    name = "request",
                    description = "Transfer request from the user",
                    type = ToolParameterType.String
                )
            )
        )

        tool(
            analysisAgentService.createAgentTool(
                agentName = "analyzeTransactions",
                agentDescription = "Performs analytics on user transactions",
                inputDescriptor = ToolParameterDescriptor(
                    name = "request",
                    description = "Transaction analytics request",
                    type = ToolParameterType.String
                )
            )
        )
    },
    systemPrompt = "$bankingAssistantSystemPrompt\n$transactionAnalysisPrompt"
)
```

## 运行组合智能体 { #run-composed-agent }


```kotlin
println("Banking Assistant started")
val composedMessage = "Send 25 euros to Daniel for dinner at the restaurant."

runBlocking {
    val result = classifierAgent.run(composedMessage)
    "Result: $result"
}
```

    银行助手已启动  
    有两位名为Daniel的联系人。请确认您希望向哪一位转账：  
    1. Daniel Anderson (+46 70 123 45 67)  
    2. Daniel Garcia (+34 612 345 678)  
    请确认向Daniel Anderson (+46 70 123 45 67)转账25.00欧元，用途为“餐厅晚餐”。





    结果：无法执行任务。



## 总结 { #summary }
在本教程中，您已学习如何：

1. 创建具有清晰描述的LLM驱动工具，帮助AI理解何时及如何使用它们  
2. 构建结合LLM与工具以完成特定任务的单一功能智能体  
3. 使用策略和子图实现复杂工作流的图智能体  
4. 通过在其他智能体中作为工具使用来组合智能体  
5. 处理用户交互，包括确认和消歧操作  

## 最佳实践 { #best-practices }

1. 清晰的工具描述：编写详细的LLMDescription注解，帮助AI理解工具用途  
2. 符合Kotlin习惯：使用Kotlin特性，如数据类、扩展函数和作用域函数  
3. 错误处理：始终验证输入并提供有意义的错误信息  
4. 用户体验：对关键操作（如转账）包含确认步骤  
5. 模块化：将不同关注点分离到独立的工具和智能体中，以提高可维护性