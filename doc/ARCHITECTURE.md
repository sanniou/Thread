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
- Ktor 3.5.1、SQLDelight 2.3.2、AndroidX Paging 3.4.1、KSP 2.3.10、kotlinx.serialization 1.11.0

`core-data` 显式声明序列化与 SQLDelight 运行时依赖；已删除源码未使用、只会引入旧版传递依赖的 Kottage，避免依赖树隐式决定基础设施版本。

SQLDelight `androidx-paging3-extensions` 2.3.2、Paging common/compose 3.4.1 与 Compose Multiplatform 构成当前唯一分页栈。Paging 3.4 已覆盖 Android/Desktop JVM 与 iOS 等 Native 目标，项目不再保留历史 Cash Paging catalog；Repository、RemoteMediator、SQL 查询 PagingSource 和 UI collector 全部使用 `androidx.paging` 契约。

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
- CI 使用 JDK 21 执行上述门禁并生成 release Debian 安装包；本地非 Debian 环境以 `createReleaseDistributable` 验证运行镜像，产品版本统一来自 `gradle.properties`。发行构建将 runtime classpath 合并为单一 JAR，避免 Linux jpackage launcher 通过 pipe 传输超长 classpath 时的部分读取崩溃；自定义 runtime 显式包含 jdeps 识别的 SQL、HTTP、management、instrumentation 与 unsupported 模块。Ktor、Sketch 与 XML 的重复 ServiceLoader 描述符由应用层显式合并并由 JVM 测试固定，避免单 JAR 输出丢失可选运行时组件。

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

### 已完成的 G12：Desktop 工作区与 common 设计系统（2026-07）

- `core-ui` 建立统一的现代设计 token：安静的靛蓝/青色中性色板、Desktop 字体层级、圆角、间距、工作区/侧栏/阅读宽度，亮暗主题使用同一套语义。
- 组合根新增 92dp 全局工作区导航 rail；Forum、Reader、Feed 使用 304dp 功能侧栏，职责从“每页重复塞全局入口”拆成全局导航、功能筛选和内容画布三层。
- Forum 频道图片玻璃侧栏已删除；来源、趋势、功能与版块使用通用 Surface 层级。主题、趋势和详情统一限制内容宽度，主贴与回复使用一致的描边卡片。
- Reader、聚合 Feed、收藏、历史、文章详情和同步设置全部改为居中 Desktop 画布，复用 `PageHeader`、`ThreadCard`、`ModernEmptyState`、`SidebarHeader` 等 common 组件。
- Desktop 默认窗口提升为 1240x820 并保留原生窗口控件。此次 UI 改造没有新增 Android 专属实现，后续平台只在系统能力确有收益时分叉。
- 测试凭据仍按开发需求保留；通用运行时登录覆盖入口和来源管理入口未被 UI 改造移除。

### 已完成的 G13：全平台自适应 Forum / Reader 工作区（2026-07）

- `ThreadAdaptiveWindow` 把窗口归一为 Compact、Medium、Expanded、Large 四类；feature 只读取能力，不再各自维护 `BoxWithConstraints`、Desktop 判断和 drawer 分支。
- 全局导航在 Compact 使用底栏与完整 overflow sheet，在 Medium 使用无标签紧凑 rail，在 Expanded/Large 使用带标签工作区 rail；所有目标在任意宽度均可到达。
- Forum、Reader、Feed 共用 `AdaptiveSidebarScaffold`：窄/中窗口为 overlay，宽窗口为永久功能侧栏。旧 `LocalAppDrawer`、`DrawerMenuRow`、`DrawerHeader` 注入链删除。
- Reader 在 Large 窗口提供“来源—列表—文章预览”三栏，同一点击在其他宽度进入完整详情；文章列表图像摘要、搜索 Hero、沉浸阅读画布和字体缩放仍全部位于 common。
- Forum 主题标签改为可换行，主题卡片和列表 padding 读取窗口 token；主题详情与文章详情共用带最大阅读宽度的 `ReadingCanvas`。
- `ThreadTheme` 替代遗留训练样例命名，并增加 success/warning/reader/interactive 语义色。详细布局契约见 `doc/ADAPTIVE_UI.md`。
- Desktop JDK 21 下 `:composeApp:compileKotlinJvm` 通过；本阶段不执行 Android/iOS 编译或测试。

