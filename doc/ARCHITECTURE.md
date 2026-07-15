# Thread 架构基线与 Goal

## 目标架构

```text
androidApp / iosApp
        |
        v
    composeApp  --------> feature-forum / feature-reader / feature-feed
        |                              |
        +-------------> core-ui <------+
        |                              |
        +-------------> core-domain <--+
                               ^
                               |
                    core-data + core-network
```

依赖方向以领域契约为中心：UI 与平台壳可以依赖领域层，数据层实现领域接口；领域层不能引用 Ktor multipart、SQLDelight 实体或具体 Source 实现。

## 平台实现准入规则

默认实现放在 `commonMain`。只有满足下列条件之一才新增平台实现：

- 系统入口：Android `Application` / `Activity`、iOS App 入口。
- 平台资源：Android `Context`、系统相册、文件目录、权限。
- 原生驱动：SQLDelight Android/iOS/JVM Driver、平台网络 Engine、平台 I/O Dispatcher。
- 平台 UX 的收益明确高于维护成本，且无法由 Compose Multiplatform 通用实现覆盖。

平台代码不能反向依赖 feature。Android `Context` 由 `androidApp` 初始化后交给 data 平台桥接，feature 不再承担 Application 所有权。

共享数据代码统一使用 `ioDispatcher` 契约：Android/JVM 映射到 `Dispatchers.IO`，iOS 映射到公开可用的 `Dispatchers.Default`。SQLDelight 保留异步共享接口，并在 iOS Driver 边界把 Schema 转为同步 Schema；这些差异不进入 Repository 和 feature。

## 当前基线（2026-07）

- Kotlin 2.4.10
- Compose Multiplatform 1.11.1
- Android Gradle Plugin 9.1.0
- Gradle 9.5.0
- Android compile/target SDK 36，min SDK 24
- JVM target 17
- Ktor 3.5.1、SQLDelight 2.3.2、KSP 2.3.10、kotlinx.serialization 1.11.0

`core-data` 显式声明序列化与 SQLDelight 运行时依赖；已删除源码未使用、只会引入旧版传递依赖的 Kottage，避免依赖树隐式决定基础设施版本。

Android API 37 暂不进入基线：它需要超出 Kotlin 2.4 官方兼容矩阵的 AGP 版本。待兼容矩阵更新后整体升级，不使用未经支持的版本拼接。

## 凭据与登录

仓库内的 Discourse 凭据是开发测试数据，允许保留以减少重复登录。它通过 `DiscourseConnectionConfig.developmentApiKey` 初始化 `DiscourseCredentialProvider`，不再散落在请求实现中；用户页的通用 API 登录入口可以在运行时覆盖该默认值，网络客户端无需重建。

## 分阶段 Goal

### G1：构建与模块地基

- Android 应用壳从 KMP 共享模块拆分为 `androidApp`。
- 所有共享模块迁移到 AGP 9 Android-KMP Library 插件。
- 清除 feature 对 Android Application 和 `Context` 的所有权。
- 全平台 JVM target 统一为 17，移除不可构建的 SNAPSHOT 依赖。
- 验收：Gradle 配置成功，Desktop 组合根及全部 common 依赖链编译通过。

### G2：领域边界收敛

- `core-domain` 只保留通用模型、能力、Repository 与 UseCase。
- 发帖附件使用 `PostDraft` / `PostAttachment`，multipart 组装留在 data。
- Source 能力只有一套 `SourceCapabilities`。
- feature 逐步停止直接引用具体 Source、DataPolicy、DTO 和 data DI module。
- 验收：feature 只依赖 domain/core-ui；具体 connector 可独立替换。

### G3：Connector 插件化

- 将 NMB、Tieba、Discourse、AcFun、NGA 按统一 `Source` 契约注册。
- 用能力标志驱动 UI 降级，禁止 UI 通过 `sourceId` 硬编码业务分支。
- 登录、初始化、分页、发帖、趋势按可选能力接口拆分。
- 验收：新增一个 Source 不修改 Forum 主流程，只增加 connector、映射与 DI 注册。

### G4：功能闭环

