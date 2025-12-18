# Thread - 通用信息流聚合平台

`Thread` 是一个基于 Kotlin Multiplatform 构建的通用信息流平台。
我们的愿景是将互联网上碎片化的信息（如论坛讨论、社交动态、深度文章）抽象为标准化的数据流，通过统一、现代且高效的界面呈现给用户。

## ✨ 核心特性 (Core Features)

我们不再为每个来源（Source）构建独立的 App 或模块，而是提供三个高度抽象的通用业务模块：

1.  **通用论坛 (Forum)**:
    -   专为“板块-帖子-回复”结构设计。
    -   支持 **Tieba (贴吧)**, **NGA**, **Reddit**, **Discuz** 等社区型数据源。
    -   统一的楼层浏览体验与交互。

2.  **通用信息流 (Feed)**:
    -   专为“社交动态/时间线”结构设计，侧重短内容与实时互动。
    -   支持 **Twitter**, **Weibo (微博)**, **Mastodon**, **Bluesky** 等社交媒体流。
    -   提供聚合时间线阅读体验。

3.  **通用阅读器 (Reader)**:
    -   专为“内容订阅/沉浸式阅读”设计，侧重长文章与排版体验。
    -   支持 **RSS/Atom**, **固定链接 (URL 解析)**, **JSON 订阅源**, **HTML 解析**。
    -   提供纯净的阅读模式 (Readability)。

## 🏛️ 架构 (Architecture)

项目严格遵循 **Clean Architecture** (整洁架构) 与 **MVI** (Model-View-Intent) 模式。

### 1. 模块化设计

-   **`composeApp`**: 应用主入口，负责导航与依赖注入初始化。
-   **`core-domain`**: **核心大脑**。定义 UseCase（业务逻辑）和 Repository 接口。纯 Kotlin，无平台依赖。
-   **`core-data`**: **数据引擎**。负责实现 Repository，管理 API (Ktorfit) 和 数据库 (SQLDelight)。
-   **`core-ui`**: **设计系统**。包含主题、通用组件、尺寸定义。
-   **`feature-forum`**: 通用论坛业务 UI。
-   **`feature-feed`**: 通用社交流业务 UI。
-   **`feature-reader`**: 通用阅读器业务 UI。

### 2. 技术栈

-   **Kotlin Multiplatform**: 共享逻辑的核心。
-   **Compose Multiplatform**: 声明式 UI 框架。
-   **Voyager**: 跨平台导航。
-   **Kodein**: 依赖注入。
-   **Ktorfit / Ktor**: 网络请求。
-   **SQLDelight**: 数据库与缓存。

## 💻 开发指南 (Development Guide)

### 添加新的数据源 (Source)
添加一个新源（例如 `Tieba`）不再需要创建新的 UI 模块，只需在 `core-data` 中实现数据适配：

1.  **数据层 (`core-data`)**:
    -   在 `source/` 下新建 `tieba` 包。
    -   实现 API 定义和 `TiebaSource`。
    -   将数据映射为 Domain 层的通用模型 (`Post`, `Comment`)。
2.  **注册**:
    -   在 `SourceRepository` 中注册新源。
3.  **UI**:
    -   `feature-forum` 会自动通过通用接口展示来自 `Tieba` 的内容。

## 📈 路线图 (Roadmap)

-   [x] **架构重构**: 完成 Domain/Data 分层与 MVI 模式确立。
-   [x] **核心抽象**: 定义 Source、Repository 等核心接口。
-   [ ] **Feature - Forum**: 构建通用论坛模块，接管原 `feature-nmb` 功能。
-   [ ] **Feature - Feed**: 构建通用社交流模块，支持 Mastodon/Twitter。
-   [ ] **Feature - Reader**: 构建通用阅读模块，支持 RSS/URL 解析。
-   [ ] **数据同步**: 支持 WebDAV 数据备份与同步。

## 🚀 构建与运行

1.  **环境**: JDK 17+, Android Studio.
2.  **构建**: `./gradlew build`
3.  **运行**:
    -   Android: Run `composeApp`.
    -   Desktop: `./gradlew :composeApp:run`

## 📚 参考资料 (References)

-   [X 岛匿名版 API 文档](https://github.com/TransparentLC/xdcmd/wiki/%E8%87%AA%E5%B7%B1%E6%95%B4%E7%90%86%E7%9A%84-X-%E5%B2%9B%E5%8C%BF%E5%90%8D%E7%89%88-API-%E6%96%87%E6%A1%A3)
-   [Material Design 3 - Adaptive Design](https://m3.material.io/foundations/adaptive-design)
-   [Compose Multiplatform Adaptive Navigation](https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation)
- https://github.com/lumina37/aiotieba 。 https://github.com/HuanCheng65/TiebaLite
- discourse api :https://docs.discourse.org/
- https://github.com/goplayegg/AcfunQml/blob/d63d5fc2b4e570235f0dc5d92a4b8f65f08997f0/src/ui/global/AcService.qml#L568
