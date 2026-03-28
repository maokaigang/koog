<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:49:28+00:00", "source_path": "a2a-protocol-overview.md", "source_sha256": "1c309150478539f30d9579b23f94fc3d27e705d14186d2cbd65fefd9e2d4f374", "source_tag": "0.7.3", "translation_status": "changed"} -->
# A2A 协议 { #a2a-protocol }

本文档概述了 Koog 智能体框架中 A2A（Agent-to-Agent）协议的实现。

## 什么是 A2A 协议？ { #what-is-the-a2a-protocol }

A2A（Agent-to-Agent）协议是一种标准化的通信协议，使 AI 智能体能够相互交互并与客户端应用程序通信。
它定义了一组方法、消息格式和行为，以实现一致且可互操作的智能体通信。
有关 A2A 协议的更多信息和详细规范，请参阅
官方 [A2A 协议网站](https://a2a-protocol.org/latest/)。

## 快速开始 { #getting-started }

**重要提示**：A2A 依赖项**不**默认包含在 `koog-agents` 元依赖中。
您必须显式地将所需的 A2A 模块添加到您的项目中。

要在项目中使用 A2A，请根据您的用例添加依赖项：

- **对于 A2A 客户端**：请参阅 [A2A 客户端文档](a2a-client.md#dependencies)
- **对于 A2A 服务器**：请参阅 [A2A 服务器文档](a2a-server.md#dependencies)
- **对于 Koog 集成**：请参阅 [A2A Koog 集成文档](a2a-koog-integration.md#dependencies)

## 关键 A2A 组件 { #key-a2a-components }

Koog 提供了完整的 A2A 协议 v0.3.0 实现，包括客户端和服务器，以及与
Koog 智能体框架的集成：

- [A2A 服务器](a2a-server.md) 是一个智能体或智能体系统，它公开了一个实现 A2A 协议的端点。它
  接收来自客户端的请求，处理任务，并返回结果或状态更新。它也可以独立于 Koog 智能体使用。
- [A2A 客户端](a2a-client.md) 是一个客户端应用程序或智能体，它使用 A2A 协议发起与 A2A 服务器的通信。
  它也可以独立于 Koog 智能体使用。
- [A2A Koog 集成](a2a-koog-integration.md) 是一组类和实用工具，简化了 A2A 与 Koog 智能体的集成。
  它包含用于在 Koog 框架内实现无缝 A2A 智能体连接和通信的组件（A2A 功能和节点）。

更多示例，请参阅
[示例](https://github.com/JetBrains/koog/tree/develop/examples/simple-examples/src/main/kotlin/ai/koog/agents/example/a2a)