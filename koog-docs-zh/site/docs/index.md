<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:52:57+00:00", "source_path": "index.md", "source_sha256": "8dace95b90bf01b32f3e01b4595e77cac6c040255a76ee26a7f5a011a61d3617", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 概述 { #overview }

Koog 是一个专为 JVM 生态系统设计的开源 JetBrains 框架，用于构建 AI 智能体。它为 Kotlin 和 Java 开发者提供了一流的开发体验，具备符合语言习惯、类型安全的 Kotlin DSL 和流畅的构建器风格 Java API。

Java 开发者可以利用 Koog 在 JVM 上的全部能力，并通过符合语言习惯的 API 进行调用；同时，Kotlin 开发者也可以使用 Kotlin 多平台将智能体部署到 JS、WasmJS、Android 和 iOS 目标平台。

<div class="grid cards" markdown>

-   :material-rocket-launch:{ .lg .middle } [**快速开始**](quickstart.md)

    ---

    构建并运行你的第一个 AI 智能体

-   :material-book-open-variant:{ .lg .middle } [**术语表**](glossary.md)

    ---

    学习核心术语

</div>

## 智能体 { #agents }

了解[智能体的基本概念](agents/index.md)以及如何使用 Koog 创建不同类型的智能体：

<div class="grid cards" markdown>

-   :material-robot-outline:{ .lg .middle } [**基础智能体**](agents/basic-agents.md)

    ---

    使用适用于大多数常见场景的预定义策略

-   :material-function:{ .lg .middle } [**函数式智能体**](agents/functional-agents.md)

    ---

    使用纯 Kotlin 或 Java 将自定义逻辑定义为 lambda 函数

-   :material-state-machine:{ .lg .middle } [**基于图的智能体**](agents/graph-based-agents.md)

    ---

    将自定义工作流实现为策略图

-   :material-list-status:{ .lg .middle } [**规划器智能体**](agents/planner-agents/index.md)

    ---

    迭代构建并执行计划，直到状态满足预期条件

</div>

## 核心组件 { #core-components }

详细了解 Koog 智能体的核心组件：

<div class="grid cards" markdown>

-   :material-chat-processing-outline:{ .lg .middle } [**提示词**](prompts/index.md)

    ---

    创建、管理和运行驱动智能体与 LLM 交互的提示词

-   :material-strategy:{ .lg .middle } [**策略**](predefined-agent-strategies.md)

    ---

    将智能体的预期工作流设计为有向图

-   :material-tools:{ .lg .middle } [**工具**](tools-overview.md)

    ---

    使智能体能够与外部数据源和服务交互

-   :material-toy-brick-outline:{ .lg .middle } [**功能特性**](features/index.md)

    ---

    扩展和增强 AI 智能体的功能

</div>

## 高级用法 { #advanced-usage }

<div class="grid cards" markdown>

-   :material-history:{ .lg .middle } [**历史压缩**](history-compression.md)

    ---

    使用高级技术优化token使用，同时在长对话中保持上下文

-   :material-floppy:{ .lg .middle } [**智能体持久化**](features/agent-persistence.md)

    ---

    在执行过程中的特定时间点恢复智能体状态

-   :material-code-braces:{ .lg .middle } [**结构化输出**](structured-output.md)

    ---

    生成结构化格式的响应

-   :material-waves:{ .lg .middle } [**流式 API**](streaming-api.md)

    ---

    通过流式支持和并行工具调用实时处理响应

-   :material-database-search:{ .lg .middle } [**知识检索**](embeddings.md)

    ---

    利用[向量嵌入](embeddings.md)、[排序文档存储](ranked-document-storage.md)和[共享智能体记忆](features/agent-memory.md)跨对话保留和检索知识

-   :material-timeline-text:{ .lg .middle } [**追踪**](features/tracing.md)

    ---通过详细且可配置的追踪功能调试和监控智能体执行

-   :material-timeline-text:{ .lg .middle } [**长期记忆**](features/long-term-memory.md)

    ---

    集成向量数据库与记忆提供器，实现RAG与持久化记忆

</div>

## 集成方案 { #integrations }

<div class="grid cards" markdown>

-   :material-puzzle:{ .lg .middle } [**模型上下文协议（MCP）**](model-context-protocol.md)

    ---

    在AI智能体中直接使用MCP工具

-   :material-leaf:{ .lg .middle } [**Spring Boot**](spring-boot.md)

    ---

    为Spring应用程序添加Koog功能

-   :material-cloud-outline:{ .lg .middle } [**Ktor**](ktor-plugin.md)

    ---

    将Koog集成至Ktor服务器

-   :material-chart-timeline-variant:{ .lg .middle } [**OpenTelemetry**](features/open-telemetry/index.md)

    ---

    通过主流可观测性工具实现智能体的追踪、日志记录与度量

-   :material-lan:{ .lg .middle } [**A2A协议**](a2a-protocol-overview.md)

    ---

    基于共享协议连接智能体与服务

</div>