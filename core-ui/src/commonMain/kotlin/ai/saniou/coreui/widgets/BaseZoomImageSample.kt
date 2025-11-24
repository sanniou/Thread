//package com.kmpalette.palette.internal
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.slideInHorizontally
//import androidx.compose.animation.slideOutHorizontally
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.layout.windowInsetsPadding
//import androidx.compose.foundation.layout.wrapContentHeight
//import androidx.compose.foundation.layout.wrapContentSize
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.Button
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.ColorScheme
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.FilledIconButton
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.IconButtonDefaults
//import androidx.compose.material3.LocalTextStyle
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.NavigationBarDefaults
//import androidx.compose.material3.Slider
//import androidx.compose.material3.SliderDefaults
//import androidx.compose.material3.Text
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.CompositionLocalProvider
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.MutableState
//import androidx.compose.runtime.Stable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.derivedStateOf
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.runtime.snapshotFlow
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.ImageBitmap
//import androidx.compose.ui.graphics.Shadow
//import androidx.compose.ui.graphics.toArgb
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalInspectionMode
//import androidx.compose.ui.platform.LocalLayoutDirection
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.LayoutDirection
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.round
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.window.Dialog
//import com.github.panpf.sketch.BitmapImage
//import com.github.panpf.sketch.ability.progressIndicator
//import com.github.panpf.sketch.decode.DecodeInterceptor
//import com.github.panpf.sketch.decode.DecodeResult
//import com.github.panpf.sketch.painter.rememberSectorProgressPainter
//import com.github.panpf.sketch.rememberAsyncImageState
//import com.github.panpf.sketch.request.ComposableImageRequest
//import com.github.panpf.sketch.request.ImageOptions
//import com.github.panpf.sketch.request.ImageResult
//import com.github.panpf.sketch.request.LoadState
//import com.github.panpf.sketch.state.ThumbnailMemoryCacheStateImage
//import com.github.panpf.zoomimage.SketchZoomAsyncImage
//import com.github.panpf.zoomimage.compose.ZoomState
//import com.github.panpf.zoomimage.compose.subsampling.SubsamplingState
//import com.github.panpf.zoomimage.compose.util.toPlatform
//import com.github.panpf.zoomimage.compose.zoom.ScrollBarSpec
//import com.github.panpf.zoomimage.compose.zoom.ZoomAnimationSpec
//import com.github.panpf.zoomimage.compose.zoom.ZoomableState
//import com.github.panpf.zoomimage.compose.zoom.bindKeyZoomWithKeyEventFlow
//import com.github.panpf.zoomimage.rememberSketchZoomState
//import com.github.panpf.zoomimage.sample.AppEvents
//import com.github.panpf.zoomimage.sample.AppSettings
//import com.github.panpf.zoomimage.sample.buildScalesCalculator
//import com.github.panpf.zoomimage.sample.image.PhotoPalette
//import com.github.panpf.zoomimage.sample.resources.Res
//import com.github.panpf.zoomimage.sample.resources.ic_info
//import com.github.panpf.zoomimage.sample.resources.ic_more_vert
//import com.github.panpf.zoomimage.sample.resources.ic_photo_camera
//import com.github.panpf.zoomimage.sample.resources.ic_rotate_right
//import com.github.panpf.zoomimage.sample.resources.ic_zoom_in
//import com.github.panpf.zoomimage.sample.resources.ic_zoom_out
//import com.github.panpf.zoomimage.sample.ui.components.MoveKeyboard
//import com.github.panpf.zoomimage.sample.ui.components.MyDialogState
//import com.github.panpf.zoomimage.sample.ui.components.ZoomImageMinimap
//import com.github.panpf.zoomimage.sample.ui.components.rememberMoveKeyboardState
//import com.github.panpf.zoomimage.sample.ui.components.rememberMyDialogState
//import com.github.panpf.zoomimage.sample.ui.gallery.photoPagerTopBarHeight
//import com.github.panpf.zoomimage.sample.ui.util.crop
//import com.github.panpf.zoomimage.sample.ui.util.limitTo
//import com.github.panpf.zoomimage.sample.ui.util.toShortString
//import com.github.panpf.zoomimage.subsampling.TileAnimationSpec
//import com.github.panpf.zoomimage.zoom.ContainerWhitespace
//import com.github.panpf.zoomimage.zoom.ReadMode
//import com.kmpalette.palette.graphics.Palette
//import kotlinx.coroutines.launch
//import org.jetbrains.compose.resources.painterResource
//import org.koin.compose.koinInject
//import thread.core_ui.generated.resources.Res
//import kotlin.math.roundToInt
//
//
//expect fun getPlatformSketchZoomAsyncImageSampleImageOptions(): ImageOptions
//
//@Composable
//fun SketchZoomAsyncImageSample(
//    photo: Photo,
//    photoPaletteState: MutableState<PhotoPalette>,
//    pageSelected: Boolean,
//) {
//    BaseZoomImageSample(
//        photo = photo,
//        photoPaletteState = photoPaletteState,
//        createZoomState = { rememberSketchZoomState() },
//        pageSelected = pageSelected,
//    ) {
//        val imageState = rememberAsyncImageState()
//        SketchZoomAsyncImage(
//            request = ComposableImageRequest(photo.originalUrl) {
//                placeholder(ThumbnailMemoryCacheStateImage(photo.listThumbnailUrl))
//                crossfade(fadeStart = false)
//                merge(getPlatformSketchZoomAsyncImageSampleImageOptions())
//                addComponents {
//                    addRequestInterceptor(DelayedLoadRequestInterceptor(2000L))
//                }
//            },
//            contentDescription = "view image",
//            contentScale = contentScale,
//            alignment = alignment,
//            modifier = Modifier
//                .fillMaxSize()
//                .progressIndicator(imageState, rememberSectorProgressPainter())
//                .capturable(capturableState),
//            state = imageState,
//            zoomState = zoomState,
//            scrollBar = scrollBar,
//            onLongPress = { onLongClick.invoke() },
//        )
//
//        val pageState by remember {
//            derivedStateOf {
//                if (imageState.loadState is LoadState.Error) {
//                    PageState.Error { imageState.restart() }
//                } else null
//            }
//        }
//        PageState(pageState = pageState)
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun <T : ZoomState> BaseZoomImageSample(
//    photo: Photo,
//    photoPaletteState: MutableState<PhotoPalette>,
//    pageSelected: Boolean,
//    createZoomState: @Composable () -> T,
//    content: @Composable ContentScope<T>.() -> Unit,
//) {
//    Box(modifier = Modifier.fillMaxSize()) {
//        val appSettings: AppSettings = koinInject()
//        val infoDialogState = rememberMyDialogState()
//        val capturableState = rememberCapturableState()
//        val zoomState = createZoomState().apply { BindSettings(appSettings) }
//
//        val rtlLayoutDirectionEnabled by appSettings.rtlLayoutDirectionEnabled.collectAsState()
//        val layoutDirection =
//            if (rtlLayoutDirectionEnabled) LayoutDirection.Rtl else LocalLayoutDirection.current
//        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
//            val contentScale by appSettings.contentScale.collectAsState()
//            val alignment by appSettings.alignment.collectAsState()
//            val scrollBarEnabled by appSettings.scrollBarEnabled.collectAsState()
//            val contentScope =
//                remember(zoomState, contentScale, alignment, capturableState, scrollBarEnabled) {
//                    ContentScope(
//                        zoomState = zoomState,
//                        contentScale = contentScale.toPlatform(),
//                        alignment = alignment.toPlatform(),
//                        capturableState = capturableState,
//                        scrollBar = if (scrollBarEnabled) ScrollBarSpec.Default.copy(color = photoPaletteState.value.containerColor) else null,
//                        onLongClick = { infoDialogState.show() },
//                    )
//                }
//            with(contentScope) {
//                content()
//            }
//        }
//
//        Row(
//            Modifier
//                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
//                .padding(top = photoPagerTopBarHeight)
//                .padding(horizontal = 20.dp)
//        ) {
//            val headerInfo = remember {
//                """
//                    scale:
//                    offset:
//                    rotation:
//                """.trimIndent()
//            }
//            Text(
//                text = headerInfo,
//                color = Color.White,
//                fontSize = 13.sp,
//                lineHeight = 16.sp,
//                style = LocalTextStyle.current.copy(
//                    shadow = Shadow(offset = Offset(0f, 0f), blurRadius = 10f),
//                ),
//                overflow = TextOverflow.Ellipsis,
//            )
//            val transformInfo = remember(zoomState.zoomable.transform) {
//                val transform = zoomState.zoomable.transform
//                """
//                    ${transform.scale.toShortString()}
//                    ${transform.offset.round().toShortString()}
//                    ${transform.rotation.roundToInt()}
//                """.trimIndent()
//            }
//            Text(
//                text = transformInfo,
//                color = Color.White,
//                fontSize = 13.sp,
//                lineHeight = 16.sp,
//                style = LocalTextStyle.current.copy(
//                    shadow = Shadow(offset = Offset(0f, 0f), blurRadius = 10f),
//                ),
//                overflow = TextOverflow.Ellipsis,
//            )
//        }
//
//        if (pageSelected) {
//            val appEvents: AppEvents = koinInject()
//            bindKeyZoomWithKeyEventFlow(appEvents.keyEvent, zoomState.zoomable)
//        }
//
//        ZoomImageTool(
//            photo = photo,
//            zoomableState = zoomState.zoomable,
//            subsamplingState = zoomState.subsampling,
//            capturableState = capturableState,
//            infoDialogState = infoDialogState,
//            photoPaletteState = photoPaletteState,
//        )
//
//        Dialog(infoDialogState) {
//            ZoomImageInfo(
//                photo = photo,
//                zoomState = zoomState,
//            )
//        }
//    }
//}
//
//@Composable
//private fun ZoomState.BindSettings(appSettings: AppSettings) {
//    val logLevel by appSettings.logLevel.collectAsState()
//    logger.level = logLevel
//
//    val threeStepScale by appSettings.threeStepScaleEnabled.collectAsState()
//    zoomable.setThreeStepScale(threeStepScale)
//
//    val rubberBandScale by appSettings.rubberBandScaleEnabled.collectAsState()
//    zoomable.setRubberBandScale(rubberBandScale)
//
//    val animateScale by appSettings.zoomAnimateEnabled.collectAsState()
//    val slowerScaleAnimation by appSettings.zoomSlowerAnimationEnabled.collectAsState()
//    val zoomAnimationSpec by remember {
//        derivedStateOf {
//            val durationMillis = if (animateScale) (if (slowerScaleAnimation) 3000 else 300) else 0
//            ZoomAnimationSpec.Default.copy(durationMillis = durationMillis)
//        }
//    }
//    zoomable.setAnimationSpec(zoomAnimationSpec)
//
//    val reverseMouseWheelScale by appSettings.reverseMouseWheelScaleEnabled.collectAsState()
//    zoomable.setReverseMouseWheelScale(reverseMouseWheelScale)
//
//    val scalesCalculatorName by appSettings.scalesCalculatorName.collectAsState()
//    val scalesMultiple by appSettings.fixedScalesCalculatorMultiple.collectAsState()
//    val scalesCalculator by remember {
//        derivedStateOf {
//            buildScalesCalculator(scalesCalculatorName, scalesMultiple.toFloat())
//        }
//    }
//    zoomable.setScalesCalculator(scalesCalculator)
//
//    val limitOffsetWithinBaseVisibleRect by appSettings.limitOffsetWithinBaseVisibleRect.collectAsState()
//    zoomable.setLimitOffsetWithinBaseVisibleRect(limitOffsetWithinBaseVisibleRect)
//
//    val containerWhitespaceMultiple by appSettings.containerWhitespaceMultiple.collectAsState()
//    zoomable.setContainerWhitespaceMultiple(containerWhitespaceMultiple)
//
//    val containerWhitespaceEnabled by appSettings.containerWhitespaceEnabled.collectAsState()
//    val containerWhitespace by remember {
//        derivedStateOf {
//            if (containerWhitespaceEnabled) {
//                ContainerWhitespace(left = 100f, top = 200f, right = 300f, bottom = 400f)
//            } else {
//                ContainerWhitespace.Zero
//            }
//        }
//    }
//    zoomable.setContainerWhitespace(containerWhitespace)
//
//    val readModeEnabled by appSettings.readModeEnabled.collectAsState()
//    val readModeAcceptedBoth by appSettings.readModeAcceptedBoth.collectAsState()
//    val horizontalLayout by appSettings.horizontalPagerLayout.collectAsState()
//    val readMode by remember {
//        derivedStateOf {
//            val sizeType = when {
//                readModeAcceptedBoth -> ReadMode.SIZE_TYPE_HORIZONTAL or ReadMode.SIZE_TYPE_VERTICAL
//                horizontalLayout -> ReadMode.SIZE_TYPE_VERTICAL
//                else -> ReadMode.SIZE_TYPE_HORIZONTAL
//            }
//            if (readModeEnabled) ReadMode.Default.copy(sizeType = sizeType) else null
//        }
//    }
//    zoomable.setReadMode(readMode)
//
//    val disabledGestureTypes by appSettings.disabledGestureTypes.collectAsState()
//    zoomable.setDisabledGestureTypes(disabledGestureTypes)
//
//    val keepTransform by appSettings.keepTransformEnabled.collectAsState()
//    zoomable.setKeepTransformWhenSameAspectRatioContentSizeChanged(keepTransform)
//
//    val subsamplingEnabled by appSettings.subsamplingEnabled.collectAsState()
//    subsampling.setDisabled(!subsamplingEnabled)
//
//    val autoStopWithLifecycleEnabled by appSettings.autoStopWithLifecycleEnabled.collectAsState()
//    subsampling.setDisabledAutoStopWithLifecycle(!autoStopWithLifecycleEnabled)
//
//    val pausedContinuousTransformTypes by appSettings.pausedContinuousTransformTypes.collectAsState()
//    subsampling.setPausedContinuousTransformTypes(pausedContinuousTransformTypes)
//
//    val backgroundTilesEnabled by appSettings.backgroundTilesEnabled.collectAsState()
//    subsampling.setDisabledBackgroundTiles(!backgroundTilesEnabled)
//
//    val tileBoundsEnabled by appSettings.tileBoundsEnabled.collectAsState()
//    subsampling.setShowTileBounds(tileBoundsEnabled)
//
//    val tileAnimationEnabled by appSettings.tileAnimationEnabled.collectAsState()
//    val tileAnimationSpec by remember {
//        derivedStateOf {
//            if (tileAnimationEnabled) TileAnimationSpec.Default else TileAnimationSpec.None
//        }
//    }
//    subsampling.setTileAnimationSpec(tileAnimationSpec)
//
//    val tileMemoryCache by appSettings.tileMemoryCacheEnabled.collectAsState()
//    subsampling.setDisabledTileImageCache(!tileMemoryCache)
//}
//
//@Stable
//data class ContentScope<T : ZoomState>(
//    val zoomState: T,
//    val contentScale: ContentScale,
//    val alignment: Alignment,
//    val capturableState: CapturableState,
//    val scrollBar: ScrollBarSpec?,
//    val onLongClick: () -> Unit,
//)
//
//@Composable
//fun ZoomImageTool(
//    photo: Photo,
//    zoomableState: ZoomableState,
//    subsamplingState: SubsamplingState,
//    capturableState: CapturableState,
//    infoDialogState: MyDialogState,
//    photoPaletteState: MutableState<PhotoPalette>,
//) {
//    val coroutineScope = rememberCoroutineScope()
//    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(NavigationBarDefaults.windowInsets)) {
//        ZoomImageMinimap(
//            imageUri = photo.listThumbnailUrl,
//            zoomableState = zoomableState,
//            subsamplingState = subsamplingState,
//        )
//
//        Column(
//            modifier = Modifier
//                .padding(20.dp)
//                .align(Alignment.BottomEnd)
//                .wrapContentHeight()
//                .width(205.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            val inspectionMode = LocalInspectionMode.current
//            var moreShow by remember { mutableStateOf(inspectionMode) }
//            val photoPalette by photoPaletteState
//            AnimatedVisibility(
//                visible = moreShow,
//                enter = slideInHorizontally(initialOffsetX = { it * 2 }),
//                exit = slideOutHorizontally(targetOffsetX = { it * 2 }),
//            ) {
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    val moveKeyboardState = rememberMoveKeyboardState()
//                    LaunchedEffect(Unit) {
//                        snapshotFlow { zoomableState.containerSize }.collect { size ->
//                            moveKeyboardState.maxStep = Offset(size.width / 20f, size.height / 20f)
//                        }
//                    }
//                    LaunchedEffect(Unit) {
//                        moveKeyboardState.moveFlow.collect {
//                            zoomableState.offsetBy(it * -1f)
//                        }
//                    }
//                    MoveKeyboard(
//                        state = moveKeyboardState,
//                        iconTint = photoPalette.containerColor,
//                        modifier = Modifier.size(100.dp)
//                    )
//
//                    Spacer(modifier = Modifier.size(6.dp))
//
//                    Row(modifier = Modifier.fillMaxWidth()) {
//                        FilledIconButton(
//                            onClick = {
//                                coroutineScope.launch {
//                                    zoomableState.scaleBy(addScale = 0.67f, animated = true)
//                                }
//                            },
//                            modifier = Modifier.size(30.dp),
//                            colors = IconButtonDefaults.iconButtonColors(
//                                containerColor = photoPalette.containerColor,
//                                contentColor = photoPalette.contentColor
//                            ),
//                        ) {
//                            Icon(
//                                painter = painterResource(Res.drawable.ic_zoom_out),
//                                contentDescription = "zoom out",
//                            )
//                        }
//
//                        Box(
//                            modifier = Modifier
//                                .weight(1f)
//                                .height(1.dp)
//                        )
//
//                        FilledIconButton(
//                            onClick = {
//                                coroutineScope.launch {
//                                    zoomableState.scaleBy(addScale = 1.5f, animated = true)
//                                }
//                            },
//                            modifier = Modifier.size(30.dp),
//                            colors = IconButtonDefaults.iconButtonColors(
//                                containerColor = photoPalette.containerColor,
//                                contentColor = photoPalette.contentColor
//                            )
//                        ) {
//                            Icon(
//                                painter = painterResource(Res.drawable.ic_zoom_in),
//                                contentDescription = "zoom in",
//                            )
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.size(6.dp))
//
//                    Slider(
//                        value = zoomableState.transform.scaleX,
//                        valueRange = zoomableState.minScale..zoomableState.maxScale,
//                        onValueChange = {
//                            coroutineScope.launch {
//                                zoomableState.scale(targetScale = it, animated = true)
//                            }
//                        },
//                        steps = 8,
//                        colors = SliderDefaults.colors(
//                            thumbColor = photoPalette.containerColor,
//                            activeTickColor = photoPalette.contentColor,
//                            activeTrackColor = photoPalette.containerColor,
//                            inactiveTickColor = photoPalette.contentColor,
//                            inactiveTrackColor = photoPalette.containerColor,
//                        ),
//                    )
//
//                    Spacer(modifier = Modifier.size(6.dp))
//                }
//            }
//
//            ButtonPad(infoDialogState, zoomableState, capturableState, photoPaletteState) {
//                moreShow = !moreShow
//            }
//        }
//    }
//}
//
//@Composable
//private fun ButtonPad(
//    infoDialogState: MyDialogState,
//    zoomableState: ZoomableState,
//    capturableState: CapturableState,
//    photoPaletteState: MutableState<PhotoPalette>,
//    onClickMore: () -> Unit,
//) {
//    val coroutineScope = rememberCoroutineScope()
//    val photoPalette by photoPaletteState
//    val screenshotDialog = rememberMyDialogState(false)
//    var screenshotBitmap by mutableStateOf<ImageBitmap?>(null)
//    Row(
//        background(photoPalette.containerColor, RoundedCornerShape(50))
//            .padding(horizontal = 10.dp)
//    ) {
//        IconButton(
//            onClick = {
//                coroutineScope.launch {
//                    zoomableState.rotateBy(90)
//                }
//            },
//            modifier = Modifier.size(40.dp)
//        ) {
//            Icon(
//                painter = painterResource(Res.drawable.ic_rotate_right),
//                contentDescription = "Rotate",
//                tint = photoPalette.contentColor
//            )
//        }
//
//        IconButton(
//            onClick = {
//                coroutineScope.launch {
//                    zoomableState.switchScale(animated = true)
//                }
//            },
//            modifier = Modifier.size(40.dp)
//        ) {
//            val zoomIn by remember {
//                derivedStateOf {
//                    zoomableState.getNextStepScale() > zoomableState.transform.scaleX
//                }
//            }
//            val description = if (zoomIn) {
//                "zoom in"
//            } else {
//                "zoom out"
//            }
//            val icon = if (zoomIn) {
//                painterResource(Res.drawable.ic_zoom_in)
//            } else {
//                painterResource(Res.drawable.ic_zoom_out)
//            }
//            Icon(
//                painter = icon,
//                contentDescription = description,
//                tint = photoPalette.contentColor
//            )
//        }
//
//        IconButton(
//            onClick = {
//                coroutineScope.launch {
//                    val imageBitmap = capturableState.capture()
//                    val cropRect = zoomableState.contentDisplayRect
//                        .limitTo(zoomableState.containerSize)
//                    screenshotBitmap = imageBitmap.crop(cropRect)
//                    screenshotDialog.show()
//                }
//            },
//            modifier = Modifier.size(40.dp)
//        ) {
//            Icon(
//                painter = painterResource(Res.drawable.ic_photo_camera),
//                contentDescription = "Capture",
//                tint = photoPalette.contentColor
//            )
//        }
//
//        IconButton(
//            onClick = { infoDialogState.showing = !infoDialogState.showing },
//            modifier = Modifier.size(40.dp)
//        ) {
//            Icon(
//                painter = painterResource(Res.drawable.ic_info),
//                contentDescription = "Info",
//                tint = photoPalette.contentColor
//            )
//        }
//
//        IconButton(
//            onClick = { onClickMore() },
//            modifier = Modifier.size(40.dp)
//        ) {
//            Icon(
//                painter = painterResource(Res.drawable.ic_more_vert),
//                contentDescription = "More",
//                tint = photoPalette.contentColor
//            )
//        }
//
//        Dialog(screenshotDialog) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .clickable {
//                        screenshotDialog.dismiss()
//                    }
//                    .padding(40.dp)
//            ) {
//                Image(
//                    bitmap = screenshotBitmap!!,
//                    contentDescription = "screenshot",
//                    modifier = Modifier
//                        .wrapContentSize()
//                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
//                        .align(Alignment.Center)
//                )
//            }
//        }
//    }
//}
//
//
//@Composable
//fun ZoomImageInfo(photo: Photo, zoomState: ZoomState) {
//    val zoomable = zoomState.zoomable
//    val subsampling = zoomState.subsampling
//    val items by remember {
//        derivedStateOf {
//            buildList {
//                add(InfoItem(null, photo.originalUrl))
//
//                val baseInfo = """
//                        containerSize: ${zoomable.containerSize.toShortString()}
//                        contentSize: ${zoomable.contentSize.toShortString()}
//                        contentOriginSize: ${zoomable.contentOriginSize.toShortString()}
//                        rotation: ${zoomable.transform.rotation.roundToInt()}
//                        mimeType: ${subsampling.imageInfo?.mimeType}
//                    """.trimIndent()
//                add(InfoItem("Base", baseInfo))
//
//                val scaleFormatted = zoomable.transform.scale.toShortString()
//                val baseScaleFormatted = zoomable.baseTransform.scale.toShortString()
//                val userScaleFormatted = zoomable.userTransform.scale.toShortString()
//                val scales = floatArrayOf(
//                    zoomable.minScale,
//                    zoomable.mediumScale,
//                    zoomable.maxScale
//                ).joinToString(prefix = "[", postfix = "]") { it.format(2).toString() }
//                val scaleInfo = """
//                        scale: $scaleFormatted
//                        baseScale: $baseScaleFormatted
//                        userScale: $userScaleFormatted
//                        scales: $scales
//                    """.trimIndent()
//                add(InfoItem("Scale：", scaleInfo))
//
//                val offsetInfo = """
//                        offset: ${zoomable.transform.offset.round().toShortString()}
//                        baseOffset: ${zoomable.baseTransform.offset.round().toShortString()}
//                        userOffset: ${zoomable.userTransform.offset.round().toShortString()}
//                        userOffsetBounds: ${zoomable.userOffsetBoundsRectF.round().toShortString()}
//                        edge: ${zoomable.scrollEdge.toShortString()}
//                    """.trimIndent()
//                add(InfoItem("Offset：", offsetInfo))
//
//                val displayAndVisibleInfo = """
//                        contentBaseDisplay: ${
//                    zoomable.contentBaseDisplayRectF.round().toShortString()
//                }
//                        contentBaseVisible: ${
//                    zoomable.contentBaseVisibleRectF.round().toShortString()
//                }
//                        contentDisplay: ${zoomable.contentDisplayRectF.round().toShortString()}
//                        contentVisible: ${zoomable.contentVisibleRectF.round().toShortString()}
//                    """.trimIndent()
//                add(InfoItem("Display&Visible：", displayAndVisibleInfo))
//
//                val foregroundTiles = subsampling.foregroundTiles
//                val loadedTileCount = foregroundTiles.count { it.tileImage != null }
//                val loadedTileBytes =
//                    foregroundTiles.sumOf { it.tileImage?.byteCount ?: 0 }.formatFileSize()
//                val backgroundTiles = subsampling.backgroundTiles
//                val backgroundTilesLoadedCount = backgroundTiles.count { it.tileImage != null }
//                val backgroundTilesLoadedBytes =
//                    backgroundTiles.sumOf { it.tileImage?.byteCount ?: 0 }.formatFileSize()
//                val tileGridSizeMapString = subsampling.tileGridSizeMap.entries
//                    .joinToString(prefix = "[", postfix = "]", separator = ", ") {
//                        "${it.key}:${it.value.toShortString()}"
//                    }
//                val tileInfo = """
//                        tileGridSizeMap：$tileGridSizeMapString
//                        sampleSize：${subsampling.sampleSize}
//                        imageLoadRect：${subsampling.imageLoadRect.toShortString()}
//                        foreground：size=${foregroundTiles.size}, load=$loadedTileCount, bytes=$loadedTileBytes
//                        background：size=${backgroundTiles.size}, load=$backgroundTilesLoadedCount, bytes=$backgroundTilesLoadedBytes
//                    """.trimIndent()
//                add(InfoItem("Tiles：", tileInfo))
//            }.toImmutableList()
//        }
//    }
//    InfoItems(items)
//}
//
//
//fun Photo(uri: String): Photo = Photo(
//    originalUrl = uri,
//    mediumUrl = uri,
//    thumbnailUrl = uri,
//    width = null,
//    height = null,
//    index = null,
//)
//
//data class Photo(
//    val originalUrl: String,
//    val mediumUrl: String?,
//    val thumbnailUrl: String?,
//    val width: Int?,
//    val height: Int?,
//    val index: Int? = null,
//)
//
//data class InfoItem(val title: String? = null, val content: String)
//
//
//fun PhotoPalette(palette: SimplePalette): PhotoPalette {
//    return PhotoPalette(
//        palette = palette,
//        primaryColor = 0xFFFFFF,
//        tertiaryColor = 0xFFFFFF
//    )
//}
//
//fun PhotoPalette(palette: SimplePalette?, colorScheme: ColorScheme): PhotoPalette {
//    return PhotoPalette(
//        palette = palette,
//        primaryColor = colorScheme.primary.toArgb(),
//        tertiaryColor = colorScheme.tertiary.toArgb()
//    )
//}
//
//fun PhotoPalette(colorScheme: ColorScheme): PhotoPalette {
//    return PhotoPalette(
//        palette = null,
//        colorScheme = colorScheme
//    )
//}
//
//fun PhotoPalette(primaryColor: Int, tertiaryColor: Int): PhotoPalette {
//    return PhotoPalette(
//        palette = null,
//        primaryColor = primaryColor,
//        tertiaryColor = tertiaryColor,
//    )
//}
//
//
//data class PhotoPalette constructor(
//    private val palette: SimplePalette?,
//    private val primaryColor: Int,
//    private val tertiaryColor: Int,
//) {
//
//    val containerColor: Color by lazy {
//        val preferredSwatch = palette?.run {
//            listOfNotNull(
//                darkMutedSwatch,
//                mutedSwatch,
//                lightMutedSwatch,
//                darkVibrantSwatch,
//                vibrantSwatch,
//                lightVibrantSwatch,
//            ).firstOrNull()
//        }
//        if (preferredSwatch != null) {
//            Color(preferredSwatch.rgb).copy(0.6f)
//        } else {
//            Color(primaryColor).copy(0.6f)
//        }
//    }
//
//    val containerColorInt: Int by lazy { containerColor.toArgb() }
//
//    val accentColor: Color by lazy {
//        val preferredSwatch = palette?.run {
//            listOfNotNull(
//                lightVibrantSwatch,
//                vibrantSwatch,
//                darkVibrantSwatch,
//                lightMutedSwatch,
//                mutedSwatch,
//                darkMutedSwatch,
//            ).firstOrNull()
//        }
//        if (preferredSwatch != null) {
//            Color(preferredSwatch.rgb).copy(0.6f)
//        } else {
//            Color(tertiaryColor).copy(0.6f)
//        }
//    }
//
//    val accentColorInt: Int by lazy { accentColor.toArgb() }
//
//    val contentColor: Color = Color.White
//    val contentColorInt: Int = contentColor.toArgb()
//}
//
//
//sealed interface PageState {
//
//    @Stable
//    data object Loading : PageState
//
//    @Stable
//    data class Empty(val message: String? = null, val onRetry: (() -> Unit)? = null) : PageState
//
//    @Stable
//    data class Error(val message: String? = null, val onRetry: (() -> Unit)? = null) : PageState
//}
//
//@Composable
//fun PageState(
//    pageState: PageState?,
//    modifier: Modifier = Modifier.fillMaxSize(),
//    loadingInterceptClick: Boolean = true,
//) {
//    val colorScheme = MaterialTheme.colorScheme
//    when (pageState) {
//        is PageState.Loading -> {
//            Box(
//                modifier.ifLet(loadingInterceptClick) {
//                    it.clickable(
//                        onClick = {},
//                        indication = null,  // Remove ripple effect on click
//                        interactionSource = remember { MutableInteractionSource() }
//                    )
//                }
//            ) {
//                CircularProgressIndicator(Modifier.size(30.dp).align(Alignment.Center))
//            }
//        }
//
//        is PageState.Empty -> {
//            Box(
//                modifier.clickable(
//                    onClick = {},
//                    indication = null,  // Remove ripple effect on click
//                    interactionSource = remember { MutableInteractionSource() }
//                )
//            ) {
//                Column(
//                    modifier = Modifier
//                        .size(240.dp)
//                        .background(
//                            colorScheme.primaryContainer.copy(alpha = 0.5f),
//                            RoundedCornerShape(16.dp)
//                        )
//                        .align(Alignment.Center)
//                        .padding(20.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Center
//                ) {
//                    Text(
//                        text = pageState.message ?: "No Content",
//                        color = colorScheme.onPrimaryContainer,
//                        fontSize = 12.sp,
//                        maxLines = 4,
//                        overflow = TextOverflow.Ellipsis,
//                    )
//
//                    if (pageState.onRetry != null) {
//                        Spacer(modifier = Modifier.size(24.dp))
//                        Button(
//                            onClick = {
//                                pageState.onRetry.invoke()
//                            },
//                            shape = RoundedCornerShape(50)
//                        ) {
//                            Text(text = "Reload")
//                        }
//                    }
//                }
//            }
//        }
//
//        is PageState.Error -> {
//            Box(
//                modifier.clickable(
//                    onClick = {},
//                    indication = null,  // Remove ripple effect on click
//                    interactionSource = remember { MutableInteractionSource() }
//                )
//            ) {
//                Column(
//                    modifier = Modifier
//                        .size(240.dp)
//                        .background(
//                            colorScheme.errorContainer.copy(alpha = 0.5f),
//                            RoundedCornerShape(16.dp)
//                        )
//                        .align(Alignment.Center)
//                        .padding(20.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Center
//                ) {
//                    Icon(
//                        painter = painterResource(Res.drawable.ic_error_baseline),
//                        contentDescription = "icon",
//                        tint = colorScheme.onErrorContainer,
//                    )
//
//                    Spacer(modifier = Modifier.size(6.dp))
//                    Text(
//                        text = pageState.message ?: "Load failure",
//                        color = colorScheme.onErrorContainer,
//                        fontSize = 12.sp,
//                        maxLines = 4,
//                        overflow = TextOverflow.Ellipsis,
//                    )
//
//                    if (pageState.onRetry != null) {
//                        Spacer(modifier = Modifier.size(24.dp))
//                        Button(
//                            onClick = {
//                                pageState.onRetry.invoke()
//                            },
//                            shape = RoundedCornerShape(50)
//                        ) {
//                            Text(text = "Retry")
//                        }
//                    }
//                }
//            }
//        }
//
//        else -> {
//            // Show nothing
//        }
//    }
//}
//
//
//class PaletteDecodeInterceptor : DecodeInterceptor {
//
//    override val key: String? = null
//
//    override val sortWeight: Int = 95
//
//    override suspend fun intercept(chain: DecodeInterceptor.Chain): Result<DecodeResult> {
//        val result = chain.proceed()
//        val decodeResult = result.getOrNull() ?: return result
//        val image = decodeResult.image
//        val bitmap = if (image is BitmapImage) {
//            image.bitmap
//        } else {
//            return result
//        }
//        val palette = try {
//            Palette.Builder(bitmap).generate()
//        } catch (e: Throwable) {
//            e.printStackTrace()
//            return result
//        }
//        val propertyString = palette.toPropertyString()
//        @Suppress("FoldInitializerAndIfToElvis", "RedundantSuppression")
//        if (propertyString == null) {
//            chain.sketch.logger.e("PaletteDecodeInterceptor. palette is empty")
//            return result
//        }
//        val newDecodeResult = decodeResult.newResult {
//            addExtras("simple_palette", propertyString)
//        }
//        return Result.success(newDecodeResult)
//    }
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        return other != null && this::class == other::class
//    }
//
//    override fun hashCode(): Int {
//        return this::class.hashCode()
//    }
//
//    override fun toString(): String = "PaletteDecodeInterceptor(sortWeight=$sortWeight)"
//}
//
//val DecodeResult.simplePalette: SimplePalette?
//    get() = extras?.get("simple_palette")
//        ?.trim()?.takeIf { it.isNotEmpty() }
//        ?.let {
//            try {
//                Palette.fromPropertyString(it)
//            } catch (e: Exception) {
//                Exception("SimplePalette fromPropertyString error: $it", e).printStackTrace()
//                null
//            }
//        }
//
//val ImageResult.Success.simplePalette: SimplePalette?
//    get() = extras?.get("simple_palette")
//        ?.trim()?.takeIf { it.isNotEmpty() }
//        ?.let {
//            try {
//                Palette.fromPropertyString(it)
//            } catch (e: Exception) {
//                Exception("SimplePalette fromPropertyString error: $it", e).printStackTrace()
//                null
//            }
//        }
//
//fun Palette.toPropertyString(): String? = buildString {
//    listOf(
//        "dominantSwatch" to dominantSwatch,
//        "vibrantSwatch" to vibrantSwatch,
//        "darkVibrantSwatch" to darkVibrantSwatch,
//        "lightVibrantSwatch" to lightVibrantSwatch,
//        "mutedSwatch" to mutedSwatch,
//        "darkMutedSwatch" to darkMutedSwatch,
//        "lightMutedSwatch" to lightMutedSwatch,
//    ).forEach { (name, value) ->
//        if (value != null) {
//            if (this@buildString.isNotEmpty()) {
//                append(";")
//            }
//            append("${name}=${value.rgb},${value.population}")
//        }
//    }
//}.trim().takeIf { it.isNotEmpty() }
//
//fun Palette.Companion.fromPropertyString(propertyString: String): SimplePalette {
//    val swatchMap = propertyString.split(";").associate { line ->
//        val (name, value) = line.split("=")
//        val (rgb, population) = value.split(",")
//        name to Palette.Swatch(
//            rgb = rgb.toInt(),
//            population = population.toInt(),
//        )
//    }
//    return SimplePalette(
//        dominantSwatch = swatchMap["dominantSwatch"],
//        vibrantSwatch = swatchMap["vibrantSwatch"],
//        darkVibrantSwatch = swatchMap["darkVibrantSwatch"],
//        lightVibrantSwatch = swatchMap["lightVibrantSwatch"],
//        mutedSwatch = swatchMap["mutedSwatch"],
//        darkMutedSwatch = swatchMap["darkMutedSwatch"],
//        lightMutedSwatch = swatchMap["lightMutedSwatch"],
//    )
//}
//
//fun Palette.toSimplePalette(): SimplePalette = SimplePalette(
//    dominantSwatch = dominantSwatch,
//    darkMutedSwatch = darkMutedSwatch,
//    mutedSwatch = mutedSwatch,
//    lightMutedSwatch = lightMutedSwatch,
//    darkVibrantSwatch = darkVibrantSwatch,
//    vibrantSwatch = vibrantSwatch,
//    lightVibrantSwatch = lightVibrantSwatch,
//)
//
//class SimplePalette(
//    val dominantSwatch: Palette.Swatch?,
//    val darkMutedSwatch: Palette.Swatch?,
//    val mutedSwatch: Palette.Swatch?,
//    val lightMutedSwatch: Palette.Swatch?,
//    val darkVibrantSwatch: Palette.Swatch?,
//    val vibrantSwatch: Palette.Swatch?,
//    val lightVibrantSwatch: Palette.Swatch?,
//)
