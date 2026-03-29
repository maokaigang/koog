<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:58:16+00:00", "source_path": "examples/Chess.md", "source_sha256": "7701e7da1c97228b4af2c0034874d0ac59a581335617a7c3842c636400d929bf", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 使用 Koog 框架构建 AI 国际象棋棋手 { #building-an-ai-chess-player-with-koog-framework }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/Chess.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/Chess.ipynb
){ .md-button }

本教程演示了如何使用 Koog 框架构建一个智能的国际象棋对弈智能体。我们将探讨关键概念，包括工具集成、智能体策略、内存优化以及交互式 AI 决策。

## 你将学到什么 { #what-you-ll-learn }

- 如何为复杂游戏建模领域特定的数据结构
- 创建智能体可用于与环境交互的自定义工具
- 实现具有内存管理功能的高效智能体策略
- 构建具有选择能力的交互式 AI 系统
- 针对回合制游戏优化智能体性能

## 设置 { #setup }

首先，让我们导入 Koog 框架并设置开发环境：

```kotlin
%useLatestDescriptors
%use koog
```

## 为国际象棋领域建模 { #modeling-the-chess-domain }

创建健壮的领域模型对于任何游戏 AI 都至关重要。在国际象棋中，我们需要表示玩家、棋子及其关系。让我们从定义核心数据结构开始：

### 核心枚举和类型 { #core-enums-and-types }

```kotlin
enum class Player {
    White, Black, None;

    fun opponent(): Player = when (this) {
        White -> Black
        Black -> White
        None -> throw IllegalArgumentException("No opponent for None player")
    }
}

enum class PieceType(val id: Char) {
    King('K'), Queen('Q'), Rook('R'),
    Bishop('B'), Knight('N'), Pawn('P'), None('*');

    companion object {
        fun fromId(id: String): PieceType {
            require(id.length == 1) { "Invalid piece id: $id" }

            return entries.first { it.id == id.single() }
        }
    }
}

enum class Side {
    King, Queen
}
```

`Player` 枚举代表国际象棋中的双方，其 `opponent()` 方法便于在玩家之间切换。`PieceType` 枚举将每个国际象棋棋子映射到其标准记谱字符，便于解析国际象棋走法。

`Side` 枚举有助于区分王翼和长易位走法。

### 棋子和位置建模 { #piece-and-position-modeling }

```kotlin
data class Piece(val pieceType: PieceType, val player: Player) {
    init {
        require((pieceType == PieceType.None) == (player == Player.None)) {
            "Invalid piece: $pieceType $player"
        }
    }

    fun toChar(): Char = when (player) {
        Player.White -> pieceType.id.uppercaseChar()
        Player.Black -> pieceType.id.lowercaseChar()
        Player.None -> pieceType.id
    }

    fun isNone(): Boolean = pieceType == PieceType.None

    companion object {
        val None = Piece(PieceType.None, Player.None)
    }
}

data class Position(val row: Int, val col: Char) {
    init {
        require(row in 1..8 && col in 'a'..'h') { "Invalid position: $col$row" }
    }

    constructor(position: String) : this(
        position[1].digitToIntOrNull() ?: throw IllegalArgumentException("Incorrect position: $position"),
        position[0],
    ) {
        require(position.length == 2) { "Invalid position: $position" }
    }
}

class ChessBoard {
    private val backRow = listOf(
        PieceType.Rook, PieceType.Knight, PieceType.Bishop,
        PieceType.Queen, PieceType.King,
        PieceType.Bishop, PieceType.Knight, PieceType.Rook
    )

    private val board: List<MutableList<Piece>> = listOf(
        backRow.map { Piece(it, Player.Black) }.toMutableList(),
        List(8) { Piece(PieceType.Pawn, Player.Black) }.toMutableList(),
        List(8) { Piece.None }.toMutableList(),
        List(8) { Piece.None }.toMutableList(),
        List(8) { Piece.None }.toMutableList(),
        List(8) { Piece.None }.toMutableList(),
        List(8) { Piece(PieceType.Pawn, Player.White) }.toMutableList(),
        backRow.map { Piece(it, Player.White) }.toMutableList()
    )

    override fun toString(): String = board
        .withIndex().joinToString("\n") { (index, row) ->
            "${8 - index} ${row.map { it.toChar() }.joinToString(" ")}"
        } + "\n  a b c d e f g h"

    fun getPiece(position: Position): Piece = board[8 - position.row][position.col - 'a']
    fun setPiece(position: Position, piece: Piece) {
        board[8 - position.row][position.col - 'a'] = piece
    }
}
```

