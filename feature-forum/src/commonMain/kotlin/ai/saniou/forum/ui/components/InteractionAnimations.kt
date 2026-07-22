package ai.saniou.forum.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.theme.LocalThreadUiPreferences
import ai.saniou.coreui.theme.ThreadMotion
import ai.saniou.coreui.theme.threadTween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Quiet press feedback for icon actions.
 * Honors reducedMotion — no bounce/spring toy feel.
 */
@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedIcon: ImageVector? = null,
    isSelected: Boolean = false,
    selectedTint: Color = MaterialTheme.colorScheme.primary,
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val reduced = LocalThreadUiPreferences.current.reducedMotion
    val pressScale = ThreadMotion.cardPressScale
    val pressIn = threadTween(ThreadMotion.FastMs)
    val pressOut = threadTween(ThreadMotion.MediumMs)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                if (!reduced) {
                    scope.launch {
                        scale.animateTo(pressScale, animationSpec = pressIn)
                        scale.animateTo(1f, animationSpec = pressOut)
                    }
                }
                onClick()
            }
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isSelected && selectedIcon != null) selectedIcon else icon,
            contentDescription = contentDescription,
            tint = if (isSelected) selectedTint else tint,
            modifier = Modifier
                .size(Dimens.icon_size_medium)
                .scale(scale.value),
        )
    }
}

/**
 * Modern like chip — primary tint, soft pill, no hard-coded pink.
 */
@Composable
fun LikeButton(
    isLiked: Boolean,
    count: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isLiked) active.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Like",
            tint = if (isLiked) active else inactive,
            modifier = Modifier.size(Dimens.icon_size_medium),
        )
        if (count != null && count > 0) {
            Text(
                text = formatCompactCount(count),
                style = MaterialTheme.typography.labelMedium,
                color = if (isLiked) active else inactive,
            )
        }
    }
}

/**
 * Modern dislike chip — muted, quiet product feel.
 */
@Composable
fun DislikeButton(
    count: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
            contentDescription = "Dislike",
            tint = color,
            modifier = Modifier.size(Dimens.icon_size_medium),
        )
        if (count != null && count > 0) {
            Text(
                text = formatCompactCount(count),
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}

private fun formatCompactCount(count: Long): String = when {
    count >= 10_000 -> "${count / 1000}k"
    count >= 1_000 -> {
        val whole = count / 1000
        val frac = (count % 1000) / 100
        if (frac == 0L) "${whole}k" else "$whole.${frac}k"
    }
    else -> count.toString()
}
