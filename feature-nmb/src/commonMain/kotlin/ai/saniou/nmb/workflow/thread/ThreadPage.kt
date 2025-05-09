package ai.saniou.nmb.workflow.thread

import ai.saniou.coreui.state.LoadingWrapper
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.nmb.data.entity.Reply
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.*
import ai.saniou.nmb.workflow.home.LocalSnackbarHostState
import ai.saniou.nmb.workflow.image.ImagePreviewNavigationDestination
import ai.saniou.nmb.workflow.reference.ReferenceViewModel
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.DI
import org.kodein.di.instance

@Composable
fun ThreadPage(
    threadId: Long?,
    navController: NavController,
    di: DI = nmbdi,
    onUpdateTitle: ((String) -> Unit)? = null,
    onSetupMenuButton: ((@Composable () -> Unit) -> Unit)? = null,
) {
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
    val snackbarHostState = LocalSnackbarHostState.current


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
            snackbarHostState.showSnackbar(it)
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
                        navController.navigate(
                            ImagePreviewNavigationDestination.createRoute(
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { threadViewModel.setThreadId(threadId) }
                        ) {
                            Text("重试")
                        }
                    }
                }
            },
            loading = {
                // 加载状态显示骨架屏
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    // 创建动画效果
                    val shimmerColors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )

                    val transition = rememberInfiniteTransition()
                    val translateAnim by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1000f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1200, delayMillis = 300),
                            repeatMode = RepeatMode.Restart
                        )
                    )

                    val brush = Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(10f, 10f),
                        end = Offset(translateAnim, translateAnim)
                    )

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

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(brush)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

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
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
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
            onToggleSubscribe = { state.onToggleSubscribe(it) },
            onCopyLink = {
                // 复制帖子链接
                val url = "https://nmb.com/t/${threadId}"
                clipboardManager.setText(AnnotatedString(url))
                scope.launch {
                    snackbarHostState.showSnackbar("链接已复制到剪贴板")
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
                (referenceState as UiStateWrapper.Success<Reply>).value
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
fun ThreadContent(
    uiState: ThreadUiState,
    onReplyClicked: (Long) -> Unit,
    refClick: (Long) -> Unit,
    onImageClick: (String, String) -> Unit
) {
    PullToRefreshWrapper(
        onRefreshTrigger = {
            uiState.onRefresh()
        }
    ) {
        // 检查是否有回复
        if (uiState.thread.replies.isEmpty() && uiState.thread.replyCount == 0L) {
            // 空回复状态
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    // 主帖
                    ThreadMainPost(uiState.thread, refClick = refClick, onImageClick = onImageClick)

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

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "成为第一个回复的人吧！",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { uiState.onRefresh() }
                    ) {
                        Text("刷新")
                    }
                }
            }
        } else {
            // 有回复时显示列表
            val scrollState = rememberLazyListState()

            // 监听滚动到底部，加载更多
            LaunchedEffect(scrollState) {
                snapshotFlow {
                    val layoutInfo = scrollState.layoutInfo
                    val totalItemsCount = layoutInfo.totalItemsCount
                    val lastVisibleItemIndex =
                        (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

                    // 当最后一个可见项是列表中的最后一项，且列表不为空
                    lastVisibleItemIndex >= totalItemsCount && totalItemsCount > 0
                }.collect { isAtBottom ->
                    if (isAtBottom) {
                        // 加载下一页
                        uiState.onLoadNextPage()
                    }
                }
            }

            LazyColumn(
                state = scrollState,
                modifier = Modifier.padding(8.dp)
            ) {
                // 主帖
                item {
                    ThreadMainPost(
                        thread = uiState.thread,
                        forumName = "",
                        refClick = refClick,
                        onImageClick = onImageClick
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 回复列表
                items(uiState.thread.replies) { reply ->
                    ThreadReply(reply, onReplyClicked, refClick, onImageClick)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 底部加载指示器
                item {
                    if (uiState.thread.replies.isNotEmpty() && uiState.hasMoreData) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else if (uiState.thread.replies.isNotEmpty()) {
                        // 显示已经加载完所有数据的提示
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "已加载全部回复",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
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
    onImageClick: (String, String) -> Unit
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

                Spacer(modifier = Modifier.width(8.dp))

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

                    Row {
                        Text(
                            text = thread.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = thread.userHash,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = thread.now,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 不再显示菜单按钮，因为已经移到顶部应用栏
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // 内容 - 使用HtmlText支持HTML标签
            HtmlText(
                text = thread.content,
                style = MaterialTheme.typography.bodyMedium,
                onReferenceClick = refClick
            )

            // 如果有图片，显示图片
            if (thread.img.isNotEmpty() && thread.ext.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                NmbImage(
                    imgPath = thread.img,
                    ext = thread.ext,
                    modifier = Modifier.fillMaxWidth(),
                    isThumb = false,
                    contentDescription = "帖子图片",
                    contentScale = ContentScale.FillWidth,
                    onClick = { onImageClick(thread.img, thread.ext) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
    onImageClick: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReplyClicked(reply.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 回复者信息
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text(
                            text = reply.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = reply.userHash,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = reply.now,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 显示回复号码
                Text(
                    text = "No.${reply.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 回复内容 - 使用HtmlText支持HTML标签
            HtmlText(
                text = reply.content,
                style = MaterialTheme.typography.bodyMedium,
                onReferenceClick = refClick
            )

            // 如果有图片，显示图片
            if (reply.img.isNotEmpty() && reply.ext.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                NmbImage(
                    imgPath = reply.img,
                    ext = reply.ext,
                    modifier = Modifier.fillMaxWidth(),
                    isThumb = true,
                    contentDescription = "回复图片",
                    contentScale = ContentScale.FillWidth,
                    onClick = { onImageClick(reply.img, reply.ext) }
                )
            }
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
