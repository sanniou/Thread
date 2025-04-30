package ai.saniou.nmb.workflow.forum

import ai.saniou.coreui.state.LoadingWrapper
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.nmb.data.entity.Reply
import ai.saniou.nmb.data.entity.ShowF
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.NmbImage
import ai.saniou.nmb.ui.components.SkeletonLoader
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.DI
import org.kodein.di.instance


/**
 * ForumScreen 作为一个轻量级包装器，用于独立页面导航
 *
 * 该组件主要用于从非Drawer入口（如搜索结果、收藏列表等）进入论坛
 */
@Composable
fun ForumScreen(
    di: DI = nmbdi,
    onThreadClicked: (Long) -> Unit = {},
    onNewPostClicked: (Long) -> Unit = {},
    forumId: Long = 0,
    onUpdateTitle: ((String) -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.background
    ) {
        // 使用可复用的ForumContent组件
        ForumContent(
            forumId = forumId,
            onThreadClicked = onThreadClicked,
            onNewPostClicked = onNewPostClicked,
            onUpdateTitle = onUpdateTitle,
            showFloatingActionButton = true
        )
    }
}

@Composable
@Preview
fun Forum(
    uiState: ShowForumUiState,
    onThreadClicked: (Long) -> Unit,
    innerPadding: PaddingValues? = null
) {
    PullToRefreshWrapper(
        onRefreshTrigger = {
            uiState.onUpdateForumId(uiState.id)
        },
        modifier = Modifier.run {
            if (innerPadding != null) {
                padding(innerPadding)
            } else {
                this
            }
        }
    ) {
        // 检查是否有帖子
        if (uiState.showF.isEmpty()) {
            // 空状态显示
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
                        text = "暂无帖子",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "该板块当前没有帖子，点击右下角按钮发布第一个帖子吧！",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { uiState.onUpdateForumId(uiState.id) }
                    ) {
                        Text("刷新")
                    }
                }
            }
        } else {
            // 有帖子时显示列表
            val scrollState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            // 监听滚动到底部，加载更多
            LaunchedEffect(scrollState) {
                snapshotFlow {
                    val layoutInfo = scrollState.layoutInfo
                    val totalItemsCount = layoutInfo.totalItemsCount
                    val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

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
                modifier = Modifier
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            coroutineScope.launch {
                                scrollState.scrollBy(-delta)
                            }
                        },
                    ),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(uiState.showF) { thread ->
                    ThreadCard(
                        thread = thread,
                        onClick = { onThreadClicked(thread.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 底部加载指示器
                item {
                    if (uiState.showF.isNotEmpty()) {
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
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadCard(
    thread: ShowF,
    onClick: () -> Unit
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
                if (thread.admin > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(2.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "管理员",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // 标题
                Text(
                    text = if (thread.title.isNotBlank() && thread.title != "无标题") thread.title else "无标题",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 作者信息行
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = thread.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (thread.userHash != null && thread.userHash.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = thread.userHash,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = thread.now,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // SAGE标记
                if (thread.sage > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAGE",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // 内容
            Text(
                text = thread.content.replace(Regex("<.*?>"), ""), // 简单移除HTML标签
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // 如果有图片，显示图片预览
            if (thread.img.isNotBlank() && thread.ext.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                NmbImage(
                    imgPath = thread.img,
                    ext = thread.ext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    isThumb = true,
                    contentDescription = "帖子图片"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 回复信息
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "回复",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "${thread.replyCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // 如果有剩余回复
                if (thread.remainReplies != null && thread.remainReplies > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(省略${thread.remainReplies}条回复)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 如果有最近回复，显示"查看全部"
                if (thread.replies.isNotEmpty()) {
                    Text(
                        text = "查看全部",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 如果有最近回复，显示最近回复
            if (thread.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                RecentReplies(thread.replies)
            }
        }
    }
}

@Composable
fun RecentReplies(replies: List<Reply>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(8.dp)
    ) {
        replies.forEachIndexed { index, reply ->
            if (index > 0) {
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            ReplyItem(reply)
        }
    }
}

@Composable
fun ReplyItem(reply: Reply) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 回复者信息
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = reply.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (reply.userHash != null && reply.userHash.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = reply.userHash,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = reply.now,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 回复内容
        Text(
            text = reply.content.replace(Regex("<.*?>"), ""), // 简单移除HTML标签
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}



