package ai.saniou.nmb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.seiko.imageloader.ui.AutoSizeImage

/**
 * Drawer 头部组件，显示欢迎图片
 * 作为背景层使用，其他内容可以叠加在上面
 *
 * @param imageUrl 图片URL
 * @param modifier 修饰符
 */
@Composable
fun DrawerHeader(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // 背景图片
            if (imageUrl != null) {
                AutoSizeImage(
                    url = imageUrl,
                    contentDescription = "欢迎图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
//                SubcomposeAsyncImage(
//                    model = ImageRequest.Builder(LocalPlatformContext.current)
//                        .data(imageUrl)
//                        .crossfade(true)
//                        .memoryCachePolicy(CachePolicy.ENABLED)
//                        .diskCachePolicy(CachePolicy.ENABLED)
//                        .build(),
//                    contentDescription = "欢迎图片",
//                    contentScale = ContentScale.Crop, // 使用Crop而非FillWidth，确保图片填满区域
//                    modifier = Modifier.fillMaxSize()
//                )

                // 添加渐变遮罩，使图片与下方内容过渡更自然
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.1f)
                                )
                            )
                        )
                )
            } else {
                // 如果没有图片，显示纯色背景
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }
        }
    }
}
