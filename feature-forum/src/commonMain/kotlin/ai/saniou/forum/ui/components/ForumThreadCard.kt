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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.empty_title
import thread.feature_forum.generated.resources.label_anonymous
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
    val anonymous = stringResource(Res.string.label_anonymous)
    ThreadCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        TopicMetaSection(
            topic = topic,
            onUserClick = onUserClick,
            showChannelBadge = showChannelBadge,
            anonymous = anonymous,
        )

        topic.title?.takeIf { it.isNotBlank() && it != stringResource(Res.string.empty_title) }?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CollapsibleThreadBody(
            content = topic.content,
            images = topic.images,
            collapsedLines = 5,
            onImageClick = { img -> onImageClick?.invoke(img) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.clickable(onClick = onClick),
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = Dimens.padding_medium,
                        vertical = Dimens.padding_tiny + 2.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.icon_size_small),
                    )
                    Text(
                        text = stringResource(Res.string.s_b6543d7470, topic.commentCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (topic.images.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = Dimens.padding_medium,
                            vertical = Dimens.padding_tiny + 2.dp,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(Dimens.icon_size_small),
                        )
                        Text(
                            text = topic.images.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        if (topic.comments.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.padding_medium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.padding_small),
                ) {
                    RecentReplies(topic.comments.take(2))
                    if (topic.commentCount > 0) {
                        Text(
                            text = stringResource(Res.string.s_3363bd3488, topic.commentCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onClick() },
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
    anonymous: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)) {
        if (showChannelBadge) {
            val channelText =
                if (topic.sourceName.isNotBlank()) "${topic.sourceName} · ${topic.channelName}" else topic.channelName
            Text(
                text = channelText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny),
        ) {
            val authorLabel = topic.author.name.takeIf {
                it.isNotBlank() && it != anonymous
            } ?: topic.author.id
            Text(
                text = authorLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable(enabled = onUserClick != null) {
                        onUserClick?.invoke(topic.author.id)
                    },
            )

            Text(
                text = "· ${topic.createdAt.toRelativeTimeString()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
