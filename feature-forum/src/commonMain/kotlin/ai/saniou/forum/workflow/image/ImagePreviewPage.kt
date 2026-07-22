package ai.saniou.forum.workflow.image

import ai.saniou.coreui.widgets.PhotoPagerBackground
import ai.saniou.coreui.widgets.ZoomAsyncImage
import ai.saniou.coreui.widgets.palette.PhotoPalette
import ai.saniou.forum.workflow.image.ImagePreviewContract.Event
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.post_page_back
import thread.feature_forum.generated.resources.s_12f92d07e1
import thread.feature_forum.generated.resources.s_7d25c5442f
import thread.feature_forum.generated.resources.action_save

data class ImagePreviewPage(
    val params: ImagePreviewViewModelParams,
) : Screen {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val di = localDI()
        val viewModel: ImagePreviewViewModel =
            rememberScreenModel(tag = params.toString()) {
                di.direct.instance(arg = params)
            }
        val uiState by viewModel.state.collectAsState()

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
        val thumbnailUrl = currentImageInfo?.thumbnailUrl

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
                ) { page ->
                    if (page < uiState.images.size) {
                        val image = uiState.images[page]
                        ZoomAsyncImage(
                            uri = image.originalUrl,
                            thumbnailUrl = image.thumbnailUrl,
                            contentDescription = stringResource(Res.string.s_7d25c5442f),
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    contentDescription = stringResource(Res.string.post_page_back),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(50),
                color = photoPalette.containerColor,
                border = BorderStroke(1.dp, photoPalette.contentColor.copy(alpha = 0.18f)),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                Box(
                    Modifier
                        .height(36.dp)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val numberText by remember {
                        derivedStateOf {
                            if (uiState.images.isNotEmpty()) {
                                "${pagerState.currentPage + 1} / ${uiState.images.size}"
                            } else {
                                "0 / 0"
                            }
                        }
                    }
                    Text(
                        text = numberText,
                        textAlign = TextAlign.Center,
                        color = photoPalette.contentColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val image = uiState.images[pagerState.currentPage]
                        imageSaver.save(image.originalUrl)
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = photoPalette.containerColor,
                    contentColor = photoPalette.contentColor
                ),
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = stringResource(Res.string.action_save),
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
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp),
                shape = RoundedCornerShape(50),
                color = photoPalette.containerColor,
                border = BorderStroke(1.dp, photoPalette.contentColor.copy(alpha = 0.18f)),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                Text(
                    stringResource(Res.string.s_12f92d07e1),
                    color = photoPalette.contentColor,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
