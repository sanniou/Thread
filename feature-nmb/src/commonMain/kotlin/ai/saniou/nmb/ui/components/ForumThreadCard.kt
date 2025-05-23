package ai.saniou.nmb.ui.components

import ai.saniou.nmb.data.entity.Feed
import ai.saniou.nmb.data.entity.IBaseAuthor
import ai.saniou.nmb.data.entity.IBaseThread
import ai.saniou.nmb.data.entity.IBaseThreadReply
import ai.saniou.nmb.data.entity.IThreadBody
import ai.saniou.nmb.data.entity.Reply
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.feature_nmb.generated.resources.Res
import thread.feature_nmb.generated.resources.empty_title

/**
 * 订阅卡片
 */
@Composable
fun SubscriptionCard(
    feed: Feed,
    onClick: () -> Unit,
    onImageClick: ((String, String) -> Unit)? = null,
    onUnsubscribe: () -> Unit
) {
    ThreadCard(
        thread = feed,
        threadReply = null,
        onClick = onClick,
        onImageClick = onImageClick
    )
}


@Composable
fun ThreadCard(
    thread: IBaseThread,
    threadReply: IBaseThreadReply? = (thread as? IBaseThreadReply),
    onClick: () -> Unit,
    onImageClick: ((String, String) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    text = thread.title.ifBlank { stringResource(Res.string.empty_title) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 作者信息行
            ThreadAuthor(thread)
            // SAGE标记
            if (thread.sage > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SAGE",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // 内容
            HtmlText(
                text = thread.content,
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
                    modifier = Modifier.height(240.dp)
                        .clickable { onImageClick?.invoke(thread.img, thread.ext) },
                    contentScale = ContentScale.FillHeight,
                    isThumb = true,
                    contentDescription = "帖子图片",
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 回复信息
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Face,
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
                threadReply?.run {
                    if ((threadReply.remainReplies ?: 0) > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(省略${threadReply.remainReplies}条回复)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 如果有最近回复，显示"查看全部"
                    if (threadReply.replies.isNotEmpty()) {
                        Text(
                            text = "查看全部",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

            }

            // 如果有最近回复，显示最近回复
            if (!threadReply?.replies.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                RecentReplies(threadReply.replies)
            }
        }
    }
}

@Composable
fun ThreadSpacer(
) {
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun ThreadBody(
    body: IThreadBody,
    onReferenceClick: ((Long) -> Unit)? = null,
    onImageClick: (String, String) -> Unit
) {
    // 内容 - 使用HtmlText支持HTML标签
    HtmlText(
        text = body.content,
        style = MaterialTheme.typography.bodyMedium,
        onReferenceClick = onReferenceClick
    )

    // 如果有图片，显示图片
    if (body.img.isNotEmpty() && body.ext.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        NmbImage(
            imgPath = body.img,
            ext = body.ext,
            isThumb = true,
            contentDescription = "帖子图片",
            modifier = Modifier.height(240.dp)
                .wrapContentWidth(Alignment.Start)
                .clickable { onImageClick(body.img, body.ext) },
            contentScale = ContentScale.FillHeight,
        )
    }
}

@Composable
fun ThreadAuthor(author: IBaseAuthor) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = author.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (author.userHash.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = author.userHash,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = author.now,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
                HorizontalDivider(
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
        ThreadAuthor(reply)

        Spacer(modifier = Modifier.height(4.dp))

        // 回复内容
        HtmlText(
            text = reply.content,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
@androidx.compose.desktop.ui.tooling.preview.Preview
fun PreviewReplyItem(reply: Reply) {

}
