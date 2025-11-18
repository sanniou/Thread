# NMB 功能模块 (`feature-nmb`)

## 概述 (Overview)

`feature-nmb` 是应用的核心功能模块，提供了浏览 NMB 岛匿名版所需的所有功能。它包含了板块列表、帖子列表、帖子详情、回复、收藏等核心用户界面的实现。

## 架构 (Architecture)

本模块严格遵循**单向数据流 (Unidirectional Data Flow - UDF)** 的 **MVI (Model-View-Intent)** 设计模式。该架构旨在提供一个可预测、可维护且高度可测试的代码结构。

数据流向如下：

**View -> Event -> ViewModel -> State -> View**

-   **View (Page/Screen)**: 用户与界面交互，触发 `Event`。
-   **Event**: 封装了用户的意图或动作（如点击、刷新）。
-   **ViewModel**: 接收 `Event`，调用相应的 `UseCase` 处理业务逻辑，并生成一个新的 `State`。
-   **State**: 一个不可变的纯数据类，完整地描述了当前 UI 应有的状态。
-   **View**: 订阅 `State` 的变化，并根据新的 `State` 重新渲染自己。

### 核心组件 (Core Components)

1.  **`Contract.kt`**
    -   每个功能页面的“契约”，以 `sealed interface` 的形式定义了该页面所有的 `State` 和 `Event`。
    -   **`State`**: 一个 `data class`，包含了渲染 UI 所需的**所有**数据（加载状态、错误信息、业务数据等）。它是 UI 状态的唯一真实来源 (Single Source of Truth)。
    -   **`Event`**: 一个 `sealed interface`，枚举了所有可能的用户交互和生命周期事件。

2.  **`ViewModel`**
    -   持有并管理一个 `StateFlow<State>`。
    -   提供一个 `onEvent(Event)` 方法作为所有事件的入口。
    -   **不直接**与数据源（Repository, Api, Database）交互，而是将业务逻辑委托给 `UseCase`。
    -   负责编排业务流程，并根据 `UseCase` 的结果更新 `State`。

3.  **`UseCase`**
    -   遵循**单一职责原则**，每个 `UseCase` 只封装一个独立的、可复用的业务操作（例如 `GetThreadDetailUseCase`, `ToggleFavoriteUseCase`）。
    -   负责与 `Repository` 或 `Database` 交互，执行具体的业务逻辑。

4.  **`Page` / `Screen` (View)**
    -   UI 的实现层，使用 Voyager 导航库和 Jetpack Compose 构建。
    -   职责非常纯粹：
        1.  订阅 `ViewModel` 暴露的 `StateFlow<State>`，并根据 `State` 的内容来渲染界面。
        2.  将用户的交互（如点击按钮）转换为对应的 `Event`，并发送给 `ViewModel`。
    -   View 本身是“哑”的，不包含任何业务逻辑。

## 如何添加新功能 (How to Add a New Feature)

在 `feature-nmb` 模块中添加一个遵循此架构的新功能页面（例如 "搜索页面"）的步骤如下：

1.  **创建 Contract**: 在 `workflow` 包下创建 `search` 子包，并添加 `SearchContract.kt` 文件，定义好 `State` 和 `Event`。
2.  **创建 UseCase**: 在 `domain` 包下创建处理搜索逻辑的 `UseCase`，例如 `SearchThreadsUseCase`。
3.  **创建 ViewModel**: 在 `workflow/search` 包下创建 `SearchViewModel.kt`，它接收 `SearchThreadsUseCase` 作为依赖，并实现 `onEvent` 方法来处理 `SearchEvent`。
4.  **创建 Page**: 在 `workflow/search` 包下创建 `SearchPage.kt` (Composable Screen)，它将观察 `SearchViewModel` 的状态并向其发送事件。
5.  **依赖注入**: 在 `di/Di.kt` 中注册新的 `SearchViewModel` 和 `SearchThreadsUseCase`。

## 主要依赖 (Key Dependencies)

-   **[Koin](https://insert-koin.io/)**: 用于依赖注入。
-   **[Voyager](https://voyager.adriel.cafe/)**: 用于 Compose Multiplatform 的导航。
-   **[Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)**: 用于实现列表的分页加载。
-   **[SQLDelight](https://cashapp.github.io/sqldelight/)**: 用于数据库操作，提供类型安全的 SQL 查询。
-   **[Ktorfit](https://foso.github.io/Ktorfit/)**: 用于类型安全地创建 HTTP 客户端。
