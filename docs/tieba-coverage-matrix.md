# TiebaLite → Thread 功能覆盖矩阵

对照基准：`TiebaLite-4.0-dev`（页面 + `ITiebaApi` ~63 逻辑接口）  
Thread 基线：`0.38.0` / tip `21d83e2a`（实现从本矩阵后续版本起增量覆盖）

## 0. 覆盖原则

1. **能力契约优先**：UI 只看 `SourceCapabilities` / Connector；禁止 `if (sourceId == "tieba")` 业务分支。
2. **接口可移植、交互可抽象**：TiebaLite 的贴吧专用布局（OKSign、吧务、屏蔽词）映射为 Thread 通用能力或 Operations/Settings 扩展，不复刻 Android 单源 App 壳。
3. **100% 功能覆盖** = TiebaLite 用户可完成的业务动作在 Thread 有等价主路径；不等于像素级 1:1 UI。
4. **API 层可先于 UI**：remote 接口已移植但未接 Connector 的，记为 `API only`。

状态：`done` | `partial` | `API only` | `missing` | `n/a`

## 1. 主路径（浏览 / 互动）

| TiebaLite 模块 | 关键接口 / 页面 | Thread 现状 | 状态 | 分期 |
|---|---|---|---|---|
| 首页关注吧列表 | `forumRecommend` / Home | `TiebaSource.fetchChannels` + Channel 列表 | done | — |
| 吧帖列表 | `frsPage` / ForumThreadList | `getChannelTopics` protobuf | done | — |
| 帖详情 + 楼层 | `pbPage` / Thread | `getTopicDetail` + `getTopicComments` | done | — |
| 楼中楼 | `pbFloor` / SubPosts | `SubCommentConnector` | done | — |
| 只看楼主 | Thread 筛选 | `hasPoOnly` + comments cursor | done | — |
| 点赞 | `agree` / opAgree | `TiebaReactionConnector.upvote` | done | — |
| 点踩 / 不感兴趣 | `disagree` / `submitDislike` | API only | missing UI | P2 |
| 回复 | `addPost` | `TiebaPostingConnector.createReply` + 图传 | done | — |
| 发主题 | `addPost` tid 空 | `createThread` → official `addPost` 空 tid | done (0.39) | — |
| 搜索帖/回复 | hybrid `searchThread` | `TiebaSearchConnector` + DI | done (0.39) | — |
| 搜索吧 | hybrid `searchForum` | API only | missing | P1（可并入全局搜索） |
| 搜索用户 | hybrid `searchUser` | API only | missing | P1 |
| 吧内搜帖 | `searchPost` | Mini API only | missing | P1 |
| 热门 / 推荐 / 关注流 | hotThread / personalized / userLike | `TiebaTrendSource` tabs | done | — |
| 话题详情 | topicDetail / topicList | Trend 部分 | partial | P2 |
| 用户主页帖/回 | `userPost` | `TiebaUserContentConnector` | done | — |
| 用户关注的吧 | `userLikeForum` | API only | missing | P1 |
| 收藏夹 | `threadStore` / add/removeStore | API only；本地 Bookmark 独立 | partial | **P1** |
| 消息：回复/艾特/赞 | replyMe / atMe / agreeMe / msg | NewTiebaApi only；Inbox 本地 | missing 远端 | **P1** |
| 登录 | WebView BDUSS/STOKEN | `TiebaLoginConnector` | done | — |
| 签到 / 一键签 | sign / mSign / OKSign | API only | missing | P2 |
| 关注/取关吧 | likeForum / unlikeForum | Mini/Official API | missing | P1 |
| 关注/取关用户 | follow / unfollow | API only | missing | P2 |
| 删帖/删楼 | delThread / delPost | API only | missing | P3 |
| 吧规 | forumRuleDetail | protobuf API only | missing | P2 |
| 吧详情/成员/等级/吧务 | getForumDetail / Member / Level / Bawu | protobuf API only | missing | P3 |
| 资料编辑/头像 | profileModify / imgPortrait | API only | missing | P3 |
| 图片浏览器 | picPage / PhotoView | 通用 ImagePreview；无贴吧相册流 | partial | P2 |
| 浏览历史 | History 本地 | Thread History 本地 | partial（无贴吧云历史） | P3 |
| 屏蔽词/黑名单 | Block settings | missing | missing | P3 |
| 设置/主题/习惯 | Settings* | Thread 通用 Appearance/Settings | n/a 映射 | P3 |
| WebView 杂项 | WebViewPage | Desktop 外链/系统浏览器 | n/a | — |

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
| Reactions | — | ✅ | ✅ |
| Trend | — | — | ✅ |

`SourceCapabilities` 当前 Tieba：`supportsSearch=false`（与 Search 缺口一致）、`supportsTopicCreation=false`。

## 3. API 移植完整度（remote 包）

已移植客户端：`Official` / `OfficialProtobuf` / `Mini` / `New` / `Web` / `AppHybrid` / `Sofire` / `Lite`。  
`ITiebaApi` 逻辑面约 **63** 个；Thread 侧 **方法声明基本齐**，缺口主要在 **Connector + UI 主路径接线**，不是从零写协议。

## 4. 验收句（100%）

> 在 Thread 里用贴吧账号：登录 → 刷关注吧 → 进吧读帖/楼中楼 → 搜帖 → 回复/发帖 → 点赞 → 收藏同步 → 看回复/艾特消息 → 签到/关注吧，且不打开官方 App。

## 5. 分期（版本刀）

| 刀 | 版本建议 | 范围 |
|---|---|---|
| **P0** | **0.39** | ✅ 搜索 `ForumSearchConnector`；`supportsSearch`；发主题 `createThread` |
| **P1** | 0.40 | 收藏 store 同步、关注/取关吧、用户 likeForum 列表、消息 replyMe/atMe→Inbox |
| **P2** | 0.41 | 签到/mSign、吧规、点踩/dislike、关注用户、图片相册增强 |
| **P3** | 0.42+ | 删帖权限、吧务/成员、资料编辑、屏蔽、历史云同步、设置 parity |

## 6. 明确不做（非 100% 定义内）

- Monet / 贴吧皮肤 1:1
- 吧内广告 / Sofire 商业链路
- 官方 WebView 活动页全量内嵌
- 复刻 TiebaLite 底部四 Tab 信息架构（Thread 用统一侧栏工作台）
