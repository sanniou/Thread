package ai.saniou.thread

import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.coreui.layout.ThreadAdaptiveWindow
import ai.saniou.coreui.theme.ThreadTheme
import ai.saniou.coreui.widgets.WorkspaceNavigationItem
import ai.saniou.coreui.widgets.WorkspaceNavigationSuite
import ai.saniou.forum.di.forumFeatureModule
import ai.saniou.forum.workflow.home.ChannelPage
import ai.saniou.forum.workflow.image.imagePreviewModule
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.forum.workflow.user.UserPage
import ai.saniou.feature.feed.di.feedModule
import ai.saniou.feature.feed.workflow.UnifiedFeedPage
import ai.saniou.feature.feed.workflow.FeedViewModel
import ai.saniou.reader.di.readerModule
import ai.saniou.reader.workflow.articledetail.ArticleDetailPage
import ai.saniou.reader.workflow.reader.ReaderPage
import ai.saniou.thread.data.di.dataModule
import ai.saniou.thread.di.appModule
import ai.saniou.thread.domain.di.domainModule
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.domain.repository.WorkspaceSessionRepository
import ai.saniou.thread.domain.model.source.DEFAULT_FORUM_SOURCE_ID
import ai.saniou.thread.domain.model.search.GlobalSearchResult
import ai.saniou.thread.domain.model.search.GlobalSearchType
import ai.saniou.thread.domain.model.workspace.WorkspaceDestination
import ai.saniou.thread.domain.model.workspace.WorkspaceSession
import ai.saniou.thread.domain.model.workspace.RestorableContentKind
import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.model.operations.OperationsSnapshot
import ai.saniou.thread.domain.model.operations.ProductCommandAction
import ai.saniou.thread.domain.model.operations.ProductCommandDescriptor
import ai.saniou.thread.domain.usecase.search.SearchLocalContentUseCase
import ai.saniou.thread.domain.usecase.workspace.ValidateRestorableContentUseCase
import ai.saniou.thread.domain.usecase.operations.BuildProductCommandsUseCase
import ai.saniou.thread.domain.usecase.operations.ExportDiagnosticUseCase
import ai.saniou.thread.domain.usecase.operations.ObserveOperationsUseCase
import ai.saniou.thread.domain.usecase.channel.FetchChannelsUseCase
import ai.saniou.thread.domain.usecase.reader.RefreshAllFeedsUseCase
import ai.saniou.thread.domain.usecase.reader.RefreshFeedSourceUseCase
import ai.saniou.thread.domain.usecase.source.SetSourceEnabledUseCase
import ai.saniou.thread.feature.cellularautomaton.CellularAutomatonScreen
import ai.saniou.thread.feature.challenge.CloudflareVerificationDialog
import ai.saniou.thread.feature.bookmark.BookmarkPage
import ai.saniou.thread.feature.challenge.UiChallengeHandler
import ai.saniou.thread.feature.history.HistoryPage
import ai.saniou.thread.feature.settings.SyncSettingsPage
import ai.saniou.thread.feature.search.GlobalSearchPage
import ai.saniou.thread.feature.operations.OperationsPage
import ai.saniou.thread.feature.commands.CommandPalette
import ai.saniou.thread.feature.commands.ProductCommand
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
import ai.saniou.thread.db.Database
import ai.saniou.forum.workflow.post.AttachmentPicker
import ai.saniou.forum.workflow.post.LocalAttachmentPicker
import ai.saniou.coreui.interaction.rememberThreadClipboard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.rememberScreenModel
import kotlinx.coroutines.launch
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.DI
import org.kodein.di.compose.withDI
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.bindSingleton

fun createAppDi(databaseOverride: Database? = null) = DI {
    import(domainModule)
    import(dataModule)
    import(readerModule)
    import(feedModule)
    import(imagePreviewModule)
    import(forumFeatureModule)
    import(appModule)
    if (databaseOverride != null) {
        bindSingleton<Database>(overrides = true) { databaseOverride }
    }
}

