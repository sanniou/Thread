package ai.saniou.forum.workflow.topic

import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.forum.ui.components.ForumRichText
import ai.saniou.forum.workflow.home.ListThreadPage
import ai.saniou.forum.workflow.image.ImagePreviewPage
import ai.saniou.forum.workflow.image.ImagePreviewViewModelParams
import ai.saniou.forum.workflow.post.PostPage
import ai.saniou.forum.workflow.topic.components.SubForumList
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.forum.workflow.user.UserDetailPage
import ai.saniou.forum.workflow.user.UserPage
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Forum
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.*
import thread.feature_forum.generated.resources.topic_page_back
import thread.feature_forum.generated.resources.topic_page_interval
import thread.feature_forum.generated.resources.topic_page_menu
import thread.feature_forum.generated.resources.topic_page_post
import thread.feature_forum.generated.resources.topic_page_thread_count
import thread.feature_forum.generated.resources.topic_page_user_center

data class TopicPage(
    val forumId: String,
    val fgroupId: String,
    val sourceId: String? = null,
    val onMenuClick: (() -> Unit)? = null,
) : Screen {

    // Compatibility constructor for existing navigation calls (mostly NMB)
    constructor(forumId: Long, fgroupId: Long, onMenuClick: (() -> Unit)? = null) : this(
        sourceId = null,
        forumId = forumId.toString(),
        fgroupId = fgroupId.toString(),
        onMenuClick = onMenuClick
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val di = localDI()
        val navigator = LocalNavigator.currentOrThrow
        // Fallback to LocalSourceId if sourceId is not provided (legacy behavior)
        val actualSourceId = sourceId ?: LocalForumSourceId.current

        val viewModel: TopicViewModel =
            rememberScreenModel(tag = "${actualSourceId}_${fgroupId}_${forumId}") {
                di.direct.instance(arg = Triple(actualSourceId, forumId, fgroupId))
            }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val threads = viewModel.topics.collectAsLazyPagingItems()

        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var showQuickActionBar by remember { mutableStateOf(true) }
        val fabNestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -15f) {
                        showQuickActionBar = false
                    } else if (available.y > 15f) {
                        showQuickActionBar = true
                    }
                    return Offset.Zero
                }
            }
        }

        LaunchedEffect(lazyListState.firstVisibleItemIndex) {
            if (lazyListState.firstVisibleItemIndex == 0) {
                showQuickActionBar = true
            }
        }

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    TopicContract.Effect.ScrollToTop -> lazyListState.animateScrollToItem(0)
                }
            }
        }

        val canRefresh by remember {
            derivedStateOf {
                lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            }
        }

        val channelDetail = (state.channelDetail as? UiStateWrapper.Success)?.value
        ThreadDetailScaffold(
            title = state.channelName.ifBlank { "社区板块" },
            eyebrow = "FORUM CHANNEL",
            subtitle = channelDetail?.let { detail ->
                buildString {
                    detail.topicCount?.let { append("$it 个主题") }
                    detail.interval?.let {
                        if (isNotEmpty()) append(" · ")
                        append("$it 秒刷新间隔")
                    }
                }.ifBlank { "主题、规则与子版块" }
            } ?: "主题、规则与子版块",
            onBack = navigator::pop,
            modifier = Modifier.nestedScroll(fabNestedScrollConnection),
            navigationIcon = {
                if (onMenuClick != null) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(Res.string.topic_page_menu))
                    }
                } else {
                    IconButton(onClick = navigator::pop) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.topic_page_back))
                    }
                }
            },
            actions = {
                        IconButton(onClick = {
                            navigator.push(UserPage())
                        }) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = stringResource(Res.string.topic_page_user_center)
                            )
                        }
            },
            bottomBar = {
                ai.saniou.coreui.widgets.UnifiedActionBar(
                    visible = showQuickActionBar,
                    actions = buildList {
                        if (state.capabilities.supportsTopicCreation) {
                            add(
                                ai.saniou.coreui.widgets.ActionItem(
                                    label = stringResource(Res.string.topic_page_post),
                                    icon = Icons.Default.Add,
                                    emphasized = true,
                                    onClick = {
                                        navigator.push(
                                            PostPage(
                                                sourceId = actualSourceId,
                                                channelId = forumId,
                                                forumName = state.channelName
                                            )
                                        )
                                    }
                                )
                            )
                        }
                        add(
                            ai.saniou.coreui.widgets.ActionItem(
                                label = stringResource(Res.string.refresh),
                                icon = Icons.Default.Refresh,
                                onClick = {
                                    if (canRefresh) {
                                        threads.refresh()
                                    } else {
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(0)
                                        }
                                    }
                                }
                            )
                        )
                        add(
                            ai.saniou.coreui.widgets.ActionItem(
                                label = stringResource(Res.string.topic_page_user_center),
                                icon = Icons.Default.AccountCircle,
                                onClick = { navigator.push(UserPage()) }
                            )
                        )
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ListThreadPage(
                    state = lazyListState,
                    threadFlow = viewModel.topics,
                    onThreadClicked = { threadId -> navigator.push(TopicDetailPage(threadId)) },
                    onImageClick = { _, image ->
                        navigator.push(
                            ImagePreviewPage(
                                ImagePreviewViewModelParams(
                                    initialImages = listOf(image),
                                )
                            )
                        )
                    },
                    onUserClick = { userHash -> navigator.push(UserDetailPage(actualSourceId, userHash)) },
                    modifier = Modifier.weight(1f),
                    showChannelBadge = false,
                    onShowCache = { viewModel.onEvent(TopicContract.Event.ShowCache) },
                    headerContent = {
                        val detailState = state.channelDetail
                        if (detailState is ai.saniou.coreui.state.UiStateWrapper.Success) {
                            val detail = detailState.value
                            if (detail != null) {
                                ContextHero(
                                    icon = Icons.Default.Forum,
                                    title = state.channelName,
                                    subtitle = detail.description.replace(Regex("<[^>]*>"), "").trim()
                                        .ifBlank { "浏览最新主题并参与讨论" },
                                    metric = "${detail.topicCount ?: threads.itemCount} THREADS",
                                )

                                if (detail.children.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    SubForumList(
                                        subForums = detail.children,
                                        listViewStyle = detail.listViewStyle,
                                        onForumClick = { child ->
                                            navigator.push(
                                                TopicPage(
                                                    sourceId = actualSourceId,
                                                    forumId = child.id,
                                                    fgroupId = child.groupId
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
