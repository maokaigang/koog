<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:08:28+00:00", "source_path": "snippets/quickstart-snippets.md", "source_sha256": "31fa9a263aa32487dd43507dad2174777d0f01ec7292683886bbc17be1e7cb99", "source_tag": "0.7.3", "translation_status": "changed"} -->
---
search:
exclude: true
---

# --8<-- [start:prerequisites]
确保您的环境和项目满足以下要求：

- JDK 17+
- Kotlin 2.2.0+
- Gradle 8.0+ 或 Maven 3.8+

# --8<-- [end:prerequisites]
# --8<-- [start:dependencies]
添加 [Koog 包](https://central.sonatype.com/artifact/ai.koog/koog-agents/) 作为依赖项：

=== "Gradle (Kotlin)"

    ``` kotlin title="build.gradle.kts"
    dependencies {
        implementation("ai.koog:koog-agents:0.7.1")
    }
    ```

=== "Gradle (Groovy)"

    ``` groovy title="build.gradle"
    dependencies {
        implementation 'ai.koog:koog-agents:0.7.1'
    }
    ```

=== "Maven"

    ```xml title="pom.xml"
    <dependency>
        <groupId>ai.koog</groupId>
        <artifactId>koog-agents-jvm</artifactId>
        <version>0.7.1</version>
    </dependency>
    ```

# --8<-- [end:dependencies]

# --8<-- [start:api-key]
从 LLM 提供商获取 API 密钥，或通过 Ollama 运行本地 LLM。
更多信息，请参阅 [快速开始](/quickstart.md)。

# --8<-- [end:api-key]
