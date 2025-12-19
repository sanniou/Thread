package ai.saniou.forum.ui.components

import ai.saniou.coreui.widgets.NetworkImage
import ai.saniou.forum.di.nmbdi
import ai.saniou.thread.data.manager.CdnManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.kodein.di.instance

/**
 * NMB图片加载组件
 *
 * @param imgPath 图片路径
 * @param ext 图片扩展名
 * @param modifier 修饰符
 * @param isThumb 是否为缩略图
 * @param contentDescription 内容描述
 * @param autosize 是否在帖子详情页面中显示，如果为true则使用ContentScale.FillWidth显示图片，否则使用ContentScale.Crop裁剪显示
 * @param onClick 点击图片回调，如果为null则不可点击
 */
@Composable
fun NmbImage(
    imgPath: String,
    ext: String,
    isThumb: Boolean = true,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    // 获取CDN管理器
    val cdnManager by nmbdi.instance<CdnManager>()
    val coroutineScope = rememberCoroutineScope()

    // 构建完整的图片URL
    val imageUrl = if (imgPath.startsWith("http")) {
        imgPath
    } else {
        cdnManager.buildImageUrl(imgPath, ext, isThumb)
    }

    // 记住是否正在重试
    var isRetrying by remember { mutableStateOf(false) }

    NetworkImage(
        imageUrl = imageUrl,
        modifier = modifier,
        contentDescription = contentDescription,
        contentScale = contentScale,
    )
}