`Piece` 数据类将棋子类型与其所有者结合起来，在视觉表示中使用大写字母代表白方棋子，小写字母代表黑方棋子。`Position` 类封装了国际象棋坐标（例如 "e4"），并内置了验证功能。

## 游戏状态管理 { #game-state-management }

### ChessBoard 实现 { #chessboard-implementation }

`ChessBoard` 类管理 8×8 的棋盘网格和棋子位置。关键设计决策包括：

- **内部表示**：使用可变列表的列表，以实现高效的访问和修改
- **视觉显示**：`toString()` 方法提供了清晰的 ASCII 表示，包含行号和列字母
- **位置映射**：在国际象棋记谱法（a1-h8）和内部数组索引之间进行转换

### ChessGame 逻辑 { #chessgame-logic }

```kotlin
/**
 * Simple chess game without checks for valid moves.
 * Stores a correct state of the board if the entered moves are valid
 */
class ChessGame {
    private val board: ChessBoard = ChessBoard()
    private var currentPlayer: Player = Player.White
    val moveNotation: String = """
        0-0 - short castle
        0-0-0 - long castle
        <piece>-<from>-<to> - usual move. e.g. p-e2-e4
        <piece>-<from>-<to>-<promotion> - promotion move. e.g. p-e7-e8-q.
        Piece names:
            p - pawn
            n - knight
            b - bishop
            r - rook
            q - queen
            k - king
    """.trimIndent()

    fun move(move: String) {
        when {
            move == "0-0" -> castleMove(Side.King)
            move == "0-0-0" -> castleMove(Side.Queen)
            move.split("-").size == 3 -> {
                val (_, from, to) = move.split("-")
                usualMove(Position(from), Position(to))
            }

            move.split("-").size == 4 -> {
                val (piece, from, to, promotion) = move.split("-")

                require(PieceType.fromId(piece) == PieceType.Pawn) { "Only pawn can be promoted" }

                usualMove(Position(from), Position(to))
                board.setPiece(Position(to), Piece(PieceType.fromId(promotion), currentPlayer))
            }

            else -> throw IllegalArgumentException("Invalid move: $move")
        }

        updateCurrentPlayer()
    }

    fun getBoard(): String = board.toString()
    fun currentPlayer(): String = currentPlayer.name.lowercase()

    private fun updateCurrentPlayer() {
        currentPlayer = currentPlayer.opponent()
    }

    private fun usualMove(from: Position, to: Position) {
        if (board.getPiece(from).pieceType == PieceType.Pawn && from.col != to.col && board.getPiece(to).isNone()) {
            // the move is en passant
            board.setPiece(Position(from.row, to.col), Piece.None)
        }

        movePiece(from, to)
    }

    private fun castleMove(side: Side) {
        val row = if (currentPlayer == Player.White) 1 else 8
        val kingFrom = Position(row, 'e')
        val (rookFrom, kingTo, rookTo) = if (side == Side.King) {
            Triple(Position(row, 'h'), Position(row, 'g'), Position(row, 'f'))
        } else {
            Triple(Position(row, 'a'), Position(row, 'c'), Position(row, 'd'))
        }

        movePiece(kingFrom, kingTo)
        movePiece(rookFrom, rookTo)
    }

    private fun movePiece(from: Position, to: Position) {
        board.setPiece(to, board.getPiece(from))
        board.setPiece(from, Piece.None)
    }
}
```

`ChessGame` 类编排游戏逻辑并维护状态。显著特点包括：

- **走法记谱支持**：接受标准国际象棋记谱法表示常规走法、易位（0-0, 0-0-0）和兵升变
- **特殊走法处理**：实现吃过路兵和易位逻辑
- **回合管理**：每次走法后自动在玩家之间切换
- **验证**：虽然不验证走法的合法性（信任 AI 做出有效走法），但能正确处理走法解析和状态更新

`moveNotation` 字符串为 AI 智能体提供了关于可接受走法格式的清晰文档。

## 与 Koog 框架集成 { #integrating-with-koog-framework }

