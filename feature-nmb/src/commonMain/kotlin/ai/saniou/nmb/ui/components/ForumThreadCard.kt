package ai.saniou.nmb.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.thread.data.source.nmb.remote.dto.IBaseThread
import ai.saniou.thread.data.source.nmb.remote.dto.IBaseThreadReply
import ai.saniou.thread.data.source.nmb.remote.dto.ThreadReply
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.forum.Post
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import thread.feature_nmb.generated.resources.Res
import thread.feature_nmb.generated.resources.empty_title


@Composable
fun ForumThreadCard(
    thread: IBaseThread,
    onClick: () -> Unit,
    onImageClick: ((String, String) -> Unit)? = null,
    onUserClick: ((String) -> Unit)? = null,
) {
    ForumThreadCard(
        thread.toDomain(),
        onClick,
        onImageClick,
        onUserClick
    )
}

/**
 * 串内容卡片，遵循 MD3 设计风格
 */
@Composable
fun ForumThreadCard(
    thread: Post,
    onClick: () -> Unit,
    onImageClick: ((String, String) -> Unit)? = null,
    onUserClick: ((String) -> Unit)? = null,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.padding_medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
        ) {
            // 统一的头部
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)) {
                ThreadAuthor(thread.userHash, thread.name, thread.now, onClick = onUserClick)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small)
                ) {
                    if (thread.isAdmin) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "管理员",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .size(Dimens.icon_size_medium)
                                    .padding(Dimens.padding_extra_small)
                            )
                        }
                    }
                    Text(
                        text = thread.title.ifBlank { stringResource(Res.string.empty_title) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (thread.isSage) {
                        Text(
                            text = "SAGE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 内容区域
            ThreadBody(
                thread.content,
                thread.img,
                thread.ext,
                maxLines = 6,
                onImageClick = { img, ext -> onImageClick?.invoke(img, ext) }
            )

            // 尾部
            ThreadCardFooter(thread.replyCount, thread.replies?.map {
                ThreadReply(
                    id = it.id,
                    userHash = it.userHash,
                    admin = it.admin,
                    title = it.title,
                    now = it.now,
                    content = it.content,
                    img = it.img,
                    ext = it.ext,
                    name = it.name,
                    threadId = it.threadId,
                )
            }, thread.remainReplies)
        }
    }
}

@Composable
private fun ThreadCardFooter(thread: IBaseThread) {
    val threadReply = thread as? IBaseThreadReply
    ThreadCardFooter(
        replyCount = thread.replyCount,
        replies = threadReply?.replies ?: emptyList(),
        remainReplies = threadReply?.remainReplies,
    )
}

@Composable
private fun ThreadCardFooter(
    replyCount: Long,
    replies: List<ThreadReply>?,
    remainReplies: Long?,
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_extra_small)
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = "回复",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.icon_size_small)
            )
            Text(
                text = replyCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if ((remainReplies ?: 0) > 0) {
            Text(
                text = "省略${remainReplies}条",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (!replies.isNullOrEmpty()) {
        Spacer(modifier = Modifier.height(Dimens.padding_small))
        RecentReplies(replies)
    }
}

