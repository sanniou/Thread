package ai.saniou.nmb.workflow.thread

import ai.saniou.coreui.state.LoadingWrapper
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.nmb.di.nmbdi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.DI
import org.kodein.di.instance

@Composable
fun ThreadPage(threadId: Long?, di: DI = nmbdi) {
    val threadViewModel: ThreadViewModel = viewModel {
        val viewModel by di.instance<ThreadViewModel>()
        viewModel
    }

    // 加载帖子数据
    if (threadId != null && threadId > 0) {
        threadViewModel.loadThread(threadId)
    }

    val uiState by threadViewModel.uiState.collectAsStateWithLifecycle()

    Box {
        uiState.LoadingWrapper<ThreadUiState>(
            content = { state ->
                ThreadContent(state) { replyId ->
                    // 处理回复点击
                    threadViewModel.onReplyClicked(replyId)
                }
            },
            onRetryClick = {
                if (threadId != null) {
                    threadViewModel.loadThread(threadId)
                }
            }
        )

        // 添加回复按钮
        FloatingActionButton(
            onClick = {
                if (threadId != null) {
                    threadViewModel.navigateToReply(threadId)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "回复")
        }
    }
}

@Composable
fun ThreadContent(
    uiState: ThreadUiState,
    onReplyClicked: (Long) -> Unit
) {
    PullToRefreshWrapper(
        onRefreshTrigger = {
            uiState.onRefresh()
        }
    ) {
        LazyColumn(
            modifier = Modifier.padding(8.dp)
        ) {
            // 主帖
            item {
                ThreadMainPost(uiState.thread)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 回复列表
            items(uiState.thread.replies) { reply ->
                ThreadReply(reply, onReplyClicked)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ThreadMainPost(thread: ai.saniou.nmb.data.entity.Thread) {
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

                Column {
                    Text(
                        text = thread.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

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
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // 内容
            Text(
                text = thread.content,
                style = MaterialTheme.typography.bodyMedium
            )

            // 如果有图片，显示图片
            if (thread.img.isNotEmpty() && thread.ext.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                // 这里应该添加图片显示组件
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        .background(Color.LightGray, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = "图片: ${thread.img}${thread.ext}",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
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
    reply: ai.saniou.nmb.data.entity.ThreadReply,
    onReplyClicked: (Long) -> Unit
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
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            Spacer(modifier = Modifier.height(4.dp))

            // 回复内容
            Text(
                text = reply.content,
                style = MaterialTheme.typography.bodyMedium
            )

            // 如果有图片，显示图片
            if (reply.img.isNotEmpty() && reply.ext.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                // 这里应该添加图片显示组件
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        .background(Color.LightGray, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = "图片: ${reply.img}${reply.ext}",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
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
