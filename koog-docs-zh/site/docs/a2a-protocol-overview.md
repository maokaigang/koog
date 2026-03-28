<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T16:29:26+00:00", "source_path": "a2a-protocol-overview.md", "source_sha256": "1c309150478539f30d9579b23f94fc3d27e705d14186d2cbd65fefd9e2d4f374", "source_tag": "0.7.3", "translation_status": "changed"} -->
# A2A 协议 { #a2a-protocol }

本文档概述了在 Koog 智能体框架中实现的 A2A（Agent-to-Agent）协议。

## 什么是 A2A 协议？ { #what-is-the-a2a-protocol }

A2A（Agent-to-Agent）协议是一种标准化的通信协议，使 AI 智能体能够相互交互并与客户端应用程序通信。
它定义了一组方法、消息格式和行为，以实现一致且可互操作的智能体通信。
有关 A2A 协议的更多信息和详细规范，请参阅
官方 [A2A 协议网站](https://a2a-protocol.org/latest/)。

## 快速开始 { #getting-started }

**重要提示**：A2A 依赖项**不**默认包含在 `koog-agents` 元依赖中。
您必须根据项目需求显式添加所需的 A2A 模块。

要在项目中使用 A2A，请根据您的使用场景添加依赖：

- **作为 A2A 客户端**：请参阅 [A2A 客户端文档](a2a-client.md#dependencies)
- **作为 A2A 服务器**：请参阅 [A2A 服务器文档](a2a-server.md#dependencies)
- **用于 Koog 集成**：请参阅 [A2A Koog 集成文档](a2a-koog-integration.md#dependencies)

## 关键 A2A 组件 { #key-a2a-components }

Koog 完整实现了 A2A 协议 v0.3.0 的客户端和服务器端，并提供与
Koog 智能体框架的集成：

- [A2A 服务器](a2a-server.md) 是一个智能体或智能体系统，它暴露一个实现 A2A 协议的端点。它
  接收来自客户端的请求，处理任务，并返回结果或状态更新。它也可以独立于 Koog 智能体使用。
- [A2A 客户端](a2a-client.md) 是一个客户端应用程序或智能体，它使用 A2A 协议发起与 A2A 服务器的通信。
  它也可以独立于 Koog 智能体使用。
- [A2A Koog 集成](a2a-koog-integration.md) 是一组简化 A2A 与 Koog 智能体集成的类和工具。
  它包含用于在 Koog 框架内实现无缝 A2A 智能体连接和通信的组件（A2A 功能和节点）。

更多示例，请参考
[示例](https://github.com/JetBrains/koog/tree/develop/examples/simple-examples/src/main/kotlin/ai/koog/agents/example/a2a)