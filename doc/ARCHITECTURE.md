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

仓库内的 Discourse 凭据是开发测试数据，允许保留以减少重复登录。每个 `SourceDescriptor` 可携带自己的开发凭据，通过 `DiscourseCredentialProvider` 初始化；通用 API 登录入口可在运行时覆盖它，动态请求头会在每次请求前读取当前账号，网络客户端无需重建。来源被编辑、禁用或删除时会释放账号观察任务和对应 HTTP Client。

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

- 将产品可用的来源按统一 `Source` 契约注册；实验性来源只有达到最小闭环后才进入运行目录。
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
- NGA、AcFun 等骨架 Connector 已退出运行目录和启动依赖，不再以“可选但不可用”的来源暴露给用户。
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
- `:core-domain:jvmTest` 通过，包含开放来源描述符、Source 能力、Connector Registry 与默认 Feed 选择规则测试。
- `:core-data:jvmTest` 通过，包含动态来源持久化/资源释放/身份变更失效、缓存新鲜度、Discourse 深层映射、刷新策略与 Tieba Connector 映射测试。
- `:composeApp:jvmTest` 覆盖真实组合根的 NMB/Tieba/Discourse 能力矩阵；文件数据库测试覆盖进程重启后的设置与 Reader 用户状态恢复。
- CI 使用 JDK 21 执行上述门禁并生成 Debian 安装包；本地非 Debian 环境以 `createDistributable` 验证运行镜像，产品版本统一来自 `gradle.properties`。

## 已完成的 G7：Connector 深水区与可观测刷新（2026-07）

- Tieba 登录会校验 Cookie、读取账号资料并持久化 UID、头像、STOKEN 与 TBS；测试凭据仍作为开发默认值保留，通用登录入口可覆盖它们。
- Tieba 用户主题与回复接入 `UserContentConnector`；真实回帖接入 `PostingConnector`，发送前自动刷新 TBS。能力拆分为 `supportsTopicCreation` 与 `supportsReplies`，当前只声明已闭环的回复能力。
- Forum 运行目录收敛为 NMB、Discourse、Tieba；无频道目录和产品闭环的 NGA、AcFun 不再注册，也不再初始化无价值的网络依赖。
- `RefreshCoordinator` 在 common 领域层定义统一契约，data 层提供任务串行、指数退避、离线/超时/限流/认证/服务端错误分类和诊断状态。
- Forum、Feed、Reader 共用刷新协调器；失败状态由 `core-ui` 通用横幅呈现，避免每个 feature 重复维护错误识别逻辑。
- 新增刷新重试/认证失败策略测试、Tieba DTO 到通用 Topic/Comment 的映射测试，并保留 Source capability/registry 的领域测试。

## 已完成的 G8：运行时来源、缓存一致性与论坛深度闭环（2026-07）

- `SourceType` 是开放值类型；`SourceDescriptor`、`RuntimeSourceFactory` 与 `SourceCatalog` 组成可持久化的实时运行目录。内置来源可启停，Discourse 可创建多个独立实例并即时编辑、删除，feature 与 Repository 不再保存 `Set<Source>` 快照。
- 来源目录更新先构建、再持久化，失败会回滚并释放临时资源；启动恢复不会覆盖用户同时发生的修改。每个 Discourse 实例拥有独立 URL、Cookie、账号、开发凭据、HTTP Client 和缓存命名空间。
- `CachePolicyProvider` 与 `CacheFreshnessStore` 为频道目录、主题详情、评论和 Reader Feed 定义统一新鲜度；主题/评论刷新精确清理对应缓存，网络失败时主题仍可返回旧缓存。
- Discourse 映射补齐绝对头像/图片 URL、标签、频道、作者身份、管理员状态和回复引用；搜索与用户内容沿用实例名称和 URL。
- Tieba 的楼中楼通过 `SubCommentConnector` 路由，楼中楼图片映射进入通用 `Image`；主贴点赞通过 `ReactionConnector`、Repository、UseCase 与详情页完整闭环，操作后精确失效主题缓存。
- NGA、AcFun 的不可用骨架源码已删除；未来来源必须通过独立 contract tests 和运行时 factory 接入，不能以空实现进入产品编译链。
- 新增来源描述符、运行目录持久化、缓存新鲜度和 Discourse 深层映射测试；Desktop 继续作为当前唯一验收平台。

## 下一阶段大范围 Goal

### 已完成的 G9：真实同步与 Reader 自动化（2026-07）

