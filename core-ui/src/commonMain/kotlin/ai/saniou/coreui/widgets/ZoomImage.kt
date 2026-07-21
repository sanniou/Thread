package ai.saniou.coreui.widgets

import ai.saniou.coreui.widgets.palette.PhotoPalette
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
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Text
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
import org.jetbrains.compose.resources.stringResource
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.s_11f8516f82
import thread.core_ui.generated.resources.s_12e2ed4d50
import thread.core_ui.generated.resources.s_2da40f4073
import thread.core_ui.generated.resources.s_324046a8e2
import thread.core_ui.generated.resources.s_6c14bd7f6f
import thread.core_ui.generated.resources.s_9b0c6c7858
import thread.core_ui.generated.resources.s_a69e51e503
import thread.core_ui.generated.resources.s_d7f48a059c

/**
 * 一个功能丰富的可缩放图片组件，提供缩放、平移、旋转和图片信息。
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
    photoPalette: PhotoPalette,
    showTools: Boolean,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    modifier: Modifier = Modifier,
) {
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
                .progressIndicator(imageState, rememberSectorProgressPainter()),
            state = imageState,
            zoomState = zoomState,
            scrollBar = ScrollBarSpec.Default, // 启用滚动条
        )

        AnimatedVisibility(visible = showTools) {
            ZoomImageTool(
                uri = uri,
                zoomableState = zoomState.zoomable,
                photoPalette = photoPalette,
            )
        }
    }
}

@Composable
private fun ZoomImageTool(
    uri: String?,
    zoomableState: ZoomableState,
    photoPalette: PhotoPalette,
) {
    val coroutineScope = rememberCoroutineScope()
    var showInfoDialog by remember { mutableStateOf(false) }

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
            val containerColor = photoPalette.containerColor
            val contentColor = photoPalette.contentColor

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
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = stringResource(Res.string.s_11f8516f82),
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
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = stringResource(Res.string.s_d7f48a059c),
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
                onInfoClick = { showInfoDialog = true },
                onClickMore = { moreShow = !moreShow },
                photoPalette = photoPalette
            )
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(stringResource(Res.string.s_324046a8e2)) },
            text = {
                val info = """
                    URL: $uri
                    尺寸: ${zoomableState.contentSize.width} x ${zoomableState.contentSize.height}
                    原始尺寸: ${zoomableState.contentOriginSize.width} x ${zoomableState.contentOriginSize.height}
                """.trimIndent()
                Text(info)
            },
            confirmButton = {
                SaniouTextButton(onClick = { showInfoDialog = false }, text = stringResource(Res.string.s_6c14bd7f6f))
            }
        )
    }
}

@Composable
private fun ButtonPad(
    zoomableState: ZoomableState,
    onInfoClick: () -> Unit,
    onClickMore: () -> Unit,
    photoPalette: PhotoPalette,
) {
    val coroutineScope = rememberCoroutineScope()
    val containerColor = photoPalette.containerColor
    val contentColor = photoPalette.contentColor

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
                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                contentDescription = stringResource(Res.string.s_a69e51e503),
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
            val icon = if (zoomIn) Icons.Default.ZoomIn else Icons.Default.ZoomOut
            Icon(
                imageVector = icon,
                contentDescription = stringResource(Res.string.s_12e2ed4d50),
                tint = contentColor
            )
        }

        IconButton(
            onClick = onInfoClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(Res.string.s_2da40f4073),
                tint = contentColor
            )
        }

        IconButton(
            onClick = onClickMore,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.s_9b0c6c7858),
                tint = contentColor
            )
        }
    }
}
