# TiebaLite → Thread 功能覆盖矩阵

对照基准：`TiebaLite-4.0-dev`（页面 + `ITiebaApi` ~63 逻辑接口）  
Thread 基线：`0.42.0`（P0–P2 + P3 点踩/关注用户已落地；相册/资料等后续）

## 0. 覆盖原则

1. **能力契约优先**：UI 只看 `SourceCapabilities` / Connector；禁止 `if (sourceId == "tieba")` 业务分支。
2. **接口可移植、交互可抽象**：TiebaLite 的贴吧专用布局（OKSign、吧务、屏蔽词）映射为 Thread 通用能力或 Operations/Settings 扩展，不复刻 Android 单源 App 壳。
3. **100% 功能覆盖** = TiebaLite 用户可完成的业务动作在 Thread 有等价主路径；不等于像素级 1:1 UI。
4. **API 层可先于 UI**：remote 接口已移植但未接 Connector 的，记为 `API only`。
5. **已有能力也要补齐**：登录、列表/详情、回复、楼中楼、点赞、图传等路径需持续吸收设计/UI/接口质量，才算覆盖完成。
6. **管理功能暂缓**：删帖、吧务等平台管理动作本阶段不实现；若通过 webview 外链可作为扩展保留。

状态：`done` | `partial` | `API only` | `missing` | `n/a` | `deferred`

## 1. 主路径（浏览 / 互动）

| TiebaLite 模块 | 关键接口 / 页面 | Thread 现状 | 状态 | 分期 |
|---|---|---|---|---|
| 首页关注吧列表 | `forumRecommend` / Home | `TiebaSource.fetchChannels` + Channel 列表 | done | — |
| 吧帖列表 | `frsPage` / ForumThreadList | `getChannelTopics` protobuf | done | — |
| 帖详情 + 楼层 | `pbPage` / Thread | `getTopicDetail` + `getTopicComments` | done | — |
| 楼中楼 | `pbFloor` / SubPosts | `SubCommentConnector` | done | — |
| 只看楼主 | Thread 筛选 | `hasPoOnly` + comments cursor | done | — |
| 点赞 | `agree` / opAgree | `TiebaReactionConnector.upvote` + shared TBS | done (0.40 polish) | — |
| 点踩 / 不感兴趣 | `disagree` / `submitDislike` | downvote=`opAgree agree_type=5` + Topic DislikeButton；submitDislike 仍 API only | partial (0.42 downvote) | 首页不感兴趣后续 |
| 回复 | `addPost` | `TiebaPostingConnector.createReply` + 图传 + shared TBS | done | — |
| 发主题 | `addPost` tid 空 | `createThread` → official `addPost` 空 tid | done (0.39) | — |
| 搜索帖/回复 | hybrid `searchThread` | `TiebaSearchConnector` + DI | done (0.39) | — |
| 搜索吧 | hybrid `searchForum` | `TiebaSearchConnector.searchChannels` + Search CHANNEL tab | done (0.41) | — |
| 搜索用户 | hybrid `searchUser` | `TiebaSearchConnector.searchUsers` + Search USER tab | done (0.41) | — |
| 吧内搜帖 | `searchPost` | Mini API only | missing | P2 |
| 热门 / 推荐 / 关注流 | hotThread / personalized / userLike | `TiebaTrendSource` tabs | done | — |
| 话题详情 | topicDetail / topicList | Trend 部分 | partial | P2 |
| 用户主页帖/回 | `userPost` | `TiebaUserContentConnector` | done | — |
| 用户关注的吧 | `userLikeForum` | `TiebaUserLikeForumSync` → Channel/Favorite 缓存 | done (0.40) | — |
| 收藏夹 | `threadStore` / add/removeStore | 主题 Bookmark 远端 addstore/rmstore | done (0.40 主题路径) | — |
| 消息：回复/艾特/赞 | replyMe / atMe / agreeMe / msg | replyMe/atMe/agreeMe→Inbox（赞→SYSTEM） | done (0.41) | — |
| 登录 | WebView BDUSS/STOKEN | `TiebaLoginConnector` | done | — |
| 签到 / 一键签 | sign / mSign / OKSign | `TiebaChannelSign` + Topic/Channel UI | done (0.41) | — |
| 关注/取关吧 | likeForum / unlikeForum | `FavoriteRepository` remote-first | done (0.40) | — |
| 关注/取关用户 | follow / unfollow | `TiebaUserRelationConnector` + UserDetail 关注按钮 | done (0.42) | — |
| 删帖/删楼 | delThread / delPost | API only | deferred | 暂不实现（外链扩展可保留） |
| 吧规 | forumRuleDetail | `TiebaForumRuleService` + Topic 只读对话框 | done (0.41) | — |
| 吧详情/成员/等级/吧务 | getForumDetail / Member / Level / Bawu | protobuf API only | deferred 吧务 | 暂不实现 |
| 资料编辑/头像 | profileModify / imgPortrait | API only | missing | P3 |
| 图片浏览器 | picPage / PhotoView | 通用 ImagePreview；无贴吧相册流 | partial | P2 |
| 浏览历史 | History 本地 | Thread History 本地 | partial（无贴吧云历史） | P3 |
| 屏蔽词/黑名单 | Block settings | missing | missing | P3 |
| 设置/主题/习惯 | Settings* | Thread 通用 Appearance/Settings | n/a 映射 | P3 |
| WebView 杂项 | WebViewPage | Desktop 外链/系统浏览器 | n/a | 扩展保留 |

