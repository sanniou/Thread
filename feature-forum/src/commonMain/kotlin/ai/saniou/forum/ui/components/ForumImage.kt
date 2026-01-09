package ai.saniou.forum.ui.components

import ai.saniou.coreui.widgets.NetworkImage
import ai.saniou.thread.domain.model.forum.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

    NetworkImage(
        imageUrl = imageUrl,
        modifier = modifier,
        contentDescription = contentDescription,
        contentScale = contentScale,
    )
}