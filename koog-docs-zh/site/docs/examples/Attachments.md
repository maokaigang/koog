<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:55:29+00:00", "source_path": "examples/Attachments.md", "source_sha256": "38794bc2dc90be72dc43bd053820c8b1c9b504e417f7edbfe81ca343bdfc2551", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 附件 { #attachments }

[:material-github: 在 GitHub 上打开](
https://github.com/JetBrains/koog/blob/develop/examples/notebooks/Attachments.ipynb
){ .md-button .md-button--primary }
[:material-download: 下载 .ipynb](
https://raw.githubusercontent.com/JetBrains/koog/develop/examples/notebooks/Attachments.ipynb
){ .md-button }

## 环境设置 { #setting-up-the-environment }

在深入代码之前，我们确保我们的 Kotlin 笔记本已准备就绪。
这里我们加载最新的描述符并启用 **Koog** 库，
它提供了一个简洁的 API 用于与 AI 模型提供商协作。



```kotlin
// Loads the latest descriptors and activates Koog integration for Kotlin Notebook.
// This makes Koog DSL types and executors available in further cells.
%useLatestDescriptors
%use koog
```

## 配置 API 密钥 { #configuring-api-keys }

我们从环境变量中读取 API 密钥。这可以将密钥与笔记本文件分离，并允许您
切换提供商。您可以设置 `OPENAI_API_KEY`、`ANTHROPIC_API_KEY` 或 `GEMINI_API_KEY`。


```kotlin
val apiKey = System.getenv("OPENAI_API_KEY") // or ANTHROPIC_API_KEY, or GEMINI_API_KEY
```

## 创建简单的 OpenAI 执行器 { #creating-a-simple-openai-executor }

执行器封装了身份验证、基础 URL 和正确的默认设置。这里我们使用一个简单的 OpenAI 执行器，
但您可以将其替换为 Anthropic 或 Gemini，而无需更改其余代码。


```kotlin
// --- Provider selection ---
// For OpenAI-compatible models. Alternatives include:
//   val executor = simpleAnthropicExecutor(System.getenv("ANTHROPIC_API_KEY"))
//   val executor = simpleGeminiExecutor(System.getenv("GEMINI_API_KEY"))
// All executors expose the same high‑level API.
val executor = simpleOpenAIExecutor(apiKey)
```

Koog 的提示 DSL 允许您添加 **结构化 Markdown** 和 **附件**。
在此单元格中，我们构建一个提示，要求模型生成一个简短的博客风格“内容卡片”，
并附上本地 `images/` 目录中的两张图片。


```kotlin
import ai.koog.prompt.markdown.markdown
import kotlinx.io.files.Path

val prompt = prompt("images-prompt") {
    system("You are professional assistant that can write cool and funny descriptions for Instagram posts.")

    user {
        markdown {
            +"I want to create a new post on Instagram."
            br()
            +"Can you write something creative under my instagram post with the following photos?"
            br()
            h2("Requirements")
            bulleted {
                item("It must be very funny and creative")
                item("It must increase my chance of becoming an ultra-famous blogger!!!!")
                item("It not contain explicit content, harassment or bullying")
                item("It must be a short catching phrase")
                item("You must include relevant hashtags that would increase the visibility of my post")
            }
        }

        attachments {
            image(Path("images/kodee-loving.png"))
            image(Path("images/kodee-electrified.png"))
        }
    }
}
```

## 执行并检查响应 { #execute-and-inspect-the-response }

我们针对 `gpt-4.1` 运行提示，收集第一条消息，并打印其内容。
如果您需要流式传输，请在 Koog 中切换到流式 API；如需使用工具，请传递您的工具列表而非 `emptyList()`。

> 故障排除：
> * **401/403** — 检查您的 API 密钥/环境变量。
> * **文件未找到** — 验证 `images/` 路径。
> * **速率限制** — 如有需要，在调用周围添加最小重试/退避机制。


```kotlin
import kotlinx.coroutines.runBlocking

runBlocking {
    val response = executor.execute(prompt = prompt, model = OpenAIModels.Chat.GPT4_1, tools = emptyList()).first()
    println(response.content)
}
```

    标题：
    靠可爱和额外的咯咯能量运行！警告：副作用可能包括偷心氛围和自发舞蹈派对。💜🤖💃
    
    标签：  
    #ViralVibes #UltraFamousBlogger #CutieAlert #QuirkyContent #InstaFun #SpreadTheLove #DancingIntoFame #RobotLife #InstaFamous #FeedGoals



```kotlin
runBlocking {
    val response = executor.executeStreaming(prompt = prompt, model = OpenAIModels.Chat.GPT4_1)
    response.collect { print(it) }
}
```

    标题：  
    仅靠好心情和 Wi-Fi 运行！🤖💜 如果感受到电路喜悦，请点个赞！#BlogBotInTheWild #HeartDeliveryService #DancingWithWiFi #UltraFamousBlogger #MoreFunThanYourAICat #ViralVibes #InstaFun #BeepBoopFamous