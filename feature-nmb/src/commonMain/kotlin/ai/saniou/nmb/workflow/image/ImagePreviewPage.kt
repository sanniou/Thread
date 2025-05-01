package ai.saniou.nmb.workflow.image

import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.di.nmbdi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
 * 图片预览页面
 *
 * 支持图片缩放、查看上一张/下一张、保存图片等功能
 *
 * @param imgPath 图片路径
 * @param ext 图片扩展名
 * @param onNavigateBack 返回回调
 * @param onSaveImage 保存图片回调
 * @param hasNext 是否有下一张图片
 * @param hasPrevious 是否有上一张图片
 * @param onNextImage 下一张图片回调
 * @param onPreviousImage 上一张图片回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewPage(
    imgPath: String,
    ext: String,
    onNavigateBack: () -> Unit,
    onSaveImage: (String, String) -> Unit,
    hasNext: Boolean = false,
    hasPrevious: Boolean = false,
    onNextImage: () -> Unit = {},
    onPreviousImage: () -> Unit = {}
) {
    // 获取CDN管理器
    val cdnManager by nmbdi.instance<CdnManager>()
    val coroutineScope = rememberCoroutineScope()

    // 构建完整的图片URL
    val imageUrl = cdnManager.buildImageUrl(imgPath, ext, false)

    // 缩放状态
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 创建可变换状态
    val transformableState = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        // 更新缩放比例，限制在0.5到5倍之间
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        // 更新旋转角度
        rotation += rotationChange
        // 更新偏移量
        offset += offsetChange
    }

    // 记住是否正在重试
    var isRetrying by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图片预览") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 保存图片按钮
                    IconButton(onClick = { onSaveImage(imgPath, ext) }) {
                        Icon(Icons.Default.Place, contentDescription = "保存图片")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // 图片显示区域
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "预览图片",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        rotationZ = rotation,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = transformableState),
                loading = {
                    // 加载中状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                    }
                },
                error = {
                    // 加载失败状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "加载失败",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        // 切换CDN地址
                                        cdnManager.switchToNextCdn()
                                        // 触发重新加载
                                        isRetrying = !isRetrying
                                    }
                                }
                            ) {
                                Text("点击重试")
                            }
                        }
                    }
                }
            )

            // 底部导航按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 上一张按钮
                FloatingActionButton(
                    onClick = onPreviousImage,
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
//                    enabled = hasPrevious
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一张")
                }

                // 重置缩放按钮
                FloatingActionButton(
                    onClick = {
                        // 重置所有变换
                        scale = 1f
                        rotation = 0f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text("重置")
                }

                // 下一张按钮
                FloatingActionButton(
                    onClick = onNextImage,
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
//                    enabled = hasNext
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一张")
                }
            }
        }
    }
}