## 2. Connector 接线（运行目录）

| 能力 | NMB | Discourse | Tieba |
|---|---|---|---|
| Source 列表/详情 | ✅ | ✅ | ✅ |
| ForumSearch | ✅（本地） | ✅（远端） | ✅ hybrid（0.39） |
| UserContent | ✅ | ✅ | ✅ |
| Posting reply | ✅ | ✅ | ✅ |
| Posting createThread | ✅ | ✅ | ✅（0.39，验证码风险） |
| Login | ✅ | ✅ | ✅ |
| SubComments | — | — | ✅ |
| Reactions | — | ✅ | ✅ upvote + downvote + shared TBS（0.42 downvote） |
| Trend | — | — | ✅ |
| Membership / Store / Inbox | — | — | ✅ 非 Connector 服务（0.40；agreeMe 0.41） |
| Channel actions (sign / rules) | — | — | ✅ `ChannelActionRepository`（0.41） |
| User relation (follow) | — | — | ✅ `UserRelationConnector`（0.42） |

`SourceCapabilities` 当前 Tieba：`supportsSearch=true`、`supportsTopicCreation=true`、`supportsChannelSign=true`、`supportsForumRules=true`、`hasDownvote=true`、`supportsUserFollow=true`（0.42）。

## 3. API 移植完整度（remote 包）

已移植客户端：`Official` / `OfficialProtobuf` / `Mini` / `New` / `Web` / `AppHybrid` / `Sofire` / `Lite`。  
`ITiebaApi` 逻辑面约 **63** 个；Thread 侧 **方法声明基本齐**，缺口主要在 **Connector + UI 主路径接线**，不是从零写协议。

## 4. 验收句（100%）

> 在 Thread 里用贴吧账号：登录 → 刷关注吧 → 进吧读帖/楼中楼 → 搜帖 → 回复/发帖 → 点赞 → 收藏同步 → 看回复/艾特消息 → 签到/关注吧，且不打开官方 App。  
> 管理动作（删帖/吧务）不纳入本阶段 100%。

## 5. 分期（版本刀）

| 刀 | 版本建议 | 范围 |
|---|---|---|
| **P0** | **0.39** | ✅ 搜索 `ForumSearchConnector`；`supportsSearch`；发主题 `createThread` |
| **P1** | **0.40** | ✅ 收藏 store 同步、关注/取关吧、userLikeForum 缓存、replyMe/atMe→Inbox；主路径 TBS 收紧 |
| **P2** | **0.41** | ✅ 签到/mSign、吧规只读、搜索吧/用户、agreeMe→Inbox |
| **P3a** | **0.42** | ✅ 点踩 downvote、关注/取关用户 |
| **P3b** | 0.43+ | 图片相册、资料编辑、屏蔽、历史云同步、设置 parity、submitDislike（**删帖/吧务 deferred**） |

## 6. 明确不做 / 暂缓（非本阶段 100% 定义内）

- Monet / 贴吧皮肤 1:1
- 吧内广告 / Sofire 商业链路
- 官方 WebView 活动页全量内嵌（外链扩展可保留）
- 复刻 TiebaLite 底部四 Tab 信息架构（Thread 用统一侧栏工作台）
- **删帖 / 吧务等平台管理功能**（用户 2026-07-22：暂不实现）