### 已完成的 G14：辅助任务流与全平台交互收敛（2026-07）

- `ThreadDetailScaffold` 统一搜索、订阅、用户、用户动态、发帖和来源管理的返回导航、页面身份、命令动作、Snackbar、IME 与内容宽度；页面不再各自拼装移动端 TopAppBar。
- `ThreadCommandBar`、`ThreadSearchField`、`ThreadFilterBar` 组成 common 命令层，在 Compact 纵向堆叠，在 Medium/Expanded/Large 横向使用空间。搜索、收藏、历史的过滤和批量操作共享同一交互词汇。
- `AdaptiveModal` 在 Compact 使用底部 sheet，在其他窗口使用最大宽高受控的聚焦 dialog。Reader 来源、订阅导入导出、论坛登录、引用、楼中楼、订阅 ID、Discourse 编辑和完整用户数据包全部迁入该边界。
- 发帖编辑器收敛为阅读宽度画布和 IME-aware scaffold；同步凭据在窄窗口纵排、宽窗口并排，动作使用可换行组，避免未来手机、平板或自由窗口溢出。
- 无导航入口、带未实现最近访问逻辑的旧 `ForumListPage`，重复的 Material adaptive list-detail 实验页和专用搜索 AppBar 已删除；正式 Channel/Reader 的统一 sidebar/list-detail 路径成为唯一实现。
- Desktop JDK 21 下 `:composeApp:compileKotlinJvm` 作为本阶段唯一编译门禁；Android/iOS 页面继续复用 common 实现。

### 已完成的 G15：缓存优先状态系统与详情页收敛（2026-07）

- `resolvePagingContentState` 把分页呈现变成可测试纯策略：只要存在本地行，refresh loading/error 都不能替换内容；无缓存时才进入全屏 loading、blocking error 或 empty。
- `PagingAppendState` 统一 Forum、Reader、Feed、搜索、订阅、收藏和用户动态的加载更多、尾部错误与完成反馈。feature 不再依赖自己的 Loading/Failed/End indicator。
- `ThreadLoadingState`、`ThreadErrorState`、`ThreadStatusBanner` 与可展开 `RefreshDiagnosticsBanner` 使用同一离线、认证、限流、服务端和未知错误词汇；多来源失败明确表达隔离结果并提供上下文重试。
- 主题列表/详情、趋势、文章详情、来源初始化和 Web 登录迁入 `ThreadDetailScaffold` 或 Context Hero；旧 `SaniouTopAppBar` 全家族以及未使用的 LoadableState、ErrorBanner、SaniouError/Loading 第二套状态链删除。
- Reader 原始页面使用现有 Compose Multiplatform WebView 的 URL/HTML state 真正渲染，不再展示占位符。图片查看器删除从未实现的平台截图按钮、capture modifier 和整份注释样例。
- `core-ui` commonTest 固定缓存优先与错误分类呈现；当前仍只执行 Desktop JVM 测试和组合根编译。

### 已完成的 G16：可靠性门禁与可维护发布（2026-07）

- Ktor 客户端有可注入 Engine 的正式边界；Discourse 录制式 fixture 固定成功、认证失效、限流、上传超限和部分服务失败，测试不访问生产站点。HTTP 非成功响应保留 status、受限响应上下文和 Retry-After，领域重试分类与 UI 诊断不再依赖偶然异常文本。
- NMB 订阅关系成为独立排序事实，不再错误依赖频道 TopicListing；最后一处自写页码 PagingSource 迁到 SQLDelight 官方 QueryPagingSource，自写 offset/page/keyset 三套实现全部删除。
- 数据门禁固定刷新失败不清缓存、刷新成功时数据和远程键同事务提交、订阅离线分页、递归评论、图片替换、动态来源命名空间清理及真实文件重启后的领域恢复；已有同步测试继续覆盖备份恢复幂等。
- Desktop `--smoke-check` 使用隔离的内存种子库，通过真实组合根解析 Forum 缓存主题和 Reader 已读/收藏文章，全程不访问网络，也不再读写用户数据库。
- 通用 Forum/Image DI 模块移除 NMB 历史命名；旧数字参数 Topic 工厂、未使用欢迎图链与空 Wire converter 删除。UI 中仅保留集中式 NMB 富文本协议策略，不存在按来源分叉布局。

