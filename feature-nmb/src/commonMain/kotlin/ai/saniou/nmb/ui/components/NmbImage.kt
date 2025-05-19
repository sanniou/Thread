package ai.saniou.nmb.ui.components

import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.di.nmbdi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageOptions
import com.github.panpf.sketch.state.ColorPainterStateImage
import kotlinx.coroutines.launch
import org.kodein.di.instance

/**
 * NMB图片加载组件
 *
 * 使用Coil 3.1.0实现的图片加载组件
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
    val imageUrl = cdnManager.buildImageUrl(imgPath, ext, isThumb)

    // 记住是否正在重试
    var isRetrying by remember { mutableStateOf(false) }

    ImageComponent(
        imageUrl = imageUrl,
        modifier = modifier,
        contentDescription = contentDescription,
        contentScale = contentScale,
        onRetry = {
            // 点击重试
            coroutineScope.launch {
                // 切换CDN地址
                cdnManager.switchToNextCdn()
                // 触发重新加载
                isRetrying = !isRetrying
            }
        },
    )
}

@Composable
fun ImageComponent(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Fit,
    onRetry: (() -> Unit)? = null
) {

    AsyncImage(
        uri = imageUrl,
        state = rememberAsyncImageState(ComposableImageOptions {
            placeholder(ColorPainterStateImage(MaterialTheme.colorScheme.primaryContainer))
            error(ColorPainterStateImage(MaterialTheme.colorScheme.errorContainer))
            crossfade()
            // There is a lot more...
        }),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
