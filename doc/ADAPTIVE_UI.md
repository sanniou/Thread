# Thread 自适应 UI 架构

## 产品目标

Thread 不是把手机版页面放大到 Desktop，也不为每个平台维护一套独立 UI。Forum、Reader、Feed 使用同一份 Compose Multiplatform 页面代码，并根据“当前窗口能承载什么”切换信息密度、导航位置与并行任务数量。

UI 的职责分成三层：

1. 工作区导航：社区、阅读、动态、发现、收藏、通知收件箱、活动、运维、设置；历史保留在命令面板。
2. Feature 导航：论坛来源/版块，Reader 订阅源，Feed 聚合范围。
3. 内容任务：主题列表与回复、文章列表与阅读、混合时间线。

Feature 不得再次嵌入工作区导航，也不得通过 Android、iOS、Desktop 平台名决定布局。

## 窗口能力矩阵

| Width class | 范围 | 工作区导航 | Feature 导航 | 内容模式 |
| --- | ---: | --- | --- | --- |
| Compact | `< 700dp` | 底部 4 个高频入口 + 完整 overflow sheet | Modal drawer | 单任务页面；Header 操作自动换到第二行 |
| Medium | `700–1099dp` | 72dp 无标签 rail | Modal drawer | 单内容画布，增加横向留白 |
| Expanded | `1100–1599dp` | 92dp 带标签 rail | 永久侧栏 | 双区域工作区；列表/阅读内容居中 |
| Large | `>= 1600dp` | 92dp 带标签 rail | 加宽永久侧栏 | 支持来源、列表、详情三栏并行 |

高度低于 `560dp` 标记为 Compact height。它不改变业务路由，但供后续命令栏压缩、横屏手机和折叠设备姿态策略使用。

所有阈值集中在 `core-ui` 的 `Dimens` 与 `classifyThreadWindow`。业务页面只能读取 `LocalThreadWindowInfo` 的能力字段，例如 `hasPermanentFeatureSidebar` 或 `supportsListDetail`。

## 全局导航

`WorkspaceNavigationSuite` 是应用唯一的工作区切换入口。

- Compact：底栏只展示社区、阅读、动态、收藏；“更多”打开 sheet，sheet 同时列出全部入口，避免低频页面在手机上不可达。
- Medium：紧凑 rail 只显示图标和语义 content description，为内容保留宽度。
- Expanded/Large：完整 rail 显示品牌、图标和文本标签，高频入口在上，实验室和设置固定在底部。
- 切换工作区使用 `replaceAll`，工作区内部的详情使用 Voyager push/pop，避免历史栈混入其他产品区域。
- 所有宽度都提供“命令与发现”触控入口；键盘用户使用 Ctrl/Command+Shift+P。面板与导航共用同一稳定目的地表，不维护第二套路由。

旧的 `LocalAppDrawer` 会让每个 Feature 自己渲染一份全局菜单，形成重复入口和嵌套 drawer，现已删除。

## Feature 侧栏

`AdaptiveSidebarScaffold` 统一 Forum、Reader、Feed 的第二级导航。

- Expanded/Large 将侧栏作为永久 Surface，内容区域不会被覆盖。
- Compact/Medium 使用 Modal drawer；页面 Header 必须显示打开按钮。
- drawer 的关闭由 Feature 在完成来源/版块选择后触发，永久侧栏不执行无意义动画。
- 侧栏只包含当前 Feature 的筛选、来源、诊断和管理动作。

## Forum

Forum 侧栏按“当前来源—功能入口—刷新诊断—版块”组织。来源能力决定搜索、登录、发帖、回复等入口，不以 source ID 硬编码 UI。

主题列表使用统一 `ThreadCard`：

- Compact 自动收紧 padding，Medium 以上恢复舒适信息密度。
- 来源/版块、作者、时间、回复数形成稳定的视觉层级。
- 任意数量的标签使用 `FlowRow`，不再把窄屏卡片撑出窗口。
- 图片、引用、最近回复保留在主题内容层，不进入全局导航状态。

主题详情使用 `ReadingCanvas`。宽窗口显示有边界的聚焦阅读 Surface，窄窗口使用无额外边框的全宽页面；主贴、回复、只看 PO、楼中楼、跳页、收藏和回复动作共享同一数据流。

## Reader

Reader 的主 Header 是内容上下文而不是传统平台 toolbar：它显示当前订阅范围、未读数量、搜索、刷新和导入导出动作。