### 已完成的 G17：发布候选交互、可访问性与性能门禁

- 全局导航、Feature 侧栏、命令层、模态层和详情页共用 Ctrl/Command 快捷键、焦点所有权、pane/selected/state 语义、48dp 触控目标与 Escape 恢复路径；Compose 平台 MotionDurationScale 仍是 reduced-motion 的唯一事实。
- Reader 按来源/筛选、Feed 按来源集合保存独立列表位置；分页行使用来源隔离稳定 key。Forum 导航、订阅和图片查询移除 Long 兼容层，以不透明 String ID 支持未来平台。
- schema v4 补齐文章、订阅、评论和主题组合索引，用户内容查询带 sourceId；查询计划测试固定索引命中。所有生产 Pager 收敛到两页首载、半页预取、十页上限的 common 策略。
- Material/Compose、datetime、XML、URL 与 coroutine 废弃 API 已清理或隔离；Desktop 完整测试、编译、发行 launcher 启动探针和 `createReleaseDistributable` 是 0.16.0 唯一发布门禁。

### 已完成的 G18：命令、离线发现与来源运维（2026-07）

- 组合根扩展为九个稳定工作区。Ctrl/Command+1–9、触控导航与 Ctrl/Command+Shift+P 命令面板使用同一目标表；面板可过滤工作区并直接搜索本地主题、回复和 Reader 文章，结果按原始上下文路由。
- `GlobalSearchRepository` 直接查询 SQLDelight 的 Topic、Comment、Article 缓存并返回轻量、来源隔离的导航投影；全局发现页支持内容类型筛选、命中统计、离线空状态和查询恢复，不与 Connector 的远程论坛搜索混为一体。
- `WorkspaceSessionRepository` 使用版本化 common 契约保存工作区、Forum 来源和发现查询。所有部分更新由 Mutex 串行并在锁内重新读取，命令导航、来源切换和搜索输入不会用旧快照覆盖彼此；组合根只在首次恢复完成后创建 Navigator，后续设置写入不会重置详情栈。
- 来源运维页组合实时 SourceCatalog、Reader 订阅、SQL 缓存统计和 RefreshCoordinator 状态，统一展示就绪、停用、刷新、离线、认证、限流和异常状态；论坛/Reader 都能单来源重试，失败诊断可独立清除。
- Desktop 数据库从不稳定的进程工作目录迁到 `~/.thread`；首次升级只在目标不存在时复制旧数据库及 WAL/SHM，绝不覆盖已有产品库。运维页公开实际平台数据目录并允许复制，Android/iOS 继续使用已有平台存储边界。
- 0.17.0 的组合测试以同一内存产品图验证三类离线搜索、缓存健康、认证降级、诊断清理和并发会话合并；数据库位置测试固定首次迁移与禁止覆盖的不变量。

### 已完成的 G19：连续阅读、恢复动作与诊断导出（2026-07）

- `WorkspaceSession` v2 保存 Forum 来源/频道/展开组、Reader 来源/筛选/查询、Feed 来源集合及各自稳定列表锚点；v1 JSON 自动迁移。主题和文章只在 SQLDelight 缓存仍存在时恢复详情，不为恢复动作访问网络。
- 发帖/回复草稿使用独立的来源与目标 key、版本和更新时间保存文本、选项及附件；编辑防抖落盘、返回前强制落盘、成功发布和显式丢弃都只清理当前草稿。
- `RefreshHistoryRepository` 在进程外保留最后成功、连续失败、失败类型和限流恢复时间；Operations 将持久历史、进程内任务和缓存年龄投影为同一来源健康状态。
- 命令面板由当前来源能力和状态生成单来源刷新、论坛连接器启停、认证恢复、Reader 全量同步与诊断导出操作；停用来源不会暴露无效刷新动作。
- 脱敏诊断只输出来源状态、计数、时间和能力，不读取账号、连接器 options、正文或绝对存储路径；认证字段、Bearer 和 URL 查询值在持久化前已替换。组合测试固定泄漏防护、v1/v2 恢复、草稿附件和 3,600 行混合离线缓存的有界结果窗口。

### 已完成的 G20：统一活动中心、身份状态与扩展平台契约（2026-07）

