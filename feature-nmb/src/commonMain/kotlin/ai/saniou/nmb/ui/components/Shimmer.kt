package ai.saniou.nmb.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

// Shimmer.kt
@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(0.30f),
        MaterialTheme.colorScheme.surfaceVariant.copy(0.50f),
        MaterialTheme.colorScheme.surfaceVariant.copy(0.30f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        0f, 1000f,
        animationSpec = infiniteRepeatable(
            tween(1200, 300), RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    return remember(offset) {
        Brush.linearGradient(
            shimmerColors,
            start = Offset.Zero,
            end = Offset(offset, offset)
        )
    }
}


@Composable
fun ShimmerContainer(
    modifier: Modifier = Modifier,
    content: @Composable (Brush) -> Unit
) {
    val brush = rememberShimmerBrush()
    Box(modifier) { content(brush) }
}


