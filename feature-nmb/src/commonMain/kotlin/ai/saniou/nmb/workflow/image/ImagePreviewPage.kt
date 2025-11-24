package ai.saniou.nmb.workflow.image

import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.di.nmbdi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.rememberCoroutineScope
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
import ai.saniou.coreui.widgets.ZoomAsyncImage
import ai.saniou.nmb.workflow.image.ImagePreviewContract.Event
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.coroutines.cancellation.CancellationException

/**
 * 图片预览页面
 *
 * 支持图片缩放、查看上一张/下一张、保存图片等功能
 * 支持双击放大功能
 */
data class ImagePreviewPage(
    val params: ImagePreviewViewModelParams,
    val di: DI = nmbdi,
) : Screen {

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: ImagePreviewViewModel =
            rememberScreenModel(tag = params.toString()) {
                nmbdi.direct.instance(arg = params)
            }
        val uiState by viewModel.state.collectAsState()

        PredictiveBackHandler { progress ->
            try {
                progress.collect { backEvent ->

                }
                navigator.pop()
            } catch (e: CancellationException) {
//                callback.cancelPop()
            }
        }

        // 获取CDN管理器
        val cdnManager by di.instance<CdnManager>()

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
                viewModel.onEvent(Event.LoadMore)
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
                        val imageUrl =
                            cdnManager.buildImageUrl(imageInfo.imgPath, imageInfo.ext, false)
                        val thumbnailUrl =
                            cdnManager.buildImageUrl(imageInfo.imgPath, imageInfo.ext, true)

                        ImageItem(imageUrl, thumbnailUrl)
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
            if (uiState.endReached && uiState.images.size > 1 && pagerState.currentPage == uiState.images.size - 1) {
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
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ImageItem(imageUrl: String, thumbnailUrl: String) {
    // 缩放状态
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    val offsetAnimatable = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = constraints.maxHeight.toFloat()

        // 创建可变换状态
        val transformableState =
            rememberTransformableState { zoomChange, offsetChange, rotationChange ->
                scale = (scale * zoomChange).coerceIn(1f, 5f)
                rotation += rotationChange
                scope.launch {
                    offsetAnimatable.snapTo(offsetAnimatable.value + offsetChange)
                }
            }

        LaunchedEffect(transformableState.isTransformInProgress, scale) {
            if (!transformableState.isTransformInProgress) {
                val maxX = ((boxWidth * scale - boxWidth) / 2f).coerceAtLeast(0f)
                val maxY = ((boxHeight * scale - boxHeight) / 2f).coerceAtLeast(0f)

                val currentOffset = offsetAnimatable.value
                val targetOffset = Offset(
                    x = currentOffset.x.coerceIn(-maxX, maxX),
                    y = currentOffset.y.coerceIn(-maxY, maxY)
                )
                if (currentOffset != targetOffset) {
                    scope.launch {
                        offsetAnimatable.animateTo(targetOffset, animationSpec = spring())
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformableState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scope.launch {
                                if (scale > 1f) {
                                    scale = 1f
                                    offsetAnimatable.animateTo(
                                        Offset.Zero,
                                        animationSpec = spring()
                                    )
                                } else {
                                    scale = 2f
                                }
                                rotation = 0f
                            }
                        }
                    )
                }
        ) {
            ZoomAsyncImage(
                uri = imageUrl,
                thumbnailUrl = thumbnailUrl,
                contentDescription = "预览图片",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        rotationZ = rotation,
                        translationX = offsetAnimatable.value.x,
                        translationY = offsetAnimatable.value.y
                    ),
            )
        }
    }
}