### 创建自定义工具 { #creating-custom-tools }

```kotlin
import kotlinx.serialization.Serializable

class Move(val game: ChessGame) : SimpleTool<Move.Args>(
    argsSerializer = Args.serializer(),
    descriptor = ToolDescriptor(
        name = "move",
        description = "Moves a piece according to the notation:\n${game.moveNotation}",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "notation",
                description = "The notation of the piece to move",
                type = ToolParameterType.String,
            )
        )
    )
) {
    @Serializable
    data class Args(val notation: String) : ToolArgs

    override suspend fun execute(args: Args): String {
        game.move(args.notation)
        println(game.getBoard())
        println("-----------------")
        return "Current state of the game:\n${game.getBoard()}\n${game.currentPlayer()} to move! Make the move!"
    }
}
```

`Move` 工具展示了 Koog 框架的工具集成模式：1. **扩展 SimpleTool**：继承基础工具功能，提供类型安全的参数处理
2. **可序列化参数**：使用 Kotlin 序列化定义工具的输入参数
3. **丰富文档**：`ToolDescriptor` 为 LLM 提供关于工具用途和参数的详细信息
4. **构造函数参数**：将 `argsSerializer` 和 `descriptor` 传递给构造函数
5. **执行逻辑**：`execute` 方法处理实际的走棋执行并提供格式化反馈

关键设计方面：
- **上下文注入**：工具接收 `ChessGame` 实例，允许其修改游戏状态
- **反馈循环**：返回当前棋盘状态并提示下一位玩家，维持对话流程
- **错误处理**：依赖游戏类进行走棋验证和错误报告

## 智能体策略设计 { #agent-strategy-design }

### 内存优化技术 { #memory-optimization-technique }

```kotlin
import ai.koog.agents.core.environment.ReceivedToolResult

/**
 * Chess position is (almost) completely defined by the board state,
 * So we can trim the history of the LLM to only contain the system prompt and the last move.
 */
inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.nodeTrimHistory(
    name: String? = null
): AIAgentNodeDelegate<T, T> = node(name) { result ->
    llm.writeSession {
        rewritePrompt { prompt ->
            val messages = prompt.messages

            prompt.copy(messages = listOf(messages.first(), messages.last()))
        }
    }

    result
}

val strategy = strategy<String, String>("chess_strategy") {
    val nodeCallLLM by nodeLLMRequest("sendInput")
    val nodeExecuteTool by nodeExecuteTool("nodeExecuteTool")
    val nodeSendToolResult by nodeLLMSendToolResult("nodeSendToolResult")
    val nodeTrimHistory by nodeTrimHistory<ReceivedToolResult>()

    edge(nodeStart forwardTo nodeCallLLM)
    edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
    edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
    edge(nodeExecuteTool forwardTo nodeTrimHistory)
    edge(nodeTrimHistory forwardTo nodeSendToolResult)
    edge(nodeSendToolResult forwardTo nodeFinish onAssistantMessage { true })
    edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })
}
```

`nodeTrimHistory` 函数实现了国际象棋游戏的关键优化。由于棋局状态主要由当前棋盘状态而非完整走棋历史决定，我们可以通过仅保留以下内容显著减少令牌使用：

1. **系统提示**：包含智能体的核心指令和行为准则
2. **最新消息**：最近的棋盘状态和游戏上下文

这种方法：
- **减少令牌消耗**：防止对话历史呈指数级增长
- **保持上下文**：保留必要的游戏状态信息
- **提升性能**：通过更短的提示实现更快处理
- **支持长时对局**：允许延长游戏时间而不触及令牌限制

国际象棋策略展示了 Koog 基于图的智能体架构：

**节点类型：**
- `nodeCallLLM`：处理输入并生成响应/工具调用
- `nodeExecuteTool`：使用提供的参数执行走棋工具
- `nodeTrimHistory`：按上述描述优化对话记忆
- `nodeSendToolResult`：将工具执行结果发送回 LLM

**控制流程：**
- **线性路径**：开始 → LLM 请求 → 工具执行 → 历史修剪 → 发送结果
- **决策点**：LLM 响应可结束对话或触发另一个工具调用
- **内存管理**：每次工具执行后进行历史修剪

该策略在保持对话连贯性的同时，确保了高效、有状态的游戏体验。

