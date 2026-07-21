package ai.saniou.forum.ui.components

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.empty_title
import thread.feature_forum.generated.resources.s_0f666abf00
import thread.feature_forum.generated.resources.s_3363bd3488
import thread.feature_forum.generated.resources.s_b6543d7470

@Composable
fun TopicCard(
    topic: Topic,
    onClick: () -> Unit,
    onImageClick: ((Image) -> Unit)? = null,
    onUserClick: ((String) -> Unit)? = null,
    showChannelBadge: Boolean = true,
    modifier: Modifier = Modifier,
) {
    ThreadCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
            TopicMetaSection(
                topic = topic,
                onUserClick = onUserClick,
                showChannelBadge = showChannelBadge
            )

            topic.title?.takeIf { it.isNotBlank() && it != stringResource(Res.string.empty_title) }?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            CollapsibleThreadBody(
                content = topic.content,
                images = topic.images,
                collapsedLines = 6,
                onImageClick = { img -> onImageClick?.invoke(img) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.clickable(onClick = onClick)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = Dimens.padding_medium,
                            vertical = Dimens.padding_tiny
                        ),
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
                            text = stringResource(Res.string.s_b6543d7470, topic.commentCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (topic.comments.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.padding_small),
                        verticalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)
                    ) {
                        RecentReplies(topic.comments.take(2))
                        if (topic.commentCount > 0) {
                            Text(
                                text = stringResource(Res.string.s_3363bd3488, topic.commentCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { onClick() }
                            )
                        }
                    }
                }
            }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicMetaSection(
    topic: Topic,
    onUserClick: ((String) -> Unit)?,
    showChannelBadge: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)
        ) {
            if (showChannelBadge) {
                val channelText =
                    if (topic.sourceName.isNotBlank()) "${topic.sourceName} · ${topic.channelName}" else topic.channelName
                Text(
                    text = channelText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)
        ) {
            Text(
                text = topic.author.name.takeIf { it.isNotBlank() && it != stringResource(Res.string.s_0f666abf00) } ?: topic.author.id,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable(enabled = onUserClick != null) {
                    onUserClick?.invoke(topic.author.id)
                }
            )

            Text(
                text = "· ${topic.createdAt.toRelativeTimeString()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (topic.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny),
                verticalArrangement = Arrangement.spacedBy(Dimens.padding_tiny),
            ) {
                topic.tags.forEach { tag ->
                    Badge(
                        text = tag.name,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}
