package ai.saniou.forum.ui.components

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.theme.Dimens
import ai.saniou.thread.data.source.nmb.remote.dto.IBaseThread
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.forum.Topic as Post
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


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
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(Dimens.padding_standard),
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_medium)
        ) {
            // Header: Author & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                ThreadAuthor(
                    userName = thread.userHash,
                    showName = thread.name,
                    threadTime = thread.createdAt.toRelativeTimeString(),
                    isPo = false,
                    onClick = onUserClick,
                    modifier = Modifier.weight(1f)
                )

                // Badges (Admin, Sage)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (thread.isAdmin) {
                        Badge(
                            text = "ADMIN",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    if (thread.isSage) {
                        Badge(
                            text = "SAGE",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Title (Only if meaningful)
            if (thread.title.isNotBlank() && thread.title != "无标题") {
                Text(
                    text = thread.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Content
            ThreadBody(
                content = thread.content,
                img = thread.img,
                ext = thread.ext,
                maxLines = 6,
                onImageClick = { img, ext -> onImageClick?.invoke(img, ext) }
            )

            // Footer: Forum Name & Stats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Source & Forum Name Badge
                val sourceText = if (thread.sourceName.isNotBlank()) "${thread.sourceName.uppercase()} · " else ""
                val footerText = "$sourceText${thread.forumName}"

                if (footerText.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = footerText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Dimens.padding_small, vertical = Dimens.padding_tiny)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Reply Count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Replies",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.icon_size_small)
                    )
                    Text(
                        text = thread.replyCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Recent Replies
            if (!thread.replies.isNullOrEmpty()) {
                RecentReplies(thread.replies!!)
                if ((thread.remainReplies ?: 0) > 0) {
                    Text(
                        text = "还有 ${thread.remainReplies} 条回复...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = Dimens.padding_small, top = Dimens.padding_tiny)
                    )
                }
            }
        }
    }
}