- 九工作区中的实验室主入口由 Activity Center 替换；刷新、认证、草稿、来源生命周期、Reader 转移和用户数据同步投影为同一可搜索、可筛选、可重试队列。成功记录可清理，进程中断的运行记录在下次启动显式降级为可重试失败。
- `SourceIdentityStatus` 是不含凭据的独立持久事实，覆盖匿名、有效、过期、停用和无需身份状态。登录成功、论坛刷新成功与认证失败更新同一 Repository；账号值、Cookie 和令牌不会进入活动或命令模型。
- `ProductActionExecutor` 以来源、Reader 库、用户数据、诊断和草稿为冲突域，统一执行页面、Activity Center 与命令面板动作。重叠动作立即失败，互不相关的来源仍可并行；动作声明危险等级、输出、影响 ID 和完成反馈。
- 草稿新增有界可观察索引，支持活动中心和命令面板恢复/丢弃；已移除来源的孤立草稿继续保留但不生成无效深链。Reader JSON/OPML、用户数据包和 WebDAV 工作流全部进入动态命令注册。
- Social/stream common 契约固定不透明双向 cursor、来源作者身份、互动集合、内容警告、媒体类型和网格上限。新增提供方只实现 Connector，UI 不增加 Mastodon、Bluesky 或未来平台分支。
- Desktop 门禁覆盖身份过期、冲突拒绝、动作历史重启、深链失效回退、154 项混合活动时间线和 Social 合约；SQLDelight 2.3.2 + Paging 3.4.1 + Compose Multiplatform 仍是唯一跨平台分页方案。

### 已完成的 G21：统一内容图、通知收件箱与跨平台入口（2026-07）

- 建立统一 `ContentReference` 与外部链接路由，把 Forum 主题/回复、Reader 文章、未来 Social 帖文和应用内深链解析为同一安全导航动作，并提供缺失内容的缓存/来源回退。
- 将论坛通知、提及、订阅更新、Reader 新文章与本地系统事件收敛到可分页、可批量已读、可按来源静音的通知收件箱；计数和列表使用 SQLDelight + Paging common 实现。
- 增加跨来源智能集合与规则：来源、作者、标签、未读、收藏、内容警告和媒体类型可以组合成保存视图，并直接驱动 Feed/Reader/Forum 的过滤和恢复状态。
- 完成主题/密度/字体/动效与阅读排版偏好的 common 配置，把可移植设置纳入用户数据包；Android/iOS/Desktop 壳只负责系统能力桥接。
- 提供一个 fixture 驱动的 Social 运行时适配器与大规模混合内容图测试，继续用 Desktop 完成测试、编译、发布镜像和真实 launcher smoke。

实现结果：

- `ContentReference` 覆盖主题、回复、文章、Social 帖文与外链；根级 `ContentLinkRepository` 只读取本地缓存判断可用性，RichText 统一委托根路由，未知 HTTP(S) 才交给系统浏览器，非安全协议被拒绝。
- schema v5 新增 Inbox 事件和来源静音偏好。旧论坛公告与 Reader 首次缓存文章进入同一事件表，主导航提供 SQLDelight `QueryPagingSource` 收件箱、实时计数、搜索、类型/来源筛选、批量已读、静音和删除。
- 智能集合保存完整跨源规则，并以最多 1,000 行的受控本地扫描解析 Forum/Comment/Reader 缓存；全局发现页可直接切换集合。主题标签、未读/收藏、作者、来源与媒体条件均由同一规则执行。
- `ThreadTheme` 接收持久化的主题、密度、字体、减弱动效、Reader 版心与行高；设置页可实时修改。用户数据包升级为 v2，并同步外观和智能集合设置。
- `FixtureSocialConnector` 提供不透明双向 cursor、能力校验、分页和幂等互动，154 条混合内容 fixture 固定内容警告、媒体与多页行为。Desktop 门禁增加 schema v5 迁移、Inbox 查询计划、240 条通知以及 70 个并发集合的上限测试。

### G22：生产 Social、内容关系与系统入口

