<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:07:33+00:00", "source_path": "prompts/prompt-creation/multimodal-content.md", "source_sha256": "ec369abfe41598c080247c595166258264f09fee07c7c9657f6130e4ca668c34", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 多模态内容 { #multimodal-content }

多模态内容指不同类型的内容，例如文本、图像、音频、视频和文件。
Koog允许您在`user`消息中向LLM发送图像、音频、视频和文件以及文本。
您可以通过使用Kotlin中的相应函数或Java中的方法将它们添加到`user`消息中：

- `image()`：附加图像（JPG、PNG、WebP、GIF）。
- `audio()`：附加音频文件（MP3、WAV、FLAC）。
- `video()`：附加视频文件（MP4、AVI、MOV）。
- `file()` / `binaryFile()` / `textFile()`：附加文档（PDF、TXT、MD等）。

每个函数或方法支持两种配置附件参数的方式，因此您可以：

- 将URL或文件路径传递给函数或方法，它会自动处理附件参数。对于`file()`、`binaryFile()`和`textFile()`，您还必须提供MIME类型。
- 创建`ContentPart`对象并将其传递给函数或方法，以自定义控制附件参数。

!!! note
    多模态内容支持因[LLM提供商](../../llm-providers.md)而异。
    请查阅提供商文档以了解支持的内容类型。

### 自动配置的附件 { #auto-configured-attachments }

如果您将URL或文件路径传递给附件函数或方法，Koog会根据文件扩展名自动构建相应的附件参数。

包含文本消息和自动配置附件列表的`user`消息的一般格式如下：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    import kotlinx.io.files.Path
    val prompt = prompt("image_analysis") {
    -->
    <!--- SUFFIX
    }
    -->

    ```kotlin
    user {
        +"描述这些图像："

        image("https://example.com/test.png")
        image(Path("/path/to/image.png"))

        +"重点关注主要主体。"
    }
    ```
    <!--- KNIT example-multimodal-content-01.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    ContentPartsBuilder partsBuilder = new ContentPartsBuilder();
    partsBuilder.text("描述这些图像：");
    partsBuilder.image("https://example.com/test.png");
    partsBuilder.text("重点关注主要主体。");

    Prompt prompt = Prompt.builder("image_analysis")
        .user(partsBuilder.build())
        .build();
    ```
    <!--- KNIT example-multimodal-content-java-01.java -->

在Kotlin中，`+`运算符将文本内容与附件一起添加到用户消息中。在Java中，使用`ContentPartsBuilder`的`text()`方法。

### 自定义配置的附件 { #custom-configured-attachments }

[`ContentPart`](api:prompt-model::ai.koog.prompt.message.ContentPart)接口允许您为每个附件单独配置参数。

所有附件都实现了`ContentPart.Attachment`接口。
您可以为每个附件创建特定实现的实例，配置其参数，并将其传递给Kotlin中的相应`image()`、`audio()`、`video()`或`file()`函数，或Java中的方法。包含文本消息和自定义配置附件列表的 `user` 消息通用格式如下：

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    import ai.koog.prompt.message.AttachmentContent
    import ai.koog.prompt.message.ContentPart
    val prompt = prompt("custom_image") {
    -->
    <!--- SUFFIX
    }
    -->

    ```kotlin
    user {
        +"描述这张图片"
        image(
            ContentPart.Image(
                content = AttachmentContent.URL("https://example.com/capture.png"),
                format = "png",
                mimeType = "image/png",
                fileName = "capture.png"
            )
        )
    }
    ```
    <!--- KNIT example-multimodal-content-02.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    Prompt prompt = Prompt.builder("custom_image")
        .user(List.of(
            new ContentPart.Text("描述这张图片"),
            new ContentPart.Image(
                new AttachmentContent.URL("https://example.com/capture.png"),
                "png",
                "image/png",
                "capture.png"
            )
        ))
        .build();
    ```
    <!--- KNIT example-multimodal-content-java-02.java -->

Koog 为每种媒体类型提供了以下实现 `ContentPart.Attachment` 接口的专用类：

- [`ContentPart.Image`](api:prompt-model::ai.koog.prompt.message.ContentPart.Image)：图像附件，例如 JPG 或 PNG 文件。
- [`ContentPart.Audio`](api:prompt-model::ai.koog.prompt.message.ContentPart.Audio)：音频附件，例如 MP3 或 WAV 文件。
- [`ContentPart.Video`](api:prompt-model::ai.koog.prompt.message.ContentPart.Video)：视频附件，例如 MP4 或 AVI 文件。
- [`ContentPart.File`](api:prompt-model::ai.koog.prompt.message.ContentPart.File)：文件附件，例如 PDF 或 TXT 文件。

