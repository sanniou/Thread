package ai.saniou.forum.workflow.home

import ai.saniou.coreui.theme.Dimens
import ai.saniou.thread.domain.model.forum.Forum
import ai.saniou.thread.domain.repository.Source
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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

/**
 * A scrollable, animated source selector.
 */
@Composable
fun AnimatedSourceSelector(
    sources: List<Source>,
    currentSourceId: String,
    onSourceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Dimens.padding_medium, vertical = Dimens.padding_small),
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
    onClick: () -> Unit
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
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
@Composable
fun StylizedForumItem(
    forum: Forum,
    isSelected: Boolean,
    isFavorite: Boolean,
    onForumClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected)
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    else
        Color.Transparent

    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.padding_small, vertical = 2.dp)
            .clip(RoundedCornerShape(Dimens.corner_radius_medium))
            .background(backgroundColor)
            .clickable(onClick = onForumClick)
            .padding(Dimens.padding_medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Forum Icon / Avatar Placeholder (Optional, can be added later)
        // For now, we use a colored bar or just spacing

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (forum.showName.isNullOrBlank()) forum.name else forum.showName!!,
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
                if (forum.threadCount != null) {
                    MetadataBadge(text = "${forum.threadCount} 串")
                }
                
                // Clean msg
                val cleanMsg = forum.msg.replace(Regex("<[^>]*>"), "").trim()
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
            onClick = onFavoriteToggle,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isFavorite) stringResource(Res.string.forum_favorite_remove) else stringResource(Res.string.forum_favorite_add),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
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
    content: @Composable () -> Unit
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