- [x] 把 Social Connector 纳入运行时来源注册、统一 Feed 和 Inbox ingestion，提供至少一个真实协议适配器的认证、双向分页、内容警告、媒体和互动闭环。
- [x] 建立持久化内容关系图：引用、回复、转载、同作者、同标签与 canonical URL 去重形成可分页的“相关内容”和阅读路径，不让页面临时拼接关系。
- [x] 智能集合规则执行从受控缓存扫描升级为索引化 SQL 查询与物化投影；排序/分组/固定保留在 common 契约中。
- [x] common 深链 `thread://social/{sourceId}/{postId}`、搜索、Inbox 与 ContentLink 统一内开 Social 详情；分享/通知/后台刷新等系统入口继续按平台补齐。
- [x] Desktop 门禁覆盖 schema v6、Social 离线搜索、集合上限、产品图发现与 `composeApp` JVM 编译；发行镜像与 launcher smoke 仍沿用 0.20 既有路径。

### G22 后半 / 0.22.0：系统入口桥接

- [x] `core-ui` 暴露平台无关的分享、入口总线、文件导入导出、系统通知与后台刷新契约；Desktop 实现剪贴板分享、启动参数深链、托盘通知点击回跳与文件对话框。
- [x] App 统一消费入口队列：`thread://inbox` / `thread://feed` / 内容深链 / 用户数据包文件导入走同一路由边界。
- [x] 生命周期后台刷新桥接 Reader due-feeds 与 Social newer timeline；收件箱未读上升触发 Desktop 通知。

### 0.23.0：安静产品体验层

- [x] `ThreadMotion` / `threadTweenSpec` 尊重 reducedMotion，卡片与内容尺寸变化用短过渡。
- [x] 统一 `SaniouButton` 家族（Filled/Outlined/Text/Tonal/Danger）与 loading 态；错误与刷新诊断复用同一按钮体系。
- [x] Unified Feed 缓存态横幅 + PullToRefresh；刷新失败隔离文案本地化。
- [x] `ThreadCard` / `PageHeader` 间距跟随 interface density。
- [x] Desktop 编译/测试门禁覆盖平台契约与既有 jvmTest 矩阵。

### 0.24.0：体验深潜第一波

- [x] 设置 / 运维 / 活动中心 / 收件箱 / 产品确认框主路径按钮统一到 `SaniouButton` 家族，危险操作走 `SaniouDangerButton`，异步动作支持 loading。
- [x] 文章详情接入底部 `UnifiedActionBar`（收藏 / 分享 / 浏览器），阅读眉标与间距改用 human-case + density tokens。
- [x] Social 详情、分页 append/refresh 重试与 Reader 添加源对话框对齐共享按钮与间距体系。
- [x] `ArticleItem` 强化标题 / 摘要 / 元信息层级与自适应缩略图。
- [x] Desktop 编译与 jvmTest 门禁覆盖 UX 表面。

### 0.25.0：体验深潜第二波

- [x] 论坛次级路径（登录/跳页/来源管理/订阅 ID/引用弹层/用户与主题重试）统一 `SaniouButton`。
- [x] Reader 复用 `CacheStatusBanner`，与 Unified Feed 同一套缓存优先文案与刷新态。
- [x] History / Global Search 空态与加载态对齐 `ModernEmptyState` / `ThreadLoadingState`，搜索眉标中文化。
- [x] Channel 抽屉展开、Post 成功浮层、缓存条入场接入 `threadTweenSpec` 并尊重 reducedMotion。
- [x] Desktop 编译与 jvmTest 门禁覆盖 UX 表面。

### 0.29.0：产品打磨波（0.26–0.29）

- [x] 去控制台味：论坛次级页中文 human-case eyebrow；残留 Material 按钮扫尾到 `SaniouButton` 家族。
- [x] 多语言推进（首波）：`feature-forum` / `core-ui` 增加 `values-en`；产品 eyebrow / 空态 / 缓存条文案资源化。
- [x] 缓存故事闭环：Forum Channel 接入 `CacheStatusBanner`，失败可重试并跳转运维健康页（经 workspace session destination）。
- [x] 空工作区主任务流：无来源时引导加源/导入；Source Manager / Reader / Feed 空态补齐主路径 CTA。
- [x] 阅读节奏与安静动效：文章详情分隔与间距、Reader 预览入场 `threadTweenSpec`、主题卡片 content-size 动效。
- [x] Desktop 编译与 jvmTest 门禁覆盖 UX 表面。

