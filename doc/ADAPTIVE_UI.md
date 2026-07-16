# Thread 自适应 UI 架构

## 产品目标

Thread 不是把手机版页面放大到 Desktop，也不为每个平台维护一套独立 UI。Forum、Reader、Feed 使用同一份 Compose Multiplatform 页面代码，并根据“当前窗口能承载什么”切换信息密度、导航位置与并行任务数量。

UI 的职责分成三层：

1. 工作区导航：社区、阅读、动态、收藏、历史、实验室、设置。
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

## 设计系统

`ThreadTheme` 提供 Material 3 颜色、字型、shape 与额外语义色：success、warning、reader surface、interactive surface。页面不得重新声明品牌色或使用来源图片制造不可预测的全屏背景。

核心组件：

- `ThreadAdaptiveWindow`：应用窗口能力边界。
- `WorkspaceNavigationSuite`：全局工作区导航。
- `AdaptiveSidebarScaffold`：Feature 导航。
- `ContextHero` / `PageHeader`：响应式页面层级与动作。
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
./gradlew :composeApp:compileKotlinJvm
```

该门禁覆盖 common Forum、Reader、Feed、core-ui、AndroidX Paging、SQLDelight 代码生成与 Desktop 组合根。Android/iOS 不在每轮重复编译；平台回归在 common 架构和产品功能完成后集中执行。
