# Thread - 通用信息流聚合平台

`Thread` 是一个基于 Kotlin Multiplatform 构建的通用信息流平台。
我们的愿景是将互联网上碎片化的信息（如论坛讨论、社交动态、深度文章）抽象为标准化的数据流，通过统一、现代且高效的界面呈现给用户。

## ✨ 核心特性 (Core Features)

我们不再为每个来源（Source）构建独立的 App 或模块，而是提供三个高度抽象的通用业务模块：

1.  **通用论坛 (Forum)**:
    -   专为“板块-帖子-回复”结构设计。
    -   当前运行目录支持 **Tieba (贴吧)**、**NMB/X 岛** 与 **Discourse**；实验性来源只有形成可用闭环后才会开放。
    -   Discourse 支持在来源管理中新增多个独立实例、启停、编辑和删除。
    -   统一的楼层浏览体验、缓存策略与按能力交互。

2.  **通用信息流 (Feed)**:
    -   将论坛主题、Reader 文章与 Social 动态统一为按时间排序的跨来源时间线。
    -   支持来源筛选、并行分页、部分来源失败隔离和统一刷新反馈。
    -   生产 Social Connector 已接入 ActivityPub/Mastodon 兼容时间线；新平台只加 Connector，不改 UI 分支。

3.  **通用阅读器 (Reader)**:
    -   专为“内容订阅/沉浸式阅读”设计，侧重长文章与排版体验。
    -   支持 **RSS/Atom**, **固定链接 (URL 解析)**, **JSON 订阅源**, **HTML 解析**。
    -   支持按订阅源间隔自动刷新，以及 JSON/OPML 双向导入导出。
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
-   **SQLDelight 2.3.2 + AndroidX Paging 3.4.1**: Android/iOS/Desktop 共用数据库分页、远端协调与 Compose 分页 UI。

## 💻 开发指南 (Development Guide)

### 添加新的数据源 (Source)
添加一个新源不需要创建新的 UI 模块，也不需要修改 Domain 枚举：

1.  **数据层 (`core-data`)**:
    -   实现通用 `Source`，并按真实能力选择实现 Search、UserContent、Posting、Login、SubComment、Reaction Connector。
    -   将远程 DTO 映射为 Domain 层的 `Topic`、`Comment`、`Channel`。
    -   用户可创建多实例的来源再提供 `RuntimeSourceFactory`；固定来源直接提供 `RuntimeSourceRegistration`。
2.  **注册**:
    -   在组合根把固定来源或 factory 注册进 `SourceCatalog`；Repository 始终从实时目录查询，不保存 Source 快照。
3.  **UI**:
    -   `feature-forum`、`feature-feed` 会观察运行目录并按 `SourceCapabilities` 自动展示和降级。
4.  **合约门禁**:
    -   `RuntimeSourceRegistration` 会强制校验能力声明和 Connector 一一对应；虚假能力或隐藏 Connector 会在组合根创建时立即失败。
    -   Desktop 组合测试必须解析 NMB、Tieba、Discourse 的真实运行注册，新增来源不能只提交空实现或样例入口。

## 📈 路线图 (Roadmap)