- Compact/Medium/Expanded：文章点击进入完整阅读页。
- Large：文章点击在右侧打开即时预览，保留左侧来源和中间分页列表；用户可关闭预览或进入完整阅读页。
- 文章卡片在 Medium 以上显示封面摘要，Compact 保留纯文本密度。
- 搜索、全部/未读/收藏筛选和 Paging load state 位于列表任务内，不污染订阅源导航。
- 完整详情使用 `ReadingCanvas`、最大阅读宽度、可选 Hero 图、正文选择、收藏、分享、浏览器/WebView 和 80%–150% 字体缩放。

Large 三栏只是窗口能力带来的组合变化，不会创建 Desktop ViewModel 或 Desktop Repository。

## Feed

Feed 复用相同 Feature 侧栏和 Context Hero。侧栏负责论坛来源与 Reader 范围，内容区只负责时间顺序、局部失败、分页、刷新与详情路由。

论坛主题与文章卡片仍使用各自的共享组件，因此 Reader 的未读/图片语义和 Forum 的回复/作者语义不会在聚合页面退化。

## 辅助任务与命令层

搜索、订阅、账号、用户动态、发帖、来源管理和同步不是独立的“手机版子页面”，而是工作区内的次级任务：

- `ThreadDetailScaffold` 统一返回导航、eyebrow、标题、上下文说明、动作、Snackbar、底部工具栏和系统 inset；Compact 只压缩辅助文案，不删除必要命令。
- `ThreadCommandBar` 在 Compact 纵向排列搜索与筛选，在其他宽度横向排列；`ThreadSearchField` 和 `ThreadFilterBar` 由 Forum 搜索、收藏和历史复用。
- 发帖编辑器的正文限制在阅读宽度内，底部编辑工具栏独立于滚动内容，并由 IME inset 驱动，不依赖 Android Activity 或 Desktop Window。
- 收藏选择模式通过 Hero 直接呈现选择数量和批量命令；历史用同一筛选条表达全部、帖子和文章，避免页面专属 segmented control。
- 同步页在 Compact 纵向排列凭据，在更大窗口并排；按钮组允许换行，数据包编辑器使用大尺寸自适应模态层。

旧 `ForumListPage` 与 `SubscriptionPaneScreen` 没有正式导航入口，且重复实现频道和 list-detail 逻辑，已经删除。ChannelPage、AdaptiveSidebarScaffold 与 Reader Large 三栏是唯一正式组合路径。

## 自适应模态层

`AdaptiveModal` 是跨 Feature 的临时任务边界：

- Compact：使用底部 sheet，符合触控拇指区域和手机返回手势。
- Medium/Expanded/Large：使用居中、限宽、限高的 dialog，避免 Desktop 上把表单拉成整屏。
- Reader 来源分析、订阅 JSON/OPML 传输、Forum 手动登录、引用内容、楼中楼、订阅 ID、Discourse 来源和全量用户数据包都复用该行为。
- AlertDialog 只保留短确认和短错误；长表单、长 JSON、可滚动内容必须使用 `AdaptiveModal`。

## 键盘、焦点与辅助技术

键盘能力是 common 交互契约，不是 Desktop 专用页面。`ThreadShortcut` 同时识别 Ctrl 与 Command，并只处理 KeyDown；命令键必需的快捷键不会吞掉输入框中的普通字符。

- Ctrl/Command+1–9 切换全局工作区，目标语义同时公布选中状态和对应快捷键。
- Ctrl/Command+Shift+P 打开命令面板；方向键选择，Enter 打开，Escape 关闭。面板搜索输入不会拦截普通字符。
- Ctrl/Command+B 打开或关闭 Compact/Medium 的 Feature 侧栏；永久侧栏只暴露 pane 语义，不执行无意义的开关动画。
- Ctrl/Command+K 聚焦 Reader 搜索，Ctrl/Command+R 刷新当前 Reader、Feed 或 Forum 上下文；Forum 详情额外支持 Ctrl/Command+G 跳页、Ctrl/Command+Home 回到顶部。
- Escape 由最内层模态任务优先关闭；没有模态任务时，`ThreadDetailScaffold` 执行返回。搜索和 Large 文章预览具有显式清理路径。
- 工作区根获得初始焦点，搜索打开后主动转移到输入框。侧栏条目至少 48dp，并向辅助技术提供 button role、selected、pane title 与 state description。

Compose 动画遵循平台 `MotionDurationScale`；本项目不绕过该缩放创建自行计时的导航动画。系统请求 reduced motion 时，过渡和滚动动画由 Compose 统一降速或关闭，不新增平台分支。

## 长列表与性能预算

Reader 为每个“来源 + 全部/未读/收藏”组合保存独立 `LazyListState`，Feed 为每组论坛来源与 Reader 开关保存独立状态；返回任务或切换筛选不会把用户送回顶部。Article、Topic、Timeline 和 Subscription 项使用来源隔离的稳定 key，刷新只更新变化行。

