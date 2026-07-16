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
import ai.saniou.thread.domain.model.source.DEFAULT_FORUM_SOURCE_ID
import ai.saniou.thread.feature.cellularautomaton.CellularAutomatonScreen
import ai.saniou.thread.feature.challenge.CloudflareVerificationDialog
import ai.saniou.thread.feature.bookmark.BookmarkPage
import ai.saniou.thread.feature.challenge.UiChallengeHandler
import ai.saniou.thread.feature.history.HistoryPage
import ai.saniou.thread.feature.settings.SyncSettingsPage
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
import ai.saniou.thread.db.Database
import ai.saniou.forum.workflow.post.AttachmentPicker
import ai.saniou.forum.workflow.post.LocalAttachmentPicker
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
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
            .collectAsState(initial = DEFAULT_FORUM_SOURCE_ID)

        val challengeHandler: UiChallengeHandler by di.instance()
        var challengeRequest by remember { mutableStateOf<UiChallengeHandler.ChallengeRequest?>(null) }
        val scope = rememberCoroutineScope()
        val readerScheduler: ReaderRefreshScheduler by di.instance()

        DisposableEffect(readerScheduler) {
            readerScheduler.start()
            onDispose { readerScheduler.stop() }
        }

        LaunchedEffect(Unit) {
            challengeHandler.challengeEvents.collect {
                challengeRequest = it
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
            Navigator(
                screen = ForumRoute,
                disposeBehavior = NavigatorDisposeBehavior(disposeSteps = false),
            ) { navigator ->
                var selectedWorkspace by rememberSaveable { mutableStateOf(WorkspaceDestination.FORUM) }
                fun navigateTo(destination: WorkspaceDestination, screen: Screen) {
                    selectedWorkspace = destination
                    navigator.replaceAll(screen)
                }
                val currentScreen = navigator.lastItem
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
                        WorkspaceNavigationItem(Icons.Default.Bookmark, "收藏", selectedWorkspace == WorkspaceDestination.BOOKMARKS) {
                            navigateTo(WorkspaceDestination.BOOKMARKS, BookmarkPage)
                        },
                        WorkspaceNavigationItem(Icons.Default.History, "历史", selectedWorkspace == WorkspaceDestination.HISTORY) {
                            navigateTo(WorkspaceDestination.HISTORY, HistoryPage())
                        },
                        WorkspaceNavigationItem(Icons.Default.Games, "实验室", selectedWorkspace == WorkspaceDestination.LAB, bottom = true) {
                            navigateTo(WorkspaceDestination.LAB, CellularAutomatonRoute)
                        },
                        WorkspaceNavigationItem(Icons.Default.Settings, "设置", selectedWorkspace == WorkspaceDestination.SETTINGS, bottom = true) {
                            navigateTo(WorkspaceDestination.SETTINGS, SyncSettingsPage())
                        },
                    )
                    WorkspaceNavigationSuite(navigationItems) {
                            CompositionLocalProvider(
                                LocalForumSourceId provides (currentSource ?: DEFAULT_FORUM_SOURCE_ID),
                                LocalAttachmentPicker provides attachmentPicker,
                            ) {
                                navigator.saveableState("currentScreen") {
                                    currentScreen.Content()
                                }
                            }
                    }
                }
            }
        }
    }
}

private enum class WorkspaceDestination {
    FORUM,
    READER,
    FEED,
    BOOKMARKS,
    HISTORY,
    LAB,
    SETTINGS,
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
