package ai.saniou.nmb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * NMB图片加载组件
 *
 * 由于我们还没有实现实际的图片加载功能，这个组件目前只是一个占位符。
 * 在实际应用中，你需要使用平台特定的图片加载库，如Coil、Glide或Ktor Client。
 */
@Composable
fun NmbImage(
    cdnUrl: String,
    imgPath: String,
    ext: String,
    modifier: Modifier = Modifier,
    isThumb: Boolean = true,
    contentDescription: String? = null
) {
    // 图片加载状态
    var loadState by remember { mutableStateOf(ImageLoadState.LOADING) }

    // 构建完整的图片URL
    val imageUrl = buildImageUrl(cdnUrl, imgPath, ext, isThumb)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
    ) {
        when (loadState) {
            ImageLoadState.LOADING -> {
                // 加载中状态
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )

                // 模拟加载完成
                // 在实际应用中，这里应该是图片加载库的回调
                loadState = ImageLoadState.SUCCESS
            }

            ImageLoadState.SUCCESS -> {
                // 加载成功状态
                // 在实际应用中，这里应该显示实际加载的图片
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = contentDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "图片预览",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            ImageLoadState.ERROR -> {
                // 加载失败状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "加载失败",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "加载失败",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

/**
 * 构建完整的图片URL
 */
private fun buildImageUrl(cdnUrl: String, imgPath: String, ext: String, isThumb: Boolean): String {
    val baseUrl = cdnUrl.removeSuffix("/")
    val path = if (isThumb) "thumb" else "image"
    return "$baseUrl/$path/$imgPath$ext"
}

/**
 * 图片加载状态
 */
enum class ImageLoadState {
    LOADING,
    SUCCESS,
    ERROR
}
