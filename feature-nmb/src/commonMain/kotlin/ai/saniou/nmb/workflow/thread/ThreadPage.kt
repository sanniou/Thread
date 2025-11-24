package ai.saniou.nmb.workflow.thread

import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.coreui.widgets.ShimmerContainer
import ai.saniou.coreui.widgets.VerticalSpacerSmall
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.LoadEndIndicator
import ai.saniou.nmb.ui.components.LoadingFailedIndicator
import ai.saniou.nmb.ui.components.LoadingIndicator
import ai.saniou.nmb.ui.components.PageJumpDialog
import ai.saniou.nmb.ui.components.ReferencePopup
import ai.saniou.nmb.ui.components.SkeletonReplyItem
import ai.saniou.nmb.ui.components.ThreadAuthor
import ai.saniou.nmb.ui.components.ThreadBody
import ai.saniou.nmb.workflow.image.ImagePreviewPage
import ai.saniou.nmb.workflow.post.PostPage
import ai.saniou.nmb.workflow.reference.ReferenceContract
import ai.saniou.nmb.workflow.reference.ReferenceViewModel
import ai.saniou.nmb.workflow.thread.ThreadContract.Effect
import ai.saniou.nmb.workflow.thread.ThreadContract.Event
import ai.saniou.nmb.workflow.thread.ThreadContract.State
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cash.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import org.kodein.di.direct
import org.kodein.di.instance

