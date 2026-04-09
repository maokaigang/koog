<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T12:56:45+00:00", "source_path": "data-transfer-between-nodes.md", "source_sha256": "b17b7895d96773cbb4371394c34bc0fad66bc1d1f21db315687d260397e17482", "source_tag": "0.7.3", "translation_status": "changed"} -->
## 概述 { #overview }

Koog 提供了一种使用 `AIAgentStorage` 存储和传递数据的方式，这是一个键值存储系统，设计为在不同节点甚至子图之间传递数据的类型安全方法。

该存储可通过智能体节点中可用的 `storage` 属性（`storage: AIAgentStorage`）进行访问，允许在 AI 智能体系统的不同组件之间无缝共享数据。

## 键值结构 { #key-and-value-structure }

键值数据存储结构依赖于 `AIAgentStorageKey` 数据类。有关 `AIAgentStorageKey` 的更多信息，请参阅以下部分。

### AIAgentStorageKey { #aiagentstoragekey }

存储使用类型化键系统，在存储和检索数据时提供类型安全。

`AIAgentStorageKey<T>` 数据类表示用于标识和访问数据的存储键。以下是该类的主要特性：

- 泛型类型参数 `T` 指定与此键关联的数据类型，提供类型安全。

- 每个键都有一个 `name` 属性，这是一个字符串标识符，便于识别和调试。

- 每个键实例都是唯一的。`name` 不用于确定唯一性，因此可以有多个具有相同名称的键。这允许重用现有的策略组件，而不会意外覆盖存储中的数据。

## 使用示例 { #usage-examples }

以下部分提供了创建存储键并使用它存储和检索数据的实际示例。

### 定义表示数据的类 { #defining-a-class-that-represents-your-data }

存储要传递的数据的第一步是创建一个表示数据的类。以下是一个包含基本用户数据的简单类示例：

=== "Kotlin"

    ```kotlin
    class UserData(
       val name: String,
       val age: Int
    )
    ```

=== "Java"

    ```java
    record UserData(
        String name,
        int age
    ) {}
    ```

定义完成后，按照以下描述使用该类创建存储密钥。

### 创建存储密钥 { #creating-a-storage-key }

为定义的数据结构创建类型化存储键：

=== "Kotlin"

    ```kotlin
    val userDataKey = createStorageKey<UserData>("user-data")
    ```

=== "Java"

    ```java
    AIAgentStorageKey<UserData> userDataKey = AIAgentStorage.createStorageKey("user-data");
    ```

`createStorageKey` 函数接收一个字符串参数，该参数用于唯一标识密钥。

### 存储数据 { #storing-data }

要使用已创建的存储键保存数据，请在节点中使用 `storage.set(key: AIAgentStorageKey<T>, value: T)` 方法：

=== "Kotlin"

    ```kotlin
    val nodeSaveData by node<Unit, Unit> {
        storage.set(userDataKey, UserData("John", 26))
    }
    ```

=== "Java"

    ```java
    var nodeSaveData = AIAgentNode.builder("nodeSaveData")
        .withInput(String.class)
        .withOutput(String.class)
        .withAction((input, ctx) -> {
            ctx.getStorage().set(userDataKey, new UserData("John", 26));
            return "";
        })
        .build();
    ```

### 正在检索数据 { #retrieving-data }

要检索数据，请在节点中使用 `storage.get` 方法：

=== "Kotlin"

    ```kotlin
    val nodeRetrieveData by node<String, Unit> { message ->
        storage.get(userDataKey)?.let { userFromStorage ->
            println("Hello dear $userFromStorage, here's a message for you: $message")
        }
    }
    ```

=== "Java"

    ```java
    var nodeRetrieveData = AIAgentNode.builder("nodeRetrieveData")
        .withInput(String.class)
        .withOutput(String.class)
        .withAction((message, ctx) -> {
            var userData = ctx.getStorage().get(userDataKey);
            System.out.println("Hello dear %s, here's a message for you: %s".formatted(userData, message));
            return "";
        })
        .build();
    ```

## API 文档 { #api-documentation }

有关 `AIAgentStorage` 类的完整参考，请参阅 [AIAgent存储](api:agents-core::ai.koog.agents.core.agent.entity.AIAgentStorage)。

有关 `AIAgentStorage` 类中可用的独立函数，请参阅以下 API 参考资料：

- [clear](api:agents-core::ai.koog.agents.core.agent.entity.AIAgentStorage.clear)
- [get](api:agents-core::ai.koog.agents.core.agent.entity.AIAgentStorage.get)
- [getValue](api:agents-core::ai.koog.agents.core.agent.entity.AIAgentStorage.getValue)
- [putAll](api:agents-core::ai.koog.agents.core.agent.entity.AIAgentStorage.putAll)
- [remove](api:agents-core::ai.koog.agents.core.agent.entity.AIAgentStorage.remove)
- [set](api:agents-core::ai.koog.agents.core.agent.entity.AIAgentStorage.set)
- [toMap](api:agents-core::ai.koog.agents.core.agent.entity.AIAgentStorage.toMap)

## 附加信息 { #additional-information }

- `AIAgentStorage` 是线程安全的，它使用互斥锁来确保并发访问得到正确处理。
- 在检索数值时，类型转换会自动处理，确保整个应用程序的类型安全。
- 对于非空值访问，请使用 `getValue` 方法，该方法会在键不存在时抛出异常。
- 您可以使用 `clear` 方法完全清空存储，该方法会移除所有已存储的键值对。