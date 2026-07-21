package ai.saniou.thread

import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.coreui.composition.LocalContentLinkHandler
import ai.saniou.coreui.layout.ThreadAdaptiveWindow
import ai.saniou.coreui.theme.ThreadTheme
import ai.saniou.coreui.theme.ThreadInterfaceDensity
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
import ai.saniou.forum.workflow.post.PostPage
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
import ai.saniou.thread.domain.model.activity.ActivityCenterSnapshot
import ai.saniou.thread.domain.model.activity.ProductActionDanger
import ai.saniou.thread.domain.model.operations.ProductCommandAction
import ai.saniou.thread.domain.model.operations.ProductCommandDescriptor
import ai.saniou.thread.domain.usecase.search.SearchLocalContentUseCase
import ai.saniou.thread.domain.usecase.workspace.ValidateRestorableContentUseCase
import ai.saniou.thread.domain.usecase.operations.BuildProductCommandsUseCase
import ai.saniou.thread.domain.usecase.activity.ObserveActivityCenterUseCase
import ai.saniou.thread.domain.usecase.activity.ExecuteProductActionUseCase
import ai.saniou.thread.feature.challenge.CloudflareVerificationDialog
import ai.saniou.thread.feature.bookmark.BookmarkPage
import ai.saniou.thread.feature.challenge.UiChallengeHandler
import ai.saniou.thread.feature.history.HistoryPage
import ai.saniou.thread.feature.settings.SyncSettingsPage
import ai.saniou.thread.feature.search.GlobalSearchPage
import ai.saniou.thread.feature.operations.OperationsPage
import ai.saniou.thread.feature.activity.ActivityCenterPage
import ai.saniou.thread.feature.inbox.InboxPage
import ai.saniou.thread.feature.social.SocialDetailPage
import ai.saniou.thread.feature.commands.CommandPalette
import ai.saniou.thread.feature.commands.ProductCommand
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
import ai.saniou.thread.db.Database
import ai.saniou.forum.workflow.post.AttachmentPicker
import ai.saniou.forum.workflow.post.LocalAttachmentPicker
import ai.saniou.coreui.interaction.rememberThreadClipboard
import ai.saniou.coreui.platform.AppEntryController
import ai.saniou.coreui.platform.AppEntrySource
import ai.saniou.coreui.platform.BackgroundRefreshBridge
import ai.saniou.coreui.platform.LocalAppEntryController
import ai.saniou.coreui.platform.LocalBackgroundRefreshBridge
import ai.saniou.coreui.platform.LocalShareService
import ai.saniou.coreui.platform.LocalSystemNotificationService
import ai.saniou.coreui.platform.LocalUserDataFileService
import ai.saniou.coreui.platform.ShareService
import ai.saniou.coreui.platform.SystemNotificationService
import ai.saniou.coreui.platform.UserDataFileService
import ai.saniou.thread.domain.model.social.CursorDirection
import ai.saniou.thread.domain.repository.InboxRepository
import ai.saniou.thread.domain.repository.SocialRepository
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouDangerButton
import ai.saniou.coreui.widgets.SaniouTextButton
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import ai.saniou.thread.domain.repository.AppearanceRepository
import ai.saniou.thread.domain.repository.ContentLinkRepository
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.content.LinkResolution
import ai.saniou.thread.domain.model.settings.AppearancePreferences
import ai.saniou.thread.domain.model.settings.InterfaceDensity
import ai.saniou.thread.domain.model.settings.MotionMode
import ai.saniou.thread.domain.model.settings.ThemeMode
import androidx.compose.ui.platform.LocalUriHandler
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
fun App(
    attachmentPicker: AttachmentPicker? = null,
    appEntryController: AppEntryController? = null,
    shareService: ShareService? = null,
    userDataFileService: UserDataFileService? = null,
    systemNotificationService: SystemNotificationService? = null,
    backgroundRefreshBridge: BackgroundRefreshBridge? = null,
) {
    val di = remember { createAppDi() }
    withDI(di) {
        val settingsRepository: SettingsRepository by di.instance()
        val currentSource by settingsRepository.observeValue<String>("current_source_id")
            .collectAsState(initial = null)
        val workspaceSessionRepository: WorkspaceSessionRepository by di.instance()
        var workspaceSession by remember { mutableStateOf<WorkspaceSession?>(null) }
        val searchLocalContent: SearchLocalContentUseCase by di.instance()
        val validateRestorableContent: ValidateRestorableContentUseCase by di.instance()
        val observeActivityCenter: ObserveActivityCenterUseCase by di.instance()
        val executeProductAction: ExecuteProductActionUseCase by di.instance()
        val buildProductCommands: BuildProductCommandsUseCase by di.instance()
        val activitySnapshot by observeActivityCenter().collectAsState(initial = ActivityCenterSnapshot())
        val appearanceRepository: AppearanceRepository by di.instance()
        val appearance by appearanceRepository.observe().collectAsState(initial = AppearancePreferences())
        val contentLinkRepository: ContentLinkRepository by di.instance()
        val socialRepository: SocialRepository by di.instance()
        val inboxRepository: InboxRepository by di.instance()
        val resolvedEntryController = remember(appEntryController) { appEntryController ?: AppEntryController() }
        val resolvedShareService = shareService
        val resolvedUserDataFileService = userDataFileService
        val resolvedNotificationService = systemNotificationService

        val challengeHandler: UiChallengeHandler by di.instance()
        var challengeRequest by remember { mutableStateOf<UiChallengeHandler.ChallengeRequest?>(null) }
        var showCommandPalette by remember { mutableStateOf(false) }
        var pendingProductCommand by remember { mutableStateOf<ProductCommandDescriptor?>(null) }
        val scope = rememberCoroutineScope()
        val readerScheduler: ReaderRefreshScheduler by di.instance()
        val appSnackbar = remember { SnackbarHostState() }
        val clipboard = rememberThreadClipboard()
        val uriHandler = LocalUriHandler.current

        DisposableEffect(readerScheduler, backgroundRefreshBridge, socialRepository) {
            readerScheduler.start()
            val bridge = backgroundRefreshBridge ?: object : BackgroundRefreshBridge {
                private var job: kotlinx.coroutines.Job? = null
                override fun start() {
                    if (job?.isActive == true) return
                    job = scope.launch {
                        while (true) {
                            runCatching {
                                readerScheduler.refreshDueNow()
                                socialRepository.refresh(direction = CursorDirection.NEWER)
                            }
                            kotlinx.coroutines.delay(15 * 60 * 1000L)
                        }
                    }
                }
                override fun stop() {
                    job?.cancel()
                    job = null
                }
            }
            bridge.start()
            onDispose {
                bridge.stop()
                readerScheduler.stop()
            }
        }

        LaunchedEffect(inboxRepository, resolvedNotificationService) {
            var lastUnread = -1
            inboxRepository.observeSummary().collect { summary ->
                if (lastUnread >= 0 && summary.unread > lastUnread) {
                    val delta = summary.unread - lastUnread
                    resolvedNotificationService?.notify(
                        title = "Thread 收件箱",
                        body = if (delta == 1) "有 1 条新通知" else "有 $delta 条新通知",
                        deepLink = "thread://inbox",
                        notificationId = "inbox-unread",
                    )
                }
                lastUnread = summary.unread
            }
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

        val systemDark = isSystemInDarkTheme()
        ThreadTheme(
            darkTheme = when (appearance.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            },
            interfaceDensity = when (appearance.density) {
                InterfaceDensity.COMPACT -> ThreadInterfaceDensity.COMPACT
                InterfaceDensity.COMFORTABLE -> ThreadInterfaceDensity.COMFORTABLE
                InterfaceDensity.SPACIOUS -> ThreadInterfaceDensity.SPACIOUS
            },
            fontScale = appearance.fontScale,
            reducedMotion = appearance.motionMode == MotionMode.REDUCED,
            readerWidth = appearance.readerWidthDp.dp,
            readerLineHeightMultiplier = appearance.readerLineHeight,
        ) {
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
                // Child features may request shell navigation by updating workspace session destination
                // (e.g. Forum cache banner -> Operations, empty workspace -> Settings import).
                LaunchedEffect(workspaceSession?.destination, workspaceSession?.updatedAtEpochMillis) {
                    val dest = workspaceSession?.destination ?: return@LaunchedEffect
                    if (dest.key == selectedWorkspaceKey) return@LaunchedEffect
                    selectedWorkspaceKey = dest.key
                    navigator.replaceAll(screenFor(dest))
                }
                fun openContentUrl(url: String) {
                    scope.launch {
                        val normalized = url.trim()
                        if (normalized.equals("thread://inbox", ignoreCase = true) ||
                            normalized.equals("thread://inbox/", ignoreCase = true)
                        ) {
                            navigateTo(WorkspaceDestination.INBOX, InboxPage)
                            return@launch
                        }
                        if (normalized.equals("thread://feed", ignoreCase = true) ||
                            normalized.equals("thread://feed/", ignoreCase = true)
                        ) {
                            navigateTo(WorkspaceDestination.FEED, FeedRoute)
                            return@launch
                        }
                        when (val resolution = contentLinkRepository.resolveUrl(normalized)) {
                            is LinkResolution.External -> uriHandler.openUri(resolution.url)
                            is LinkResolution.Unsupported -> appSnackbar.showSnackbar(resolution.reason)
                            is LinkResolution.Internal -> when (resolution.reference.kind) {
                                ContentReferenceKind.TOPIC -> {
                                    navigateTo(WorkspaceDestination.FORUM, ForumRoute)
                                    navigator.push(FeedTopicRoute(checkNotNull(resolution.reference.sourceId), resolution.reference.id))
                                }
                                ContentReferenceKind.COMMENT -> {
                                    navigateTo(WorkspaceDestination.FORUM, ForumRoute)
                                    navigator.push(FeedTopicRoute(
                                        checkNotNull(resolution.reference.sourceId),
                                        checkNotNull(resolution.reference.parentId),
                                    ))
                                }
                                ContentReferenceKind.ARTICLE -> {
                                    navigateTo(WorkspaceDestination.READER, ReaderRoute)
                                    navigator.push(ArticleDetailPage(resolution.reference.id))
                                }
                                ContentReferenceKind.SOCIAL_POST -> {
                                    navigateTo(WorkspaceDestination.FEED, FeedRoute)
                                    navigator.push(
                                        SocialDetailPage(
                                            sourceId = checkNotNull(resolution.reference.sourceId),
                                            postId = resolution.reference.id,
                                        ),
                                    )
                                }
                                ContentReferenceKind.EXTERNAL_URL -> resolution.reference.canonicalUrl
                                    ?.let(uriHandler::openUri)
                            }
                        }
                    }
                }
                LaunchedEffect(resolvedEntryController, navigator) {
                    resolvedEntryController.entries.collect { entry ->
                        when (entry.source) {
                            AppEntrySource.FILE_IMPORT -> {
                                navigateTo(
                                    WorkspaceDestination.SETTINGS,
                                    SyncSettingsPage(showImportOnOpen = true, initialImportPayload = entry.url),
                                )
                            }
                            else -> openContentUrl(entry.url)
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
                        GlobalSearchType.SOCIAL -> openContentUrl("thread://social/${result.sourceId}/${result.id}")
                    }
                }
                val productCommands = remember(activitySnapshot) {
                    buildProductCommands(activitySnapshot).map(::ProductCommand)
                }
                fun notifyCommand(message: String) {
                    scope.launch { appSnackbar.showSnackbar(message) }
                }
                fun performProductCommand(command: ProductCommandDescriptor, confirmed: Boolean = false) {
                    if (!confirmed && command.action == ProductCommandAction.EXECUTE_PRODUCT_ACTION &&
                        command.danger != ProductActionDanger.NORMAL
                    ) {
                        pendingProductCommand = command
                        return
                    }
                    when (command.action) {
                        ProductCommandAction.OPEN_ACTIVITY_CENTER ->
                            navigateTo(WorkspaceDestination.ACTIVITY, ActivityCenterPage)
                        ProductCommandAction.OPEN_SOURCE_LOGIN -> {
                            val sourceId = command.sourceId ?: return
                            navigateTo(WorkspaceDestination.FORUM, ForumRoute)
                            navigator.push(ForumUserRoute(sourceId))
                        }
                        ProductCommandAction.EXECUTE_PRODUCT_ACTION -> scope.launch {
                            val request = command.request ?: return@launch
                            executeProductAction(request).fold(
                                onSuccess = { result ->
                                    result.output?.let(clipboard::copyText)
                                    notifyCommand(result.message + if (result.output == null) "" else "，内容已复制")
                                },
                                onFailure = { notifyCommand(it.message ?: "${command.label}失败") },
                            )
                        }
                        ProductCommandAction.OPEN_READER_IMPORT ->
                            navigateTo(
                                WorkspaceDestination.READER,
                                ReaderPage(command.request?.readerFormat),
                            )
                        ProductCommandAction.OPEN_USER_DATA_IMPORT ->
                            navigateTo(WorkspaceDestination.SETTINGS, SyncSettingsPage(showImportOnOpen = true))
                        ProductCommandAction.RESUME_DRAFT -> {
                            val key = command.draftKey ?: return
                            navigateTo(WorkspaceDestination.FORUM, ForumRoute)
                            navigator.push(
                                PostPage(
                                    sourceId = key.sourceId,
                                    channelId = key.targetId.takeIf {
                                        key.targetKind == ai.saniou.thread.domain.model.forum.PostDraftTargetKind.CHANNEL
                                    },
                                    topicId = key.targetId.takeIf {
                                        key.targetKind == ai.saniou.thread.domain.model.forum.PostDraftTargetKind.TOPIC
                                    },
                                )
                            )
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
                        WorkspaceNavigationItem(Icons.Default.Inbox, "收件箱", selectedWorkspace == WorkspaceDestination.INBOX) {
                            navigateTo(WorkspaceDestination.INBOX, InboxPage)
                        },
                        WorkspaceNavigationItem(Icons.Default.NotificationsActive, "活动", selectedWorkspace == WorkspaceDestination.ACTIVITY, bottom = true) {
                            navigateTo(WorkspaceDestination.ACTIVITY, ActivityCenterPage)
                        },
                        WorkspaceNavigationItem(Icons.Default.MonitorHeart, "运维", selectedWorkspace == WorkspaceDestination.OPERATIONS, bottom = true) {
                            navigateTo(WorkspaceDestination.OPERATIONS, OperationsPage)
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
                                LocalContentLinkHandler provides ::openContentUrl,
                                LocalAppEntryController provides resolvedEntryController,
                                LocalShareService provides resolvedShareService,
                                LocalUserDataFileService provides resolvedUserDataFileService,
                                LocalSystemNotificationService provides resolvedNotificationService,
                                LocalBackgroundRefreshBridge provides backgroundRefreshBridge,
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
                        onProductCommand = { performProductCommand(it) },
                        onResult = ::openSearchResult,
                    )
                }
                pendingProductCommand?.let { command ->
                    AlertDialog(
                        onDismissRequest = { pendingProductCommand = null },
                        title = { Text(if (command.danger == ProductActionDanger.DESTRUCTIVE) "确认永久操作" else "确认数据变更") },
                        text = { Text(command.description) },
                        confirmButton = {
                            if (command.danger == ProductActionDanger.DESTRUCTIVE) {
                                SaniouDangerButton(onClick = {
                                    pendingProductCommand = null
                                    performProductCommand(command, confirmed = true)
                                }, text = "继续")
                            } else {
                                SaniouButton(onClick = {
                                    pendingProductCommand = null
                                    performProductCommand(command, confirmed = true)
                                }, text = "继续")
                            }
                        },
                        dismissButton = {
                            SaniouTextButton(onClick = { pendingProductCommand = null }, text = "取消")
                        },
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
    WorkspaceDestination.INBOX -> InboxPage
    WorkspaceDestination.HISTORY -> HistoryPage()
    WorkspaceDestination.ACTIVITY -> ActivityCenterPage
    WorkspaceDestination.OPERATIONS -> OperationsPage
    WorkspaceDestination.SETTINGS -> SyncSettingsPage()
}

object ForumRoute : Screen {
    @Composable
    override fun Content() {
        ChannelPage().Content()
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
            onOpenSocial = { post ->
                navigator.push(SocialDetailPage(post.sourceId, post.id))
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