### 设置AI智能体 { #setting-up-the-ai-agent }

```kotlin
val baseExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY"))
```

本节初始化我们的 OpenAI 执行器。`simpleOpenAIExecutor` 使用环境变量中的 API 密钥创建与 OpenAI 的 API 的连接。

**配置说明：**
- 将您的 OpenAI API 密钥存储在 `OPENAI_API_KEY` 环境变量中
- 执行器自动处理身份验证和 API 通信
- 提供不同类型的执行器以适配各种 LLM 服务商

### 智能体组装 { #agent-assembly }

```kotlin
val game = ChessGame()
val toolRegistry = ToolRegistry { tools(listOf(Move(game))) }

// Create a chat agent with a system prompt and the tool registry
val agent = AIAgent(
    executor = baseExecutor,
    strategy = strategy,
    llmModel = OpenAIModels.Chat.O3Mini,
    systemPrompt = """
            You are an agent who plays chess.
            You should always propose a move in response to the "Your move!" message.

            DO NOT HALLUCINATE!!!
            DO NOT PLAY ILLEGAL MOVES!!!
            YOU CAN SEND A MESSAGE ONLY IF IT IS A RESIGNATION OR A CHECKMATE!!!
        """.trimMargin(),
    temperature = 0.0,
    toolRegistry = toolRegistry,
    maxIterations = 200,
)
```

此处我们将所有组件组装成功能完整的国际象棋对弈智能体：

**关键配置：**- **模型选择**：使用 `OpenAIModels.Chat.O3Mini` 实现高质量的象棋对弈
- **温度参数**：设置为 0.0，确保确定性的策略走法
- **系统提示词**：精心设计的指令，强调合法走法与规范行为
- **工具注册**：为智能体提供走子工具的访问权限
- **最大迭代次数**：设置为 200，确保对局完整进行

**系统提示词设计：**
- 强调走子提议的责任
- 禁止幻觉走法与非法移动
- 限制消息内容仅包含认输或将死声明
- 创建专注的、以对局为导向的行为

### 运行基础智能体 { #running-the-basic-agent }

```kotlin
import kotlinx.coroutines.runBlocking

println("Chess Game started!")

val initialMessage = "Starting position is ${game.getBoard()}. White to move!"

runBlocking {
    agent.run(initialMessage)
}
```

    象棋对局开始！
    8 r n b q k b n r
    7 p p p p p p p p
    6 * * * * * * * *
    5 * * * * * * * *
    4 * * * * P * * *
    3 * * * * * * * *
    2 P P P P * P P P
    1 R N B Q K B N R
      a b c d e f g h
    -----------------
    8 r n b q k b n r
    7 p p p p * p p p
    6 * * * * * * * *
    5 * * * * p * * *
    4 * * * * P * * *
    3 * * * * * * * *
    2 P P P P * P P P
    1 R N B Q K B N R
      a b c d e f g h
    -----------------
    8 r n b q k b n r
    7 p p p p * p p p
    6 * * * * * * * *
    5 * * * * p * * *
    4 * * * * P * * *
    3 * * * * * N * *
    2 P P P P * P P P
    1 R N B Q K B * R
      a b c d e f g h
    -----------------
    8 r n b q k b * r
    7 p p p p * p p p
    6 * * * * * n * *
    5 * * * * p * * *
    4 * * * * P * * *
    3 * * * * * N * *
    2 P P P P * P P P
    1 R N B Q K B * R
      a b c d e f g h
    -----------------
    8 r n b q k b * r
    7 p p p p * p p p
    6 * * * * * n * *
    5 * * * * p * * *
    4 * * * * P * * *
    3 * * N * * N * *
    2 P P P P * P P P
    1 R * B Q K B * R
      a b c d e f g h
    -----------------

    执行被中断

该基础智能体自主对弈，自动执行走子。对局输出展示了AI自我对弈时的走子序列与棋盘状态。

## 高级功能：交互式选择机制 { #advanced-feature-interactive-choice-selection }

后续章节将展示更复杂的实现方式，用户可通过从AI生成的多个走法中选择来参与AI的决策过程。

### 自定义选择策略 { #custom-choice-selection-strategy }

