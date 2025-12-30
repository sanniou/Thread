package ai.saniou.forum.workflow.topicdetail

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.coreui.state.AppError
import ai.saniou.coreui.state.DefaultError
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.coreui.widgets.ShimmerContainer
import ai.saniou.coreui.widgets.VerticalSpacerSmall
import ai.saniou.forum.di.nmbdi
import ai.saniou.forum.ui.components.Badge
import ai.saniou.forum.ui.components.LoadEndIndicator
import ai.saniou.forum.ui.components.LoadingFailedIndicator
import ai.saniou.forum.ui.components.LoadingIndicator
import ai.saniou.forum.ui.components.PageJumpDialog
import ai.saniou.forum.ui.components.ReferenceSheet
import ai.saniou.forum.ui.components.SkeletonReplyItem
import ai.saniou.forum.ui.components.ThreadAuthor
import ai.saniou.forum.ui.components.ThreadBody
import ai.saniou.forum.workflow.image.ImagePreviewPage
import ai.saniou.forum.workflow.image.ImagePreviewViewModelParams
import ai.saniou.forum.workflow.image.ThreadImageProvider
import ai.saniou.forum.workflow.post.PostPage
import ai.saniou.forum.workflow.reference.ReferenceContract
import ai.saniou.forum.workflow.reference.ReferenceViewModel
import ai.saniou.forum.workflow.topicdetail.TopicDetailContract.Effect
import ai.saniou.forum.workflow.topicdetail.TopicDetailContract.Event
import ai.saniou.forum.workflow.topicdetail.TopicDetailContract.State
import ai.saniou.forum.workflow.user.UserDetailPage
import ai.saniou.thread.domain.model.forum.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.unit.sp
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
import ai.saniou.thread.domain.model.forum.Comment as ThreadReply
import ai.saniou.thread.domain.model.forum.Topic as Post

