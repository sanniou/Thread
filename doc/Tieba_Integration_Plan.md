# Tieba Source Integration Plan

## 1. 目标
将百度贴吧 (Tieba) 数据源完整集成到 `feature-forum` 模块中。

## 2. 现状与策略调整
*   **原计划**: 等待 Protobuf 模型生成后才开始集成。
*   **新策略 (Mixed/Hybrid)**: 优先使用 JSON/Web 接口 (`MiniTiebaApi`, `OfficialTiebaApi`, `WebTiebaApi`) 实现核心功能，快速打通链路。Protobuf 仅作为性能优化和特定功能（如 Feed 流）的补充，不再作为阻塞点。

## 3. API 路由策略 (Mixed Implementation)
我们将在 `TiebaSource` 中直接调度不同的 API，策略如下：

| 功能 | 原始实现 (MixedTiebaApiImpl) | 目标 Ktorfit API (JSON/Non-Protobuf) | 状态 |
| :--- | :--- | :--- | :--- |
| **关注吧列表** | `MINI_TIEBA_API.forumRecommend` | `MiniTiebaApi.forumRecommend` | ✅ Ready |
| **吧帖子列表 (Forum Page)** | `MINI_TIEBA_API.forumPage` | `MiniTiebaApi.forumPage` | ✅ Ready |
| **帖子详情 (Thread Content)** | `OFFICIAL_TIEBA_API.threadContent` | `OfficialTiebaApi.threadContent` | ✅ Ready |
| **楼中楼 (Comments)** | `OFFICIAL_TIEBA_API.floor` | `OfficialTiebaApi.floor` | ✅ Ready |
| **搜索** | `WEB_TIEBA_API` | `WebTiebaApi` | ✅ Ready |
| **首页推荐 (Feed)** | `OFFICIAL_PROTOBUF_TIEBA` | *(暂缓，需 Protobuf)* | ⏳ Pending |

## 4. 执行计划

### Phase 1: 基础设施完善 (已完成)
*   [x] 移植 `NewTiebaApi`, `MiniTiebaApi`, `OfficialTiebaApi`, `WebTiebaApi`。
*   [x] 配置 Ktorfit DI。
*   [x] 解决 MD5 和 平台兼容性问题。

### Phase 2: TiebaSource 实现 (当前重点)
*   [ ] **创建 `TiebaSource` 类**: 实现 `Source` 接口。
*   [ ] **实现 `observeChannels()`**:
    *   调用 `MiniTiebaApi.forumRecommend()`。
    *   Mapping: `ForumRecommend` -> `List<Channel>`。
*   [ ] **实现 `getTopicsPager()` (吧内)**:
    *   调用 `MiniTiebaApi.forumPage()`。
    *   Mapping: `ForumPageBean` -> `PagingData<Topic>`。
*   [ ] **实现 `getTopicDetail()`**:
    *   调用 `OfficialTiebaApi.threadContent()`。
    *   Mapping: `ThreadContentBean` -> `Topic` (包含首楼内容)。
*   [ ] **实现 `getTopicCommentsPager()`**:
    *   调用 `OfficialTiebaApi.floor()` (楼中楼) 或 `OfficialTiebaApi.threadContent` (主楼回复)。
    *   Mapping: `SubFloorListBean` / `ThreadContentBean` -> `PagingData<Comment>`。

### Phase 3: DI 注册与联调
*   [ ] 在 `Di.kt` 中注册 `TiebaSource`。
*   [ ] 启动 App 验证：
    1.  登录 Tieba。
    2.  加载关注吧列表。
    3.  进入吧内浏览帖子。
    4.  查看帖子详情。

### Phase 4: Protobuf (后续优化)
*   [ ] 引入 Wire 插件。
*   [ ] 生成 Protobuf 模型。
*   [ ] 替换 `OfficialProtobufTiebaApi` 中的 `Any`。
*   [ ] 迁移 `getTopicsPager` (Feed) 到 Protobuf 接口。

## 5. 风险控制
*   **JSON 接口稳定性**: 部分接口 (如 `MiniTiebaApi`) 可能是旧版接口，需关注是否会下线。
*   **API 签名**: 需重点验证 `TiebaSortAndSignPlugin` 在 JSON 接口上的签名正确性。