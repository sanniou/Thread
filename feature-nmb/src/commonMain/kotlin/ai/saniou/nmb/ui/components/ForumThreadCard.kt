package ai.saniou.nmb.ui.components

import ai.saniou.nmb.data.entity.Feed
import ai.saniou.nmb.data.entity.IBaseThread
import ai.saniou.coreui.widgets.RichText
import ai.saniou.nmb.data.entity.IBaseThreadReply
import ai.saniou.nmb.data.entity.IThreadBody
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
import ai.saniou.coreui.theme.Dimens

/**
 * 订阅卡片，对 ThreadCard 的封装
 */
@Composable
fun SubscriptionCard(
    feed: Feed,
    onClick: () -> Unit,
    onImageClick: ((String, String) -> Unit)? = null,
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
        Column(modifier = Modifier.padding(Dimens.padding_medium)) {
            ThreadCardHeader(thread)
            Spacer(modifier = Modifier.height(Dimens.padding_small))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThreadAuthor(thread)
                if (thread.sage > 0) {
                    Spacer(modifier = Modifier.width(Dimens.padding_small))
                    Text(
                        text = "SAGE",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.padding_small))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Dimens.padding_small))
            ThreadCardContent(thread, onImageClick)
            Spacer(modifier = Modifier.height(Dimens.padding_small))
            ThreadCardFooter(thread)
        }
    }
}

@Composable
private fun ThreadCardHeader(thread: IBaseThread) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small)
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
            .size(Dimens.icon_size_medium)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(Dimens.padding_extra_small),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "管理员",
            tint = Color.White,
            modifier = Modifier.size(Dimens.icon_size_small)
        )
    }
}

@Composable
private fun ThreadCardContent(
    thread: IBaseThread,
    onImageClick: ((String, String) -> Unit)?,
) {
    RichText(
        text = thread.content,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )

    if (thread.img.isNotBlank() && thread.ext.isNotBlank()) {
        Spacer(modifier = Modifier.height(Dimens.padding_small))
        NmbImage(
            imgPath = thread.img,
            ext = thread.ext,
            modifier = Modifier
                .height(Dimens.image_height_medium)
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
            modifier = Modifier.size(Dimens.icon_size_small)
        )
        Spacer(modifier = Modifier.width(Dimens.padding_extra_small))
        Text(
            text = thread.replyCount.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )

        threadReply?.run {
            if ((remainReplies ?: 0) > 0) {
                Spacer(modifier = Modifier.width(Dimens.padding_small))
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
        Spacer(modifier = Modifier.height(Dimens.padding_small))
        RecentReplies(threadReply.replies)
    }
}

@Composable
fun ThreadBody(
    body: IThreadBody,
    onReferenceClick: ((Long) -> Unit)? = null,
    onImageClick: (String, String) -> Unit,
) {
    RichText(
        text = body.content,
        style = MaterialTheme.typography.bodyMedium,
        onReferenceClick = { onReferenceClick?.invoke(it.toLong()) },
        referencePattern = ">>No\\.(\\d+)".toRegex()
    )

    if (body.img.isNotEmpty() && body.ext.isNotEmpty()) {
        Spacer(modifier = Modifier.height(Dimens.padding_small))
        NmbImage(
            imgPath = body.img,
            ext = body.ext,
            isThumb = true,
            contentDescription = "帖子图片",
            modifier = Modifier
                .height(Dimens.image_height_medium)
                .wrapContentWidth(Alignment.Start)
                .clickable { onImageClick(body.img, body.ext) },
            contentScale = ContentScale.FillHeight,
        )
    }
}
