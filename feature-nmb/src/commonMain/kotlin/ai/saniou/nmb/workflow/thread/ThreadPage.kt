package ai.saniou.nmb.workflow.thread

import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.coreui.widgets.ShimmerContainer
import ai.saniou.coreui.widgets.VerticalSpacerSmall
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.ThreadReply
import androidx.compose.material.icons.filled.EmojiEmotions
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.LoadEndIndicator
import ai.saniou.nmb.ui.components.LoadingFailedIndicator
import ai.saniou.nmb.ui.components.LoadingIndicator
import ai.saniou.nmb.ui.components.PageJumpDialog
import ai.saniou.nmb.ui.components.ReferencePopup
import ai.saniou.nmb.ui.components.SkeletonReplyItem
import ai.saniou.nmb.ui.components.ThreadAuthor
import ai.saniou.nmb.ui.components.ThreadBody
import ai.saniou.nmb.workflow.image.ImageInfo
import ai.saniou.nmb.workflow.image.ImagePreviewPage
import ai.saniou.nmb.workflow.image.ImagePreviewViewModelParams
import ai.saniou.nmb.workflow.image.ThreadImageProvider
import ai.saniou.nmb.workflow.post.PostPage
import ai.saniou.nmb.workflow.reference.ReferenceContract
import ai.saniou.nmb.workflow.reference.ReferenceViewModel
import ai.saniou.nmb.workflow.thread.ThreadContract.Effect
import ai.saniou.nmb.workflow.thread.ThreadContract.Event
import ai.saniou.nmb.workflow.thread.ThreadContract.State
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Reply
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
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
                        // This navigation is now handled with parameters, not from a shared state
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
                                                if (lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
                                                    viewModel.onEvent(Event.Refresh)
                                                } else {
                                                    lazyListState.animateScrollToItem(0)
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
                onImageClick = { initialIndex, images ->
                    navigator.push(
                        ImagePreviewPage(
                            ImagePreviewViewModelParams(
                                imageProvider = ThreadImageProvider(
                                    threadId = threadId,
                                    getThreadImagesUseCase = nmbdi.direct.instance()
                                ),
                                initialImages = images,
                                initialIndex = initialIndex
                            )
                        )
                    )
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
    onImageClick: (Int, List<ImageInfo>) -> Unit,
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 主帖骨架屏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
            ) {
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

            // 回复骨架屏
            repeat(3) {
                SkeletonReplyItem(brush)
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
    onImageClick: (Int, List<ImageInfo>) -> Unit,
    onUpdateLastReadId: (Long) -> Unit,
    onCopy: (String) -> Unit,
    onBookmarkThread: (Thread) -> Unit,
    onBookmarkReply: (ThreadReply) -> Unit,
) {
    val replies = state.replies.collectAsLazyPagingItems()
    val allImages by remember(state.thread, replies.itemSnapshotList) {
        derivedStateOf {
            val imageList = mutableListOf<ImageInfo>()
            state.thread?.let {
                if (it.img.isNotBlank()) {
                    imageList.add(ImageInfo(it.img, it.ext))
                }
            }
            replies.itemSnapshotList.items.forEach { reply ->
                if (reply.img.isNotBlank()) {
                    imageList.add(ImageInfo(reply.img, reply.ext))
                }
            }
            imageList
        }
    }

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
            onImageClick = { imgPath, _ ->
                val initialIndex = allImages.indexOfFirst { it.imgPath == imgPath }
                    .coerceAtLeast(0)
                onImageClick(initialIndex, allImages)

            },
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
        modifier = Modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
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
            state.thread?.let { thread ->
                ThreadMainPost(
                    thread = thread,
                    refClick = onRefClick,
                    onImageClick = onImageClick,
                    onCopy = { onCopy(thread.content) },
                    onBookmark = { onBookmarkThread(thread) }
                )
                HorizontalDivider(
                    thickness = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // 工具栏
        stickyHeader {
            state.thread?.let {
                Surface(
                    modifier = Modifier.fillParentMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column {
                        ThreadToolbar(
                            replyCount = it.replyCount.toString(),
                            isPoOnly = state.isPoOnlyMode,
                            onTogglePoOnly = onTogglePoOnly
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    }
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
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadToolbar(
    replyCount: String,
    isPoOnly: Boolean,
    onTogglePoOnly: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "全部回复",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = " · $replyCount",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FilterChip(
            selected = isPoOnly,
            onClick = onTogglePoOnly,
            label = { Text("只看PO") },
            leadingIcon = if (isPoOnly) {
                {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            border = null
        )
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
    var showMenu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 16.dp, bottom = 24.dp),
    ) {
        // 标题
        if (thread.title.isNotBlank() && thread.title != "无标题") {
            Text(
                text = thread.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // 头部信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ThreadAuthor(thread, isPo = true)

            Text(
                text = "No.${thread.id}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 正文
        Box(
            modifier = Modifier.fillMaxWidth()
                .combinedClickable(
                    onClick = { /* No-op, allow inner clicks */ },
                    onLongClick = { showMenu = true }
                )
                .padding(horizontal = 16.dp)
        ) {
            ThreadBody(thread, onReferenceClick = refClick, onImageClick = onImageClick)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 底部操作栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 复制
            androidx.compose.material3.IconButton(
                onClick = { onCopy() },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "复制",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 收藏
            androidx.compose.material3.IconButton(
                onClick = { onBookmark() },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.BookmarkBorder,
                    contentDescription = "收藏",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 更多 (保留给长按菜单的显式入口，如果用户不知道长按)
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("复制内容") },
                leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                onClick = {
                    onCopy()
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("收藏串") },
                leadingIcon = { Icon(Icons.Filled.BookmarkBorder, null) },
                onClick = {
                    onBookmark()
                    showMenu = false
                }
            )
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
    var showMenu by remember { mutableStateOf(false) }

    // Haptic feedback
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onReplyClicked(reply.id) },
                onLongClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    showMenu = true
                }
            )
            .padding(16.dp, 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThreadAuthor(reply, isPo = isPo)

                Text(
                    text = "No.${reply.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("回复") },
                    leadingIcon = { Icon(Icons.Filled.Reply, null) },
                    onClick = {
                        onReplyClicked(reply.id)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("复制内容") },
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                    onClick = {
                        onCopy()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("收藏回复") },
                    leadingIcon = { Icon(Icons.Filled.BookmarkBorder, null) },
                    onClick = {
                        onBookmark()
                        showMenu = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ThreadBody(reply, onReferenceClick = refClick, onImageClick = onImageClick)
        }
    }
}
