package ai.saniou.nmb.workflow.image

import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.di.nmbdi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ai.saniou.coreui.widgets.ZoomAsyncImage
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.coroutines.cancellation.CancellationException

/**
 * 图片预览页面
 *
 * 支持图片缩放、查看上一张/下一张、保存图片等功能
 * 支持双击放大功能
 *
 * @param threadId 帖子ID
 * @param imgPath 图片路径
 * @param ext 图片扩展名
 * @param onUpdateTitle 更新标题回调
 * @param onSetupMenuButton 设置菜单按钮回调
 */
data class ImagePreviewPage(
    val threadId: Long,
    val imgPath: String,
    val ext: String,
    val di: DI = nmbdi,
    val onUpdateTitle: ((String) -> Unit)? = null,
    val onSetupMenuButton: ((@Composable () -> Unit) -> Unit)? = null,
) : Screen {

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
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

        val imagePreviewViewModel: ImagePreviewViewModel = rememberScreenModel(tag = "$threadId-$imgPath") {
            nmbdi.direct.instance(arg = threadId to imgPath)
        }
        val uiState by imagePreviewViewModel.uiState.collectAsState()

        // 获取CDN管理器
        val cdnManager by nmbdi.instance<CdnManager>()

        // Pager State
        val pagerState = rememberPagerState(
            initialPage = uiState.initialIndex,
            pageCount = { uiState.images.size }
        )

        // Sync initial index when it changes (only once really)
        LaunchedEffect(uiState.initialIndex) {
            if (uiState.images.isNotEmpty() && pagerState.currentPage != uiState.initialIndex) {
                pagerState.scrollToPage(uiState.initialIndex)
            }
        }

        // Load next page when reaching the end
        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage >= uiState.images.size - 1 && !uiState.endReached && !uiState.isLoading) {
                imagePreviewViewModel.loadNextPage()
            }
        }

        // 设置标题和菜单按钮
        LaunchedEffect(pagerState.currentPage, uiState.images) {
            if (uiState.images.isNotEmpty() && pagerState.currentPage < uiState.images.size) {
                val currentImage = uiState.images[pagerState.currentPage]
                
                // 设置标题
                onUpdateTitle?.invoke("图片预览 (${pagerState.currentPage + 1}/${uiState.images.size})")

                // 设置菜单按钮
                onSetupMenuButton?.invoke {
                    // 保存图片按钮
                    IconButton(onClick = {
                        imagePreviewViewModel.saveCurrentImage(currentImage)
                    }) {
                        Icon(Icons.Default.Place, contentDescription = "保存图片")
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (uiState.images.isEmpty()) {
                // Show loading or empty state
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     CircularProgressIndicator(color = Color.White)
                 }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { index -> 
                         if (index < uiState.images.size) uiState.images[index].imgPath + uiState.images[index].ext else index
                    }
                ) { page ->
                    if (page < uiState.images.size) {
                        val imageInfo = uiState.images[page]
                        val imageUrl = cdnManager.buildImageUrl(imageInfo.imgPath, imageInfo.ext, false)
                        
                        ImageItem(imageUrl)
                    }
                }
            }
            
            // Loading indicator for next page
            if (uiState.isLoading) {
                 Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)) {
                     CircularProgressIndicator(color = Color.White)
                 }
            }
            
            // End indicator
            if (uiState.endReached && pagerState.currentPage == uiState.images.size - 1) {
                 Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)) {
                     Text("没有更多图片了", color = Color.White)
                 }
            }

            // 底部导航按钮 (Optional, Pager handles swipe, but buttons are good for accessibility)
            // For now, let's keep it simple and rely on swipe, or add simple arrows overlay
            
            // Back button
            IconButton(
                onClick = { navigator.pop() },
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
        }
    }
}

@Composable
fun ImageItem(imageUrl: String) {
    // 缩放状态
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 创建可变换状态
    val transformableState =
        rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            scale = (scale * zoomChange).coerceIn(0.5f, 5f)
            rotation += rotationChange
            offset += offsetChange
        }
        
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .transformable(state = transformableState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2f
                        offset = Offset.Zero
                        rotation = 0f
                    }
                )
            }
    ) {
        ZoomAsyncImage(
            uri = imageUrl,
            contentDescription = "预览图片",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = rotation,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}