- 假 Local/WebDAV Provider 已删除。`SyncRepository` 现在提供版本化 `UserDataBundle`，覆盖来源描述符、Reader 订阅、收藏、文章阅读/收藏状态及必要设置；未知版本、重复 ID/URL 和无运行时 factory 的来源会在写入前被拒绝。
- 导入采用“先完整解码校验、再来源目录变更、后数据库事务”的策略；数据库失败时会恢复原来源目录。订阅按 ID/URL 合并，文章用户状态按更新时间合并，可重复恢复而不产生重复记录。
- WebDAV 使用 common Ktor 传输执行真实 GET/PUT 和 Basic Auth。组合根新增通用“数据与同步”页面，用文本数据包覆盖所有平台；文件选择器和安全凭据库仍遵守平台价值准入规则，不为当前 Desktop-first 阶段制造平台分叉。
- Reader `autoRefresh`、`refreshInterval` 与 `CachePolicyProvider` 已接入应用生命周期调度；同源刷新由 `RefreshCoordinator` 串行，失败按源隔离，手动刷新可强制绕过新鲜度。
- Reader 阅读/收藏状态进入独立 `ArticleUserState` 迁移表，远端文章覆盖不会再重置用户状态；RSS 增加 Atom fallback，JSON selector 支持点路径且解析失败不再伪装成空成功。
- Reader 支持 JSON/OPML 双向导入导出，编辑和自动刷新配置仍完全位于 common UI。新增数据包版本、同步幂等、Reader 编解码和 JSON 深路径测试。

### 已完成的 G10：Source 合约门禁与 Desktop 可发布产品（2026-07）

- `SourceConformance` 将能力声明定义为产品承诺；Search、UserContent、Posting、Login、SubComment、Reaction Connector 必须与声明精确对应，页大小与附件依赖也在运行目录装配时硬校验。
- 真实组合根 smoke 固定验证 NMB、Tieba、Discourse 的运行注册矩阵；数据库门禁同时覆盖 v1 到 v2 迁移和关闭/重开文件数据库后的设置、阅读与收藏状态恢复。
- 发帖附件从 common `AttachmentPicker` 边界进入，Desktop 仅实现具有明确价值的 AWT 图片选择；NMB 开放附件能力，其他来源在上传闭环完成前保持禁用。
- 历史记录使用正确来源名称并按来源路由论坛详情，文章分享可复制标题与链接；刷新协调器、错误页和重试策略共用同一失败分类词汇。
- 网络日志降为 INFO 并脱敏认证、Cookie 与 User API Key；无价值的 KCEF 启动画面和反射堆栈输出已清理。
- `.java-version`、统一版本号、发行说明和 Desktop CI 组成发布元数据；CI 固定 JDK 21 并运行领域/数据/组合根测试、JVM 编译与 Debian 打包。

### G11：论坛产品深度与离线体验

- 完成 Tieba/Discourse 附件上传能力，只有真实上传、发布与失败回滚 contract test 通过后才开放对应能力。
- 将最近访问、收藏标签筛选、引用图片持久化和楼中楼预览从数据层占位收敛为 common 业务闭环。
- 建立离线启动首页、缓存过期提示、手动重试和来源级故障隔离的 Desktop 交互 smoke，避免“有旧数据但页面不可用”。
- 补发行包启动验证、应用图标/桌面元数据和升级说明，再决定是否开始 Android 平台回归。

### 已完成的 G11：附件、内容持久化与离线 Desktop（2026-07）

- Discourse 按 composer upload 协议先上传附件、再发布 Markdown 引用；贴吧在 common 层按字节分片上传并生成官方 `#(pic,...)` 标记。上传失败不会触发发布副作用，两类来源均通过能力声明开放附件入口。
- `Channel` 拆分稳定 `sourceId` 与显示 `sourceName`；最近访问进入 schema v3 的来源隔离表，列表提供最近访问快捷组，动态来源删除会同步清理访问命名空间。
- 收藏筛选支持多标签交集语义，卡片标签可直接加入或移出筛选；重复 Paging collector 和可空 favicon 崩溃路径已清理。
- NMB 引用页图片会解析并保存到通用 Image 表；贴吧主回复携带楼中楼预览，缓存递归保存图片与父子关系，详情重启后可从本地恢复预览。
- `PagingStateLayout` 将已有缓存置于刷新状态之上：断网或单来源失败时保留可阅读内容，只显示来源无关的错误提示与重试入口。
- 0.10.0 Desktop 运行镜像包含发行元数据和 Linux 图标；CI 在 Debian 打包前执行无图形界面的组合根启动探针，并上传已通过启动检查的运行镜像。

### G12：可靠性门禁与可维护发布

- 为真实 HTTP Connector 增加可录制的请求/响应契约夹具，覆盖登录失效、限流、上传超限和部分成功，不依赖生产服务做回归。
- 建立数据库缓存不变量测试：分页刷新、父子评论、图片替换、来源删除和备份恢复必须在重启前后得到相同领域结果。
- 将 Desktop 启动探针扩展为离线种子数据库场景，并加入来源故障隔离、过期缓存提示和手动重试的交互状态测试。
- 收敛剩余 source 专属 UI 判断与历史命名，完成无价值平台分叉审计；之后再以同一 common 合约启动 Android 回归。