所有 `ContentPart.Attachment` 类型均接受以下参数：| 名称       | 数据类型                                                                                                          | 必填 | 描述                                                                                                                                                                                                                             |
|------------|--------------------------------------------------------------------------------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `content`  | [AttachmentContent](api:prompt-model::ai.koog.prompt.message.AttachmentContent) | 是      | 所提供文件内容的来源。                                                                                                                                                                                                |
| `format`   | 字符串                                                                                                             | 是      | 所提供文件的格式。例如 `png`。                                                                                                                                                                                    |
| `mimeType` | 字符串                                                                                                             | 仅适用于 `ContentPart.File`      | 所提供文件的 MIME 类型。<br/>对于 `ContentPart.Image`、`ContentPart.Audio` 和 `ContentPart.Video`，默认为 `<type>/<format>`（例如 `image/png`）。<br/>对于 `ContentPart.File`，必须显式提供。 |
| `fileName` | 字符串?                                                                                                            | 否       | 所提供文件的名称（包含扩展名）。例如 `screenshot.png`。                                                                                                                                                   |

#### 附件内容 { #attachment-content }

AttachmentContent 接口的实现定义了作为输入提供给 LLM 的内容类型和来源：

- [`AttachmentContent.URL`](api:prompt-model::ai.koog.prompt.message.AttachmentContent.URL) 定义所提供内容的 URL：
    ```kotlin
    AttachmentContent.URL("https://example.com/image.png")
    ```
    <!--- KNIT example-multimodal-content-01.txt -->

- [`AttachmentContent.Binary.Bytes`](api:prompt-model::ai.koog.prompt.message.AttachmentContent.Binary) 将文件内容定义为字节数组：
    ```kotlin
    AttachmentContent.Binary.Bytes(byteArrayOf(/* ... */))
    ```
    <!--- KNIT example-multimodal-content-02.txt -->

- [`AttachmentContent.Binary.Base64`](api:prompt-model::ai.koog.prompt.message.AttachmentContent.Binary) 将文件内容定义为包含文件数据的 Base64 编码字符串：
    ```kotlin
    AttachmentContent.Binary.Base64("iVBORw0KGgoAAAANS...")
    ```
    <!--- KNIT example-multimodal-content-03.txt -->

- [`AttachmentContent.PlainText`](api:prompt-model::ai.koog.prompt.message.AttachmentContent.PlainText) 将文件内容定义为纯文本（仅适用于 [`ContentPart.File`](api:prompt-model::ai.koog.prompt.message.ContentPart.File)）：
    ```kotlin
    AttachmentContent.PlainText("This is the file content.")
    ```
    <!--- KNIT example-multimodal-content-04.txt -->

### 混合附件除了在单独的提示或消息中提供不同类型的附件外，您还可以在单个 `user()` 消息中提供多种混合类型的附件： { #mixed-attachments }

=== "Kotlin"

    <!--- INCLUDE
    import ai.koog.prompt.dsl.prompt
    import kotlinx.io.files.Path
    -->

    ```kotlin
    val prompt = prompt("mixed_content") {
        system("You are a helpful assistant.")

        user {
            +"Compare the image with the document content."
            image(Path("/path/to/image.png"))
            binaryFile(Path("/path/to/page.pdf"), "application/pdf")
            +"Structure the result as a table"
        }
    }
    ```
    <!--- KNIT example-multimodal-content-03.kt -->

=== "Java"

    <!--- INCLUDE
    /**
    -->
    <!--- SUFFIX
    **/
    -->
    ```java
    Prompt prompt = Prompt.builder("mixed_content_example")
    .system("You are a helpful assistant.")
    .user(List.of(
        new ContentPart.Text("Please analyze this image and the attached document."),
        new ContentPart.Image(
            new AttachmentContent.URL("https://example.com/image.png"),
            "png",
            "image/png",
            "image.png"
        ),
        new ContentPart.File(
            new AttachmentContent.URL("https://example.com/document.pdf"),
            "pdf",
            "application/pdf",
            "document.pdf"
        ),
        new ContentPart.Text("Summarize the differences.")
    ))
    .build();
    ```
    <!--- KNIT example-multimodal-content-java-03.java -->

## 后续步骤 { #next-steps }

- 如果您与单个 LLM 提供商合作，请使用 [LLM 客户端](../llm-clients.md)运行提示。
- 如果您与多个 LLM 提供商合作，请使用 [提示执行器](../prompt-executors.md)运行提示。