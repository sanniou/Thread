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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.compose.viewmodel.rememberViewModel

data class ThreadPage(
    val threadId: Long?,
    val di: DI = nmbdi,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val clipboardManager = LocalClipboardManager.current

        val viewModel: ThreadViewModel by rememberViewModel()
        val state by viewModel.state.collectAsState()

        var showJumpDialog by remember { mutableStateOf(false) }

        // 引用弹窗状态
        val referenceViewModel: ReferenceViewModel by rememberViewModel()
        var showReferencePopup by remember { mutableStateOf(false) }
        var currentReferenceId by remember { mutableStateOf(0L) }
        val referenceState by referenceViewModel.uiState.collectAsState()


        // 加载数据
        LaunchedEffect(threadId) {
            if (threadId != null) {
                viewModel.onEvent(Event.LoadThread(threadId))
            }
        }

        // TODO: [AUTO-SCROLL] Implement auto-scroll to last read position and update on scroll.
//        LaunchedEffect(lazyListState, state.thread) {
//            if (state.thread == null) return@LaunchedEffect
//            val lastReadId = state.thread?.last_read_reply_id ?: 0
//            if (lastReadId > 0) {
//                val replies = state.replies.collectAsLazyPagingItems()
//                val index = replies.itemSnapshotList.indexOfFirst { it?.id == lastReadId }
//                if (index != -1) {
//                    lazyListState.scrollToItem(index + 1) // +1 for the main post
//                }
//            }
//
//            snapshotFlow { lazyListState.firstVisibleItemIndex }
//                .collect { index ->
//                    val replies = state.replies.collectAsLazyPagingItems()
//                    if (index > 0 && index < replies.itemCount) {
//                        replies[index - 1]?.let {
//                            viewModel.onEvent(Event.UpdateLastReadReplyId(it.id))
//                        }
//                    }
//                }
//        }

        // 处理 Snackbar
        LaunchedEffect(state.snackbarMessage) {
            state.snackbarMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.onEvent(Event.SnackbarMessageShown)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                        })
                    },
                    title = {
                        if (state.thread != null) {
                            Column {
                                Text(
                                    text = "No.${state.thread?.id}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (state.forumName.isNotBlank()) {
                                    Text(
                                        text = state.forumName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (state.thread != null) {
                    ThreadActionsFab(
                        isSubscribed = state.isSubscribed,
                        isPoOnlyMode = state.isPoOnlyMode,
                        onRefresh = { viewModel.onEvent(Event.Refresh) },
                        onToggleSubscription = { viewModel.onEvent(Event.ToggleSubscription) },
                        onJumpToPage = { showJumpDialog = true },
                        onTogglePoOnly = { viewModel.onEvent(Event.TogglePoOnlyMode) },
                        onCopyLink = {
                            val url = "https://nmb.com/t/${threadId}"
                            clipboardManager.setText(AnnotatedString(url))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("链接已复制到剪贴板")
                            }
                        },
                        onPost = {
                            state.thread?.let {
                                navigator.push(PostPage(it.fid, it.id))
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            ThreadContentRouter(
                modifier = Modifier.padding(innerPadding),
                state = state,
                lazyListState = lazyListState,
                onRefresh = { viewModel.onEvent(Event.Refresh) },
                onRefClick = { refId ->
                    currentReferenceId = refId
                    referenceViewModel.onEvent(
                        ReferenceContract.Event.GetReference(
                            refId
                        )
                    )
                    showReferencePopup = true
                },
                onImageClick = { imgPath, ext ->
                    navigator.push(ImagePreviewPage(imgPath, ext))
                }
            )
        }

        // 跳页对话框
        if (showJumpDialog) {
            PageJumpDialog(
                currentPage = state.currentPage,
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
    onRefClick: (Long) -> Unit,
    onImageClick: (String, String) -> Unit
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
                onRefClick = onRefClick,
                onImageClick = onImageClick
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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

@Composable
fun ThreadSuccessContent(
    state: State,
    lazyListState: LazyListState,
    onRefresh: () -> Unit,
    onReplyClicked: (Long) -> Unit,
    onRefClick: (Long) -> Unit,
    onImageClick: (String, String) -> Unit,
) {
    val replies = state.replies.collectAsLazyPagingItems()
    PullToRefreshWrapper(onRefreshTrigger = { replies.refresh() }) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 主帖
            item {
                state.thread?.let {
                    ThreadMainPost(
                        thread = it,
                        forumName = state.forumName,
                        refClick = onRefClick,
                        onImageClick = onImageClick
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 回复列表
            items(replies.itemCount) { replyIndex ->
                replies[replyIndex]?.let { reply ->
                    ThreadReply(reply, onReplyClicked, onRefClick, onImageClick)
                } ?: ShimmerContainer { SkeletonReplyItem(it) }
            }

            // Paging 加载状态
            item {
                when {
                    replies.loadState.refresh is LoadState.Loading && replies.itemCount == 0 -> ThreadShimmer()
                    replies.loadState.refresh is LoadState.Error -> {
                        // 错误状态已在顶层处理
                    }

                    replies.loadState.append is LoadState.Error -> LoadingFailedIndicator()
                    replies.loadState.append is LoadState.Loading -> LoadingIndicator()
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
}

@Composable
private fun EmptyReplyContent(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
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
fun ThreadMainPost(
    thread: Thread,
    forumName: String = "",
    refClick: (Long) -> Unit,
    onImageClick: (String, String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题和作者信息
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PO",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "No.${thread.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (forumName.isNotBlank()) {
                        Text(
                            text = forumName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (thread.title.isNotBlank() && thread.title != "无标题") {
                        Text(
                            text = thread.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    ThreadAuthor(thread)
                }
            }

            HorizontalDivider()
            ThreadBody(thread, onReferenceClick = refClick, onImageClick = onImageClick)

            Text(
                text = "回复: ${thread.replyCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ThreadReply(
    reply: ThreadReply,
    onReplyClicked: (Long) -> Unit,
    refClick: (Long) -> Unit,
    onImageClick: (String, String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReplyClicked(reply.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 回复者信息
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ThreadAuthor(reply)
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "No.${reply.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            ThreadBody(reply, onReferenceClick = refClick, onImageClick = onImageClick)
        }
    }
}

// endregion

// =================================================================================
// region Floating Action Buttons
// =================================================================================

@Composable
fun ThreadActionsFab(
    isSubscribed: Boolean,
    isPoOnlyMode: Boolean,
    onRefresh: () -> Unit,
    onToggleSubscription: () -> Unit,
    onJumpToPage: () -> Unit,
    onTogglePoOnly: () -> Unit,
    onCopyLink: () -> Unit,
    onPost: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val transition = updateTransition(targetState = expanded, label = "fab_transition")

    val rotation by transition.animateFloat(label = "fab_rotation") { isExpanded ->
        if (isExpanded) 45f else 0f
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FabAction(icon = Icons.Default.Refresh, text = "刷新", onClick = onRefresh)
                FabAction(
                    icon = if (isSubscribed) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    text = "收藏",
                    onClick = onToggleSubscription
                )
                FabAction(icon = Icons.Default.PlayArrow, text = "跳页", onClick = onJumpToPage)
                FabAction(
                    icon = if (isPoOnlyMode) Icons.Filled.Person else Icons.Outlined.Person,
                    text = "仅看PO",
                    onClick = onTogglePoOnly
                )
                FabAction(icon = Icons.Default.Call, text = "复制链接", onClick = onCopyLink)
                FabAction(icon = Icons.Default.Edit, text = "回复", onClick = onPost)
            }
        }

        FloatingActionButton(onClick = { expanded = !expanded }) {
            Icon(
                Icons.Default.Add,
                contentDescription = "操作",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
private fun FabAction(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(icon, contentDescription = text)
        }
    }
}

// endregion
