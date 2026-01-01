# Tieba Feature Integration Roadmap

本文档规划了将 `TiebaLite` 的核心功能完整移植到 `Thread` 项目的路线图。当前已完成基础 API 移植、账户系统重构和登录流程。

## 1. 当前状态 (Status Quo)

### 已完成 (Completed)
*   **API Layer**:
    *   Ktorfit 移植: `NewTiebaApi`, `OfficialTiebaApi`, `WebTiebaApi`, `MiniTiebaApi`, `AppHybridTiebaApi`, `LiteApiInterface`, `SofireApi`.
    *   Network Plugins: `TiebaCommonHeaderPlugin` (Headers/Cookie), `TiebaCommonParamPlugin` (Public Params), `TiebaSortAndSignPlugin` (Sign).
    *   Protobuf Interface: `OfficialProtobufTiebaApi` 接口定义 (Models 待生成).
*   **Data Layer**:
    *   Unified Account System: `Account` domain model, `AccountRepository`, `Cookie.sq` schema update.
    *   Reactive Provider: `TiebaParameterProvider` 自动从 Repo 获取当前账号凭证.
*   **Domain Layer**:
    *   `LoginTiebaUseCase`: 处理登录凭证保存.
*   **UI Layer**:
    *   `TiebaLoginScreen`: 基于 WebView 拦截 `BDUSS` 和 `STOKEN`.
    *   `TiebaLoginViewModel`: 串联 UI 和 UseCase.

### 进行中 (In Progress)
*   `OfficialProtobufTiebaApi` 的 Protobuf 模型生成与 Wire 配置.

---

## 2. 剩余任务清单 (ToDo List)

### Phase 1: Protobuf & Wire Integration (高优先级)
**目标**: 启用基于 Protobuf 的高性能 API 接口 (如首页推荐、帖子列表)。

1.  [ ] **Gradle Configuration**:
    *   在 `core-data` 配置 `com.squareup.wire` 插件.
    *   设置 `.proto` 文件源目录 (需从 TiebaLite 复制或定义).
2.  [ ] **Model Migration**:
    *   将 TiebaLite 的 `.proto` 文件迁移至 `core-data/src/commonMain/proto`.
    *   生成 Kotlin Data Classes (`FrsPageResponse`, `PbPageResponse` 等).
3.  [ ] **Network Integration**:
    *   配置 `Ktorfit` 使用 Wire 的 `ProtoConverterFactory`.
    *   在 `Di.kt` 中绑定 `OfficialProtobufTiebaApi` 实例.

### Phase 2: Core Browsing Features (基础浏览)
**目标**: 实现浏览贴吧首页、进吧看帖、查看回复。

1.  [ ] **Domain UseCases**:
    *   `GetForumHomeUseCase`: 获取首页推荐/关注列表.
    *   `GetForumThreadsUseCase`: 获取吧内帖子列表 (`FrsPage`).
    *   `GetThreadDetailUseCase`: 获取帖子详情与楼层 (`PbPage`).
2.  [ ] **Data Repository**:
    *   `TiebaRepository`: 封装上述 API 调用，处理分页 (`PagingSource`).
    *   缓存策略: 集成 `CacheStrategy` (Room/SQLDelight).
3.  [ ] **UI Implementation**:
    *   `ForumScreen`: 吧列表/首页.
    *   `ThreadListScreen`: 帖子列表页.
    *   `ThreadDetailScreen`: 帖子详情页 (楼层渲染).
    *   **RichText Support**: 移植 TiebaLite 的富文本解析逻辑 (`PbContentRender`) 到 `core-ui`.

### Phase 3: Interaction & User Features (交互与用户)
**目标**: 实现发帖、回复、点赞、收藏、用户中心。

1.  [ ] **Interaction UseCases**:
    *   `ReplyThreadUseCase`: 回复帖子/楼层.
    *   `CreateThreadUseCase`: 发布新帖.
    *   `LikeThreadUseCase`: 点赞.
    *   `FollowForumUseCase`: 关注/签到.
2.  [ ] **User Profile**:
    *   `GetTiebaUserProfileUseCase`: 获取详细个人信息 (等级、关注数等).
    *   `UserDetailScreen`: 展示用户信息.
3.  [ ] **Login Refinement**:
    *   完善 `TiebaLoginScreen`: 登录成功后自动调用 `getUserInfo` 补全头像和昵称 (目前仅保存了 Cookie).

### Phase 4: Advanced Features (高级功能)
1.  [ ] **Multimedia**: 图片查看器、视频播放支持.
2.  [ ] **Emotion**: 表情包支持 (Tieba 表情解析).
3.  [ ] **Settings**: 贴吧专属设置 (小尾巴、签名等).

---

## 3. 下一步具体行动 (Next Actions)

建议优先完成 **Phase 1**，因为核心数据接口 (`FrsPage`, `PbPage`) 依赖 Protobuf。

1.  **Task**: 配置 Wire 插件并生成 Protobuf 模型.
    *   *Input*: TiebaLite 的 `.proto` 文件.
    *   *Output*: 生成的 Kotlin 类, `OfficialProtobufTiebaApi` 可用.
2.  **Task**: 完善 `TiebaLoginViewModel` 的用户信息补全逻辑.
    *   在拦截到 Cookie 后，调用 `NewTiebaApi.getUserInfo` 获取 `uid`, `name`, `portrait`.
    *   更新 `LoginTiebaUseCase` 以接收这些额外信息.

## 4. 架构规范提醒
*   **Single Source of Truth**: UI 只能观察 `Repository` (via `Flow`), 不直接持有数据.
*   **MVI**: 所有 UI 交互通过 `Event` 发送给 ViewModel, 状态通过 `State` 流回 UI.
*   **Error Handling**: 使用 `runCatching` 和 `Result` 封装，统一错误 UI (`StateLayout`).