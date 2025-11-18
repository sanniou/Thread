package ai.saniou.nmb.workflow.image

import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.di.nmbdi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ai.saniou.coreui.widgets.ZoomAsyncImage
import org.kodein.di.DI
import org.kodein.di.compose.viewmodel.rememberViewModel
import org.kodein.di.instance
import kotlin.coroutines.cancellation.CancellationException

/**
 * 图片预览页面
 *
 * 支持图片缩放、查看上一张/下一张、保存图片等功能
 * 支持双击放大功能
 *
 * @param imgPath 图片路径
 * @param ext 图片扩展名
 * @param onSaveImage 保存图片回调
 * @param hasNext 是否有下一张图片
 * @param hasPrevious 是否有上一张图片
 * @param onNextImage 下一张图片回调
 * @param onPreviousImage 上一张图片回调
 * @param onUpdateTitle 更新标题回调
 * @param onSetupMenuButton 设置菜单按钮回调
 */
data class ImagePreviewPage(
    val imgPath: String,
    val ext: String,
    val di: DI = nmbdi,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val onNextImage: () -> Unit = {},
    val onPreviousImage: () -> Unit = {},
    val onUpdateTitle: ((String) -> Unit)? = null,
    val onSetupMenuButton: ((@Composable () -> Unit) -> Unit)? = null,
) : Screen {

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        PredictiveBackHandler { progress ->
            try {
                progress.collect { backEvent ->

                }
                navigator.pop()
            } catch (e: CancellationException) {
//                callback.cancelPop()
            }
        }

        val imagePreviewViewModel: ImagePreviewViewModel by rememberViewModel()

        // 获取CDN管理器
        val cdnManager by nmbdi.instance<CdnManager>()
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
                IconButton(onClick = {
                    // 保存图片
                    imagePreviewViewModel.setCurrentImage(imgPath, ext)
                    imagePreviewViewModel.saveCurrentImage()
                }) {
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
            ZoomAsyncImage(
                uri = imageUrl,
                contentDescription = "预览图片",
                modifier = Modifier.fillMaxSize(),
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
