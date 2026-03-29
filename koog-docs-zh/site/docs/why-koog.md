<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:11:12+00:00", "source_path": "why-koog.md", "source_sha256": "acd1d400f2fa946989a590127e5a44dd89fd32dfffb368a2629e4dd30e61e87e", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 为什么选择 Koog { #why-koog }

Koog 旨在以 JetBrains 级别的质量解决现实世界的问题。
它提供先进的 AI 算法、开箱即用的成熟技术、Kotlin DSL，以及超越传统框架的稳健多平台支持。

## 与 JVM 和 Kotlin 应用程序的集成 { #integration-with-jvm-and-kotlin-applications }

Koog 提供了一种专为 JVM 和 Kotlin 开发者设计的 Kotlin 领域特定语言（DSL）。
这确保了与 Kotlin 和基于 Java 的应用程序的平滑集成，
显著提升生产力并优化整体开发者体验。

## 通过 JetBrains 产品进行实际验证 { #real-world-validation-with-jetbrains-products }

Koog 为多个 JetBrains 产品提供支持，包括内部 AI 智能体。
这种实际集成确保 Koog 能够持续针对实际用例进行测试、优化和验证。
它专注于实践中有效的方法，融合了来自广泛反馈和真实产品场景的洞察。
这种集成赋予 Koog 区别于其他框架的优势。

## 开箱即用的高级解决方案 { #advanced-solutions-available-out-of-the-box }

Koog 包含预构建、可组合的解决方案，以简化和加速智能体系统的开发，这使其区别于仅提供基础组件的框架：

* **多种历史压缩策略。** Koog 内置了先进的策略来压缩和管理长对话，无需手动尝试不同方法。经过机器学习工程师测试和优化的提示词、技术和算法，让您可以依赖经过验证的方法来提升性能。有关压缩策略的更多详情，请参阅[历史压缩](https://docs.koog.ai/history-compression/)。要了解 Koog 在实际场景中如何处理压缩和上下文管理，请查看[这篇文章](https://blog.jetbrains.com/ai/2025/07/when-tool-calling-becomes-an-addiction-debugging-llm-patterns-in-koog/)。
* **无缝的 LLM 切换。** 您可以随时将对话切换到不同的大型语言模型（LLM）及其新工具集，而不会丢失现有对话历史。Koog 会自动重写历史记录并处理不可用的工具，实现平滑过渡和自然的交互流程。
* **高级持久化功能。** Koog 允许您恢复完整的智能体状态机，而不仅仅是聊天消息。这支持诸如检查点、故障恢复，甚至回滚到状态机执行的任意时间点等功能。
* **健壮的重试组件。** Koog 包含一个重试机制，允许您将智能体系统中的任何操作集包装起来，并在满足可配置条件前进行重试。您可以提供反馈并调整每次尝试，以确保可靠的结果。如果 LLM 调用超时、工具未按预期工作或出现网络问题，Koog 能确保您的智能体保持韧性并有效运行，即使在临时故障期间也是如此。更多技术细节，请参阅[重试功能](https://docs.koog.ai/history-compression/)。
* **支持 Markdown DSL 的结构化类型流式输出。** Koog 流式传输 LLM 输出，并使用 Markdown DSL 将其解析为结构化、类型化的事件。您可以为特定元素（如标题、项目符号或正则表达式模式）注册处理程序，并实时接收相关部分。这种方法通过 Markdown 提供人类可读的反馈，并通过结构化类型提供机器可解析的数据，有效消除了不透明性并提升了用户体验。它确保了可预测的输出和具有渐进式内容渲染的动态用户界面。

## 广泛集成、多平台支持、增强的可观测性 { #broad-integration-multiplatform-support-enhanced-observability }

Koog 支持在各种平台和环境中开发和部署智能体应用：

*  **多平台支持**。您可以将智能体应用部署到 JVM、JS、WasmJS、Android 和 iOS 目标平台。
*  **广泛的 AI 集成**。Koog 集成了主流的 LLM 提供商，包括 OpenAI 和 Anthropic，以及企业级 AI 云如 Bedrock。同时支持本地模型，例如 Ollama。有关完整的可用提供商列表，请参阅 [LLM 提供商](https://docs.koog.ai/llm-providers/)。
*  **OpenTelemetry 支持**。Koog 提供与流行可观测性提供商（如 [W&B Weave](https://wandb.ai/site/weave/) 和 [Langfuse](https://langfuse.com/)）的开箱即用集成，用于监控和调试 AI 应用。借助原生 OpenTelemetry 支持，您可以使用系统中已有的相同工具来追踪、记录和测量您的智能体。了解更多信息，请参阅 [OpenTelemetry](https://docs.koog.ai/opentelemetry-support/)。
*  **Spring Boot 与 Ktor 集成**。Koog 与广泛使用的企业环境集成。
      * 如果您有 Ktor 服务器，可以安装 Koog 作为插件，通过配置文件配置提供商，并直接从任何路由调用智能体，无需手动连接 LLM 客户端。
      * 对于 Spring Boot，Koog 提供即用型 Bean 和自动配置的 LLM 客户端，让您轻松开始构建 AI 驱动的工作流。

## 与机器学习工程师和产品团队协作 { #collaboration-with-ml-engineers-and-product-teams }

Koog 的一个独特优势在于其与 JetBrains 机器学习工程师和产品团队的直接协作。
这确保了使用 Koog 构建的功能不仅是理论上的，而且经过实际产品需求的测试和优化。
这意味着 Koog 包含：

* **经过精细调优的提示词和策略**，针对实际性能进行了优化。
* **经过验证的工程方法**，通过产品开发发现并验证，如其独特的历史压缩策略。您可以在[这篇详细文章](https://blog.jetbrains.com/ai/2025/07/when-tool-calling-becomes-an-addiction-debugging-llm-patterns-in-koog/)中了解更多。
* **持续改进**，帮助 Koog 保持高效并适应不断变化的需求。

## 对开发者社区的承诺 { #commitment-to-the-developer-community }

Koog 团队致力于构建一个强大的开发者社区。
通过积极收集并采纳反馈，Koog 不断演进，以有效满足开发者的需求。
我们正在积极扩展对多样化 AI 架构的支持，提供全面的基准测试、详细的使用案例指南以及教育资源，以赋能开发者。

## 从何处开始 { #where-to-start }

* 在[概述](index.md)中探索 Koog 的能力。
* 通过我们的[快速入门](quickstart.md)指南构建您的第一个 Koog 智能体。
* 在 Koog [发布说明](https://github.com/JetBrains/koog/blob/main/CHANGELOG.md)中查看最新更新。
* 从[示例](https://docs.koog.ai/examples/)中学习。