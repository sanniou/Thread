package ai.saniou.forum.ui.components

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.thread.domain.model.forum.Comment as ThreadReply
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 引用底部弹窗组件
 *
 * 使用 ModalBottomSheet 展示引用的回复内容，提供更自然的交互体验。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceSheet(
    refId: Long,
    reply: ThreadReply?,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onJumpToThread: (String) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),

        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars) // Add padding for navigation bar
                .padding(bottom = 24.dp) // Extra bottom padding
        ) {
            // 标题栏
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "引用内容",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No.$refId",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledIconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            // 内容区域
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    reply != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()) // Allow scrolling if content is long
                        ) {
                            ThreadAuthor(
                                author = reply.author,
                                threadTime = reply.createdAt.toRelativeTimeString(),
                                badges = {
                                    if (reply.isAdmin) {
                                        Badge(
                                            text = "ADMIN",
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ThreadBody(
                                content = reply.content,
                                images = reply.images,
                                onImageClick = { _ -> },
                                onReferenceClick = null // Disable nested reference clicks in popup for simplicity
                            )
                            if (reply.topicId.isNotBlank() && reply.topicId != "0") {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onJumpToThread(reply.topicId) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("跳转到原串")
                                }
                            }
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "未找到该引用内容",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