```kotlin
import ai.koog.agents.core.feature.choice.ChoiceSelectionStrategy

/**
 * `AskUserChoiceStrategy` allows users to interactively select a choice from a list of options
 * presented by a language model. The strategy uses customizable methods to display the prompt
 * and choices and read user input to determine the selected choice.
 *
 * @property promptShowToUser A function that formats and displays a given `Prompt` to the user.
 * @property choiceShowToUser A function that formats and represents a given `LLMChoice` to the user.
 * @property print A function responsible for displaying messages to the user, e.g., for showing prompts or feedback.
 * @property read A function to capture user input.
 */
class AskUserChoiceSelectionStrategy(
    private val promptShowToUser: (Prompt) -> String = { "Current prompt: $it" },
    private val choiceShowToUser: (LLMChoice) -> String = { "$it" },
    private val print: (String) -> Unit = ::println,
    private val read: () -> String? = ::readlnOrNull
) : ChoiceSelectionStrategy {
    override suspend fun choose(prompt: Prompt, choices: List<LLMChoice>): LLMChoice {
        print(promptShowToUser(prompt))

        print("Available LLM choices")

        choices.withIndex().forEach { (index, choice) ->
            print("Choice number ${index + 1}: ${choiceShowToUser(choice)}")
        }

        var choiceNumber = ask(choices.size)
        while (choiceNumber == null) {
            print("Invalid response.")
            choiceNumber = ask(choices.size)
        }

        return choices[choiceNumber - 1]
    }

    private fun ask(numChoices: Int): Int? {
        print("Please choose a choice. Enter a number between 1 and $numChoices: ")

        return read()?.toIntOrNull()?.takeIf { it in 1..numChoices }
    }
}
```

`AskUserChoiceSelectionStrategy` 实现了 Koog 的 `ChoiceSelectionStrategy` 接口，使人类能够参与AI决策过程：

**核心特性：**
- **可定制化显示**：提供格式化提示与选项的函数
- **交互式输入**：使用标准输入/输出进行用户交互
- **输入验证**：确保用户输入在有效范围内
- **灵活I/O**：可配置打印与读取函数以适应不同环境

**应用场景：**
- 人机协作对弈
- AI决策透明化与可解释性
- 训练与调试场景
- 教学演示

### 集成选择机制的增强策略 { #enhanced-strategy-with-choice-selection }

```kotlin
inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.nodeTrimHistory(
    name: String? = null
): AIAgentNodeDelegate<T, T> = node(name) { result ->
    llm.writeSession {
        rewritePrompt { prompt ->
            val messages = prompt.messages

            prompt.copy(messages = listOf(messages.first(), messages.last()))
        }
    }

    result
}

val strategy = strategy<String, String>("chess_strategy") {
    val nodeCallLLM by nodeLLMRequest("sendInput")
    val nodeExecuteTool by nodeExecuteTool("nodeExecuteTool")
    val nodeSendToolResult by nodeLLMSendToolResult("nodeSendToolResult")
    val nodeTrimHistory by nodeTrimHistory<ReceivedToolResult>()

    edge(nodeStart forwardTo nodeCallLLM)
    edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
    edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
    edge(nodeExecuteTool forwardTo nodeTrimHistory)
    edge(nodeTrimHistory forwardTo nodeSendToolResult)
    edge(nodeSendToolResult forwardTo nodeFinish onAssistantMessage { true })
    edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })
}

val askChoiceStrategy = AskUserChoiceSelectionStrategy(promptShowToUser = { prompt ->
    val lastMessage = prompt.messages.last()
    if (lastMessage is Message.Tool.Call) {
        lastMessage.content
    } else {
        ""
    }
})
```

```kotlin
val promptExecutor = PromptExecutorWithChoiceSelection(baseExecutor, askChoiceStrategy)
```

第一种交互式方法采用 `PromptExecutorWithChoiceSelection`，该封装器为基础执行器添加了选择功能。自定义显示函数从工具调用中提取走子信息，向用户展示AI的意图。

**架构调整：**
- **封装执行器**：`PromptExecutorWithChoiceSelection` 为任意基础执行器添加选择功能
- **上下文感知显示**：展示最近工具调用内容而非完整提示
- **更高温度参数**：提升至1.0以生成更多样化的走法选项#

## 高级策略：手动选择

