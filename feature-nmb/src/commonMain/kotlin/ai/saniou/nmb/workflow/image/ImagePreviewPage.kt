package ai.saniou.nmb.workflow.image

import ai.saniou.coreui.widgets.PhotoPagerBackground
import ai.saniou.coreui.widgets.ZoomAsyncImage
import ai.saniou.coreui.widgets.palette.PhotoPalette
import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.workflow.image.ImagePreviewContract.Event
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

data class ImagePreviewPage(
    val params: ImagePreviewViewModelParams,
    val di: DI = nmbdi,
) : Screen {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val viewModel: ImagePreviewViewModel =
            rememberScreenModel(tag = params.toString()) {
                di.direct.instance(arg = params)
            }
        val uiState by viewModel.state.collectAsState()
        val cdnManager by di.instance<CdnManager>()

        val pagerState = rememberPagerState(
            initialPage = uiState.initialIndex,
            pageCount = { uiState.images.size }
        )

        LaunchedEffect(uiState.initialIndex) {
            if (uiState.images.isNotEmpty() && pagerState.currentPage != uiState.initialIndex) {
                pagerState.scrollToPage(uiState.initialIndex)
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage >= uiState.images.size - 1 && !uiState.endReached && !uiState.isLoading) {
                viewModel.onEvent(Event.LoadMore)
            }
        }

        val colorScheme = MaterialTheme.colorScheme
        val photoPaletteState = remember { mutableStateOf(PhotoPalette(colorScheme)) }
        var isHudVisible by remember { mutableStateOf(true) }
        val currentImageInfo =
            if (uiState.images.isNotEmpty() && pagerState.currentPage < uiState.images.size) {
                uiState.images[pagerState.currentPage]
            } else {
                null
            }
        val thumbnailUrl = currentImageInfo?.let {
            cdnManager.buildImageUrl(it.imgPath, it.ext, true)
        }

        if (thumbnailUrl != null) {
            PhotoPagerBackground(thumbnailUrl, photoPaletteState)
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.images.isEmpty()) {
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
                        val thumbUrl =
                            cdnManager.buildImageUrl(imageInfo.imgPath, imageInfo.ext, true)
                        ZoomAsyncImage(
                            uri = imageUrl,
                            thumbnailUrl = thumbUrl,
                            contentDescription = "预览图片",
                            photoPalette = photoPaletteState.value,
                            showTools = isHudVisible,
                            modifier = Modifier.fillMaxSize().clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { isHudVisible = !isHudVisible },
                        )
                    }
                }
            }
            AnimatedVisibility(visible = isHudVisible) {
                ImagePreviewHud(
                    pagerState = pagerState,
                    uiState = uiState,
                    photoPalette = photoPaletteState.value
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImagePreviewHud(
    pagerState: PagerState,
    uiState: ImagePreviewContract.State,
    photoPalette: PhotoPalette
) {
    val navigator = LocalNavigator.currentOrThrow
    val imageSaver = rememberImageSaver()
    val coroutineScope = rememberCoroutineScope()
    val cdnManager by nmbdi.instance<CdnManager>()

    Box(modifier = Modifier.fillMaxSize()) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { navigator.pop() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = photoPalette.containerColor,
                    contentColor = photoPalette.contentColor
                ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                Modifier
                    .height(40.dp)
                    .background(
                        color = photoPalette.containerColor,
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                val numberText by remember {
                    derivedStateOf {
                        if (uiState.images.isNotEmpty()) {
                            "${pagerState.currentPage + 1}/${uiState.images.size}"
                        } else {
                            "0/0"
                        }
                    }
                }
                Text(
                    text = numberText,
                    textAlign = TextAlign.Center,
                    color = photoPalette.contentColor,
                    style = TextStyle(lineHeight = 12.sp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val imageInfo = uiState.images[pagerState.currentPage]
                        val imageUrl =
                            cdnManager.buildImageUrl(imageInfo.imgPath, imageInfo.ext, false)
                        imageSaver.save(imageUrl)
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = photoPalette.containerColor,
                    contentColor = photoPalette.contentColor
                ),
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "保存",
                )
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp) // Adjusted padding to not overlap with ZoomImageTool
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // End of list message
        if (uiState.endReached && uiState.images.size > 1 && pagerState.currentPage == uiState.images.size - 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp) // Adjusted padding
            ) {
                Text("没有更多图片了", color = Color.White)
            }
        }
    }
}
