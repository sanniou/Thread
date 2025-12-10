# Thread - 一个多平台信息流聚合器

`Thread` 是一个使用 Kotlin Multiplatform 技术构建的信息流聚合平台，旨在将来自不同来源（如论坛、RSS）的信息整合到一个统一、现代且高性能的跨平台应用中。

## ✨ 核心功能 (Core Features)

-   **多源聚合**: 支持从多个信息源（当前已接入 NMB，规划中：NGA, Tieba, RSS, Mastodon）获取内容，并以统一的时间线呈现。
-   **可扩展架构**: 基于整洁架构（Clean Architecture），添加新的信息源或同步方式变得简单高效。
-   **数据同步**: （规划中）支持通过 WebDAV 或本地文件导入/导出用户数据。
-   **跨平台**: 一套代码库，可编译运行于 Android, iOS, Desktop (JVM) 等多个平台。

## 🏛️ 架构 (Architecture)

项目采用标准的整洁架构（Clean Architecture）与 MVI (Model-View-Intent) 模式，以确保代码的可维护性、可测试性和可扩展性。

### 1. 整体分层

-   **依赖倒置原则**: 上层模块不依赖于下层模块的具体实现，而是依赖于抽象。`Presentation` -> `Domain` <- `Data`。
-   **分层设计**:
    -   **`Presentation` (表示层)**: 由 `composeApp` 和 `feature-*` 模块组成，负责 UI 展示和用户交互。
    -   **`Domain` (领域层)**: 由 `core-domain` 模块实现。定义了应用的核心业务逻辑 (`UseCase`) 和数据抽象 (`Repository` 接口)，不依赖任何具体框架。
    -   **`Data` (数据层)**: 由 `core-data` 模块实现。负责实现 `Domain` 层的接口，处理所有数据的获取、存储和同步。
-   **依赖注入**: 使用 Kodein (DI) 实现各层之间的解耦。

### 2. UI 架构 (MVI & UDF)

UI 层（`feature-*` 模块）严格遵循 **单向数据流 (Unidirectional Data Flow - UDF)** 的 **MVI (Model-View-Intent)** 设计模式。

数据流向：**View -> Event -> ViewModel -> State -> View**

-   **View (Page/Screen)**: 用户与界面交互，触发 `Event`。
-   **Event**: 封装了用户的意图或动作（如点击、刷新）。
-   **ViewModel**: 接收 `Event`，调用相应的 `UseCase` 处理业务逻辑，并生成一个新的 `State`。
-   **State**: 一个不可变的纯数据类，完整地描述了当前 UI 应有的状态。UI 状态的唯一真实来源 (Single Source of Truth)。
-   **View**: 订阅 `State` 的变化，并根据新的 `State` 重新渲染自己。

#### 核心组件契约
-   **`Contract`**: 每个页面都有一个契约接口（如 `HomeContract`），定义了该页面的 `State`（数据类）和 `Event`（密封接口）。
-   **`ViewModel`**: 持有 `StateFlow<State>`，暴露 `onEvent(Event)` 方法。它不直接操作数据源，而是通过调用 `Domain` 层的 `UseCase` 来执行业务。

### 3. 数据层与缓存

数据库（SQLDelight）作为 `Data` 层的一个实现细节，充当远程数据的 **本地缓存**。

-   **`Repository` 模式**: 作为数据流的“总指挥官”，优先查询本地数据库。
-   **缓存策略**: 仅当本地数据缺失或过期时，才请求远程 `Source`。
-   **透明性**: `Domain` 和 `Presentation` 层不感知数据库的存在。

## 🛠️ 技术栈 (Tech Stack)

