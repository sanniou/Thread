package ai.saniou.coreui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.ability.progressIndicator
import com.github.panpf.sketch.painter.rememberSectorProgressPainter
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.state.ThumbnailMemoryCacheStateImage
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.github.panpf.zoomimage.compose.zoom.ScrollBarSpec
import com.github.panpf.zoomimage.compose.zoom.ZoomableState
import com.github.panpf.zoomimage.rememberSketchZoomState
import kotlinx.coroutines.launch

/**
 * 一个功能丰富的可缩放图片组件，提供了缩放、平移、旋转和截图等交互控件。
 *
 * @param uri 要显示的图片URI
 * @param thumbnailUrl 缩略图URL，用于在加载时占位
 * @param contentDescription 图片的无障碍描述
 * @param alignment 图片在边界内的对齐方式
 * @param contentScale 图片的缩放方式
 * @param modifier 应用于组件的Modifier
 */
@Composable
fun ZoomAsyncImage(
    uri: String?,
    thumbnailUrl: String?,
    contentDescription: String?,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    modifier: Modifier = Modifier,
) {
    val capturableState = rememberCapturableState()
    val zoomState = rememberSketchZoomState()
    val imageState = rememberAsyncImageState()
    Box(modifier = modifier.fillMaxSize()) {
        SketchZoomAsyncImage(
            request = ComposableImageRequest(uri) {
                placeholder(ThumbnailMemoryCacheStateImage(thumbnailUrl))
                crossfade(fadeStart = false)
            },
            contentDescription = contentDescription,
            contentScale = contentScale,
            alignment = alignment,
            modifier = Modifier
                .fillMaxSize()
                .progressIndicator(imageState, rememberSectorProgressPainter())
                .capturable(capturableState),
            state = imageState,
            zoomState = zoomState,
            scrollBar = ScrollBarSpec.Default, // 启用滚动条
        )

        ZoomImageTool(
            zoomableState = zoomState.zoomable,
            onCaptureClick = {
                // TODO: 实现截图保存逻辑，这部分是平台相关的
                // val imageBitmap = capturableState.capture()
            }
        )
    }
}

@Composable
private fun ZoomImageTool(
    zoomableState: ZoomableState,
    onCaptureClick: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp, end = 20.dp) // 避免与导航栏重叠
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var moreShow by remember { mutableStateOf(false) }
            val containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

            AnimatedVisibility(
                visible = moreShow,
                enter = slideInHorizontally(initialOffsetX = { it / 2 }),
                exit = slideOutHorizontally(targetOffsetX = { it / 2 }),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(120.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilledIconButton(
                            onClick = {
                                coroutineScope.launch {
                                    zoomableState.scaleBy(addScale = 0.67f, animated = true)
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = containerColor,
                                contentColor = contentColor
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "缩小",
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                coroutineScope.launch {
                                    zoomableState.scaleBy(addScale = 1.5f, animated = true)
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = containerColor,
                                contentColor = contentColor
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "放大",
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    Slider(
                        value = zoomableState.transform.scaleX,
                        valueRange = zoomableState.minScale..zoomableState.maxScale,
                        onValueChange = {
                            coroutineScope.launch {
                                zoomableState.scale(targetScale = it, animated = true)
                            }
                        },
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = contentColor,
                            activeTrackColor = containerColor,
                            inactiveTrackColor = containerColor.copy(alpha = 0.5f),
                        ),
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                }
            }

            ButtonPad(
                zoomableState = zoomableState,
                onCaptureClick = onCaptureClick,
                onClickMore = { moreShow = !moreShow }
            )
        }
    }
}

@Composable
private fun ButtonPad(
    zoomableState: ZoomableState,
    onCaptureClick: () -> Unit,
    onClickMore: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        Modifier
            .background(containerColor, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        IconButton(
            onClick = {
                coroutineScope.launch {
                    zoomableState.rotateBy(90)
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "旋转",
                tint = contentColor
            )
        }

        IconButton(
            onClick = {
                coroutineScope.launch {
                    zoomableState.switchScale(animated = true)
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            val zoomIn by remember {
                derivedStateOf {
                    zoomableState.getNextStepScale() > zoomableState.transform.scaleX
                }
            }
            val icon = if (zoomIn) Icons.Default.Info else Icons.Default.Info
            Icon(
                imageVector = icon,
                contentDescription = "缩放",
                tint = contentColor
            )
        }

        IconButton(
            onClick = onCaptureClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "截图",
                tint = contentColor
            )
        }

        IconButton(
            onClick = { /*TODO: 显示信息弹窗*/ },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "信息",
                tint = contentColor
            )
        }

        IconButton(
            onClick = onClickMore,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "更多",
                tint = contentColor
            )
        }
    }
}