### 0.30.0：composeResources 基建 + 可组合 UI 字符串迁移

- [x] `composeApp` / `feature-reader` / `feature-feed` 建立 `composeResources/values` + `values-en` 产品文案目录。
- [x] `feature-forum` / `core-ui` 扩展中英文字符串表；高流量 `@Composable` UI 迁入 `stringResource(Res.string.*)`。
- [x] ViewModel / `LaunchedEffect` / `scope.launch` / semantics 等非 Composable 上下文在 0.30 暂保留字面量；0.31 已用 suspend `getString` 清零。
- [x] Desktop 编译与 jvmTest 门禁覆盖 UX 表面。



### 0.31.0：非 Composable 全量多语言 + 次级页清零

- [x] ViewModel / `LaunchedEffect` / `scope.launch` snackbar·notification 使用 suspend `getString`。
- [x] `AppError.messageRes` + `localizedMessage()`；`toAppError` 映射到 core-ui 资源。
- [x] 次级页、filter label、semantics contentDescription、Emoticon 分组标题资源化；`stringResource` 禁止进入 semantics / 非 Composable lambda。
- [x] values-en 与 zh key 对齐，英文表不再含中文硬编码。
- [x] Desktop 编译与 jvmTest 门禁覆盖 UX 表面。


### 0.32.0：发布候选 Desktop 门禁

- [x] jvmTest + compileKotlinJvm 矩阵再验（core-domain / core-data / core-ui / composeApp）。
- [x] `createReleaseDistributable` 产出 main-release 分发镜像。
- [x] launcher `--smoke-check`：`Thread Desktop offline startup probe passed: discourse, nmb, tieba`。
- [ ] 真机 ActivityPub / 登录回归（可选，非 Desktop 门禁阻塞）。
- [x] git tag `v0.32.0` 已推送（指向 `424b05ff`）。


### 0.33.0：主路径全页 loading 一致性

- [x] App 会话恢复、Social/Article 详情、User、Source Manager、Subscription、Reader 列表、Inbox 初始刷新 → `ThreadLoadingState`。
- [x] 保留紧凑 spinner：分页 append、按钮 loading、图片预览、Post 发送 overlay。
- [x] Desktop compile + jvmTest 门禁通过。


### 0.34.0：安静列表进入动效 + loading 收尾

- [x] tag `v0.34.0` pushed.

- [x] `threadAnimateItem()` / fade+placement specs（reducedMotion → null）。
- [x] Forum 主题列表、Unified Feed、Reader 文章、Bookmark、User 主题卡片接入。
- [x] Bookmark 刷新 / UserDetail 回复初始加载 → `ThreadLoadingState`。
- [x] Desktop compile + jvmTest 门禁通过。


### 0.35.0：次级列表安静动效 + Inbox 尾部一致

- [x] tag `v0.35.0` pushed.

- [x] Search 主题/回复、Subscription、Source Manager、Trend、TopicDetail 回复列表接入 `threadAnimateItem`。
- [x] Global Search 结果行 + Inbox 事件卡片接入；`GlobalSearchResultRow(modifier, onOpen)` 兼容 trailing lambda。
- [x] Inbox append → `PagingAppendState`。
- [x] Desktop compile + jvmTest 门禁通过。


### 0.36.0：高频 i18n key 语义化

- [x] tag `v0.36.0` pushed.

- [x] 22 个高流量 `s_*` → `action_*` / `label_*` / `filter_*` / `a11y_*`。
- [x] 模块：composeApp / core-ui / feature-forum / feature-reader / feature-feed（zh+en 同步）。
- [x] Kotlin 引用与 import 同批更新；无旧 key 残留。
- [x] Desktop compile + jvmTest 门禁通过。


### 0.37.0：二波语义 key + 壳层列表动效

- [x] tag `v0.37.0` pushed.

- [x] ~50 个额外 `s_*` → error_/cache_/action_/label_/confirm_/filter_ 等（含 jvmMain）。
- [x] Activity Center / Operations 来源健康 / Command Palette 行接入 `threadAnimateItem`。
- [x] Desktop compile + jvmTest 门禁通过。


### 0.38.0：剩余次级列表动效 + Reader key 尾波

