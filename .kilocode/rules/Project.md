# Project.md - AI 开发手册

## 1. 项目愿景 (Identity)
**Thread** 是一个基于 Kotlin Multiplatform 的通用信息流聚合平台。
目标是将碎片化的互联网信息（论坛帖子、社交动态、RSS 订阅、长文章）统一抽象为标准化的数据流，并通过高度一致、现代化的 UI 进行展示。

**核心模块策略**:
我们不再为每个数据源（如 Tieba, NGA）创建独立的 feature 模块。相反，我们构建三个高度抽象的通用业务模块：

- **`feature-forum`**: **通用论坛模块** (Forum)。
    - **职责**: 用于展示“板块-帖子-回复”结构的内容。
    - **适用源**: Tieba (贴吧), NGA, Reddit, Discuz 等社区。

- **`feature-feed`**: **通用信息流模块** (Social Feed)。
    - **职责**: 用于展示“社交动态/时间线”结构的内容，强调短内容与互动。
    - **适用源**: Twitter, Weibo (微博), Mastodon, Bluesky。

- **`feature-reader`**: **通用阅读模块** (Content Reader)。
    - **职责**: 用于内容订阅、解析与沉浸式阅读，强调长文章体验。
    - **适用源**: RSS/Atom, 固定链接 (Permalink Parsing), JSON 订阅源, HTML 解析。

## 2. 架构规范 (Architecture Rules)

### 2.1 总体架构
遵循 **Clean Architecture** (整洁架构) 和 **MVI** (Model-View-Intent) 模式。

- **`Domain` (领域层)**: 纯 Kotlin。定义 `UseCase` (业务逻辑) 和 `Repository` 接口。**不依赖 Android/Compose**。
- **`Data` (数据层)**: 实现 `Repository`。处理 API (Ktorfit)、DB (SQLDelight) 和数据映射。
- **`UI` (表现层)**: `composeApp` 和 `feature-*`。使用 Compose Multiplatform。

### 2.2 响应式状态管理原则 (Reactive Rules)
在设计 `UseCase` 和 `Repository` 时，严格遵循以下返回类型规则：

1.  **可观察状态 (Observable State)**:
    -   **定义**: 随时间变化的数据，UI 需要实时响应其变更（如：帖子列表、用户资料、设置项）。
    -   **规则**: 必须返回 `Flow<T>`。
    -   **原因**: 确保 UI 总是反映数据的最新状态 (Single Source of Truth)。

2.  **一次性操作 (One-shot Operation)**:
    -   **定义**: 执行即结束的命令，没有持续的状态变更（如：登录、点赞、发送评论、刷新）。
    -   **规则**: 必须使用 `suspend` 函数。
    -   **原因**: 避免滥用 Flow 导致逻辑复杂化，利用 Coroutine 的结构化并发处理异步任务。

### 2.3 错误处理与 UI 状态 (Error Handling & UI State)
统一使用 **LCE** (Loading-Content-Error) 模式处理页面或组件状态。

1.  **UiStateWrapper**:
    -   所有包含加载过程的数据状态，必须使用 `UiStateWrapper<T>` 封装。
    -   **Loading**: 对应 `UiStateWrapper.Loading`。
    -   **Error**: 对应 `UiStateWrapper.Error`，必须包含 `AppError`。
    -   **Success**: 对应 `UiStateWrapper.Success<T>`。
    -   **Helper**: 使用 `MutableStateFlow<UiStateWrapper<T>>.updateData { }` 扩展函数来安全地更新 Success 状态下的数据，避免手动类型检查。

2.  **Error Handling**:
    -   禁止在 UI 层直接处理 `Throwable`。
    -   必须在 ViewModel 中使用 `Throwable.toAppError(onRetry)` 扩展函数，将异常转换为标准化的 `AppError`。
    -   `AppError` 自动识别网络错误、服务器错误等，并匹配对应的 UI 文案和图标。