- Forum：初始化、登录、列表、详情、回复/发帖、搜索、订阅闭环。
- Reader：RSS/Atom 添加、解析、刷新、阅读状态与收藏闭环。
- Feed：实现聚合时间线与刷新策略，删除空实现。
- 验收：每个 feature 至少一条 commonTest 业务链路，并有 Desktop smoke build。

### G5：质量与发布

- 加入领域、Repository、解析器和迁移测试。
- CI 固定 JDK 17+，执行 common tests、Android assemble 与 desktop package 检查。
- 对失败源隔离、缓存一致性、数据库迁移和离线场景建立回归门禁。
- 验收：无未说明的 `TODO()` 运行时崩溃路径，发布构建可重复生成。

## 已完成的 Feed/Reader 闭环（2026-07）

- `Source.getFeedPage` 提供 channel-based Connector 的 common 默认实现，并允许 Connector 自定义游标。
- `SourceRepository` 并行聚合多个来源，按来源隔离失败，稳定去重并按发布时间排序。
- NGA、AcFun 等骨架 Connector 通过能力声明退出聚合主流程，不再触发运行期 `TODO()`。
- Feed 同时合并论坛主题和 Reader 文章，支持来源筛选、分页、刷新报告和详情跳转。
- Reader 刷新改为显式 `Result` / `ReaderRefreshReport`，不再吞掉网络和解析异常。
- `feature-feed` 已接入组合根导航，并提供 Desktop 永久侧栏与窄窗口抽屉布局。

## 已完成的 Connector Registry 横向改造（2026-07）

- `DefaultConnectorRegistry` 统一注册 `Source` 及 Search、UserContent、Posting、Login 四类可选能力，并拒绝同来源重复注册。
- Forum 搜索、用户内容、发帖/回复、用户登录全部通过 `sourceId + capability` 路由；页面参数不再使用 NMB 专属的 `fid/resto` 整数模型。
- NMB 保留本地搜索、用户内容、multipart 发帖与饼干登录；NMB HTML 错误解析下沉到 Posting Connector。
- Discourse 接通 `search.json`、`user_actions.json`、`posts.json`，支持搜索、用户内容、主题/回复发布及 User API Key 运行时覆盖。
- Tieba WebView 登录合并到通用 Cookie 捕获流程，BDUSS/STOKEN 通过 Login Connector 持久化；旧 Tieba 专用页面和固定账号仓库已删除。
- Source 初始化改为通用流程；UI 依据 `SourceCapabilities` 显示搜索、发帖、回复和登录入口，不再通过来源 ID 决定平台功能。
- Topic 元数据的分页大小、能力和公开 URL 由 Source 提供，不再在共享 Mapper 中硬编码 NMB/Tieba 分支。

## 当前验证策略与结果

当前阶段只验证 Desktop；common 代码仍保持跨平台边界，但不在每轮重复消耗时间编译 Android/iOS。

- `:composeApp:compileKotlinJvm` 通过，覆盖 Feed、Forum、Reader、core-data 与数据库代码生成链。
- `:core-domain:jvmTest` 通过，包含 Source 能力与默认 Feed 选择规则测试。

## 下一阶段大范围 Goal

### G7：Connector 深水区与可观测刷新

- 完成 Tieba 用户主题/回复与安全可验证的回帖能力，能力未闭环前不声明支持；补全登录后的用户资料和 TBS 刷新。
- 将 NGA 从骨架替换为可运行 Connector；重新评估 AcFun Forum 能力，无法形成产品价值的入口从 Source 目录移除。
- 为 Forum、Reader、Feed 建立共享刷新调度器、离线状态、失败来源诊断、退避重试与缓存失效策略。
- 增加 Connector contract tests 和 Desktop smoke 场景：来源切换、Discourse 搜索/发帖、Tieba Cookie 捕获、聚合分页、离线回退。
- 清理 Discourse/NMB 映射中的遗留 TODO 与硬编码展示文本，并将来源实例配置扩展为可注册的多实例描述符。
- 完成后统一执行一次 Desktop 验证并全量提交，不按单一页面或单一 Connector 拆成小补丁。