```kotlin
val game = ChessGame()
val toolRegistry = ToolRegistry { tools(listOf(Move(game))) }

val agent = AIAgent(
    executor = promptExecutor,
    strategy = strategy,
    llmModel = OpenAIModels.Chat.O3Mini,
    systemPrompt = """
            You are an agent who plays chess.
            You should always propose a move in response to the "Your move!" message.

            DO NOT HALLUCINATE!!!
            DO NOT PLAY ILLEGAL MOVES!!!
            YOU CAN SEND A MESSAGE ONLY IF IT IS A RESIGNATION OR A CHECKMATE!!!
        """.trimMargin(),
    temperature = 1.0,
    toolRegistry = toolRegistry,
    maxIterations = 200,
    numberOfChoices = 3,
)
```

高级策略将选择功能直接集成到智能体的执行图中：

**新增节点：**
- `nodeLLMSendResultsMultipleChoices`：同时处理多个 LLM 选项
- `nodeSelectLLMChoice`：将选择策略集成到工作流中

**增强的控制流：**
- 工具结果以列表形式包装，以支持多个选项
- 用户在选择后继续执行所选路径
- 所选选项被解包并继续通过正常流程执行

**优势：**
- **更强的控制力**：与智能体工作流精细集成
- **灵活性**：可与其他智能体功能结合使用
- **透明度**：用户可清晰查看 AI 正在考虑的内容

### 运行交互式智能体 { #advanced-strategy-manual-choice-selection }

```kotlin
println("Chess Game started!")

val initialMessage = "Starting position is ${game.getBoard()}. White to move!"

runBlocking {
    agent.run(initialMessage)
}
```

    国际象棋游戏开始！

    可用的 LLM 选项
    选项 1: [Call(id=call_K46Upz7XoBIG5RchDh7bZE8F, tool=move, content={"notation": "p-e2-e4"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:17:40.368252Z, totalTokensCount=773, inputTokensCount=315, outputTokensCount=458, additionalInfo={}))]
    选项 2: [Call(id=call_zJ6OhoCHrVHUNnKaxZkOhwoU, tool=move, content={"notation": "p-e2-e4"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:17:40.368252Z, totalTokensCount=773, inputTokensCount=315, outputTokensCount=458, additionalInfo={}))]
    选项 3: [Call(id=call_nwX6ZMJ3F5AxiNUypYlI4BH4, tool=move, content={"notation": "p-e2-e4"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:17:40.368252Z, totalTokensCount=773, inputTokensCount=315, outputTokensCount=458, additionalInfo={}))]
    请选择一个选项。输入 1 到 3 之间的数字：
    8 r n b q k b n r
    7 p p p p p p p p
    6 * * * * * * * *
    5 * * * * * * * *
    4 * * * * P * * *
    3 * * * * * * * *
    2 P P P P * P P P
    1 R N B Q K B N R
      a b c d e f g h
    -----------------

    可用的 LLM 选项
    选项 1: [Call(id=call_2V93GXOcIe0fAjUAIFEk9h5S, tool=move, content={"notation": "p-e7-e5"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:17:47.949303Z, totalTokensCount=1301, inputTokensCount=341, outputTokensCount=960, additionalInfo={}))]
    选项 2: [Call(id=call_INM59xRzKMFC1w8UAV74l9e1, tool=move, content={"notation": "p-e7-e5"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:17:47.949303Z, totalTokensCount=1301, inputTokensCount=341, outputTokensCount=960, additionalInfo={}))]
    选项 3: [Call(id=call_r4QoiTwn0F3jizepHH5ia8BU, tool=move, content={"notation": "p-e7-e5"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:17:47.949303Z, totalTokensCount=1301, inputTokensCount=341, outputTokensCount=960, additionalInfo={}))]
    请选择一个选项。输入 1 到 3 之间的数字：
    8 r n b q k b n r
    7 p p p p * p p p
    6 * * * * * * * *
    5 * * * * p * * *
    4 * * * * P * * *
    3 * * * * * * * *
    2 P P P P * P P P
    1 R N B Q K B N R
      a b c d e f g h
    -----------------可用的 LLM 选项
