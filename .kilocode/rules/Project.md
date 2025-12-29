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

2.  **Error Handling**:
    -   禁止在 UI 层直接处理 `Throwable`。
    -   必须在 ViewModel 中使用 `Throwable.toAppError(onRetry)` 扩展函数，将异常转换为标准化的 `AppError`。
    -   `AppError` 自动识别网络错误、服务器错误等，并匹配对应的 UI 文案和图标。

3.  **StateLayout**:
    -   UI 必须使用 `StateLayout` Composable 来统一渲染 LCE 状态。
    -   禁止手动编写 if-else 判断 `isLoading` / `isError`。

4.  **PagingStateLayout**:
    -   对于 Paging 3 列表，使用 `PagingStateLayout` 包裹 `LazyColumn` 等组件。
    -   它自动处理 `loadState.refresh` 的 Loading、Error 和 Empty 状态，并与 `AppError` 集成。

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


## 5. 最佳实践检查清单
- [ ] **KISS**: 即使是复杂功能，API 设计也应保持简单。
- [ ] **Theme**: 禁止在 UI 中硬编码颜色、字号和圆角。必须引用 `MaterialTheme` 和 `Dimens`。
- [ ] **Compose**: 优先使用 Material 3 组件。
- [ ] **Error Handling**: ViewModel 使用 `UiStateWrapper` 和 `StateLayout` 处理加载和错误。
- [ ] **Caching**: 所有 `Source` 实现必须具备缓存能力，特别是对于配置类数据（如板块列表），应实现"网络优先，缓存兜底"或"缓存优先，过期更新"策略。
- [ ] **Comments**: 复杂逻辑必须写中文注释解释 "Why"。
- [ ] **Utils**: 涉及特定格式字符串解析（如日期、特定文本结构）时，优先考虑封装为独立的 Utils 类或扩展函数，避免在业务逻辑中散落硬编码。
- 为了兼容不同系统，ID 应该使用String，在针对不同 API 时，再进行转换
- UI 细节：随着不同源的加入，应该丰富各种UI细节。。
- 目标是打造一个全 source 支持的，风格化，细节丰富的设计感优秀页面
- 开始编码前请先思考：本次用到了哪些公共资源和组件，是否有必要下沉到 core 模块

## 6. 2025年12月 重构交接文档 (Handoff Note)

### 6.1 Domain 层重构 (Domain Refactoring)
为了支持多源聚合（Multi-Source），我们对 `feature-forum` 相关的领域模型进行了标准化重构。

- **`Topic` (原 Post)**: 代表一个讨论串/主题。
    - `remainComments` 重命名为 `remainingCount`，消除歧义。
    - `replyCount` 重命名为 `commentCount`。
    - 移除 `img`/`ext` 字段，改为 `images: List<Image>`，支持多图。
    - `title` 改为 `String?`，适配无标题内容（如微博）。
    - 包含 `comments: List<Comment>` 字段，用于预览或详情页数据。

- **`Comment` (原 ThreadReply)**: 代表主题下的评论/回复。
    - 移除 `img`/`ext`，改为 `images: List<Image>`。
    - `userHash` 替换为 `author: Author` 对象。

- **`Image`**: 新增图片模型。
    - `originalUrl`: 原图链接（对于 NMB 可能是相对路径，需在 UI 层/VM 层解析）。
    - `thumbnailUrl`: 缩略图链接。
    - `width`/`height`: 图片尺寸（可选）。

- **`Author`**: 作者模型。
    - 新增 `sourceName`: 用于区分不同源的同名/同ID用户。

### 6.2 Data 层变更 (Data Layer Changes)
- **SQLDelight**:
    - `Topic` 和 `Comment` 表已更新以匹配 Domain 模型。
    - `Topic` 表新增 `remainingCount` (原 remainReplies 迁移至 `TopicInformation` 表)。
    - 图片数据不再直接存储在 `Topic`/`Comment` 表的 `img` 字段，而是（建议）迁移至独立的 `Image` 表（虽目前为了兼容性，Mapper 中仍有部分逻辑处理相对路径，但方向是独立化）。

- **Mapper**:
    - `TopicMapper` 和 `NmbMapper` 已更新，负责将 DB/API 数据映射为新的 Domain 模型。
    - **注意**: 对于 NMB 源，Mapper 目前可能仍将相对路径（如 `image/123.jpg`）放入 `originalUrl`。

### 6.3 图片处理策略 (Image Handling Strategy)
- **原则**: UI 组件 (`ThreadBody`, `ImagePreviewPage`) 应当尽量 "愚蠢"，只负责渲染完整的 URL。
- **URL 解析**:
    - **View Model / Page Controller 层**: 负责将 Domain 中的相对路径（如 NMB 的 `originalUrl`）解析为绝对 URL。
    - **`CdnManager`**: 核心数据层组件，用于解析 NMB 的相对路径。
    - **实践**:
        - `ImagePreviewViewModel`: 注入 `CdnManager`，在加载图片列表时，将 `Image` 对象中的 URL 解析为绝对路径。
        - `ThreadPage`: 在传递初始图片列表给预览页时，使用 `CdnManager` 解析。
        - `ThreadBody`: 使用 `NmbImage` 组件（该组件内部暂仍保留了 `CdnManager` 逻辑以支持旧有逻辑，但未来应逐步移除，改由传入完整 URL）。

### 6.4 待办/注意事项 (TODOs & Caveats)
- **UI 组件迁移**: `ForumThreadCard`, `ThreadBody`, `ThreadPage` 已适配新模型。其他 Feature 模块若有引用需检查。
- **多源扩展**: 新增数据源（如 Reddit）时，应直接在 Mapper 层生成绝对 URL，无需修改 UI 层的解析逻辑。

**中文沟通**：所有交流,注释使用中文，但在代码中的变量命名、Commit Message 保持英文。
**中文沟通**：所有交流,注释使用中文，但在代码中的变量命名、Commit Message 保持英文。