-   [x] **架构重构**: 完成 Domain/Data 分层与 MVI 模式确立。
-   [x] **核心抽象**: 定义 Source、Repository 等核心接口。
-   [x] **Feature - Forum 基线**: 通用来源切换、登录、搜索、用户内容与按能力发帖/回复。
-   [x] **Feature - Feed 基线**: 论坛与 Reader 聚合、筛选、分页、刷新和详情跳转闭环。
-   [x] **运行时来源与缓存**: Discourse 多实例、来源管理、统一新鲜度/离线回退、Tieba 楼中楼与点赞。
-   [x] **Social Connector 契约**: 固定双向游标、作者身份、互动能力、内容警告和媒体网格；具体远端适配器按运行时来源接入。
-   [x] **Feature - Reader 基线**: RSS/Atom/JSON/HTML 来源、刷新诊断、阅读状态与收藏。
-   [x] **数据同步**: 版本化用户数据包、本地文本导入导出与 WebDAV 备份恢复。
-   [x] **Desktop 发布门禁**: Source 合约、数据库迁移/重启、真实组合根 smoke 与 Debian 包构建。
-   [x] **Desktop 现代工作区**: common 设计系统、全局导航 rail、功能侧栏、居中内容画布与核心页面统一改造。
-   [x] **全平台自适应工作区**: 手机底栏、平板紧凑 rail、Desktop 功能侧栏、超宽 Reader 三栏预览由同一 common 窗口能力模型驱动。
-   [x] **全功能自适应任务流**: 搜索、筛选、收藏批量操作、账号、发帖、来源管理、同步及 Forum/Reader 模态任务共用命令层和宽度能力模型。
-   [x] **缓存优先可靠性 UI**: 初始加载、空结果、缓存刷新失败、分页尾部失败和多来源局部失败使用统一可测试状态策略，已有内容不会被刷新错误覆盖。
-   [x] **离线与连接器可靠性门禁**: 可录制 HTTP 合约、结构化状态错误、官方 SQLDelight PagingSource、数据库不变量和离线种子启动探针组成可重复 Desktop 回归。
-   [x] **发布候选交互与性能**: 键盘/焦点/语义、长列表状态恢复、废弃 API 收敛、查询/Paging 性能预算与 Desktop 分发镜像终验。
-   [x] **产品完成度终验**: 统一命令面板、离线全局搜索、工作区会话恢复、来源健康中心与 Desktop 升级/诊断闭环。
-   [x] **连续工作区与安全运维**: Forum/Reader/Feed 精确恢复、缓存内详情续读、可恢复发布草稿、持久刷新历史、动态来源命令与脱敏诊断。
-   [x] **统一活动与身份中心**: 聚合发布草稿、刷新/同步作业、认证恢复、动态动作冲突和未来 Social Connector 状态。
-   [x] **跨页面动作执行层**: Reader、设置、运维、活动中心与命令面板共享冲突锁、持久历史、危险等级和完成反馈。
-   [x] **统一内容图与通知收件箱**: 来源通知、Reader 更新、智能集合和外部链接路由已收敛为跨平台可读状态。
-   [x] **生产 Social 与内容关系**: ActivityPub 运行时、Feed/Inbox/搜索接入、`thread://social` 内开详情、ContentGraph 相关内容与智能集合索引已进入 0.21.0 Desktop 门禁。
-   [x] **系统入口后半段**: Desktop 分享、启动深链、通知回跳、用户数据文件导入导出与 Reader+Social 后台刷新桥接已进入 0.22.0 Desktop 门禁。
-   [x] **安静产品体验层**: 动效 token、统一按钮、Feed 下拉刷新与缓存态、密度间距与排版节奏已进入 0.23.0 Desktop 门禁。
-   [x] **体验深潜第一波**: 设置/运维/活动/收件箱/详情主路径按钮统一、文章底栏操作条、列表卡片层级与分页反馈一致性已进入 0.24.0 Desktop 门禁。
-   [x] **体验深潜第二波**: 论坛次级对话框/重试、Reader 缓存态横幅、搜索/历史空态与关键动效 reducedMotion 已进入 0.25.0 Desktop 门禁。
-   [x] **产品打磨 0.26–0.29**: 去控制台味 eyebrow/按钮、Forum 缓存故事闭环、空工作区加源/导入主任务流、阅读节奏与安静动效，以及 forum/core-ui 英文本地化首波。
-   [x] **i18n 基建 0.30**: composeApp / reader / feed 建立 composeResources（zh+en）；高流量 `@Composable` UI 迁入 `stringResource`；Desktop 编译与 jvmTest 门禁通过。
-   [x] **i18n 全量 0.31**: ViewModel / side-effect `getString`、AppError 资源化、次级页 / semantics / filter labels；values-en 清零中文残留；Desktop 编译与 jvmTest 门禁通过。
-   [x] **发布候选（0.32）**: Desktop 全量门禁（jvmTest / compile / release 分发 / launcher smoke）通过；tag `v0.32.0` 已推送。真机 ActivityPub 登录回归仍可选。
-   [x] **体验一致性 0.33**: 主路径全页 loading 统一为 `ThreadLoadingState`（App/详情/用户/来源管理/订阅/Reader/Inbox）。
-   [x] **安静列表动效 0.34**: `threadAnimateItem` 进入主列表（Forum/Feed/Reader/Bookmark/User）；Bookmark/UserDetail 剩余全页 loading 收敛。
-   [x] **次级列表动效 0.35**: Search/Subscription/SourceManager/Trend/TopicDetail/GlobalSearch/Inbox 接入 `threadAnimateItem`；Inbox 分页尾部统一 `PagingAppendState`。

## 🚀 构建与运行

1.  **环境**: JDK 21；仓库通过 `.java-version` 与 CI 固定版本。
2.  **当前验证**: 本地执行 `./gradlew :core-domain:jvmTest :core-data:jvmTest :composeApp:jvmTest :composeApp:compileKotlinJvm :composeApp:createReleaseDistributable`；Ubuntu CI 额外执行 `:composeApp:packageReleaseDeb`。release 镜像将依赖合并为单一应用 JAR，并携带 JDBC、HTTP、management 与 instrumentation 所需 JDK 模块。
3.  **运行**:
    -   Android: Run `androidApp`.
    -   Desktop: `./gradlew :composeApp:run`

当前阶段采用 Desktop-first：先完成 common 架构和业务闭环，再按平台实现的实际价值补充 Android/iOS 验证。

当前架构约束、版本基线和分阶段目标见 [`doc/ARCHITECTURE.md`](doc/ARCHITECTURE.md)。
自适应 UI 的窗口分级、导航职责和页面组合规则见 [`doc/ADAPTIVE_UI.md`](doc/ADAPTIVE_UI.md)。

## 📚 参考资料 (References)

-   [X 岛匿名版 API 文档](https://github.com/TransparentLC/xdcmd/wiki/%E8%87%AA%E5%B7%B1%E6%95%B4%E7%90%86%E7%9A%84-X-%E5%B2%9B%E5%8C%BF%E5%90%8D%E7%89%88-API-%E6%96%87%E6%A1%A3)
-   [Material Design 3 - Adaptive Design](https://m3.material.io/foundations/adaptive-design)
-   [Compose Multiplatform Adaptive Navigation](https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation)
- https://github.com/lumina37/aiotieba 。 https://github.com/HuanCheng65/TiebaLite
- discourse api :https://docs.discourse.org/
- https://github.com/goplayegg/AcfunQml/blob/d63d5fc2b4e570235f0dc5d92a4b8f65f08997f0/src/ui/global/AcService.qml#L568