3.  **StateLayout**:
    -   UI 必须使用 `StateLayout` Composable 来统一渲染 LCE 状态。
    -   **禁止**手动编写 if-else 判断 `isLoading` / `isError`。
    -   **参数**:
        -   `state`: 传入 ViewModel 中的 `UiStateWrapper` 流。
        -   `onRetry`: 点击错误页重试按钮的回调。
        -   `loading`/`error`: 可选参数，仅在需要自定义加载/错误视图时提供，默认使用全局统一的 `DefaultLoading` 和 `DefaultError`。

4.  **PagingStateLayout**:
    -   对于 Paging 3 列表，**必须**使用 `PagingStateLayout` 包裹 `LazyColumn` / `LazyVerticalGrid` 等组件。
    -   **职责**: 自动监听 `items.loadState.refresh` 状态。
        -   **Loading**: 显示全屏加载动画。
        -   **Error**: 将 `LoadStateError` 转换为 `AppError` 并显示错误页。
        -   **Empty**: 当 `itemCount == 0` 时处理空状态（可通过 `empty` 参数自定义）。
5.  **SaniouResult**:
    - 所有 API 返回Response必须定义为 SaniouResult<T> ，会将结果包装成SaniouResult.Success/Failed 等状态

### 2.4 UI 开发工作流
1.  **分析**: 在写代码前，先分析 UI 需求，提取可复用的组件放入 `core-ui`。
2.  **契约**: 在 `feature` 模块中定义 `Contract` (State & Event)。
3.  **实现**: ViewModel 仅通过 UseCase 交互，不直接触碰 Repository。

### 2.5 分页数据加载 (Paging)
所有分页数据加载必须使用 `GenericRemoteMediator`，禁止手动实现 `RemoteMediator`。
- **GenericRemoteMediator**: 位于 `core-data/src/commonMain/kotlin/ai/saniou/thread/data/paging/GenericRemoteMediator.kt`。
- **设计模式**: 使用组合（Delegate）模式，而非继承。
- **职责**: 统一处理分页状态机、事务管理、LoadType 分发和 RemoteKeys 更新。
- **使用方式**: 在具体的 Mediator 中实例化 `GenericRemoteMediator` 并委托 `load` 方法。

### 2.6 数据源交互规范 (Source Interaction Rules)
为了确保响应式架构的一致性，所有 `Source` 实现必须遵循以下交互模式：

1.  **架构分层与职责 (Layer Responsibilities)**:
    -   **API Layer (Network)**: 纯网络请求，返回 `SaniouResult<Dto>`。不涉及任何 DB 操作。
    -   **Source Layer (Domain Interface)**: 防腐层 (ACL)。将 `SaniouResult<Dto>` 转换为 `Result<DomainModel>`。**严禁**在此层直接操作 DB。
    -   **Cache Layer (Data Store)**: 封装所有 DB 操作 (CRUD)。提供面向业务的接口 (如 `saveTopics(topics: List<Topic>)`)，内部处理事务、关联表保存 (如 Image/Comment) 和清理策略。
    -   **Repository Layer (Data)**: 协调者。使用 `GenericRemoteMediator` 连接 Source (读网络) 和 Cache (写 DB)，并通过 Paging 向 Domain/UI 暴露数据。

2.  **SSOT (Single Source of Truth)**:
    -   **原则**: 数据库 (DB) 是唯一的真实数据源。
    -   **读取**: UI/Domain 层永远只观察 DB 的数据流 (`Flow`)，不直接等待网络请求结果。
    -   **写入**: 数据流向为 `Network -> Source(Dto->Domain) -> Repository -> Cache(Domain->Entity->DB)`。

3.  **接口分离**:
    -   **`observeXxx(): Flow<T>`**: 纯观察方法。直接返回 DB 查询的 Flow。
    -   **`fetchXxx(): Result<Unit>`**: 纯副作用方法。执行 "Check Cache -> Network Request -> Write DB" 流程。

4.  **缓存策略 (Cache Strategy)**:
    -   使用 `CacheStrategy` 工具类统一处理缓存过期逻辑。
    -   **列表页缓存**: 采用"旧数据下沉"策略。刷新时，将当前页及之后的旧数据 `page + 1`，新数据占据当前页，实现无缝衔接的缓存体验。

5.  **API 错误处理**:
    -   API 层返回 `SaniouResult<T>`，作为防腐层隔离网络异常。
    -   Source 层将 `SaniouResult.Error` 转换为 `Result.failure`。

