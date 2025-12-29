package ai.saniou.forum.workflow.forum

import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.coreui.widgets.RichText
import ai.saniou.forum.di.nmbdi
import ai.saniou.forum.workflow.home.ListThreadPage
import ai.saniou.forum.workflow.home.StylizedForumItem
import ai.saniou.forum.workflow.home.SubCategoryBoxItem
import ai.saniou.forum.workflow.image.ImagePreviewPage
import ai.saniou.forum.workflow.image.ImagePreviewViewModelParams
import ai.saniou.forum.workflow.post.PostPage
import ai.saniou.forum.workflow.thread.ThreadPage
import ai.saniou.forum.workflow.user.UserDetailPage
import ai.saniou.forum.workflow.user.UserPage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

data class ForumPage(
    val di: DI = nmbdi,
    val sourceId: String? = null, // Optional for backward compatibility, but should be provided
    val forumId: String,
    val fgroupId: String,
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

        val viewModel: ForumViewModel = rememberScreenModel(tag = "${actualSourceId}_${fgroupId}_${forumId}") {
            di.direct.instance(arg = Triple(actualSourceId, forumId, fgroupId))
        }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val threads = viewModel.threads.collectAsLazyPagingItems()

        // Use LargeTopAppBar for better design
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var expandedFab by remember { mutableStateOf(true) }
        val fabNestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -5f) {
                        expandedFab = false
                    } else if (available.y > 5f) {
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
                    ForumContract.Effect.ScrollToTop -> lazyListState.animateScrollToItem(0)
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
                            Text(
                                text = state.forumName,
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
                            // Show subtitle only when expanded
                            if (scrollBehavior.state.collapsedFraction < 0.5f && state.forumDetail != null) {
                                val detail = state.forumDetail!!
                                val subtitle = buildString {
                                    if (detail.topicCount != null) append("${detail.topicCount} 串")
                                    if (detail.interval != null) append(" · ${detail.interval}s")
                                }
                                if (subtitle.isNotBlank()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        if (onMenuClick != null) {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = "菜单")
                            }
                        } else {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            navigator.push(UserPage())
                        }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "用户中心")
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
                                forumName = state.forumName
                            )
                        )
                    },
                    expanded = expandedFab,
                    icon = { Icon(Icons.Default.Add, "发帖") },
                    text = { Text("发帖") }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                // Integrated Forum Info (Collapsible/Expandable could be better, but static for now is cleaner than dialog)
                state.forumDetail?.let { detail ->
                    if (detail.description.isNotBlank()) {
                        ForumRulesCard(msg = detail.description)
                    }

                    // Render Sub-forums
                    if (detail.children.isNotEmpty()) {
                        if (detail.listViewStyle == "boxes") {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                detail.children.forEach { child ->
                                    SubCategoryBoxItem(
                                        forum = child,
                                        onClick = {
                                            navigator.push(
                                                ForumPage(
                                                    sourceId = actualSourceId,
                                                    forumId = child.id,
                                                    fgroupId = child.groupId
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        } else {
                            // Default list style
                            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                                detail.children.forEach { child ->
                                    StylizedForumItem(
                                        forum = child,
                                        isSelected = false,
                                        isFavorite = false, // TODO: Check favorite status
                                        onForumClick = {
                                            navigator.push(
                                                ForumPage(
                                                    sourceId = actualSourceId,
                                                    forumId = child.id,
                                                    fgroupId = child.groupId
                                                )
                                            )
                                        },
                                        onFavoriteToggle = {
                                            // TODO: Implement favorite toggle
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                ListThreadPage(
                    state = lazyListState,
                    threadFlow = viewModel.threads,
                    onThreadClicked = { threadId -> navigator.push(ThreadPage(threadId)) },
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
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    private fun ForumRulesCard(msg: String) {
        var expanded by remember { mutableStateOf(false) }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                RichText(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!expanded && msg.length > 50) { // Simple heuristic
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        modifier = Modifier.align(Alignment.CenterHorizontally).size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}





