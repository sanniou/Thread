package ai.saniou.forum.workflow.topic

import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.forum.di.nmbdi
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
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import org.kodein.di.DI
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
    val di: DI = nmbdi,
    val forumId: String,
    val fgroupId: String,
    val sourceId: String? = null,
    val onMenuClick: (() -> Unit)? = null,
) : Screen {

    // Compatibility constructor for existing navigation calls (mostly NMB)
    constructor(forumId: Long, fgroupId: Long, onMenuClick: (() -> Unit)? = null) : this(
        di = nmbdi,
        sourceId = null,
        forumId = forumId.toString(),
        fgroupId = fgroupId.toString(),
        onMenuClick = onMenuClick
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // Fallback to LocalSourceId if sourceId is not provided (legacy behavior)
        val actualSourceId = sourceId ?: LocalForumSourceId.current

        val viewModel: TopicViewModel =
            rememberScreenModel(tag = "${actualSourceId}_${fgroupId}_${forumId}") {
                di.direct.instance(arg = Triple(actualSourceId, forumId, fgroupId))
            }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val threads = viewModel.topics.collectAsLazyPagingItems()

        // Use LargeTopAppBar for better design
        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var expandedFab by remember { mutableStateOf(true) }
        val fabNestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -15f) {
                        expandedFab = false
                    } else if (available.y > 15f) {
                        expandedFab = true
                    }
                    return Offset.Zero
                }
            }
        }

        LaunchedEffect(lazyListState.firstVisibleItemIndex) {
            if (lazyListState.firstVisibleItemIndex == 0) {
                expandedFab = true
            }
        }

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    TopicContract.Effect.ScrollToTop -> lazyListState.animateScrollToItem(0)
                }
            }
        }

        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .nestedScroll(fabNestedScrollConnection),
            topBar = {
                ai.saniou.coreui.widgets.SaniouLargeTopAppBar(
                    title = {
                        Column {
                            // Title
                            Text(
                                text = state.channelName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            if (lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
                                                threads.refresh()
                                            } else {
                                                coroutineScope.launch {
                                                    lazyListState.animateScrollToItem(0)
                                                }
                                            }
                                        }
                                    )
                                }
                            )

                            // Animated visibility for extended content to prevent jumpiness
                            val showExtendedContent = scrollBehavior.state.collapsedFraction < 0.5f
                            val channelDetail = state.channelDetail

                            AnimatedVisibility(
                                visible = showExtendedContent && channelDetail is UiStateWrapper.Success,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                val detail = (channelDetail as? UiStateWrapper.Success)?.value
                                if (detail != null) {
                                    Column {
                                        // Rules Section (Collapsible in Title Area)
                                        if (detail.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            ForumRichText(
                                                text = detail.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 3, // Reduced max lines to stabilize height
                                                overflow = TextOverflow.Ellipsis,
                                                sourceId = actualSourceId,
                                                onThreadClick = { threadId ->
                                                    navigator.push(TopicDetailPage(threadId))
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        // Metadata Section
                                        val subtitle = buildString {
                                            if (detail.topicCount != null) append(
                                                stringResource(
                                                    Res.string.topic_page_thread_count,
                                                    detail.topicCount!!
                                                )
                                            )
                                            if (detail.interval != null) append(
                                                stringResource(
                                                    Res.string.topic_page_interval,
                                                    detail.interval!!
                                                )
                                            )
                                        }
                                        if (subtitle.isNotBlank()) {
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.7f
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        if (onMenuClick != null) {
                            IconButton(onClick = onMenuClick) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = stringResource(Res.string.topic_page_menu)
                                )
                            }
                        } else {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = stringResource(Res.string.topic_page_back)
                                )
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
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        navigator.push(
                            PostPage(
                                fid = forumId.toInt(),
                                forumName = state.channelName
                            )
                        )
                    },
                    expanded = expandedFab,
                    icon = { Icon(Icons.Default.Add, stringResource(Res.string.topic_page_post)) },
                    text = { Text(stringResource(Res.string.topic_page_post)) }
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
                    onUserClick = { userHash -> navigator.push(UserDetailPage(userHash)) },
                    modifier = Modifier.weight(1f),
                    showChannelBadge = false,
                    headerContent = {
                        // Integrated Forum Info
                        val detailState = state.channelDetail
                        if (detailState is ai.saniou.coreui.state.UiStateWrapper.Success) {
                            val detail = detailState.value
                            if (detail != null) {
                                // Forum Rules now moved to TopBar Action

                                // Render Sub-forums
                                if (detail.children.isNotEmpty()) {
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