所有生产 Pager 使用 `threadPagingConfig`：默认页大小 20，初始两页、半页预取、关闭 placeholder、最多保留十页。特殊页只调整业务页大小或明确的一页初始窗口，不能自行创建无限缓存配置。

schema v4 为文章来源/时间、未读/收藏时间、订阅键/接收时间、主题/楼层、来源/用户时间添加组合索引；schema v5 为通知时间、未读、来源和类型增加索引。Desktop 测试使用 `EXPLAIN QUERY PLAN` 固定关键列表必须命中指定索引；性能门禁比较查询结构和内存窗口，不使用易受机器负载影响的墙钟阈值。

## 缓存优先与来源隔离

可靠性状态不是每个页面临时判断的视觉细节。`resolvePagingContentState` 定义全产品顺序：

1. 只要 Paging 已有行，始终呈现内容；refresh error 只叠加“正在显示本地缓存”状态条。
2. 没有行时，refresh loading 才使用页面级加载，refresh error 才使用阻断错误页。
3. refresh 完成且无行进入业务 empty state；不会在首次加载过程中闪烁空页面。
4. append loading/error/end 由 `PagingAppendState` 在列表尾部非阻断呈现，不覆盖用户已经阅读的位置。

`RefreshDiagnosticsBanner` 聚合 RefreshCoordinator 的来源级状态。离线、认证、限流、超时和服务错误使用同一词汇；展开后逐来源显示尝试次数和消息。Forum、Reader、Feed 的重试只重新触发所属任务，不会隐藏仍然可读的成功来源。

详情和原始内容也遵守该规则：文章原始页面通过 common Compose WebView 加载 URL/HTML；主题与文章的初始失败使用同一错误状态，成功后进入 ReadingCanvas。

## 设计系统

`ThreadTheme` 提供 Material 3 颜色、字型、shape 与额外语义色：success、warning、reader surface、interactive surface。持久化的主题模式、界面密度、字体比例、减弱动效、Reader 版心和正文行高通过 CompositionLocal 对所有平台生效；页面不得重新声明品牌色或使用来源图片制造不可预测的全屏背景。

核心组件：

- `ThreadAdaptiveWindow`：应用窗口能力边界。
- `WorkspaceNavigationSuite`：全局工作区导航。
- `AdaptiveSidebarScaffold`：Feature 导航。
- `ContextHero` / `PageHeader`：响应式页面层级与动作。
- `ThreadDetailScaffold`：次级工作流的返回导航、命令和 inset 边界。
- `ThreadCommandBar` / `ThreadSearchField` / `ThreadFilterBar`：搜索、筛选与批量命令层。
- `AdaptiveModal`：Compact bottom sheet 与宽窗口 dialog 的统一临时任务容器。
- `ThreadLoadingState` / `ThreadErrorState` / `ThreadStatusBanner`：页面级与非阻断可靠性反馈。
- `PagingStateLayout` / `PagingAppendState`：缓存优先刷新与列表尾部状态策略。
- `ThreadCard`：统一列表 Surface 与响应式 padding。
- `ReadingCanvas`：Forum/Reader 聚焦详情容器。
- `PagingStateLayout`：缓存优先的 loading、empty、error 与 retry 状态。

## 平台边界与未来形态

Android、iOS、Desktop 共用窗口模型、导航、Paging collector 和全部 Feature 页面。只有文件选择、WebView、数据库 Driver、系统分享等确实依赖平台的能力才进入平台 source set。

未来折叠屏、桌面自由窗口、平板 Stage Manager 或新的 Compose 目标应先映射为现有 width/height capability；只有铰链遮挡、系统窗口区等无法表达的信息才扩展 `ThreadWindowInfo`，不得新建整套页面。

## 当前验证

本阶段只运行 Desktop：

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH \
./gradlew :core-domain:jvmTest :core-data:jvmTest :core-ui:jvmTest \
  :composeApp:jvmTest :composeApp:compileKotlinJvm
```

该门禁覆盖录制式 Connector 合约、缓存与刷新不变量、schema v5 迁移/查询计划、有界 Paging 策略、交互快捷键、离线种子启动探针、common Forum/Reader/Feed/Inbox/core-ui、SQLDelight 代码生成与 Desktop 组合根。最终候选轮追加 `:composeApp:createReleaseDistributable`，并实际运行发行 launcher 的 `--smoke-check`。Android/iOS 不在每轮重复编译；平台回归在 common 架构和产品功能完成后集中执行。