-   **[Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html)**: 跨平台开发框架。
-   **[Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)**: 用于构建声明式的、现代化的 UI。
-   **[Voyager](https://voyager.adriel.cafe/)**: 用于 Compose Multiplatform 的导航库。
-   **[Kodein](https://kodein.org/di/)**: 轻量级的依赖注入框架。
-   **[Ktorfit](https://foso.github.io/Ktorfit/)**: 基于 Ktor 的类型安全 HTTP 客户端。
-   **[SQLDelight](https://cashapp.github.io/sqldelight/)**: 生成类型安全的 Kotlin API 来操作 SQL 数据库。
-   **[Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)**: 用于实现列表的分页加载。

## 📦 模块划分 (Module Structure)

-   **`composeApp`**: 主应用模块，包含各平台入口和共享 UI。
-   **`feature-*`**: 各个独立的功能模块（如 `feature-nmb`），包含完整的 UI 业务闭环。
-   **`core-domain`**: **领域层**。包含核心业务逻辑 UseCase 和 Repository 接口。
-   **`core-data`**: **数据层**。负责所有数据的获取（Network/DB）与 Repository 实现。
-   **`core-common`**: 平台无关的通用工具。
-   **`core-ui`**: 跨功能模块共享的通用 UI 组件。

## 💻 开发指南 (Development Guide)

### 如何添加新源 (以 `Tieba` 为例)

1.  **`core-data` 模块 (数据实现)**
    -   创建 `source/tieba` 目录。
    -   定义 `TiebaApi` (Ktorfit) 和 `TiebaDto`。
    -   实现 `TiebaMapper` (Dto -> Domain Model)。
    -   实现 `TiebaSource` 接口。
    -   在 `di/Di.kt` 中注入。

2.  **`core-domain` 模块 (业务定义)**
    -   通常无需修改，除非有源特有的新业务逻辑（如特殊的详情页结构）。

3.  **`feature-tieba` 模块 (UI 实现)**
    -   创建新模块。
    -   实现 MVI 结构的 Screen 和 ViewModel。

4.  **`composeApp` 模块 (集成)**
    -   在 `settings.gradle.kts` 引入模块。
    -   配置导航逻辑。

### 如何开发 UI 功能页面 (以搜索页为例)

遵循 MVI 架构，在 `feature` 模块中：

1.  **创建 Contract**: 定义 `SearchState` (data class) 和 `SearchEvent` (sealed interface)。
2.  **创建 UseCase**: 在 `domain` 模块（或复用现有）定义业务逻辑，如 `SearchThreadsUseCase`。
3.  **创建 ViewModel**: 继承 `ViewModel`，实现 `onEvent`。处理事件，调用 UseCase，更新 State。
4.  **创建 Page**: 使用 Compose 实现 UI，订阅 ViewModel 的 State，并发送 Event。
5.  **依赖注入**: 在模块的 `DI` 文件中注册 ViewModel。

## 📈 项目状态与未来规划 (TODO)

-   [x] **架构重构**: 完成 Domain/Data 分层与 MVI 模式确立。
-   [x] **核心抽象**: 定义 Source、Repository 等核心接口。
-   [x] **数据迁移**: 将 NMB 数据逻辑迁移至 Data 层。
-   [x] **聚合信息流**: 实现聚合 Feed 的 UseCase 和 Repository。
-   [ ] **缓存完善**: 在 FeedRepositoryImpl 中集成完整的 SQLDelight 缓存策略。
-   [ ] **UI 完善**: 完善 `feature-nmb` 的详情页交互；实现 `feature-feed` 聚合页。
-   [ ] **数据同步**: 对接 WebDAV 服务。

## 🚀 如何构建与运行 (Build & Run)

1.  **环境要求**: JDK 17+, Android Studio (最新版), Xcode (macOS, 可选)。
2.  **构建**: `./gradlew build`
3.  **运行**:
    -   **Android**: 选择 `composeApp` 运行配置。
    -   **Desktop**: 执行 Gradle 任务 `:composeApp:run`。
    -   **iOS**: 打开 `iosApp` 项目或运行 `iosApp` 配置。

## 📚 参考资料 (References)

-   [X 岛匿名版 API 文档](https://github.com/TransparentLC/xdcmd/wiki/%E8%87%AA%E5%B7%B1%E6%95%B4%E7%90%86%E7%9A%84-X-%E5%B2%9B%E5%8C%BF%E5%90%8D%E7%89%88-API-%E6%96%87%E6%A1%A3)
-   [Material Design 3 - Adaptive Design](https://m3.material.io/foundations/adaptive-design)
-   [Compose Multiplatform Adaptive Navigation](https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation)
- https://github.com/lumina37/aiotieba 。 https://github.com/HuanCheng65/TiebaLite
