package ai.saniou.coreui.widgets

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.theme.threadTweenSpec
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp

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
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimens.padding_medium,
                        vertical = Dimens.padding_small,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    Dimens.padding_tiny,
                    Alignment.CenterHorizontally,
                ),
            ) {
                actions.forEach { action ->
                    if (action.emphasized) {
                        SaniouButton(
                            onClick = action.onClick,
                            enabled = action.enabled,
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = Dimens.padding_tiny),
                            )
                        }
                    } else {
                        SaniouTextButton(onClick = action.onClick, enabled = action.enabled) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.label,
                                tint = if (!action.enabled) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (!action.enabled) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(start = Dimens.padding_tiny),
                            )
                        }
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