### 2.7 富文本渲染 (Rich Text Rendering)
核心 UI 组件库提供了分层的富文本处理能力，请根据业务场景选择：

1.  **`SmartRichText` (插件化富文本)**:
    -   **位置**: `core-ui/src/commonMain/kotlin/ai/saniou/coreui/richtext/SmartRichText.kt`
    -   **场景**: 需要处理特定业务格式（如 `[h]` 剧透标签、`>>123` 引用、`#tag` 话题）的场景。
    -   **规则**: 严禁在 UI 层手动编写正则替换。必须实现 `RichTextPlugin` 接口（定义 `transform` 和 `getPatterns`），并通过 `Strategy` 模式按需加载。

2.  **`RichText` (底层渲染器)**:
    -   **位置**: `core-ui/src/commonMain/kotlin/ai/saniou/coreui/widgets/RichText.kt`
    -   **场景**: 仅展示标准 HTML 文本，无特殊业务逻辑的通用场景（如关于页、错误提示）。

3.  **`ForumRichText` (论坛专用封装)**:
    -   **位置**: `feature-forum/src/commonMain/kotlin/ai/saniou/forum/ui/components/ForumRichText.kt`
    -   **场景**: 论坛帖子正文、回复、板块简介。内置了 NMB 等源的引用解析策略。

## 3. 模块地图 (Module Map)

| 模块 | 职责 | 关键路径示例 |
| :--- | :--- | :--- |
| **`composeApp`** | 应用壳，导航入口 | `src/commonMain/kotlin/ai/saniou/thread/App.kt` |
| **`core-domain`** | **核心** 业务逻辑 & 接口 | `src/commonMain/kotlin/ai/saniou/thread/domain/usecase/` |
| **`core-data`** | 数据实现 (API/DB) | `src/commonMain/kotlin/ai/saniou/thread/data/repository/` |
| **`core-ui`** | 通用 UI 组件 & 主题 | `src/commonMain/kotlin/ai/saniou/coreui/` |
| **`core-common`** | 工具类 (Time, Log) | `src/commonMain/kotlin/ai/saniou/corecommon/` |
| **`core-network`** | network | `src/commonMain/kotlin/ai/saniou/network/` |
| **`feature-forum`** | 论坛业务 UI | `src/commonMain/kotlin/ai/saniou/thread/feature/forum/` |
| **`feature-feed`** | 社交流业务 UI | `src/commonMain/kotlin/ai/saniou/thread/feature/feed/` |
| **`feature-reader`** | 阅读器业务 UI | `src/commonMain/kotlin/ai/saniou/thread/feature/reader/` |

## 4. 常用资源指引 (Resource Shortcuts)

**设计系统 (Design System)**:
- **尺寸定义**: `ai.saniou.coreui.theme.Dimens` (在 `core-ui`)。使用语义化常量（如 `padding_medium`）而非硬编码。
- **颜色/主题**: `ai.saniou.coreui.theme.Theme`。使用 `MaterialTheme.colorScheme`。
- **排版**: `ai.saniou.coreui.theme.Type`。使用 `MaterialTheme.typography`。
- **形状**: `ai.saniou.coreui.theme.Shape`。使用 `MaterialTheme.shapes`。

**关键文件**:
- **字符串资源**: `feature-*/src/commonMain/composeResources/values/strings.xml`
- **导航配置**: 查看 `composeApp` 中的 App.kt 或导航相关文件。
- **依赖注入**: `core-data/src/commonMain/kotlin/ai/saniou/thread/data/di/Di.kt`
- usecase:`core-domain/src/commonMain/kotlin/ai/saniou/thread/domain/usecase/[feature]`
  - eg:ai.saniou.thread.domain.usecase.bookmark.AddBookmarkUseCase
