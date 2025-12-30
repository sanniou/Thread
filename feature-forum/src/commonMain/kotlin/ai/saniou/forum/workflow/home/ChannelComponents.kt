package ai.saniou.forum.workflow.home

import ai.saniou.coreui.theme.Dimens
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.repository.Source
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.forum_favorite_add
import thread.feature_forum.generated.resources.forum_favorite_remove
import thread.feature_forum.generated.resources.forum_limited_time

fun String.toColor(): Color {
    if (this.isBlank()) return Color.Gray
    return try {
        Color(("FF$this").toLong(16))
    } catch (e: Exception) {
        Color.Gray
    }
}

/**
 * A scrollable, animated source selector.
 */
@Composable
fun AnimatedSourceSelector(
    sources: List<Source>,
    currentSourceId: String,
    onSourceSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = Dimens.padding_medium,
            vertical = Dimens.padding_small
        ),
        horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small)
    ) {
        items(sources) { source ->
            SourceChip(
                source = source,
                isSelected = currentSourceId == source.id,
                onClick = { onSourceSelected(source.id) }
            )
        }
    }
}

@Composable
private fun SourceChip(
    source: Source,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = source.name,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * A redesigned Forum Item with better typography and metadata badges.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StylizedForumItem(
    forum: Channel,
    isSelected: Boolean,
    isFavorite: Boolean,
    onForumClick: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    indentLevel: Int = 0
) {
    val backgroundColor = if (isSelected)
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    else
        Color.Transparent

    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    val indentPadding = 16.dp * indentLevel

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Dimens.padding_small + indentPadding, end = Dimens.padding_small, top = 2.dp, bottom = 2.dp)
                .clip(RoundedCornerShape(Dimens.corner_radius_medium))
                .background(backgroundColor)
                .clickable { onForumClick(forum) }
                .padding(Dimens.padding_medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Forum Icon / Avatar Placeholder
            if (forum.color != null) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 16.dp)
                        .background(forum.color!!.toColor(), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (forum.emoji != null) {
                        Text(text = forum.emoji!!, modifier = Modifier.padding(end = 4.dp))
                    }
                    Text(
                        text = if (forum.displayName.isNullOrBlank()) forum.name else forum.displayName!!,
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    )

                    if (forum.autoDelete != null && forum.autoDelete!! > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(Res.string.forum_limited_time),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Metadata Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (forum.topicCount != null) {
                        MetadataBadge(text = "${forum.topicCount} 串")
                    }
                    if (forum.postCount != null) {
                        MetadataBadge(text = "${forum.postCount} 帖")
                    }

                    // Clean msg: prefer descriptionText, fallback to stripped description
                    val cleanMsg = forum.descriptionText ?: forum.description.replace(Regex("<[^>]*>"), "").trim()
                    if (cleanMsg.isNotBlank()) {
                        Text(
                            text = cleanMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            IconButton(
                onClick = { onFavoriteToggle(forum) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) stringResource(Res.string.forum_favorite_remove) else stringResource(
                        Res.string.forum_favorite_add
                    ),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                        alpha = 0.5f
                    ),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Render Children
        if (forum.children.isNotEmpty()) {
            if (forum.listViewStyle == "boxes") {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Dimens.padding_small + indentPadding + 12.dp, end = Dimens.padding_small, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    forum.children.forEach { child ->
                        SubCategoryBoxItem(
                            forum = child,
                            onClick = { onForumClick(child) }
                        )
                    }
                }
            } else {
                // Default list style (recursive)
                forum.children.forEach { child ->
                    StylizedForumItem(
                        forum = child,
                        isSelected = isSelected, // Logic might need adjustment if child selection is independent
                        isFavorite = isFavorite, // Logic might need adjustment
                        onForumClick = onForumClick,
                        onFavoriteToggle = onFavoriteToggle,
                        modifier = modifier,
                        indentLevel = indentLevel + 1
                    )
                }
            }
        }
    }
}

@Composable
fun SubCategoryBoxItem(
    forum: Channel,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.width(140.dp).height(80.dp) // Fixed size for boxes
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (forum.color != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(forum.color!!.toColor(), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = forum.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (forum.topicCount != null) {
                Text(
                    text = "${forum.topicCount} 串",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetadataBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.9f
        )
    }
}

/**
 * A glassmorphic container for the drawer content.
 */
@Composable
fun GlassmorphicDrawerContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        // Blur effect layer (Simulated with semi-transparent surface for now as Blur is expensive/platform dependent)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        )

        // Content
        content()
    }
}
