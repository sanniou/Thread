package ai.saniou.nmb.workflow.thread

import ai.saniou.coreui.state.LoadingWrapper
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.nmb.data.entity.Reply
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.LoadEndIndicator
import ai.saniou.nmb.ui.components.LoadingFailedIndicator
import ai.saniou.nmb.ui.components.LoadingIndicator
import ai.saniou.nmb.ui.components.PageJumpDialog
import ai.saniou.nmb.ui.components.ReferencePopup
import ai.saniou.nmb.ui.components.ShimmerContainer
import ai.saniou.nmb.ui.components.SkeletonReplyItem
import ai.saniou.nmb.ui.components.ThreadAuthor
import ai.saniou.nmb.ui.components.ThreadBody
import ai.saniou.nmb.ui.components.ThreadMenu
import ai.saniou.nmb.ui.components.ThreadSpacer
import ai.saniou.nmb.workflow.image.ImagePreviewPage
import ai.saniou.nmb.workflow.reference.ReferenceViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.DI
import org.kodein.di.instance
import thread.feature_nmb.generated.resources.Res
import thread.feature_nmb.generated.resources.favorites
import thread.feature_nmb.generated.resources.jump_to_page

data class ThreadPage(
    val threadId: Long?,
    val di: DI = nmbdi,
) : Screen {


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }

        var titleBar = remember { "" }
        val onSetupMenuButton: ((@Composable () -> Unit) -> Unit) = {}

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(titleBar)
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        onSetupMenuButton
                        // 原本在Content里的菜单按钮现在移到这里
                        val (showMenu, setShowMenu) = remember { mutableStateOf(false) }
                        IconButton(onClick = { setShowMenu(true) }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "菜单"
                            )
                        }

                        // 菜单弹出框保持在这里，但实际菜单内容可能需要根据你的需求调整
                        if (showMenu) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { setShowMenu(false) }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("刷新") },
                                    onClick = {
                                        // 添加刷新逻辑
                                        setShowMenu(false)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("分享") },
                                    onClick = {
                                        // 添加分享逻辑
                                        setShowMenu(false)
                                    }
                                )
                            }
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                ThreadContent(
                    threadId = threadId,
                    di = di,
                    onUpdateTitle = { titleBar = it },
                    onSetupMenuButton = onSetupMenuButton,
                )
            }
        }
    }

}

@Composable
fun ThreadContent(
    threadId: Long?,
    di: DI = nmbdi,
    onUpdateTitle: ((String) -> Unit)? = null,
    onSetupMenuButton: ((@Composable () -> Unit) -> Unit)? = null,
) {
    val navigator = LocalNavigator.currentOrThrow

    val threadViewModel: ThreadViewModel = viewModel {
        val viewModel by di.instance<ThreadViewModel>()
        viewModel
    }

    // 引用ViewModel
    val referenceViewModel: ReferenceViewModel = viewModel {
        val viewModel by di.instance<ReferenceViewModel>()
        viewModel
    }

    // 使用LaunchedEffect设置threadId，确保只在threadId变化时触发
    LaunchedEffect(threadId) {
        threadViewModel.setThreadId(threadId)
    }

    val uiState by threadViewModel.uiState.collectAsStateWithLifecycle()

    // 菜单状态
    var showMenu by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }

    // 引用弹窗状态
    var showReferencePopup by remember { mutableStateOf(false) }
    var currentReferenceId by remember { mutableStateOf(0L) }
    val referenceState by referenceViewModel.uiState.collectAsStateWithLifecycle()

    // 复制链接的处理
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // 监听状态变化，更新标题
    LaunchedEffect(uiState) {
        if (uiState is UiStateWrapper.Success<*>) {
            val state = (uiState as UiStateWrapper.Success<ThreadUiState>).value!!
            // 使用HTML标签来实现大标题和小标题的样式
            val title = buildString {
                append("<b>No.${threadId}</b>")
                if (state.forumName.isNotBlank()) {
                    append("<br><small>${state.forumName}</small>")
                }
            }
            onUpdateTitle?.invoke(title)
        }
    }

    LaunchedEffect((uiState as? UiStateWrapper.Success<ThreadUiState>)?.value?.subscribedMessage) {
        (uiState as? UiStateWrapper.Success<ThreadUiState>)?.value?.subscribedMessage?.let {
            //snackbarHostState.showSnackbar(it)
        }
    }


    // 设置菜单按钮
    LaunchedEffect(Unit) {
        onSetupMenuButton?.invoke {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "菜单"
                )
            }
        }
    }

    Box {
        uiState.LoadingWrapper<ThreadUiState>(
            content = { state ->
                ThreadContent(
                    uiState = state,
                    onReplyClicked = { replyId ->
                        // 处理回复点击
                        threadViewModel.onReplyClicked(replyId)
                    },
                    refClick = { refId ->
                        // 显示引用弹窗
                        currentReferenceId = refId
                        referenceViewModel.getReference(refId)
                        showReferencePopup = true
                    },
                    onImageClick = { imgPath, ext ->
                        // 导航到图片预览页面
                        navigator.push(
                            ImagePreviewPage(
                                imgPath,
                                ext
                            )
                        )
                    }
                )
            },
            error = {
                // 错误状态显示
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        ThreadSpacer()
                        Button(
                            onClick = { threadViewModel.setThreadId(threadId) }
                        ) {
                            Text("重试")
                        }
                    }
                }
            },
            loading = {
                ThreadShimmer()
            },
            onRetryClick = {
                // 重试时重新设置threadId，触发重新加载
                threadViewModel.setThreadId(threadId)
            }
        )
    }

    // 显示菜单
    if (showMenu && uiState is UiStateWrapper.Success<*>) {
        val state = (uiState as UiStateWrapper.Success<ThreadUiState>).value!!
        ThreadMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            onJumpToPage = { showJumpDialog = true },
            onTogglePoOnly = { state.onTogglePoOnly() },
            onToggleSubscribe = { state.onToggleSubscribe() },
            onCopyLink = {
                // 复制帖子链接
                val url = "https://nmb.com/t/${threadId}"
                clipboardManager.setText(AnnotatedString(url))
                scope.launch {
                    // snackbarHostState.showSnackbar("链接已复制到剪贴板")
                }
            },
            isSubscribed = state.isSubscribed,
            isPoOnlyMode = state.isPoOnlyMode
        )
    }

    // 显示跳转页面对话框
    if (showJumpDialog && uiState is UiStateWrapper.Success<*>) {
        val state = (uiState as UiStateWrapper.Success<ThreadUiState>).value!!
        PageJumpDialog(
            currentPage = state.currentPage,
            totalPages = state.totalPages,
            onDismissRequest = { showJumpDialog = false },
            onJumpToPage = { page ->
                state.onJumpToPage(page)
            }
        )
    }

    // 显示引用弹窗
    if (showReferencePopup) {
        ReferencePopup(
            refId = currentReferenceId,
            reply = if (referenceState is UiStateWrapper.Success<*>) {
                (referenceState as UiStateWrapper.Success<ThreadReply>).value
            } else null,
            isLoading = referenceState is UiStateWrapper.Loading,
            error = if (referenceState is UiStateWrapper.Error) {
                (referenceState as UiStateWrapper.Error).message
            } else null,
            onDismiss = {
                showReferencePopup = false
                referenceViewModel.clear()
            }
        )
    }
}

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

                    ThreadSpacer()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )

                    ThreadSpacer()

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
                ThreadSpacer()
            }
        }
    }
}