@Composable
fun App(attachmentPicker: AttachmentPicker? = null) {
    val di = remember { createAppDi() }
    withDI(di) {
        val settingsRepository: SettingsRepository by di.instance()
        val currentSource by settingsRepository.observeValue<String>("current_source_id")
            .collectAsState(initial = null)
        val workspaceSessionRepository: WorkspaceSessionRepository by di.instance()
        var workspaceSession by remember { mutableStateOf<WorkspaceSession?>(null) }
        val searchLocalContent: SearchLocalContentUseCase by di.instance()
        val validateRestorableContent: ValidateRestorableContentUseCase by di.instance()
        val observeOperations: ObserveOperationsUseCase by di.instance()
        val buildProductCommands: BuildProductCommandsUseCase by di.instance()
        val fetchChannels: FetchChannelsUseCase by di.instance()
        val refreshFeedSource: RefreshFeedSourceUseCase by di.instance()
        val refreshAllFeeds: RefreshAllFeedsUseCase by di.instance()
        val setSourceEnabled: SetSourceEnabledUseCase by di.instance()
        val exportDiagnostic: ExportDiagnosticUseCase by di.instance()
        val operationsSnapshot by observeOperations().collectAsState(initial = OperationsSnapshot())

        val challengeHandler: UiChallengeHandler by di.instance()
        var challengeRequest by remember { mutableStateOf<UiChallengeHandler.ChallengeRequest?>(null) }
        var showCommandPalette by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val readerScheduler: ReaderRefreshScheduler by di.instance()
        val appSnackbar = remember { SnackbarHostState() }
        val clipboard = rememberThreadClipboard()

        DisposableEffect(readerScheduler) {
            readerScheduler.start()
            onDispose { readerScheduler.stop() }
        }

        LaunchedEffect(Unit) {
            challengeHandler.challengeEvents.collect {
                challengeRequest = it
            }
        }
        LaunchedEffect(workspaceSessionRepository) {
            workspaceSessionRepository.observe().collect { workspaceSession = it }
        }
        LaunchedEffect(currentSource) {
            currentSource?.let { sourceId ->
                workspaceSessionRepository.update { current ->
                    if (current.forumSourceId == sourceId) current else current.copy(
                        forumSourceId = sourceId,
                        updatedAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                    )
                }
            }
        }

        ThreadTheme {
            if (challengeRequest != null) {
                CloudflareVerificationDialog(
                    url = challengeRequest!!.url,
                    onDismissRequest = {
                        challengeRequest?.onResult?.invoke(false)
                        challengeRequest = null
                    },
                    onChallengeSuccess = { cookies ->
                        scope.launch {
                            challengeHandler.onChallengeResult(challengeRequest!!.sourceId, true, cookies)
                            challengeRequest?.onResult?.invoke(true)
                            challengeRequest = null
                        }
                    }
                )
            }
            val restoredSession = workspaceSession
            if (restoredSession == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@ThreadTheme
            }
            val initialDestination = remember { restoredSession.destination }
            val initialContentReference = remember { restoredSession.lastContent }
            Navigator(
                screen = screenFor(initialDestination),
                disposeBehavior = NavigatorDisposeBehavior(disposeSteps = false),
            ) { navigator ->
                var selectedWorkspaceKey by rememberSaveable { mutableStateOf(initialDestination.key) }
                val selectedWorkspace = WorkspaceDestination.fromKey(selectedWorkspaceKey)
                LaunchedEffect(navigator, initialContentReference) {
                    val reference = initialContentReference ?: return@LaunchedEffect
                    if (reference.workspace != initialDestination || !validateRestorableContent(reference)) {
                        workspaceSessionRepository.update { current ->
                            if (current.lastContent == reference) current.copy(
                                lastContent = null,
                                updatedAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                            ) else current
                        }
                        return@LaunchedEffect
                    }
                    when (reference.kind) {
                        RestorableContentKind.ARTICLE -> navigator.push(ArticleDetailPage(reference.id))
                        RestorableContentKind.TOPIC -> navigator.push(
                            FeedTopicRoute(checkNotNull(reference.sourceId), reference.id)
                        )
                    }
                }
                fun navigateTo(destination: WorkspaceDestination, screen: Screen) {
                    selectedWorkspaceKey = destination.key
                    navigator.replaceAll(screen)
                    scope.launch {
                        workspaceSessionRepository.update { current ->
                            current.copy(
                                destination = destination,
                                lastContent = null,
                                updatedAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                            )
                        }
                    }
                }
                fun openSearchResult(result: GlobalSearchResult) {
                    when (result.type) {
                        GlobalSearchType.ARTICLE -> {
                            navigateTo(WorkspaceDestination.READER, ReaderRoute)
                            navigator.push(ArticleDetailPage(result.id))
                        }
                        GlobalSearchType.TOPIC -> {
                            navigateTo(WorkspaceDestination.FORUM, ForumRoute)
                            navigator.push(FeedTopicRoute(result.sourceId, result.id))
                        }
                        GlobalSearchType.COMMENT -> {
                            navigateTo(WorkspaceDestination.FORUM, ForumRoute)
                            navigator.push(FeedTopicRoute(result.sourceId, result.contextId ?: result.id))
                        }
                    }
                }
                val productCommands = remember(operationsSnapshot) {
                    buildProductCommands(operationsSnapshot).map(::ProductCommand)
                }
                fun notifyCommand(message: String) {
                    scope.launch { appSnackbar.showSnackbar(message) }
                }
                fun performProductCommand(command: ProductCommandDescriptor) {
                    when (command.action) {
                        ProductCommandAction.OPEN_SOURCE_LOGIN -> {
                            val sourceId = command.sourceId ?: return
                            navigateTo(WorkspaceDestination.FORUM, ForumRoute)
                            navigator.push(ForumUserRoute(sourceId))
                        }
                        ProductCommandAction.REFRESH_SOURCE -> scope.launch {
                            val sourceId = command.sourceId ?: return@launch
                            val result = when (command.sourceKind) {
                                ContentSourceKind.FORUM -> fetchChannels(sourceId, forceRefresh = true)
                                ContentSourceKind.READER -> refreshFeedSource(sourceId, forceRefresh = true)
                                null -> Result.failure(IllegalArgumentException("缺少来源类型"))
                            }
                            notifyCommand(result.fold(
                                onSuccess = { "${command.label}完成" },
                                onFailure = { it.message ?: "${command.label}失败" },
                            ))
                        }
                        ProductCommandAction.SET_SOURCE_ENABLED -> scope.launch {
                            val sourceId = command.sourceId ?: return@launch
                            val enabled = command.enabledValue ?: return@launch
                            runCatching { setSourceEnabled(sourceId, enabled) }
                                .onSuccess { notifyCommand("已${if (enabled) "启用" else "停用"} $sourceId") }
                                .onFailure { notifyCommand(it.message ?: "来源状态更新失败") }
                        }
                        ProductCommandAction.REFRESH_ALL_READERS -> scope.launch {
                            val report = refreshAllFeeds()
                            notifyCommand(
                                "Reader 刷新完成：${report.refreshedSourceIds.size} 成功" +
                                    if (report.failures.isEmpty()) "" else "，${report.failures.size} 失败"
                            )
                        }
                        ProductCommandAction.EXPORT_DIAGNOSTIC -> scope.launch {
                            runCatching { exportDiagnostic() }
                                .onSuccess {
                                    clipboard.copyText(it.payload)
                                    notifyCommand("脱敏诊断已复制（${it.sourceCount} 个来源）")
                                }
                                .onFailure { notifyCommand(it.message ?: "诊断导出失败") }
                        }
                    }
                }
                val currentScreen = navigator.lastItem
                Box(Modifier.fillMaxSize()) {
                ThreadAdaptiveWindow {
                    val navigationItems = listOf(
                        WorkspaceNavigationItem(Icons.Default.Forum, "社区", selectedWorkspace == WorkspaceDestination.FORUM) {
                            navigateTo(WorkspaceDestination.FORUM, ForumRoute)
                        },
                        WorkspaceNavigationItem(Icons.Default.RssFeed, "阅读", selectedWorkspace == WorkspaceDestination.READER) {
                            navigateTo(WorkspaceDestination.READER, ReaderRoute)
                        },
                        WorkspaceNavigationItem(Icons.Default.DynamicFeed, "动态", selectedWorkspace == WorkspaceDestination.FEED) {
                            navigateTo(WorkspaceDestination.FEED, FeedRoute)
                        },
                        WorkspaceNavigationItem(Icons.Default.Search, "发现", selectedWorkspace == WorkspaceDestination.SEARCH) {
                            navigateTo(WorkspaceDestination.SEARCH, GlobalSearchPage)
                        },
                        WorkspaceNavigationItem(Icons.Default.Bookmark, "收藏", selectedWorkspace == WorkspaceDestination.BOOKMARKS) {
                            navigateTo(WorkspaceDestination.BOOKMARKS, BookmarkPage)
                        },
                        WorkspaceNavigationItem(Icons.Default.History, "历史", selectedWorkspace == WorkspaceDestination.HISTORY) {
                            navigateTo(WorkspaceDestination.HISTORY, HistoryPage())
                        },
                        WorkspaceNavigationItem(Icons.Default.MonitorHeart, "运维", selectedWorkspace == WorkspaceDestination.OPERATIONS, bottom = true) {
                            navigateTo(WorkspaceDestination.OPERATIONS, OperationsPage)
                        },
                        WorkspaceNavigationItem(Icons.Default.Games, "实验室", selectedWorkspace == WorkspaceDestination.LAB, bottom = true) {
                            navigateTo(WorkspaceDestination.LAB, CellularAutomatonRoute)
                        },
                        WorkspaceNavigationItem(Icons.Default.Settings, "设置", selectedWorkspace == WorkspaceDestination.SETTINGS, bottom = true) {
                            navigateTo(WorkspaceDestination.SETTINGS, SyncSettingsPage())
                        },
                    )
                    WorkspaceNavigationSuite(
                        items = navigationItems,
                        onOpenCommandPalette = { showCommandPalette = true },
                    ) {
                            CompositionLocalProvider(
                                LocalForumSourceId provides (
                                    currentSource ?: restoredSession.forumSourceId ?: DEFAULT_FORUM_SOURCE_ID
                                ),
                                LocalAttachmentPicker provides attachmentPicker,
                            ) {
                                navigator.saveableState("currentScreen") {
                                    currentScreen.Content()
                                }
                            }
                    }
                }
                    SnackbarHost(appSnackbar, Modifier.align(Alignment.BottomCenter))
                }
                if (showCommandPalette) {
                    CommandPalette(
                        searchLocalContent = searchLocalContent,
                        productCommands = productCommands,
                        onDismiss = { showCommandPalette = false },
                        onCommand = { command -> navigateTo(command.destination, screenFor(command.destination)) },
                        onProductCommand = ::performProductCommand,
                        onResult = ::openSearchResult,
                    )
                }
            }
        }
    }
}