选项 1: [Call(id=call_f9XTizn41svcrtvnmkCfpSUQ, tool=move, content={"notation": "n-g1-f3"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:17:55.467712Z, totalTokensCount=917, inputTokensCount=341, outputTokensCount=576, additionalInfo={}))]
选项 2: [Call(id=call_c0Dfce5RcSbN3cOOm5ESYriK, tool=move, content={"notation": "n-g1-f3"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:17:55.467712Z, totalTokensCount=917, inputTokensCount=341, outputTokensCount=576, additionalInfo={}))]
选项 3: [Call(id=call_Lr4Mdro1iolh0fDyAwZsutrW, tool=move, content={"notation": "n-g1-f3"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:17:55.467712Z, totalTokensCount=917, inputTokensCount=341, outputTokensCount=576, additionalInfo={}))]
请选择一个选项。输入 1 到 3 之间的数字：
8 r n b q k b n r
7 p p p p * p p p
6 * * * * * * * *
5 * * * * p * * *
4 * * * * P * * *
3 * * * * * N * *
2 P P P P * P P P
1 R N B Q K B * R
  a b c d e f g h
-----------------

执行被中断

```kotlin
import ai.koog.agents.core.feature.choice.nodeLLMSendResultsMultipleChoices
import ai.koog.agents.core.feature.choice.nodeSelectLLMChoice

inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.nodeTrimHistory(
    name: String? = null
): AIAgentNodeDelegate<T, T> = node(name) { result ->
    llm.writeSession {
        rewritePrompt { prompt ->
            val messages = prompt.messages

            prompt.copy(messages = listOf(messages.first(), messages.last()))
        }
    }

    result
}

val strategy = strategy<String, String>("chess_strategy") {
    val nodeCallLLM by nodeLLMRequest("sendInput")
    val nodeExecuteTool by nodeExecuteTool("nodeExecuteTool")
    val nodeSendToolResult by nodeLLMSendResultsMultipleChoices("nodeSendToolResult")
    val nodeSelectLLMChoice by nodeSelectLLMChoice(askChoiceStrategy, "chooseLLMChoice")
    val nodeTrimHistory by nodeTrimHistory<ReceivedToolResult>()

    edge(nodeStart forwardTo nodeCallLLM)
    edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
    edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
    edge(nodeExecuteTool forwardTo nodeTrimHistory)
    edge(nodeTrimHistory forwardTo nodeSendToolResult transformed { listOf(it) })
    edge(nodeSendToolResult forwardTo nodeSelectLLMChoice)
    edge(nodeSelectLLMChoice forwardTo nodeFinish transformed { it.first() } onAssistantMessage { true })
    edge(nodeSelectLLMChoice forwardTo nodeExecuteTool transformed { it.first() } onToolCall { true })
}
```

```kotlin
val game = ChessGame()
val toolRegistry = ToolRegistry { tools(listOf(Move(game))) }

val agent = AIAgent(
    executor = baseExecutor,
    strategy = strategy,
    llmModel = OpenAIModels.Chat.O3Mini,
    systemPrompt = """
            You are an agent who plays chess.
            You should always propose a move in response to the "Your move!" message.

            DO NOT HALLUCINATE!!!
            DO NOT PLAY ILLEGAL MOVES!!!
            YOU CAN SEND A MESSAGE ONLY IF IT IS A RESIGNATION OR A CHECKMATE!!!
        """.trimMargin(),
    temperature = 1.0,
    toolRegistry = toolRegistry,
    maxIterations = 200,
    numberOfChoices = 3,
)
```

```kotlin
println("Chess Game started!")

val initialMessage = "Starting position is ${game.getBoard()}. White to move!"

runBlocking {
    agent.run(initialMessage)
}
```

国际象棋游戏开始！
8 r n b q k b n r
7 p p p p p p p p
6 * * * * * * * *
5 * * * * * * * *
4 * * * * P * * *
3 * * * * * * * *
2 P P P P * P P P
1 R N B Q K B N R
  a b c d e f g h
-----------------

