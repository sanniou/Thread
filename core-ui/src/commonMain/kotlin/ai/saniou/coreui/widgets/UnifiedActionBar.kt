package ai.saniou.coreui.widgets

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.theme.threadTweenSpec
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Shared bottom action strip for detail/read workflows.
 * Visual shell only — features own the action semantics.
 */
@Composable
fun UnifiedActionBar(
    visible: Boolean,
    actions: List<ActionItem>,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = threadTweenSpec()) +
            slideInVertically(animationSpec = threadTweenSpec()) { it / 2 },
        exit = fadeOut(animationSpec = threadTweenSpec()) +
            slideOutVertically(animationSpec = threadTweenSpec()) { it / 2 },
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(
                topStart = Dimens.corner_radius_extra_large,
                topEnd = Dimens.corner_radius_extra_large,
            ),
            tonalElevation = Dimens.padding_tiny,
            shadowElevation = Dimens.padding_tiny,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimens.padding_small,
                        vertical = Dimens.padding_small,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                actions.forEach { action ->
                    SaniouTextButton(onClick = action.onClick, enabled = action.enabled) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            tint = when {
                                !action.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                action.emphasized -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = when {
                                !action.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                action.emphasized -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(start = Dimens.padding_tiny),
                        )
                    }
                }
            }
        }
    }

    androidx.compose.foundation.layout.Spacer(
        modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars),
    )
}

data class ActionItem(
    val label: String,
    val icon: ImageVector,
    val emphasized: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)