private fun screenFor(destination: WorkspaceDestination): Screen = when (destination) {
    WorkspaceDestination.FORUM -> ForumRoute
    WorkspaceDestination.READER -> ReaderRoute
    WorkspaceDestination.FEED -> FeedRoute
    WorkspaceDestination.SEARCH -> GlobalSearchPage
    WorkspaceDestination.BOOKMARKS -> BookmarkPage
    WorkspaceDestination.HISTORY -> HistoryPage()
    WorkspaceDestination.OPERATIONS -> OperationsPage
    WorkspaceDestination.LAB -> CellularAutomatonRoute
    WorkspaceDestination.SETTINGS -> SyncSettingsPage()
}

object ForumRoute : Screen {
    @Composable
    override fun Content() {
        ChannelPage().Content()
    }
}

object CellularAutomatonRoute : Screen {
    @Composable
    override fun Content() {
        CellularAutomatonScreen()
    }
}

object ReaderRoute : Screen {
    @Composable
    override fun Content() {
        ReaderPage().Content()
    }
}

object FeedRoute : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val di = localDI()
        val viewModel = rememberScreenModel { di.direct.instance<FeedViewModel>() }
        UnifiedFeedPage(
            viewModel = viewModel,
            onOpenTopic = { topic ->
                navigator.push(FeedTopicRoute(topic.sourceId, topic.id))
            },
            onOpenArticle = { article ->
                navigator.push(ArticleDetailPage(article.id))
            },
        )
    }
}

data class FeedTopicRoute(
    val sourceId: String,
    val topicId: String,
) : Screen {
    @Composable
    override fun Content() {
        CompositionLocalProvider(LocalForumSourceId provides sourceId) {
            TopicDetailPage(topicId).Content()
        }
    }
}

data class ForumUserRoute(val sourceId: String) : Screen {
    @Composable
    override fun Content() {
        CompositionLocalProvider(LocalForumSourceId provides sourceId) {
            UserPage().Content()
        }
    }
}
