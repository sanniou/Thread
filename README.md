# Thread - 通用信息流聚合平台

`Thread` 是一个基于 Kotlin Multiplatform 构建的通用信息流平台。
我们的愿景是将互联网上碎片化的信息（如论坛讨论、社交动态、深度文章）抽象为标准化的数据流，通过统一、现代且高效的界面呈现给用户。

## ✨ 核心特性 (Core Features)

我们不再为每个来源（Source）构建独立的 App 或模块，而是提供三个高度抽象的通用业务模块：

1.  **通用论坛 (Forum)**:
    -   专为“板块-帖子-回复”结构设计。
    -   当前运行目录支持 **Tieba (贴吧)**、**NMB/X 岛** 与 **Discourse**；实验性来源只有形成可用闭环后才会开放。
    -   统一的楼层浏览体验与交互。

2.  **通用信息流 (Feed)**:
    -   将论坛主题与 Reader 文章统一为按时间排序的跨来源时间线。
    -   支持来源筛选、并行分页、部分来源失败隔离和统一刷新反馈。
    -   社交平台 Connector 可在统一 `TimelineItem` 契约上继续扩展。

3.  **通用阅读器 (Reader)**:
    -   专为“内容订阅/沉浸式阅读”设计，侧重长文章与排版体验。
    -   支持 **RSS/Atom**, **固定链接 (URL 解析)**, **JSON 订阅源**, **HTML 解析**。
    -   提供纯净的阅读模式 (Readability)。

## 🏛️ 架构 (Architecture)

项目以 **Clean Architecture**（整洁架构）与 **MVI**（Model-View-Intent）为目标，优先复用 `commonMain`；只有系统入口、数据库驱动、文件目录等确实依赖平台能力的代码才放入平台 source set。

### 1. 模块化设计

-   **`androidApp`**: Android 应用壳，只负责 `Application`、`Activity` 和平台初始化。
-   **`composeApp`**: 跨平台组合根，负责导航、主题与依赖注入装配，同时作为 Desktop 入口。
-   **`core-domain`**: **核心大脑**。定义通用模型、能力契约、UseCase 和 Repository 接口，不引用 Ktor、SQLDelight 或具体数据源 DTO。
-   **`core-data`**: **数据引擎**。负责实现 Repository，管理 API (Ktorfit) 和 数据库 (SQLDelight)。
-   **`core-network`**: 通用网络基础设施，不包含具体业务 UI。
-   **`core-ui`**: **设计系统**。包含主题、通用组件、尺寸定义。
-   **`feature-forum`**: 通用论坛业务 UI。
-   **`feature-feed`**: 统一聚合时间线、来源筛选与刷新状态 UI。
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
-   [x] **Feature - Forum 基线**: 通用来源切换、登录、搜索、用户内容与按能力发帖/回复。
-   [x] **Feature - Feed 基线**: 论坛与 Reader 聚合、筛选、分页、刷新和详情跳转闭环。
-   [ ] **Social Connector**: 在统一 Feed 契约上接入 Mastodon/Bluesky 等来源。
-   [x] **Feature - Reader 基线**: RSS/Atom/JSON/HTML 来源、刷新诊断、阅读状态与收藏。
-   [ ] **数据同步**: 支持 WebDAV 数据备份与同步。

## 🚀 构建与运行

1.  **环境**: JDK 17+, Android Studio.
2.  **当前验证**: `./gradlew :composeApp:compileKotlinJvm :core-domain:jvmTest :core-data:jvmTest`。
3.  **运行**:
    -   Android: Run `androidApp`.
    -   Desktop: `./gradlew :composeApp:run`

当前阶段采用 Desktop-first：先完成 common 架构和业务闭环，再按平台实现的实际价值补充 Android/iOS 验证。

当前架构约束、版本基线和分阶段目标见 [`doc/ARCHITECTURE.md`](doc/ARCHITECTURE.md)。

## 📚 参考资料 (References)

-   [X 岛匿名版 API 文档](https://github.com/TransparentLC/xdcmd/wiki/%E8%87%AA%E5%B7%B1%E6%95%B4%E7%90%86%E7%9A%84-X-%E5%B2%9B%E5%8C%BF%E5%90%8D%E7%89%88-API-%E6%96%87%E6%A1%A3)
-   [Material Design 3 - Adaptive Design](https://m3.material.io/foundations/adaptive-design)
-   [Compose Multiplatform Adaptive Navigation](https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation)
- https://github.com/lumina37/aiotieba 。 https://github.com/HuanCheng65/TiebaLite
- discourse api :https://docs.discourse.org/
- https://github.com/goplayegg/AcfunQml/blob/d63d5fc2b4e570235f0dc5d92a4b8f65f08997f0/src/ui/global/AcService.qml#L568
