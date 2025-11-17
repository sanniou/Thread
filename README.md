# Thread - A Kotlin Multiplatform NMB Client

`Thread` 是一个使用 Kotlin Multiplatform 技术构建的 NMB 岛匿名版客户端，旨在提供一个现代化、高性能且跨平台的浏览体验。

## ✨ 功能 (Features)

-   **板块浏览**: 清晰地展示所有板块分类和板块列表。
-   **帖子阅读**: 支持分页加载帖子和回复，提供流畅的阅读体验。
-   **纯净阅读**: 支持“只看PO”模式，快速筛选关键信息。
-   **收藏功能**: 方便地收藏你感兴趣的帖子。
-   **跨平台**: 一套代码库，可编译运行于 Android, iOS, Desktop (JVM) 等多个平台。

## 🏛️ 架构 (Architecture)

项目采用现代化的软件架构，以确保代码的可维护性、可测试性和可扩展性。

-   **Kotlin Multiplatform**: 核心业务逻辑、数据处理和网络请求等代码在所有平台间共享，UI 层则针对各个平台进行原生实现。
-   **MVI (Model-View-Intent)**: UI 层严格遵循单向数据流（UDF）的 MVI 模式。这使得状态管理变得可预测，并且极大地简化了 UI 逻辑。
-   **依赖注入 (Dependency Injection)**: 使用 Koin 进行依赖管理，实现了模块间的解耦。
-   **分层设计**:
    -   **`feature-*`**: 功能模块层，包含了各个独立功能的 UI 和业务流程编排 (`ViewModel`)。
    -   **`domain`**: 领域层，封装了核心的、单一职责的业务逻辑 (`UseCase`)。
    -   **`data`**: 数据层，负责数据的获取、存储和管理 (`Repository`, `API`, `Database`)。
    -   **`core-*`**: 核心模块层，提供跨模块共享的工具、UI 组件和通用功能。

## 🛠️ 技术栈 (Tech Stack)

-   **[Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html)**: 跨平台开发框架。
-   **[Jetpack Compose](https://developer.android.com/jetpack/compose)**: 用于构建声明式的、现代化的 UI。
-   **[Voyager](https://voyager.adriel.cafe/)**: 用于 Compose Multiplatform 的导航库。
-   **[Koin](https://insert-koin.io/)**: 轻量级的依赖注入框架。
-   **[Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)**: 实现高效的分页加载。
-   **[SQLDelight](https://cashapp.github.io/sqldelight/)**: 生成类型安全的 Kotlin API 来操作 SQL 数据库。
-   **[Ktorfit](https://foso.github.io/Ktorfit/)**: 基于 Ktor 的类型安全 HTTP 客户端。

## 📦 模块划分 (Module Structure)

-   **`composeApp`**: 主应用模块，包含了各个平台的入口点和共享的 Compose UI 代码。
-   **`feature-nmb`**: NMB 功能的核心实现模块。详细架构请参见 `feature-nmb/README.md`。
-   **`feature-tieba`**: (规划中) 贴吧功能模块。
-   **`core-common`**: 平台无关的通用工具和数据结构。
-   **`core-ui`**: 跨功能模块共享的通用 UI 组件。

## 🚀 如何构建与运行 (Build & Run)

1.  **环境要求**:
    -   Android Studio (最新版本)
    -   JDK 17 或更高版本
    -   (macOS) Xcode 用于运行 iOS 应用

2.  **构建**:
    在项目根目录下执行以下命令：
    ```bash
    ./gradlew build
    ```

3.  **运行**:
    -   **Android**: 在 Android Studio 中选择 `composeApp` 作为运行配置，然后选择一个模拟器或连接的设备来运行。
    -   **Desktop**: 执行 Gradle 任务 `:composeApp:run`。
    -   **iOS**: 在 Android Studio 中选择 `iosApp` 运行配置，或直接在 Xcode 中打开 `iosApp` 项目来运行。


#  reference
https://github.com/TransparentLC/xdcmd/wiki/%E8%87%AA%E5%B7%B1%E6%95%B4%E7%90%86%E7%9A%84-X-%E5%B2%9B%E5%8C%BF%E5%90%8D%E7%89%88-API-%E6%96%87%E6%A1%A3

https://github.com/lumina37/aiotieba

https://github.com/HuanCheng65/TiebaLite

// nga api
// rss feed

https://m3.material.io/foundations/adaptive-design

https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation

// sample
https://github.com/kagg886/Pixiv-MultiPlatform
https://github.com/0xZhangKe/Fread
https://github.com/panpf/zoomimage/tree/main/sample/src/commonMain/kotlin/com/github/panpf/zoomimage/sample/ui/examples
https://github.com/TheChance101/beep-beep
