package ai.saniou.forum.ui.components

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.theme.Dimens
import ai.saniou.thread.data.source.nmb.remote.dto.IBaseThread
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight


@Composable
fun TopicCard(
    thread: IBaseThread,
    onClick: () -> Unit,
    onImageClick: ((Image) -> Unit)? = null,
    onUserClick: ((String) -> Unit)? = null,
    showChannelBadge: Boolean = true,
) {
    TopicCard(
        thread.toDomain(),
        onClick,
        onImageClick,
        onUserClick,
        showChannelBadge
    )
}

/**
 * 串内容卡片，遵循 MD3 设计风格
 */
@Composable
fun TopicCard(
    topic: Topic,
    onClick: () -> Unit,
    onImageClick: ((Image) -> Unit)? = null,
    onUserClick: ((String) -> Unit)? = null,
    showChannelBadge: Boolean = true,
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
                    author = topic.author,
                    threadTime = topic.createdAt.toRelativeTimeString(),
                    isPo = false,
                    onClick = onUserClick,
                    modifier = Modifier.weight(1f)
                )

                // Badges (Admin, Sage)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (topic.isAdmin) {
                        Badge(
                            text = "ADMIN",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    if (topic.isSage) {
                        Badge(
                            text = "SAGE",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Title (Only if meaningful)
            if (!topic.title.isNullOrBlank() && topic.title != "无标题") {
                Text(
                    text = topic.title!!,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Content
            ThreadBody(
                content = topic.content,
                images = topic.images,
                maxLines = 6,
                onImageClick = { img -> onImageClick?.invoke(img) }
            )

            // Footer: Forum Name & Stats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Source & Forum Name Badge
                if (showChannelBadge) {
                    val sourceText =
                        if (topic.sourceName.isNotBlank()) "${topic.sourceName.uppercase()} · " else ""
                    val footerText = "$sourceText${topic.channelName}"

                    if (footerText.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                text = footerText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = Dimens.padding_small,
                                    vertical = Dimens.padding_tiny
                                )
                            )
                        }
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
                        text = topic.commentCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Recent Replies
            if (topic.comments.isNotEmpty()) {
                RecentReplies(topic.comments)
                if ((topic.remainingCount ?: 0) > 0) {
                    Text(
                        text = "还有 ${topic.remainingCount} 条回复...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            start = Dimens.padding_small,
                            top = Dimens.padding_tiny
                        )
                    )
                }
            }
        }
    }
}


