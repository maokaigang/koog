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
        +"Describe these images:"

        image("https://example.com/test.png")
        image(Path("/path/to/image.png"))

        +"Focus on the main subjects."
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
    partsBuilder.text("Describe these images:");
    partsBuilder.image("https://example.com/test.png");
    partsBuilder.text("Focus on the main subjects.");

    Prompt prompt = Prompt.builder("image_analysis")
        .user(partsBuilder.build())
        .build();
    ```
    <!--- KNIT example-multimodal-content-java-01.java -->

In Kotlin, the `+` operator adds text content to the user message along with the attachments. In Java, use the `text()` method of `ContentPartsBuilder`.

### Custom-configured attachments

The [`ContentPart`](api:prompt-model::ai.koog.prompt.message.ContentPart) interface
lets you configure parameters for each attachment individually.

All attachments implement the `ContentPart.Attachment` interface.
You can create an instance of a specific implementation for each attachment, configure its parameters, and pass it to 
the corresponding `image()`, `audio()`, `video()`, or `file()` functions in Kotlin or methods in Java.

The general format of the `user` message that includes a text message and a list of custom-configured attachments is as follows:

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
    <!--- KNIT example-multimodal-content-java-02.java -->

Koog provides the following specialized classes for each media type that implement the `ContentPart.Attachment` interface:

- [`ContentPart.Image`](api:prompt-model::ai.koog.prompt.message.ContentPart.Image): image attachments, such as JPG or PNG files.
- [`ContentPart.Audio`](api:prompt-model::ai.koog.prompt.message.ContentPart.Audio): audio attachments, such as MP3 or WAV files.
- [`ContentPart.Video`](api:prompt-model::ai.koog.prompt.message.ContentPart.Video): video attachments, such as MP4 or AVI files.
- [`ContentPart.File`](api:prompt-model::ai.koog.prompt.message.ContentPart.File): file attachments, such as PDF or TXT files.

All `ContentPart.Attachment` types accept the following parameters:

| Name       | Data type                                                                                                          | Required | Description                                                                                                                                                                                                                             |
|------------|--------------------------------------------------------------------------------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `content`  | [AttachmentContent](api:prompt-model::ai.koog.prompt.message.AttachmentContent) | Yes      | The source of the provided file content.                                                                                                                                                                                                |
| `format`   | String                                                                                                             | Yes      | The format of the provided file. For example, `png`.                                                                                                                                                                                    |
| `mimeType` | String                                                                                                             | Only for `ContentPart.File`      | The MIME Type of the provided file.<br/>For `ContentPart.Image`, `ContentPart.Audio`, and `ContentPart.Video`, it defaults to `<type>/<format>` (for example, `image/png`).<br/>For `ContentPart.File`, it must be explicitly provided. |
| `fileName` | String?                                                                                                            | No       | The name of the provided file including the extension. For example, `screenshot.png`.                                                                                                                                                   |

#### Attachment content

Implementations of the AttachmentContent interface define the type and source of content that is provided as input to the LLM:

- [`AttachmentContent.URL`](api:prompt-model::ai.koog.prompt.message.AttachmentContent.URL) defines the URL of the provided content:
    ```kotlin
    AttachmentContent.URL("https://example.com/image.png")
    ```
    <!--- KNIT example-multimodal-content-01.txt -->

- [`AttachmentContent.Binary.Bytes`](api:prompt-model::ai.koog.prompt.message.AttachmentContent.Binary) defines the file content as a byte array:
    ```kotlin
    AttachmentContent.Binary.Bytes(byteArrayOf(/* ... */))
    ```
    <!--- KNIT example-multimodal-content-02.txt -->

- [`AttachmentContent.Binary.Base64`](api:prompt-model::ai.koog.prompt.message.AttachmentContent.Binary) defines the file content as a Base64-encoded string containing file data:
    ```kotlin
    AttachmentContent.Binary.Base64("iVBORw0KGgoAAAANS...")
    ```
    <!--- KNIT example-multimodal-content-03.txt -->

- [`AttachmentContent.PlainText`](api:prompt-model::ai.koog.prompt.message.AttachmentContent.PlainText) defines the file content as plain text (for [`ContentPart.File`](api:prompt-model::ai.koog.prompt.message.ContentPart.File) only):
    ```kotlin
    AttachmentContent.PlainText("This is the file content.")
    ```
    <!--- KNIT example-multimodal-content-04.txt -->

### Mixed attachments

In addition to providing different types of attachments in separate prompts or messages, you can also provide multiple and mixed types of attachments in a single `user()` message:

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

## Next steps

- Run prompts with [LLM clients](../llm-clients.md) if you work with a single LLM provider.
- Run prompts with [prompt executors](../prompt-executors.md) if you work with multiple LLM providers.