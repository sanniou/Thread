package ai.saniou.thread

import ai.saniou.coreui.composition.LocalAppDrawer
import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.coreui.theme.CupcakeTheme
import ai.saniou.coreui.widgets.DrawerMenuItem
import ai.saniou.coreui.widgets.DrawerMenuRow
import ai.saniou.forum.di.nmbFeatureModule
import ai.saniou.forum.workflow.home.ChannelPage
import ai.saniou.forum.workflow.image.nmbImagePreviewModule
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
import ai.saniou.thread.feature.cellularautomaton.CellularAutomatonScreen
import ai.saniou.thread.feature.challenge.CloudflareVerificationDialog
import ai.saniou.thread.feature.bookmark.BookmarkPage
import ai.saniou.thread.feature.challenge.UiChallengeHandler
import ai.saniou.thread.feature.history.HistoryPage
import ai.saniou.thread.feature.settings.SyncSettingsPage
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
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

@Composable
fun App() {
    val di = remember {
        DI {
            import(domainModule)
            import(dataModule)
            import(readerModule)
            import(feedModule)
            import(nmbImagePreviewModule)
            import(nmbFeatureModule)
            import(appModule)
        }
    }
    withDI(di) {
        val settingsRepository: SettingsRepository by di.instance()
        val currentSource by settingsRepository.observeValue<String>("current_source_id")
            .collectAsState(initial = "nmb")

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

        CupcakeTheme {
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
                val appDrawer = @Composable {
                    DrawerMenuRow(
                        menuItems = listOf(
                            DrawerMenuItem(
                                Icons.Default.Forum,
                                "匿名版"
                            ) { navigator.replaceAll(ForumRoute) },
                            DrawerMenuItem(
                                Icons.Default.RssFeed,
                                "阅读器"
                            ) { navigator.replaceAll(ReaderRoute) },
                            DrawerMenuItem(
                                Icons.Default.DynamicFeed,
                                "聚合信息流"
                            ) { navigator.replaceAll(FeedRoute) },
                            DrawerMenuItem(
                                Icons.Default.Games,
                                "元胞自动机"
                            ) { navigator.replaceAll(CellularAutomatonRoute) },
                            DrawerMenuItem(
                                Icons.Default.Bookmark,
                                "收藏夹"
                            ) { navigator.push(BookmarkPage) },
                            DrawerMenuItem(
                                Icons.Default.History,
                                "浏览历史"
                            ) { navigator.push(HistoryPage()) },
                            DrawerMenuItem(
                                Icons.Default.Settings,
                                "数据与同步"
                            ) { navigator.push(SyncSettingsPage()) },
                        )
                    )
                }
                val navigator = LocalNavigator.currentOrThrow
                val currentScreen = navigator.lastItem

                CompositionLocalProvider(
                    LocalAppDrawer provides appDrawer,
                    LocalForumSourceId provides (currentSource ?: "nmb")
                ) {
                    navigator.saveableState("currentScreen") {
                        currentScreen.Content()
                    }
                }
            }
        }
    }
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
