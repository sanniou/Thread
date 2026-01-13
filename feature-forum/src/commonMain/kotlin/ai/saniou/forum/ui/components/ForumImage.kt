package ai.saniou.forum.ui.components

import ai.saniou.coreui.widgets.NetworkImage
import ai.saniou.thread.domain.model.forum.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale

/**
 * 论坛图片加载组件
 *
 * @param image 图片对象
 * @param isThumb 是否为缩略图
 * @param modifier 修饰符
 * @param contentDescription 内容描述
 * @param contentScale 图片缩放模式
 */
@Composable
fun ForumImage(
    image: Image,
    isThumb: Boolean = true,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    // 直接使用 Domain 对象中的 URL
    val imageUrl = if (isThumb) image.thumbnailUrl else image.originalUrl

    // 简单的点击缩放反馈 (模拟共享元素过渡的起始状态)
    // 注意：真正的 Shared Element Transition 在 Compose Multiplatform 中目前支持有限，
    // 这里使用缩放动画作为替代，提供触觉反馈。
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100)
    )

    NetworkImage(
        imageUrl = imageUrl,
        modifier = modifier.scale(scale),
        contentDescription = contentDescription,
        contentScale = contentScale,
    )
}