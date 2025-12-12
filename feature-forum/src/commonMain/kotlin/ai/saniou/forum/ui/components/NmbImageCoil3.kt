//package ai.saniou.nmb.ui.components
//
//import ai.saniou.thread.data.manager.CdnManager
//import ai.saniou.nmb.di.nmbdi
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.defaultMinSize
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.filled.Refresh
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.painter.ColorPainter
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.unit.dp
//import coil3.compose.AsyncImage
//import coil3.compose.AsyncImagePainter
//import coil3.compose.LocalPlatformContext
//import coil3.request.CachePolicy
//import coil3.request.ImageRequest
//import coil3.request.crossfade
//import kotlinx.coroutines.launch
//import org.kodein.di.instance
//
///**
// * NMB图片加载组件
// *
// * 使用Coil 3.1.0实现的图片加载组件
// *
// * @param imgPath 图片路径
// * @param ext 图片扩展名
// * @param modifier 修饰符
// * @param isThumb 是否为缩略图
// * @param contentDescription 内容描述
// * @param autosize 是否在帖子详情页面中显示，如果为true则使用ContentScale.FillWidth显示图片，否则使用ContentScale.Crop裁剪显示
// * @param onClick 点击图片回调，如果为null则不可点击
// */
//@Composable
//fun NmbImage(
//    imgPath: String,
//    ext: String,
//    isThumb: Boolean = true,
//    modifier: Modifier = Modifier,
//    contentDescription: String? = null,
//    contentScale: ContentScale = ContentScale.Fit,
//    onClick: (() -> Unit)? = null,
//) {
//    // 获取CDN管理器
//    val cdnManager by nmbdi.instance<CdnManager>()
//    val coroutineScope = rememberCoroutineScope()
//
//    // 构建完整的图片URL
//    val imageUrl = cdnManager.buildImageUrl(imgPath, ext, isThumb)
//
//    // 记住是否正在重试
//    var isRetrying by remember { mutableStateOf(false) }
//
//    ImageComponent(
//        imageUrl = imageUrl,
//        modifier = modifier,
//        contentDescription = contentDescription,
//        contentScale = contentScale,
//        onRetry = {
//            // 点击重试
//            coroutineScope.launch {
//                // 切换CDN地址
//                cdnManager.switchToNextCdn()
//                // 触发重新加载
//                isRetrying = !isRetrying
//            }
//        },
//        onClick = onClick
//    )
//}
//
//@Composable
//fun ImageComponent(
//    imageUrl: String,
//    modifier: Modifier = Modifier,
//    contentDescription: String?,
//    contentScale: ContentScale = ContentScale.Fit,
//    onRetry: (() -> Unit)? = null,
//    onClick: (() -> Unit)? = null,
//) {
//    // 准备修饰符
//    var finalModifier = modifier
//        .clip(RoundedCornerShape(4.dp))
//        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
//        .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
//
//    // 如果提供了点击回调，添加点击修饰符
//    if (onClick != null) {
//        finalModifier = finalModifier.clickable { onClick() }
//    }
//
//    // 使用 Box 自定义加载、错误和成功状态
//    var state by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
//    Box(
//        modifier = Modifier.defaultMinSize(minHeight = 100.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        AsyncImage(
//            modifier = finalModifier,
//            model = ImageRequest.Builder(LocalPlatformContext.current)
//                .data(imageUrl)
//                .crossfade(true)
//                .memoryCachePolicy(CachePolicy.ENABLED)
//                .diskCachePolicy(CachePolicy.ENABLED)
//                .build(),
//            contentDescription = contentDescription,
//            contentScale = contentScale,
//            placeholder = ColorPainter(MaterialTheme.colorScheme.primaryContainer),
//            error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
//            onLoading = { state = it },
//            onSuccess = { state = it },
//            onError = { state = it },
//        )
//        when (state) {
//            is AsyncImagePainter.State.Success, is AsyncImagePainter.State.Loading, is AsyncImagePainter.State.Empty -> {
//
//            }
//
//            is AsyncImagePainter.State.Error -> {
//
//                // 加载失败状态
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .apply {
//                            onRetry?.let {
//                                this.clickable(onClick = it)
//                            }
//                        },
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Delete,
//                        contentDescription = "加载失败",
//                        tint = MaterialTheme.colorScheme.error,
//                        modifier = Modifier.size(48.dp)
//                    )
//
//                    Icon(
//                        imageVector = Icons.Default.Refresh,
//                        contentDescription = "点击重试",
//                        tint = MaterialTheme.colorScheme.primary,
//                        modifier = Modifier
//                            .size(24.dp)
//                            .align(Alignment.BottomEnd)
//                            .padding(4.dp)
//                    )
//
//                    Text(
//                        text = "加载失败，点击重试",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.error,
//                        modifier = Modifier.align(Alignment.BottomCenter)
//                    )
//                }
//            }
//
//        }
//    }
//}
