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

    ```kotlin
    user {
        +"Describe these images:"

        image("https://example.com/test.png")
        image(Path("/path/to/image.png"))

        +"Focus on the main subjects."
    }
    ```

=== "Java"

    ```java
    ContentPartsBuilder partsBuilder = new ContentPartsBuilder();
    partsBuilder.text("Describe these images:");
    partsBuilder.image("https://example.com/test.png");
    partsBuilder.text("Focus on the main subjects.");

    Prompt prompt = Prompt.builder("image_analysis")
        .user(partsBuilder.build())
        .build();
    ```

在Kotlin中，`+`操作符会向用户消息中添加文本内容以及附件。在Java中，请使用`ContentPartsBuilder`的`text()`方法。

### 自定义配置附件 { #custom-configured-attachments }

[`ContentPart`](api:prompt-model::ai.koog.prompt.message.ContentPart) 接口允许您为每个附件单独配置参数。

所有附件均实现`ContentPart.Attachment`接口。您可以为每个附件创建具体实现的实例，配置其参数，并将其传递给Kotlin中的`image()`、`audio()`、`video()`或`file()`函数，或Java中的方法。

包含文本消息和自定义配置附件列表的`user`消息通用格式如下：

=== "Kotlin"

    ```kotlin
    user {
        +"Describe this image"
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

=== "Java"

    ```java
    Prompt prompt = Prompt.builder("custom_image")
        .user(List.of(
            new ContentPart.Text("Describe this image"),
            new ContentPart.Image(
                new AttachmentContent.URL("https://example.com/capture.png"),
                "png",
                "image/png",
                "capture.png"
            )
        ))
        .build();
    ```

Koog 为每种媒体类型提供了实现 `ContentPart.Attachment` 接口的专用类：

- [`ContentPart.Image`](api:prompt-model::ai.koog.prompt.message.ContentPart.Image): 图片附件，例如 JPG 或 PNG 文件。
- [`ContentPart.Audio`](api:prompt-model::ai.koog.prompt.message.ContentPart.Audio)：音频附件，例如MP3或WAV文件。
- [`ContentPart.Video`](api:prompt-model::ai.koog.prompt.message.ContentPart.Video)：视频附件，例如MP4或AVI文件。
- [`ContentPart.File`](api:prompt-model::ai.koog.prompt.message.ContentPart.File)：文件附件，例如PDF或TXT文件。

所有 `ContentPart.Attachment` 类型均接受以下参数：

| 姓名 | 数据类型 | 必需 | 描述 |
|------------|--------------------------------------------------------------------------------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `content` | [附件内容](api:prompt-model::ai.koog.prompt.message.AttachmentContent) | 好的 | 所提供文件内容的来源。 |
| `format` | 字符串 | 好的 | 所提供文件的格式。例如，`png`。 |
| `mimeType` | 字符串 | 仅适用于 `ContentPart.File` | 所提供文件的MIME类型。<br/>对于`ContentPart.Image`、`ContentPart.Audio`和`ContentPart.Video`，默认类型为`<type>/<format>`（例如`image/png`）。<br/>对于`ContentPart.File`，必须明确指定其类型。 |
| `fileName` | 字符串？ | 不 | 所提供文件的名称，包括扩展名。例如，`screenshot.png`。 |

#### 附件内容 { #attachment-content }

AttachmentContent接口的实现定义了提供给LLM作为输入的内容类型和来源。

- [`AttachmentContent.URL`](api:prompt-model::ai.koog.prompt.message.AttachmentContent.URL) 定义了所提供内容的URL：
    ```kotlin
    AttachmentContent.URL("https://example.com/image.png")
    ```

- [`AttachmentContent.Binary.Bytes`](api:prompt-model::ai.koog.prompt.message.AttachmentContent.Binary) 将文件内容定义为字节数组：
    ```kotlin
    AttachmentContent.Binary.Bytes(byteArrayOf(/* ... */))
    ```

- [`AttachmentContent.Binary.Base64`](api:prompt-model::ai.koog.prompt.message.AttachmentContent.Binary) 将文件内容定义为包含文件数据的 Base64 编码字符串：
    ```kotlin
    AttachmentContent.Binary.Base64("iVBORw0KGgoAAAANS...")
    ```

- [`AttachmentContent.PlainText`](api:prompt-model::ai.koog.prompt.message.AttachmentContent.PlainText) 将文件内容定义为纯文本（仅适用于 [`ContentPart.File`](api:prompt-model::ai.koog.prompt.message.ContentPart.File)）：
    ```kotlin
    AttachmentContent.PlainText("This is the file content.")
    ```

### 混合附件 { #mixed-attachments }

除了在单独的提示或消息中提供不同类型的附件外，您还可以在单个`user()`消息中提供多种混合类型的附件：

=== "Kotlin"

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

=== "Java"

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

## 下一步 { #next-steps }

- 如果使用单个LLM提供商，请通过[LLM 客户端](../llm-clients.md)运行提示。
- 如果使用多个LLM提供商，请通过[提示词执行器](../prompt-executors.md)运行提示。