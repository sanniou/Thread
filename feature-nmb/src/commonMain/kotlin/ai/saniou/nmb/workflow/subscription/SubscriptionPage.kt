package ai.saniou.nmb.workflow.subscription

import ai.saniou.coreui.state.LoadingWrapper
import ai.saniou.nmb.di.nmbdi
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.nmb.ui.components.SkeletonLoader
import ai.saniou.nmb.workflow.image.ImagePreviewNavigationDestination
import ai.saniou.nmb.workflow.thread.ThreadPageNavigationDestination
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance
import ai.saniou.nmb.ui.components.NmbImage

/**
 * 订阅列表页面
 */
@Composable
fun SubscriptionPage(
    di: DI = nmbdi,
    onThreadClicked: (Long) -> Unit,
    onUpdateTitle: ((String) -> Unit)? = null,
    navController: NavController
) {
    val subscriptionViewModel: SubscriptionViewModel = viewModel {
        val viewModel by di.instance<SubscriptionViewModel>()
        viewModel
    }

    val uiState by subscriptionViewModel.uiState.collectAsStateWithLifecycle()
    val showSubscriptionIdDialog by subscriptionViewModel.showSubscriptionIdDialog.collectAsStateWithLifecycle()
    val subscriptionId by subscriptionViewModel.subscriptionId.collectAsStateWithLifecycle()

    // 更新标题
    LaunchedEffect(Unit) {
        onUpdateTitle?.invoke("订阅列表")
    }

    Surface(
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { subscriptionViewModel.showSubscriptionIdDialog() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置订阅ID"
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                uiState.LoadingWrapper<SubscriptionUiState>(
                    content = { state ->
                        SubscriptionContent(
                            uiState = state,
                            onThreadClicked = onThreadClicked,
                            onImageClick = { imgPath, ext ->
                                navController.navigate(
                                    ImagePreviewNavigationDestination.createRoute(
                                        imgPath,
                                        ext
                                    )
                                )
                            },
                            onUnsubscribe = { threadId ->
                                subscriptionViewModel.unsubscribe(threadId)
                            },
                            innerPadding = innerPadding
                        )
                    },
                    error = {
                        // 错误状态显示
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "加载失败",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = it.message ?: "未知错误",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { subscriptionViewModel.showSubscriptionIdDialog() }
                                ) {
                                    Text("设置订阅ID")
                                }
                            }
                        }
                    },
                    loading = {
                        // 加载状态显示骨架屏
                        Box(modifier = Modifier.padding(innerPadding)) {
                            SkeletonLoader()
                        }
                    },
                    onRetryClick = {
                        // 重试时刷新订阅列表
                        subscriptionViewModel.refreshSubscription()
                    }
                )
            }
        }
    }

    // 订阅ID设置对话框
    if (showSubscriptionIdDialog) {
        SubscriptionIdDialog(
            currentId = subscriptionId ?: "",
            onDismiss = { subscriptionViewModel.hideSubscriptionIdDialog() },
            onConfirm = { id -> subscriptionViewModel.setSubscriptionId(id) },
            onGenerateRandom = { subscriptionViewModel.generateRandomSubscriptionId() }
        )
    }
}

/**
 * 订阅列表内容
 */
@Composable
fun SubscriptionContent(
    uiState: SubscriptionUiState,
    onThreadClicked: (Long) -> Unit,
    onImageClick: ((String, String) -> Unit)? = null,
    onUnsubscribe: (Long) -> Unit,
    innerPadding: PaddingValues
) {
    PullToRefreshWrapper(
        onRefreshTrigger = uiState.onRefresh,
        modifier = Modifier.padding(innerPadding)
    ) {
        if (uiState.feeds.isEmpty()) {
            // 空列表状态
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "暂无订阅",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "在帖子页面点击菜单可以添加订阅",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // 有订阅时显示列表
            val scrollState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

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
                    if (isAtBottom && uiState.hasNextPage) {
                        // 加载下一页
                        uiState.onLoadNextPage()
                    }
                }
            }

            LazyColumn(
                state = scrollState,
                contentPadding = PaddingValues(8.dp)
            ) {
                items(uiState.feeds) { feed ->
                    SubscriptionCard(
                        feed = feed,
                        onClick = { onThreadClicked(feed.id.toLong()) },
                        onImageClick = onImageClick,
                        onUnsubscribe = { onUnsubscribe(feed.id.toLong()) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * 订阅卡片
 */
@Composable
fun SubscriptionCard(
    feed: ai.saniou.nmb.data.entity.Feed,
    onClick: () -> Unit,
    onImageClick: ((String, String) -> Unit)? = null,
    onUnsubscribe: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 标题和作者信息行
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 管理员标记
                if (feed.admin.toIntOrNull() ?: 0 > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(2.dp)
                    ) {
                        Text(
                            text = "管",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // 标题
                Text(
                    text = if (feed.title.isNotBlank() && feed.title != "无标题") feed.title else "无标题",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // 取消订阅按钮
                IconButton(onClick = onUnsubscribe) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "取消订阅",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 作者信息行
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = feed.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (feed.userHash.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feed.userHash,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = feed.now,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "回复: ${feed.replyCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // 内容
            Text(
                text = feed.content.replace(Regex("<.*?>"), ""), // 简单移除HTML标签
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // 如果有图片，显示图片
            if (feed.img.isNotEmpty() && feed.ext.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                NmbImage(
                    imgPath = feed.img,
                    ext = feed.ext,
                    modifier = Modifier.fillMaxWidth(),
                    isThumb = true,
                    contentDescription = "帖子图片",
                    autosize = true,
                    onClick = { onImageClick?.invoke(feed.img, feed.ext) }
                )
            }
        }
    }
}

/**
 * 订阅ID设置对话框
 */
@Composable
fun SubscriptionIdDialog(
    currentId: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onGenerateRandom: () -> Unit
) {
    var subscriptionId by remember { mutableStateOf(currentId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置订阅ID") },
        text = {
            Column {
                Text(
                    "订阅ID用于标识您的订阅列表，可以自定义或随机生成。",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = subscriptionId,
                    onValueChange = { subscriptionId = it },
                    label = { Text("订阅ID") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm(subscriptionId) })
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(subscriptionId) }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("取消")
                }

                TextButton(
                    onClick = onGenerateRandom
                ) {
                    Text("随机生成")
                }
            }
        }
    )
}