- database-common: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/`
- database-forum: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/forum`
- database-reader: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/reader`
- mapper: `core-data/src/commonMain/kotlin/ai/saniou/thread/data/mapper/`
- [DateParser.kt](../../core-common/src/commonMain/kotlin/ai/saniou/corecommon/utils/DateParser.kt)
- [SaniouKtorfit.kt](../../core-network/src/commonMain/kotlin/ai/saniou/thread/network/SaniouKtorfit.kt)

### 4.1 数据库资源映射 (Database Map)
**统一账户与配置 (core-data)**:
- **`Cookie`**: 统一账户表 (Unified Account Table). 存储所有来源 (Tieba, NMB, Discourse) 的登录凭证与用户信息.
    - Path: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/Cookie.sq`
    - Key Fields: `id`, `source_id`, `cookie`, `uid`, `extra_data` (JSON).

- **`Bookmark`**: 跨模块收藏夹.
    - Path: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/Bookmark.sq`
    - Types: `TEXT`, `QUOTE`, `LINK`, `IMAGE`, `MEDIA`.

**论坛模块 (feature-forum)**:
- **`History`**: 浏览历史.
    - Path: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/History.sq`
    - Key Fields: `id`, `sourceId`, `parentId` (Topic ID), `parentType`.

- **`Notice`**: 通知/消息.
    - Path: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/Notice.sq`
    - Key Fields: `id`, `sourceId`, `type`, `isRead`.

- **`RemoteKey`**: 分页与缓存管理.
    - Path: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/RemoteKey.sq`
    - Key Fields: `id`, `type`, `nextKey`, `lastUpdated`.

**论坛模块 (feature-forum)**:
- **`Channel`**: 板块/贴吧/频道信息 (原 Forum).
    - Path: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/forum/Channel.sq`
    - Key Fields: `sourceId`, `id`, `name`, `description`, `fGroup`.

- **`Topic`**: 帖子/主题 (原 Thread).
    - Path: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/forum/Topic.sq`
    - Key Fields: `sourceId`, `id`, `title`, `content`.

- **`Comment`**: 回复/楼层 (原 Post/Reply).
    - Path: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/forum/Comment.sq`
    - Key Fields: `sourceId`, `id`, `topicId`, `content`.

- **`Subscription`**: 订阅/关注.
    - Path: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/forum/Subscription.sq`
    - Key Fields: `subscriptionKey`, `sourceId`, `topicId`.

**阅读器模块 (feature-reader)**:
- **`FeedSourceEntity`**: RSS/Atom 订阅源.
    - Path: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/reader/FeedSource.sq`
    - Key Fields: `id`, `url`, `title`.

- **`ArticleEntity`**: RSS/Atom 文章.
    - Path: `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/reader/Article.sq`
    - Key Fields: `id`, `feedSourceId`, `title`, `content`.

## 5. 最佳实践检查清单
- [ ] **KISS**: 即使是复杂功能，API 设计也应保持简单。
- [ ] **Theme**: 禁止在 UI 中硬编码颜色、字号和圆角。必须引用 `MaterialTheme` 和 `Dimens`。
- [ ] **Compose**: 优先使用 Material 3 组件。
- [ ] **Error Handling**: ViewModel 使用 `UiStateWrapper` 封装状态，UI 层使用 `StateLayout` (普通数据) 或 `PagingStateLayout` (分页数据) 统一处理 LCE。
- [ ] **Caching**: 所有 `Source` 实现必须具备缓存能力，特别是对于配置类数据（如板块列表），应实现"网络优先，缓存兜底"或"缓存优先，过期更新"策略。
- [ ] **Comments**: 复杂逻辑必须写中文注释解释 "Why"。
- [ ] **Utils**: 涉及特定格式字符串解析（如日期、特定文本结构）时，优先考虑封装为独立的 Utils 类或扩展函数，避免在业务逻辑中散落硬编码。
- [ ] **RichText**: 严禁硬编码正则解析业务文本。对于特定源的文本格式（如 `>>No.123`），必须实现 `RichTextPlugin`。
- [ ] **ID Type**: 为了兼容不同系统，Domain 层 ID 统一使用 `String`，在 Data 层针对不同 API 进行转换。
- [ ] **Refactoring**: 开始编码前请先思考：本次用到了哪些公共资源和组件，是否有必要下沉到 core 模块。

- 商业级交付标准:UIXUI、设计、架构。
**中文沟通**：所有交流,注释使用中文，但在代码中的变量命名、Commit Message 保持英文。