@Composable
fun ThreadContent(
    uiState: ThreadUiState,
    onReplyClicked: (Long) -> Unit,
    refClick: (Long) -> Unit,
    onImageClick: (String, String) -> Unit,
) {
    val replies = uiState.threadReplies.collectAsLazyPagingItems()
    PullToRefreshWrapper(
        onRefreshTrigger = {
            replies.refresh()
        }
    ) {
        Column {
            LazyColumn(
                modifier = Modifier.padding(8.dp)
                    .weight(1f)
            ) {
                // 主帖
                item {
                    ThreadMainPost(
                        thread = uiState.thread,
                        forumName = uiState.forumName,
                        refClick = refClick,
                        onImageClick = onImageClick
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 回复列表
                items(replies.itemCount) { replyIndex ->
                    replies[replyIndex]?.let { reply ->
                        ThreadReply(reply, onReplyClicked, refClick, onImageClick)
                        ThreadSpacer()
                    } ?: ShimmerContainer {
                        SkeletonReplyItem(it)
                    }
                }

                item {
                    when {
                        replies.loadState.refresh is LoadState.Loading
                                && replies.itemCount == 0 -> ThreadShimmer()

                        replies.loadState.refresh is LoadState.Loading -> {
                            // 显示初始加载中
                        }

                        replies.loadState.refresh is LoadState.Error -> {
                            val error = (replies.loadState.refresh as LoadState.Error).error
                            // 显示初始加载失败UI
                        }

                        replies.loadState.append is LoadState.Error -> LoadingFailedIndicator()
                        replies.loadState.append is LoadState.Loading -> LoadingIndicator()

                        replies.loadState.append.endOfPaginationReached && replies.itemCount == 0 -> {
                            // 空回复状态
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Spacer(modifier = Modifier.height(32.dp))

                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "暂无回复",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    ThreadSpacer()

                                    Text(
                                        text = "成为第一个回复的人吧！",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { replies.refresh() }
                                    ) {
                                        Text("刷新")
                                    }
                                }
                            }
                        }

                        replies.loadState.append.endOfPaginationReached -> LoadEndIndicator()
                    }
                }
            }

            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button({
                    uiState.onToggleSubscribe()
                }) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        tint = if (uiState.isSubscribed) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        contentDescription = stringResource(Res.string.favorites),
                    )
                }

                Button({

                }) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        tint = if (uiState.isSubscribed) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        contentDescription = stringResource(Res.string.jump_to_page),
                    )
                }
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
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "PO",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                ThreadSpacer()

                Column(modifier = Modifier.weight(1f)) {
                    // 大标题 = 帖子号码
                    Text(
                        text = "No.${thread.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 小标题 = 论坛名称
                    if (forumName.isNotBlank()) {
                        Text(
                            text = forumName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 帖子标题
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

            ThreadSpacer()
            HorizontalDivider()
            ThreadSpacer()
            ThreadBody(thread, onReferenceClick = refClick, onImageClick = onImageClick)
            ThreadSpacer()

            // 回复数量
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
        Column(modifier = Modifier.padding(12.dp)) {
            // 回复者信息
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ThreadAuthor(reply)
                Spacer(modifier = Modifier.weight(1f))

                // 显示回复号码
                Text(
                    text = "No.${reply.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            ThreadBody(reply, onReferenceClick = refClick, onImageClick = onImageClick)

        }
    }
}

@Preview
@Composable
fun ThreadPagePreview() {
    MaterialTheme {
        // 预览内容
    }
}