可用的 LLM 选项
选项 1: [Call(id=call_gqMIar0z11CyUl5nup3zbutj, tool=move, content={"notation": "p-e7-e5"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:18:17.313548Z, totalTokensCount=917, inputTokensCount=341, outputTokensCount=576, additionalInfo={}))]
选项 2: [Call(id=call_6niUGnZPPJILRFODIlJsCKax, tool=move, content={"notation": "p-e7-e5"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:18:17.313548Z, totalTokensCount=917, inputTokensCount=341, outputTokensCount=576, additionalInfo={}))]
选项 3: [Call(id=call_q1b8ZmIBph0EoVaU3Ic9A09j, tool=move, content={"notation": "p-e7-e5"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:18:17.313548Z, totalTokensCount=917, inputTokensCount=341, outputTokensCount=576, additionalInfo={}))]
请选择一个选项。输入 1 到 3 之间的数字：
8 r n b q k b n r
7 p p p p * p p p
6 * * * * * * * *
5 * * * * p * * *
4 * * * * P * * *
3 * * * * * * * *
2 P P P P * P P P
1 R N B Q K B N R
  a b c d e f g h
-----------------可用的 LLM 选项
选项 1: [Call(id=call_pdBIX7MVi82MyWwawTm1Q2ef, tool=move, content={"notation": "n-g1-f3"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:18:24.505344Z, totalTokensCount=1237, inputTokensCount=341, outputTokensCount=896, additionalInfo={}))]
选项 2: [Call(id=call_oygsPHaiAW5OM6pxhXhtazgp, tool=move, content={"notation": "n-g1-f3"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:18:24.505344Z, totalTokensCount=1237, inputTokensCount=341, outputTokensCount=896, additionalInfo={}))]
选项 3: [Call(id=call_GJTEsZ8J8cqOKZW4Tx54RqCh, tool=move, content={"notation": "n-g1-f3"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:18:24.505344Z, totalTokensCount=1237, inputTokensCount=341, outputTokensCount=896, additionalInfo={}))]
请选择一个选项。输入 1 到 3 之间的数字：
8 r n b q k b n r
7 p p p p * p p p
6 * * * * * * * *
5 * * * * p * * *
4 * * * * P * * *
3 * * * * * N * *
2 P P P P * P P P
1 R N B Q K B * R
  a b c d e f g h
-----------------

可用的 LLM 选项
选项 1: [Call(id=call_5C7HdlTU4n3KdXcyNogE4rGb, tool=move, content={"notation": "n-g8-f6"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:18:34.646667Z, totalTokensCount=1621, inputTokensCount=341, outputTokensCount=1280, additionalInfo={}))]
选项 2: [Call(id=call_EjCcyeMLQ88wMa5yh3vmeJ2w, tool=move, content={"notation": "n-g8-f6"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:18:34.646667Z, totalTokensCount=1621, inputTokensCount=341, outputTokensCount=1280, additionalInfo={}))]
选项 3: [Call(id=call_NBMMSwmFIa8M6zvfbPw85NKh, tool=move, content={"notation": "n-g8-f6"}, metaInfo=ResponseMetaInfo(timestamp=2025-08-18T21:18:34.646667Z, totalTokensCount=1621, inputTokensCount=341, outputTokensCount=1280, additionalInfo={}))]
请选择一个选项。输入 1 到 3 之间的数字：
8 r n b q k b * r
7 p p p p * p p p
6 * * * * * n * *
5 * * * * p * * *
4 * * * * P * * *
3 * * * * * N * *
2 P P P P * P P P
1 R N B Q K B * R
  a b c d e f g h
-----------------

执行被中断

交互式示例展示了用户如何引导 AI 的决策过程。在输出中，你可以看到：

1. **多个选项**：AI 生成 3 个不同的走法选项
2. **用户选择**：用户输入 1-3 的数字来选择偏好的走法
3. **游戏继续**：执行选定的走法，游戏继续进行

## 结论

本教程展示了使用 Koog 框架构建智能代理的几个关键方面：

### 关键要点

1. **领域建模**：结构良好的数据模型对于复杂应用至关重要
2. **工具集成**：自定义工具使代理能够有效地与外部系统交互
3. **内存管理**：策略性的历史记录修剪优化了长交互的性能
4. **策略图**：Koog 的基于图的方法提供了灵活的控制流
5. **交互式 AI**：选项选择实现了人机协作和透明度

### 探索的框架特性 { #key-takeaways }

- ✅ 自定义工具创建与集成
- ✅ 代理策略设计与基于图的控制流
- ✅ 内存优化技术
- ✅ 交互式选项选择
- ✅ 多 LLM 响应处理
- ✅ 有状态游戏管理Koog框架为构建复杂的人工智能代理提供了基础，这些代理能够处理复杂的多轮交互，同时保持高效性和透明度。