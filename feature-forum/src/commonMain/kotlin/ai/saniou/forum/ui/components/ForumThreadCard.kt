package ai.saniou.forum.ui.components

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.theme.Dimens
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
import androidx.compose.ui.unit.dp


/**
 * 串内容卡片，遵循 Modern Reddit-style Card 设计风格
 * 侧重信息密度与清晰的视觉层级
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
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.padding_standard),
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_small) // Reduced spacing for density
        ) {
            // Header: Source/Channel + User + Time (Metadata Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small)
            ) {
                // Channel Avatar/Icon placeholder (Optional, can be added later)
                // For now, using Channel Name as the primary anchor
                if (showChannelBadge) {
                    val channelText =
                        if (topic.sourceName.isNotBlank()) "${topic.sourceName} · ${topic.channelName}" else topic.channelName
                    Text(
                        text = channelText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Author Info (Clickable)
                Text(
                    text = topic.author.name.takeIf { it.isNotBlank() && it != "无名氏" }
                        ?: topic.author.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable(enabled = onUserClick != null) {
                        onUserClick?.invoke(
                            topic.author.id
                        )
                    }
                )

                Text(
                    text = "· ${topic.createdAt.toRelativeTimeString()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.weight(1f))

                // Badges (Right Aligned)
                if (topic.isAdmin) {
                    Badge(
                        text = "ADMIN",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                if (topic.isSage) {
                    Badge(
                        text = "SAGE",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Title (Bold, Larger) - If exists
            if (!topic.title.isNullOrBlank() && topic.title != "无标题") {
                Text(
                    text = topic.title!!,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Body Content (Limited lines)
            ThreadBody(
                content = topic.content,
                images = topic.images,
                maxLines = 8, // Slightly more context
                onImageClick = { img -> onImageClick?.invoke(img) }
            )

            // Footer: Action Bar (Reply Count, Share, etc.)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.padding_small)
            ) {
                // Reply Action Pill
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.clickable { /* Trigger quick reply or just open detail */ }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Replies",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${topic.commentCount} 回复",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Add Share/More actions here if needed in future
            }

            // Recent Replies (Simplified)
            if (topic.comments.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        RecentReplies(topic.comments.take(2)) // Limit to 2 recent replies for card view
                        if ((topic.remainingCount ?: 0) > 0) {
                            Text(
                                text = "查看其余 ${topic.remainingCount} 条回复...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp).clickable { onClick() }
                            )
                        }
                    }
                }
            }
        }
    }
}