- [x] SubComments / Channel source chips / SubForum chips / Cookie reorder / AddFeed source pick → `threadAnimateItem`。
- [x] LazyVerticalGrid（表情格）不接 `LazyItemScope.animateItem`（receiver 不匹配）。
- [x] Reader 多引用 key：`label_web_view` / `label_unknown_source` / `label_feed_source`。
- [x] Desktop compile + jvmTest 门禁通过。

### 0.39.0：TiebaLite 覆盖 P0（搜索 + 发主题）

- [x] `TiebaSearchConnector`：hybrid `searchThread` → `ForumSearchConnector (+ searchChannelTopics / 0.47)`；DI 注册进运行目录。
- [x] Tieba `supportsSearch` / `supportsTopicCreation`；`createThread` 走 `addPost` 空 tid。
- [x] 对照文档：`docs/tieba-coverage-matrix.md`。
- [x] Desktop compile + jvmTest 门禁通过。

### 0.40.0：TiebaLite 覆盖 P1（关注/收藏/Inbox）+ 主路径收紧

- [x] `FavoriteRepository` 贴吧路径 remote-first：`likeForum` / `unlikeForum`。
- [x] 主题 Bookmark → official `addstore` / `rmstore`；`userLikeForum` 缓存关注吧。
- [x] `replyMe` / `atMe` 写入共享 Inbox；后台刷新同步收藏与消息。
- [x] 发帖/点赞共享 `ensureTbs`；管理功能（删帖/吧务）明确 deferred。
- [x] Desktop compile + jvmTest 门禁通过。

### 0.41.0：TiebaLite 覆盖 P2（签到 / 吧规 / 搜吧搜人 / agreeMe）

- [x] `TiebaChannelSign`（sign + mSign）+ `TiebaForumRuleService` → `ChannelActionRepository` / UseCases。
- [x] Topic 签到与吧规只读对话框；Channel 抽屉一键签到；`supportsChannelSign` / `supportsForumRules`。
- [x] hybrid `searchForum` / `searchUser` + Search CHANNEL/USER tabs。
- [x] Inbox 并入 `agreeMe`（SYSTEM /「赞了」）。
- [x] Desktop compile + jvmTest 门禁通过；删帖/吧务仍 deferred。

### 0.42.0：TiebaLite 覆盖 P3a（点踩 / 关注用户）

- [x] `ReactionConnector.downvote` + `TiebaReactionConnector`（opAgree `agree_type=5`）+ Topic `DislikeButton`（`hasDownvote`）。
- [x] `UserRelationConnector` / `TiebaUserRelationConnector`（profile + follow/unfollow by portrait）+ UserDetail 关注入口（`supportsUserFollow`）。
- [x] Runtime catalog + SourceConformance 对 user-relation 能力门校验。
- [x] Desktop compile + jvmTest 门禁通过；相册/资料编辑/submitDislike 后续；删帖/吧务仍 deferred。

### 0.43.0：看帖核心体验（倒序 / 跳页 / 楼中楼分页 / 图片流）

- [x] 倒序：`isReverse` 贯通 Source → TopicRepository reverse keyset → TopicDetail FilterBar。
- [x] 跳页：`JumpToPage` 清缓存并以 `startPage` 重建 Paging；FilterBar 页码入口 + 既有 PageJumpDialog。
- [x] 楼中楼：SubCommentsSheet 分页 Load more（page/pn）。
- [x] 图片流：`picPage` + `FetchTopicImagePageUseCase` + ImagePreview 远程 loadMore。
- [x] Desktop compile + jvmTest 门禁通过；相册编辑/屏蔽/submitDislike 后续；删帖/吧务 deferred。

### 0.44.0：推荐流「不感兴趣」(submitDislike)

- [x] `ReactionConnector.submitNotInterested` + `SubmitNotInterestedUseCase` / `ReactionRepository`。
- [x] `TiebaReactionConnector` → Official `submitDislike`（DislikeBean JSON）；`TrendTab.supportsNotInterested` + payload `channelId`。
- [x] Trend 推荐 tab 卡片入口；会话 dismiss filter + snackbar。
- [x] Desktop compile + jvmTest 门禁通过；资料编辑/屏蔽/云历史/设置 parity 后续；删帖/吧务 deferred。

### 0.45.0：本地内容屏蔽（关键词 / 用户）

