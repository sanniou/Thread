package ai.saniou.nmb.ui.components

import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.di.nmbdi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
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
    modifier: Modifier = Modifier,
    isThumb: Boolean = true,
    contentDescription: String? = null,
    autosize: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    // 获取CDN管理器
    val cdnManager by nmbdi.instance<CdnManager>()
    val coroutineScope = rememberCoroutineScope()

    // 构建完整的图片URL
    val imageUrl = cdnManager.buildImageUrl(imgPath, ext, isThumb)

    // 记住是否正在重试
    var isRetrying by remember { mutableStateOf(false) }

    // 准备修饰符
    var boxModifier = modifier
        .clip(RoundedCornerShape(4.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))

    // 如果提供了点击回调，添加点击修饰符
    if (onClick != null) {
        boxModifier = boxModifier.clickable { onClick() }
    }

    Box(modifier = boxModifier) {
        // 使用SubcomposeAsyncImage可以自定义加载、错误和成功状态
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(imageUrl)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = contentDescription,
            contentScale = if (autosize) ContentScale.FillWidth else ContentScale.Crop,
            modifier = if (autosize) {
                // 自适应高度时，使用wrapContentHeight
                Modifier.fillMaxWidth()
            } else {
                // 固定高度时，使用fillMaxSize
                Modifier.fillMaxSize()
            },
            loading = {
                // 加载中状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                // 加载失败状态
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            // 点击重试
                            coroutineScope.launch {
                                // 切换CDN地址
                                cdnManager.switchToNextCdn()
                                // 触发重新加载
                                isRetrying = !isRetrying
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "加载失败",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )

                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "点击重试",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    )

                    Text(
                        text = "加载失败，点击重试",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        )
    }
}