data class TopicDetailPage(
    val threadId: String,
) : Screen {

    // Secondary constructor for compatibility
    constructor(threadId: Long) : this(threadId.toString())

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val clipboardManager = LocalClipboardManager.current
        val sourceId = LocalForumSourceId.current

        val viewModel: TopicDetailViewModel = rememberScreenModel(tag = "$sourceId:$threadId") {
            nmbdi.direct.instance(arg = TopicDetailViewModelParams(sourceId, threadId))
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
                SaniouTopAppBar(
                    title = {
                        if (state.topic != null) {
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
                    onNavigationClick = { navigator.pop() },
                    actions = {
                        if (state.topic != null) {
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
                if (state.topic != null) {
                    FloatingActionButton(
                        onClick = {
                            state.topic?.let {
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
                                    threadId = threadId.toLongOrNull() ?: 0L,
                                    getTopicImagesUseCase = nmbdi.direct.instance()
                                ),
                                initialImages = images,
                                initialIndex = initialIndex
                            )
                        )
                    )
                },
                onUpdateLastReadId = { id -> viewModel.onEvent(Event.UpdateLastReadReplyId(id)) },
                onCopy = { viewModel.onEvent(Event.CopyContent(it)) },
                onBookmarkThread = { viewModel.onEvent(Event.BookmarkTopic(it)) },
                onBookmarkReply = { viewModel.onEvent(Event.BookmarkReply(it)) },
                onBookmarkImage = { image -> viewModel.onEvent(Event.BookmarkImage(image)) },
                onUserClick = { userHash -> navigator.push(UserDetailPage(userHash)) }
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
            ReferenceSheet(
                refId = currentReferenceId,
                reply = referenceState.reply,
                isLoading = referenceState.isLoading,
                error = referenceState.error,
                onDismiss = {
                    showReferencePopup = false
                    referenceViewModel.onEvent(ReferenceContract.Event.Clear)
                },
                onJumpToThread = { threadId ->
                    showReferencePopup = false
                    referenceViewModel.onEvent(ReferenceContract.Event.Clear)
                    navigator.push(TopicDetailPage(threadId = threadId))
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
    onImageClick: (Int, List<Image>) -> Unit,
    onUpdateLastReadId: (String) -> Unit,
    onCopy: (String) -> Unit,
    onBookmarkThread: (Post) -> Unit,
    onBookmarkReply: (ThreadReply) -> Unit,
    onBookmarkImage: (Image) -> Unit,
    onUserClick: (String) -> Unit,
) {
    Box(modifier = modifier) {
        if (state.error != null) {
            ThreadErrorContent(
                error = state.error,
                onRetry = onRefresh
            )
        } else {
            ThreadSuccessContent(
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
                onBookmarkReply = onBookmarkReply,
                onBookmarkImage = onBookmarkImage,
                onUserClick = onUserClick
            )
        }
    }
}

@Composable
private fun ThreadErrorContent(error: AppError, onRetry: () -> Unit) {
    DefaultError(error = error, onRetryClick = onRetry)
}

// endregion

// =================================================================================
// region Shimmer / Loading States
// =================================================================================

@Composable
private fun ThreadShimmer() {
    ShimmerContainer { brush ->
        Column(
            modifier = Modifier.padding(Dimens.padding_standard),
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
        ) {
            // 主帖骨架屏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(Dimens.corner_radius_large)
                    )
                    .padding(Dimens.padding_standard),
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

@Composable
private fun ThreadReplyShimmer() {
    ShimmerContainer { brush ->
        Column(
            modifier = Modifier.padding(Dimens.padding_standard),
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
        ) {
            repeat(5) {
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
    onReplyClicked: (String) -> Unit,
    onTogglePoOnly: () -> Unit,
    onRefClick: (Long) -> Unit,
    onImageClick: (Int, List<Image>) -> Unit,
    onUpdateLastReadId: (String) -> Unit,
    onCopy: (String) -> Unit,
    onBookmarkThread: (Post) -> Unit,
    onBookmarkReply: (ThreadReply) -> Unit,
    onBookmarkImage: (Image) -> Unit,
    onUserClick: (String) -> Unit,
) {
    val replies = state.replies.collectAsLazyPagingItems()
    val allImages by remember(state.topic, replies.itemSnapshotList) {
        derivedStateOf {
            val imageList = mutableListOf<Image>()
            state.topic?.let { post ->
                post.images.forEach { image ->
                    imageList.add(image)
                }
            }
            replies.itemSnapshotList.items.forEach { reply ->
                reply.images.forEach { image ->
                    imageList.add(image)
                }
            }
            imageList
        }
    }

    // Auto-scroll to last read position
    var hasScrolledToLastRead by remember { mutableStateOf(false) }

    LaunchedEffect(replies.itemCount, state.lastReadCommentId) {
        if (!hasScrolledToLastRead && state.lastReadCommentId.isNullOrBlank().not()) {
            val lastReadItemIndex =
                replies.itemSnapshotList.indexOfFirst { it?.id == state.lastReadCommentId }

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
            onImageClick = { image ->
                val initialIndex = allImages.indexOfFirst { it == image }
                    .coerceAtLeast(0)
                onImageClick(initialIndex, allImages)
            },
            onRefresh = onRefresh,
            onCopy = onCopy,
            onBookmarkThread = onBookmarkThread,
            onBookmarkReply = onBookmarkReply,
            onBookmarkImage = onBookmarkImage,
            onUserClick = onUserClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThreadList(
    state: State,
    lazyListState: LazyListState,
    onReplyClicked: (String) -> Unit,
    onTogglePoOnly: () -> Unit,
    onRefClick: (Long) -> Unit,
    onImageClick: (Image) -> Unit,
    onRefresh: () -> Unit,
    onCopy: (String) -> Unit,
    onBookmarkThread: (Post) -> Unit,
    onBookmarkReply: (ThreadReply) -> Unit,
    onBookmarkImage: (Image) -> Unit,
    onUserClick: (String) -> Unit,
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
            state.topic?.let { thread ->
                ThreadMainPost(
                    thread = thread,
                    refClick = onRefClick,
                    onImageClick = onImageClick,
                    onCopy = { onCopy(thread.content) },
                    onBookmark = { onBookmarkThread(thread) },
                    onBookmarkImage = onBookmarkImage,
                    onUserClick = onUserClick
                )
                // 使用 Spacer 代替 Divider，增加呼吸感
                Spacer(modifier = Modifier.height(Dimens.padding_small))
            }
        }

        // 工具栏
        stickyHeader {
            state.topic?.let {
                ThreadToolbar(
                    replyCount = it.commentCount.toString(),
                    isPoOnly = state.isPoOnlyMode,
                    onTogglePoOnly = onTogglePoOnly
                )
            }
        }

        // 回复列表
        items(replies.itemCount) { replyIndex ->
            replies[replyIndex]?.let { reply ->
                ThreadReply(
                    reply = reply,
                    poUserHash = state.topic?.author?.id ?: "",
                    onReplyClicked = onReplyClicked,
                    refClick = onRefClick,
                    onImageClick = onImageClick,
                    onCopy = { onCopy(reply.content) },
                    onBookmark = { onBookmarkReply(reply) },
                    onBookmarkImage = onBookmarkImage,
                    onUserClick = onUserClick
                )
                // 移除分割线，使用间距区分
                Spacer(modifier = Modifier.height(Dimens.padding_small))
            } ?: ShimmerContainer { SkeletonReplyItem(it) }
        }

        // Paging 加载状态
        item {
            when {
                replies.loadState.refresh is LoadStateLoading && replies.itemCount == 0 -> {
                    if (state.topic == null) {
                        ThreadShimmer()
                    } else {
                        ThreadReplyShimmer()
                    }
                }

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
            .padding(vertical = 64.dp), // Keep enough space
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_standard)
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
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), // 增加一点透明度
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.padding_standard, vertical = Dimens.padding_medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "全部回复",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " $replyCount",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                FilterChip(
                    selected = isPoOnly,
                    onClick = onTogglePoOnly,
                    label = { Text("只看PO", style = MaterialTheme.typography.labelMedium) },
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
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = null,
                    shape = CircleShape,
                    modifier = Modifier.height(32.dp)
                )
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun ThreadMainPost(
    thread: Post,
    refClick: (Long) -> Unit,
    onImageClick: (Image) -> Unit,
    onCopy: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarkImage: (Image) -> Unit,
    onUserClick: (String) -> Unit,
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.padding_standard),
        ) {
            // 头部信息
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.padding_standard),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThreadAuthor(
                    author = thread.author,
                    threadTime = thread.createdAt.toRelativeTimeString(),
                    isPo = true,
                    onClick = onUserClick,
                    badges = {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)) {
                            if (thread.sourceName.isNotBlank()) {
                                Badge(
                                    text = thread.sourceName.uppercase(),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            if (thread.isAdmin) {
                                Badge(
                                    text = "ADMIN",
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            if (thread.isSage) {
                                Badge(
                                    text = "SAGE",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // 楼层号移到这里，使用更淡的颜色
                Text(
                    text = "#${thread.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = Dimens.padding_small, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.padding_large))

            // 标题
            if (!thread.title.isNullOrBlank() && thread.title != "无标题") {
                Text(
                    text = thread.title!!,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        bottom = Dimens.padding_medium,
                        start = Dimens.padding_standard,
                        end = Dimens.padding_standard
                    )
                )
            }

            // 正文
            Box(
                modifier = Modifier.fillMaxWidth()
                    .combinedClickable(
                        onClick = { /* No-op, allow inner clicks */ },
                        onLongClick = { showMenu = true }
                    )
                    .padding(horizontal = Dimens.padding_standard)
            ) {
                ThreadBody(
                    content = thread.content,
                    images = thread.images,
                    onReferenceClick = refClick,
                    onImageClick = onImageClick,
                    onImageLongClick = { image -> onBookmarkImage(image) }
                )
            }

            Spacer(modifier = Modifier.height(Dimens.padding_large))

            // 底部操作栏 - 优化版
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.padding_standard),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 使用 AssistChip 风格的按钮，或者纯图标但增加间距
                IconButton(onClick = { onCopy() }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { onBookmark() }) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "收藏",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { showMenu = true }) {
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
                if (thread.sourceUrl.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("打开原链接") },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, null) },
                        onClick = {
                            uriHandler.openUri(thread.sourceUrl)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ThreadReply(
    reply: ThreadReply,
    poUserHash: String,
    onReplyClicked: (String) -> Unit,
    refClick: (Long) -> Unit,
    onImageClick: (Image) -> Unit,
    onCopy: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarkImage: (Image) -> Unit,
    onUserClick: (String) -> Unit,
) {
    val isPo = remember(reply.author.id) {
        reply.author.id == poUserHash
    }
    var showMenu by remember { mutableStateOf(false) }

    // Haptic feedback
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onReplyClicked(reply.id) },
                    onLongClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
                .padding(horizontal = Dimens.padding_standard, vertical = Dimens.padding_medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThreadAuthor(
                    author = reply.author,
                    threadTime = reply.createdAt.toRelativeTimeString(),
                    isPo = isPo,
                    onClick = onUserClick,
                    badges = {
                        if (reply.isAdmin) {
                            Badge(
                                text = "ADMIN",
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "#${reply.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(start = Dimens.padding_small, top = 4.dp)
                )
            }

            if (reply.title.isNullOrBlank().not() && reply.title != "无标题") {
                Spacer(modifier = Modifier.height(Dimens.padding_medium))
                Text(
                    text = reply.title!!,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(Dimens.padding_medium))

            ThreadBody(
                content = reply.content,
                images = reply.images,
                onReferenceClick = refClick,
                onImageClick = onImageClick,
                onImageLongClick = { image -> onBookmarkImage(image) }
            )

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
        }
    }
}