- [x] Domain：`ContentBlock` + Matcher + Repository + UseCases。
- [x] Data：SQLDelight ContentBlockEntity schema v7 + RepositoryImpl。
- [x] Settings 管理 UI；Trend 客户端过滤匹配项。
- [x] Desktop compile + jvmTest 门禁通过；资料编辑/云历史/设置 parity 后续；删帖/吧务 deferred。


### 0.46 / 0.47 searchPost.0：资料编辑 (profileModify)

- [x] Domain：`ProfileEditRequest` + `updateProfile` + `UpdateUserProfileUseCase`；`supportsProfileEdit`。
- [x] Data：`TiebaUserRelationConnector` → Official `profileModify`。
- [x] UserDetail 本人编辑对话框（昵称/简介/性别）；头像 multipart 后续。
- [x] Desktop compile + jvmTest 门禁通过；云历史/设置 parity 后续；删帖/吧务 deferred。

## 0.48 Floor / sub-reply

`PostDraft.quotePostId` + `replyUserId` → Tieba Official `addPost(quote_id, repostid, reply_uid)`；UI: TopicDetail ThreadReply compose vs view-subcomments split; SubCommentsSheet compose.

## 0.49 ContentBlock surface expand

`ObserveContentBlocksUseCase` + `ContentBlockMatcher` now filter Trend, channel topic list (`TopicViewModel`), and topic replies/sub-comments (`TopicDetailViewModel`).

## 0.50 Modern UI refresh

Disable sketch-era hard-coded pink/bounce UI. Refresh `core-ui` palette/shapes/semantic surfaces and restyle main-path forum components:

- List: `TopicCard` hierarchy, channel/author/meta chips, image count, nested recent replies.
- Detail: `HeroTopicCard` / `FilterBar` / `ThreadReply` floor chips (no raw `ID:` sketch), quiet action row.
- Interaction: theme-colored `LikeButton` / `DislikeButton`, reducedMotion-aware `AnimatedIconButton`.
- Media: `ForumImageGrid` larger radius/spacing, single-image crop fill.
- Shell: `UnifiedActionBar` primary CTA + quiet border surface.

## 0.51 Redesign wave 2

Continue quiet redesign on secondary main paths and system states:

- Trend: `TrendItemCard` hierarchy + theme Rank badge (no hard-coded red/orange/yellow).
- Post: solid send CTA, quiet more-options panel, border-only bottom toolbar, success overlay without heavy shadow.
- Search: reply/channel/user cards drop sketch `No.` labels; name-first hierarchy.
- System: quieter `ModernEmptyState` / `ThreadLoadingState`; `ReferenceSheet` reference label.

## 0.52 Redesign wave 3 (shell coverage)

Expand the quiet border / zero-elevation language across navigation and remaining workspace surfaces:

- Shell: quieter `ContextHero`, border-only `AdaptiveModal`, soft rail brand, selected `AppDrawerItem` / `CacheStatusBanner`.
- Home: drawer grid tiles, announcement banner, forum item selection, subcategory boxes, metadata badges.
- User: cookie identity cards without elevation; relation header container; empty-state icon pill.
- Workspace: Bookmark cards, Operations metric tiles, Activity metric weight.

## 0.53 Redesign wave 4 (settings / media / feed / reader)

Page-level coverage for remaining primary workspaces:

- Settings: quieter section titles, preference sliders, denser group spacing.
- ImagePreview: bordered page-index pill, quiet end-of-list chip, tighter HUD padding.
- Feed: `TimelinePostCard` source chip hierarchy aligned with forum list language.
- Reader: drop sketch `QUICK READ` label; bordered source chip; Selector/AddFeed advanced panels zero-elevation.
- Edges: login dialog title, SourceInit without `ONE-TIME SETUP`, SourceManager badges/dialog titles.

## 0.54 Detail polish (secondary surfaces)

After page-level redesign coverage, tighten residual type weights and selection chrome:

- `PageHeader` / dialog titles → titleLarge or headlineSmall + SemiBold (no Bold/headlineLarge reading titles).
- Inbox metric tiles bordered + unread card soft primary alpha; CommandPalette selected row border-only.
- ArticleDetail / SocialDetail / RelatedContent quieter hierarchy; theme shape on social media.
