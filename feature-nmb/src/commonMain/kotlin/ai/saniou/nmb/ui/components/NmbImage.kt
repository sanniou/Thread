package ai.saniou.nmb.ui.components

import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.di.nmbdi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import org.kodein.di.instance

/**
 * NMB图片加载组件
 *
 * 使用Coil 3.1.0实现的图片加载组件
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun NmbImage(
    cdnUrl: String = "",
    imgPath: String,
    ext: String,
    modifier: Modifier = Modifier,
    isThumb: Boolean = true,
    contentDescription: String? = null
) {
    // 获取CDN管理器
    val cdnManager by nmbdi.instance<CdnManager>()
    val currentCdnUrl by cdnManager.currentCdnUrl.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // 构建完整的图片URL
    val imageUrl = cdnManager.buildImageUrl(imgPath, ext, isThumb)

    // 记住是否正在重试
    var isRetrying by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
    ) {
        // 使用SubcomposeAsyncImage可以自定义加载、错误和成功状态
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(imageUrl)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
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

/**
 * 重试加载图片
 */
fun retryLoadImage(cdnManager: CdnManager) {
    // 切换到下一个CDN地址
    cdnManager.switchToNextCdn()
}
