package ai.saniou.forum.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 带微交互动画的图标按钮
 * 点击时会有缩放回弹效果
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
    selectedTint: Color = MaterialTheme.colorScheme.primary
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null // 禁用默认涟漪，使用自定义缩放
            ) {
                scope.launch {
                    scale.animateTo(
                        targetValue = 0.8f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    )
                    scale.animateTo(
                        targetValue = 1.2f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    )
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isSelected && selectedIcon != null) selectedIcon else icon,
            contentDescription = contentDescription,
            tint = if (isSelected) selectedTint else tint,
            modifier = Modifier.scale(scale.value)
        )
    }
}

/**
 * 带计数和动画的点赞按钮
 */
@Composable
fun LikeButton(
    isLiked: Boolean,
    count: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        AnimatedIconButton(
            onClick = onClick,
            icon = Icons.Outlined.FavoriteBorder,
            selectedIcon = Icons.Filled.Favorite,
            isSelected = isLiked,
            selectedTint = Color(0xFFE91E63), // Pink for like
            contentDescription = "Like",
            modifier = Modifier.size(20.dp)
        )
        if (count != null && count > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}