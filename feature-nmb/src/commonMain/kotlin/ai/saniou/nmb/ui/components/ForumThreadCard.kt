package ai.saniou.nmb.ui.components

import ai.saniou.nmb.data.entity.Feed
import ai.saniou.nmb.data.entity.IBaseAuthor
import ai.saniou.nmb.data.entity.IBaseThread
import ai.saniou.nmb.data.entity.IBaseThreadReply
import ai.saniou.nmb.data.entity.IThreadBody
import ai.saniou.nmb.data.entity.Reply
import androidx.compose.desktop.ui.tooling.preview.Preview
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ElevatedCard
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

// 统一管理边距和尺寸
private val PADDING_MEDIUM = 12.dp
private val PADDING_SMALL = 8.dp
private val PADDING_EXTRA_SMALL = 4.dp
private val ICON_SIZE_SMALL = 16.dp
private val ICON_SIZE_MEDIUM = 20.dp
private val IMAGE_HEIGHT = 240.dp

/**
 * 订阅卡片，对 ThreadCard 的封装
 */
@Composable
fun SubscriptionCard(
    feed: Feed,
    onClick: () -> Unit,
    onImageClick: ((String, String) -> Unit)? = null,
    onUnsubscribe: () -> Unit, // onUnsubscribe 暂未实现
) {
    ThreadCard(
        thread = feed,
        onClick = onClick,
        onImageClick = onImageClick
    )
}

/**
 * 串内容卡片，遵循 MD3 设计风格
 */
@Composable
fun ThreadCard(
    thread: IBaseThread,
    onClick: () -> Unit,
    onImageClick: ((String, String) -> Unit)? = null,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(PADDING_MEDIUM)) {
            ThreadCardHeader(thread)
            Spacer(modifier = Modifier.height(PADDING_SMALL))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThreadAuthor(thread)
                if (thread.sage > 0) {
                    Spacer(modifier = Modifier.width(PADDING_SMALL))
                    Text(
                        text = "SAGE",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(PADDING_SMALL))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(PADDING_SMALL))
            ThreadCardContent(thread, onImageClick)
            Spacer(modifier = Modifier.height(PADDING_SMALL))
            ThreadCardFooter(thread)
        }
    }
}

@Composable
private fun ThreadCardHeader(thread: IBaseThread) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PADDING_SMALL)
    ) {
        if (thread.admin > 0) {
            AdminIcon()
        }
        Text(
            text = thread.title.ifBlank { stringResource(Res.string.empty_title) },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AdminIcon() {
    Box(
        modifier = Modifier
            .size(ICON_SIZE_MEDIUM)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(PADDING_EXTRA_SMALL),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "管理员",
            tint = Color.White,
            modifier = Modifier.size(ICON_SIZE_SMALL)
        )
    }
}

@Composable
private fun ThreadCardContent(
    thread: IBaseThread,
    onImageClick: ((String, String) -> Unit)?,
) {
    HtmlText(
        text = thread.content,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )

    if (thread.img.isNotBlank() && thread.ext.isNotBlank()) {
        Spacer(modifier = Modifier.height(PADDING_SMALL))
        NmbImage(
            imgPath = thread.img,
            ext = thread.ext,
            modifier = Modifier
                .height(IMAGE_HEIGHT)
                .clickable { onImageClick?.invoke(thread.img, thread.ext) },
            contentScale = ContentScale.FillHeight,
            isThumb = true,
            contentDescription = "帖子图片",
        )
    }
}

@Composable
private fun ThreadCardFooter(thread: IBaseThread) {
    val threadReply = thread as? IBaseThreadReply

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = "回复",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(ICON_SIZE_SMALL)
        )
        Spacer(modifier = Modifier.width(PADDING_EXTRA_SMALL))
        Text(
            text = thread.replyCount.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )

        threadReply?.run {
            if ((remainReplies ?: 0) > 0) {
                Spacer(modifier = Modifier.width(PADDING_SMALL))
                Text(
                    text = "(省略${remainReplies}条回复)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (replies.isNotEmpty()) {
                Text(
                    text = "查看全部",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (!threadReply?.replies.isNullOrEmpty()) {
        Spacer(modifier = Modifier.height(PADDING_SMALL))
        RecentReplies(threadReply.replies)
    }
}

@Composable
fun ThreadSpacer() {
    Spacer(modifier = Modifier.height(PADDING_SMALL))
}

@Composable
fun ThreadBody(
    body: IThreadBody,
    onReferenceClick: ((Long) -> Unit)? = null,
    onImageClick: (String, String) -> Unit,
) {
    HtmlText(
        text = body.content,
        style = MaterialTheme.typography.bodyMedium,
        onReferenceClick = onReferenceClick
    )

    if (body.img.isNotEmpty() && body.ext.isNotEmpty()) {
        Spacer(modifier = Modifier.height(PADDING_SMALL))
        NmbImage(
            imgPath = body.img,
            ext = body.ext,
            isThumb = true,
            contentDescription = "帖子图片",
            modifier = Modifier
                .height(IMAGE_HEIGHT)
                .wrapContentWidth(Alignment.Start)
                .clickable { onImageClick(body.img, body.ext) },
            contentScale = ContentScale.FillHeight,
        )
    }
}

@Composable
fun ThreadAuthor(author: IBaseAuthor) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PADDING_SMALL)
    ) {
        Text(
            text = author.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (author.userHash.isNotBlank()) {
            Text(
                text = author.userHash,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
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
            .clip(RoundedCornerShape(PADDING_EXTRA_SMALL))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(PADDING_SMALL),
        verticalArrangement = Arrangement.spacedBy(PADDING_EXTRA_SMALL)
    ) {
        replies.forEachIndexed { index, reply ->
            if (index > 0) {
                HorizontalDivider(
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
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PADDING_EXTRA_SMALL)
    ) {
        ThreadAuthor(reply)
        HtmlText(
            text = reply.content,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
@Preview
fun PreviewThreadCard() {
    val previewThread = object : IBaseThread, IBaseThreadReply {
        override val id: Long = 12345L
        override val replyCount: Long = 99L
        override val img: String = "/img/123.jpg"
        override val ext: String = ".jpg"
        override val now: String = "2023-10-27 10:00:00"
        override val userHash: String = "abcdef"
        override val name: String = "User Name"
        override val title: String = "这是一个非常长的标题，用于测试省略号的效果"
        override val content: String =
            "这是帖子的内容，也会很长很长很长，长到需要被截断并显示省略号...".repeat(5)
        override val sage: Long = 1
        override val fid: Long = 123L
        override val admin: Long = 1L
        override val hide: Long = 0L
        override val remainReplies: Long? = 5
        override val replies: List<Reply> = listOf(
            Reply(
                id = 1L,
                userHash = "fedcba",
                name = "ReplyUser1",
                content = "这是第一条回复。",
                now = "2023-10-27 10:05:00",
                fid = 1,
                replyCount = 99L,
                img = "/img/456.jpg",
                ext = ".jpg",
                title = "回复标题",
                sage = 0,
                admin = 0,
                hide = 0,
            ),
            Reply(
                id = 2L,
                userHash = "ghijkl",
                name = "ReplyUser2",
                content = "这是第二条回复，内容也可能很长。",
                now = "2023-10-27 10:10:00",
                fid = 1,
                replyCount = 99L,
                img = "/img/456.jpg",
                ext = ".jpg",
                title = "回复标题",
                sage = 0,
                admin = 0,
                hide = 0,
            )
        )
    }
    MaterialTheme {
        ThreadCard(thread = previewThread, onClick = {})
    }
}
