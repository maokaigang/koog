<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:59:42+00:00", "source_path": "features/index.md", "source_sha256": "c80f27a41f13c2d36ca8439483a77e8f8d2f1d4cfda852eda775ff4a50834d2c", "source_tag": "0.7.3", "translation_status": "changed"} -->
# 功能特性 { #features }

功能特性提供了一种扩展和增强 AI 智能体能力的方式。
通过功能特性，您可以：

- 为智能体添加新能力
- 拦截并修改智能体行为
- 记录和监控智能体执行过程
- 在单个功能特性内为同一事件类型注册多个处理器

Koog 框架内置了以下功能特性：

<div class="grid cards" markdown>

-   :material-flash:{ .lg .middle } [事件处理](agent-event-handlers.md)

    ---

    监控并响应智能体执行过程中的特定事件

-   :material-routes:{ .lg .middle } [追踪](tracing.md)

    ---

    捕获智能体运行的详细信息

-   :material-message-text-clock:{ .lg .middle } [对话记忆](chat-memory/index.md)

    ---

    存储和检索智能体运行之间的聊天消息历史

-   :material-chip:{ .lg .middle } [智能体记忆](agent-memory.md)

    ---

    在智能体运行期间及运行之间存储、检索和使用任意数据

-   :material-database-clock:{ .lg .middle } [长期记忆](long-term-memory.md)

    ---

    为 AI 智能体添加持久化记忆

-   :material-content-save-cog:{ .lg .middle } [智能体持久化](agent-persistence.md)

    ---

    在特定执行点保存和恢复智能体状态

-   :simple-opentelemetry:{ .lg .middle } [OpenTelemetry](open-telemetry/index.md)

    ---

    从智能体生成、收集和导出遥测数据（追踪信息）

</div>

要了解如何实现自定义功能特性，请参阅[自定义功能特性](custom-features.md)。