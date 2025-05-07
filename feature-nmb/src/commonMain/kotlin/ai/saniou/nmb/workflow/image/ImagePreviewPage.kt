package ai.saniou.nmb.workflow.image

import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.NmbImage
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.kodein.di.instance

/**
 * 图片预览页面
 *
 * 支持图片缩放、查看上一张/下一张、保存图片等功能
 * 支持双击放大功能
 *
 * @param imgPath 图片路径
 * @param ext 图片扩展名
 * @param onNavigateBack 返回回调
 * @param onSaveImage 保存图片回调
 * @param hasNext 是否有下一张图片
 * @param hasPrevious 是否有上一张图片
 * @param onNextImage 下一张图片回调
 * @param onPreviousImage 上一张图片回调
 * @param onUpdateTitle 更新标题回调
 * @param onSetupMenuButton 设置菜单按钮回调
 */
@Composable
fun ImagePreviewPage(
    imgPath: String,
    ext: String,
    onNavigateBack: () -> Unit,
    onSaveImage: (String, String) -> Unit,
    hasNext: Boolean = false,
    hasPrevious: Boolean = false,
    onNextImage: () -> Unit = {},
    onPreviousImage: () -> Unit = {},
    onUpdateTitle: ((String) -> Unit)? = null,
    onSetupMenuButton: ((@Composable () -> Unit) -> Unit)? = null
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
    val transformableState =
        rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            // 更新缩放比例，限制在0.5到5倍之间
            scale = (scale * zoomChange).coerceIn(0.5f, 5f)
            // 更新旋转角度
            rotation += rotationChange
            // 更新偏移量
            offset += offsetChange
        }

    // 记住是否正在重试
    var isRetrying by remember { mutableStateOf(false) }

    // 设置标题和菜单按钮
    LaunchedEffect(Unit) {
        // 设置标题
        onUpdateTitle?.invoke("图片预览")

        // 设置菜单按钮
        onSetupMenuButton?.invoke {
            // 保存图片按钮
            IconButton(onClick = { onSaveImage(imgPath, ext) }) {
                Icon(Icons.Default.Place, contentDescription = "保存图片")
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 图片显示区域
        NmbImage(
            imgPath, ext, false,
            contentDescription = "预览图片",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = rotation,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformableState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            // 双击放大/缩小功能
                            if (scale > 1.5f) {
                                // 如果当前已经放大，则重置为原始大小
                                scale = 1f
                                offset = Offset.Zero
                                rotation = 0f
                            } else {
                                // 否则放大到2.5倍
                                scale = 2.5f
                            }
                        }
                    )
                },
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