data class ThreadPage(
    val threadId: Long,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val clipboardManager = LocalClipboardManager.current

        val viewModel: ThreadViewModel = rememberScreenModel(tag = threadId.toString()) {
            nmbdi.direct.instance(arg = threadId)
        }
        val state by viewModel.state.collectAsState()

        var showJumpDialog by remember { mutableStateOf(false) }
        var showMenu by remember { mutableStateOf(false) }

        // 引用弹窗状态
        val referenceViewModel: ReferenceViewModel = rememberScreenModel()
        var showReferencePopup by remember { mutableStateOf(false) }
        var currentReferenceId by remember { mutableStateOf(0L) }
        val referenceState by referenceViewModel.uiState.collectAsState()

        // Scroll Behavior for TopAppBar
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        // 处理副作用
        // 处理副作用
        LaunchedEffect(Unit) {
            var snackbarJob: Job? = null
            viewModel.effect.collect { effect ->
                when (effect) {
                    is Effect.ShowSnackbar -> {
                        snackbarJob?.cancel()
                        snackbarJob = coroutineScope.launch {
                            snackbarHostState.showSnackbar(effect.message)
                        }
                    }

                    is Effect.CopyToClipboard -> {
                        clipboardManager.setText(AnnotatedString(effect.text))
                    }

                    is Effect.NavigateToImagePreview -> {
                        navigator.push(
                            ImagePreviewPage(
                                uiState = state.imagePreviewState,
                                onLoadMore = { viewModel.onEvent(Event.LoadMoreImages) }
                            )
                        )
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {
                        if (state.thread != null) {
                            Text(
                                text = state.forumName,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            coroutineScope.launch {
                                                if (lazyListState.firstVisibleItemIndex > 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
                                                    lazyListState.animateScrollToItem(0)
                                                } else {
                                                    viewModel.onEvent(Event.Refresh)
                                                }
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        if (state.thread != null) {
                            IconButton(onClick = { viewModel.onEvent(Event.Refresh) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "刷新")
                            }
                            IconButton(
                                onClick = { viewModel.onEvent(Event.ToggleSubscription) },
                                enabled = !state.isTogglingSubscription
                            ) {
                                if (state.isTogglingSubscription) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(
                                        imageVector = if (state.isSubscribed) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "收藏"
                                    )
                                }
                            }
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("跳页") },
                                    onClick = {
                                        showJumpDialog = true
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("复制链接") },
                                    onClick = {
                                        viewModel.onEvent(Event.CopyLink)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (state.thread != null) {
                    FloatingActionButton(
                        onClick = {
                            state.thread?.let {
                                navigator.push(PostPage(resto = it.id.toInt()))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "回复")
                    }
                }
            }
        ) { innerPadding ->
            ThreadContentRouter(
                modifier = Modifier.padding(innerPadding),
                state = state,
                lazyListState = lazyListState,
                onRefresh = { viewModel.onEvent(Event.Refresh) },
                onTogglePoOnly = { viewModel.onEvent(Event.TogglePoOnlyMode) },
                onRefClick = { refId ->
                    currentReferenceId = refId
                    referenceViewModel.onEvent(
                        ReferenceContract.Event.GetReference(
                            refId
                        )
                    )
                    showReferencePopup = true
                },
                onImageClick = { imgPath, _ ->
                    viewModel.onEvent(Event.ShowImagePreview(imgPath))
                },
                onUpdateLastReadId = { id -> viewModel.onEvent(Event.UpdateLastReadReplyId(id)) },
                onCopy = { viewModel.onEvent(Event.CopyContent(it)) },
                onBookmarkThread = { viewModel.onEvent(Event.BookmarkThread(it)) },
                onBookmarkReply = { viewModel.onEvent(Event.BookmarkReply(it)) }
            )
        }

        // 跳页对话框
        if (showJumpDialog) {
            PageJumpDialog(
                currentPage = 1,
                totalPages = state.totalPages,
                onDismissRequest = { showJumpDialog = false },
                onJumpToPage = { page -> viewModel.onEvent(Event.JumpToPage(page)) }
            )
        }

        // 引用弹窗
        if (showReferencePopup) {
            ReferencePopup(
                refId = currentReferenceId,
                reply = referenceState.reply,
                isLoading = referenceState.isLoading,
                error = referenceState.error,
                onDismiss = {
                    showReferencePopup = false
                    referenceViewModel.onEvent(ReferenceContract.Event.Clear)
                }
            )
        }
    }
}

// =================================================================================
// region UI Content Routers
// =================================================================================

@Composable
private fun ThreadContentRouter(
    modifier: Modifier = Modifier,
    state: State,
    lazyListState: LazyListState,
    onRefresh: () -> Unit,
    onTogglePoOnly: () -> Unit,
    onRefClick: (Long) -> Unit,
    onImageClick: (String, String) -> Unit,
    onUpdateLastReadId: (Long) -> Unit,
    onCopy: (String) -> Unit,
    onBookmarkThread: (Thread) -> Unit,
    onBookmarkReply: (ThreadReply) -> Unit,
) {
    Box(modifier = modifier) {
        when {
            state.isLoading && state.thread == null -> ThreadShimmer()
            state.error != null -> ThreadErrorContent(
                error = state.error,
                onRetry = onRefresh
            )

            state.thread != null -> ThreadSuccessContent(
                state = state,
                lazyListState = lazyListState,
                onRefresh = { /* Handled by PullToRefresh */ },
                onReplyClicked = { /* TODO */ },
                onTogglePoOnly = onTogglePoOnly,
                onRefClick = onRefClick,
                onImageClick = onImageClick,
                onUpdateLastReadId = onUpdateLastReadId,
                onCopy = onCopy,
                onBookmarkThread = onBookmarkThread,
                onBookmarkReply = onBookmarkReply
            )
        }
    }
}

@Composable
private fun ThreadErrorContent(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

// endregion

// =================================================================================
// region Shimmer / Loading States
// =================================================================================

@Composable
private fun ThreadShimmer() {
    ShimmerContainer { brush ->
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // 主帖骨架屏
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 标题和作者信息
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(brush)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Box(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(brush)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(brush)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 内容
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )

                    VerticalSpacerSmall()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )

                    VerticalSpacerSmall()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 图片占位
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 回复骨架屏
            repeat(3) {
                SkeletonReplyItem(brush)
                VerticalSpacerSmall()
            }
        }
    }
}

// endregion

// =================================================================================
// region Success State UI Components
// =================================================================================

@OptIn(FlowPreview::class)
@Composable
fun ThreadSuccessContent(
    state: State,
    lazyListState: LazyListState,
    onRefresh: () -> Unit,
    onReplyClicked: (Long) -> Unit,
    onTogglePoOnly: () -> Unit,
    onRefClick: (Long) -> Unit,
    onImageClick: (String, String) -> Unit,
    onUpdateLastReadId: (Long) -> Unit,
    onCopy: (String) -> Unit,
    onBookmarkThread: (Thread) -> Unit,
    onBookmarkReply: (ThreadReply) -> Unit,
) {
    val replies = state.replies.collectAsLazyPagingItems()

    // Auto-scroll to last read position
    var hasScrolledToLastRead by remember { mutableStateOf(false) }

    LaunchedEffect(replies.itemCount, state.lastReadReplyId) {
        if (!hasScrolledToLastRead && state.lastReadReplyId > 0L) {
            val lastReadItemIndex =
                replies.itemSnapshotList.indexOfFirst { it?.id == state.lastReadReplyId }

            if (lastReadItemIndex != -1) {
                // 主题帖占用了第一个位置，所以回复的索引需要+1
                lazyListState.scrollToItem(lastReadItemIndex + 1)
                hasScrolledToLastRead = true
            }
        }
    }

    // Update last read position
    LaunchedEffect(lazyListState, replies.itemCount) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .debounce(500L) // 防抖，避免滚动时频繁更新
            .collect { index ->
                if (replies.itemCount > 0 && index > 0) {
                    val replyIndex = index - 1
                    if (replyIndex < replies.itemCount) {
                        replies[replyIndex]?.let { reply ->
                            onUpdateLastReadId(reply.id)
                        }
                    }
                }
            }
    }

    PullToRefreshWrapper(onRefreshTrigger = { replies.refresh() }) {
        ThreadList(
            state = state,
            lazyListState = lazyListState,
            onReplyClicked = onReplyClicked,
            onTogglePoOnly = onTogglePoOnly,
            onRefClick = onRefClick,
            onImageClick = onImageClick,
            onRefresh = onRefresh,
            onCopy = onCopy,
            onBookmarkThread = onBookmarkThread,
            onBookmarkReply = onBookmarkReply
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThreadList(
    state: State,
    lazyListState: LazyListState,
    onReplyClicked: (Long) -> Unit,
    onTogglePoOnly: () -> Unit,
    onRefClick: (Long) -> Unit,
    onImageClick: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onCopy: (String) -> Unit,
    onBookmarkThread: (Thread) -> Unit,
    onBookmarkReply: (ThreadReply) -> Unit,
) {
    val replies = state.replies.collectAsLazyPagingItems()
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PREPEND 加载状态
        if (replies.loadState.prepend is LoadStateLoading) {
            item {
                LoadingIndicator()
            }
        }
        if (replies.loadState.prepend is LoadStateError) {
            item {
                LoadingFailedIndicator()
            }
        }

        // 主帖
        item {
            state.thread?.let {
                ThreadMainPost(
                    thread = it,
                    refClick = onRefClick,
                    onImageClick = onImageClick,
                    onCopy = { onCopy(it.content) },
                    onBookmark = { onBookmarkThread(it) }
                )
            }
        }

        // 工具栏
        stickyHeader {
            state.thread?.let {
                Surface(
                    modifier = Modifier.fillParentMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    ThreadToolbar(
                        replyCount = it.replyCount.toString(),
                        isPoOnly = state.isPoOnlyMode,
                        onTogglePoOnly = onTogglePoOnly
                    )
                }
            }
        }

        // 回复列表
        items(replies.itemCount) { replyIndex ->
            replies[replyIndex]?.let { reply ->
                ThreadReply(
                    reply = reply,
                    poUserHash = state.thread?.userHash ?: "",
                    onReplyClicked = onReplyClicked,
                    refClick = onRefClick,
                    onImageClick = onImageClick,
                    onCopy = { onCopy(reply.content) },
                    onBookmark = { onBookmarkReply(reply) }
                )
            } ?: ShimmerContainer { SkeletonReplyItem(it) }
        }

        // Paging 加载状态
        item {
            when {
                replies.loadState.refresh is LoadStateLoading && replies.itemCount == 0 -> ThreadShimmer()
                replies.loadState.refresh is LoadStateError -> {
                    // 错误状态已在顶层处理
                }

                replies.loadState.append is LoadStateError -> LoadingFailedIndicator()
                replies.loadState.append is LoadStateLoading -> LoadingIndicator()
                replies.loadState.append.endOfPaginationReached && replies.itemCount == 0 -> {
                    EmptyReplyContent(onRefresh)
                }

                replies.loadState.append.endOfPaginationReached -> LoadEndIndicator(
                    onClick = { replies.refresh() }
                )
            }
        }
    }
}

@Composable
private fun EmptyReplyContent(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "暂无回复",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "成为第一个回复的人吧！",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRefresh) {
                Text("刷新")
            }
        }
    }
}

@Composable
private fun ThreadToolbar(
    replyCount: String,
    isPoOnly: Boolean,
    onTogglePoOnly: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "回复: $replyCount",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FilledIconToggleButton(
            checked = isPoOnly,
            onCheckedChange = { onTogglePoOnly() }
        ) {
            Icon(Icons.Default.Person, contentDescription = "只看PO")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PostWrapper(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onBookmark: () -> Unit,
    isElevated: Boolean = false,
    content: @Composable () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        val cardModifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
        if (isElevated) {
            ElevatedCard(modifier = cardModifier) { content() }
        } else {
            Card(modifier = cardModifier) { content() }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("复制") },
                onClick = {
                    onCopy()
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("收藏") },
                onClick = {
                    onBookmark()
                    showMenu = false
                }
            )
        }
    }
}

@Composable
fun ThreadMainPost(
    thread: Thread,
    refClick: (Long) -> Unit,
    onImageClick: (String, String) -> Unit,
    onCopy: () -> Unit,
    onBookmark: () -> Unit,
) {
    PostWrapper(
        onClick = { /* 主楼不可点击 */ },
        onCopy = onCopy,
        onBookmark = onBookmark,
        isElevated = true
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // 头部信息
            PostHeader(
                author = { ThreadAuthor(thread, isPo = true) },
                id = thread.id
            )

            // 标题
            if (thread.title.isNotBlank() && thread.title != "无标题") {
                Text(
                    text = thread.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 正文
            ThreadBody(thread, onReferenceClick = refClick, onImageClick = onImageClick)
        }
    }
}

@Composable
fun ThreadReply(
    reply: ThreadReply,
    poUserHash: String,
    onReplyClicked: (Long) -> Unit,
    refClick: (Long) -> Unit,
    onImageClick: (String, String) -> Unit,
    onCopy: () -> Unit,
    onBookmark: () -> Unit,
) {
    val isPo = remember(reply.userHash) {
        reply.userHash == poUserHash
    }

    PostWrapper(
        onClick = { onReplyClicked(reply.id) },
        onCopy = onCopy,
        onBookmark = onBookmark
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            PostHeader(
                author = { ThreadAuthor(reply, isPo = isPo) },
                id = reply.id
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThreadBody(reply, onReferenceClick = refClick, onImageClick = onImageClick)
        }
    }
}

@Composable
private fun PostHeader(
    author: @Composable () -> Unit,
    id: Long,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.weight(1f)) {
            author()
        }
        Text(
            text = "No.$id",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// endregion

// endregion
