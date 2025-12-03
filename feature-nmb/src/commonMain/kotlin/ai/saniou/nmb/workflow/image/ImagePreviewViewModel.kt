package ai.saniou.nmb.workflow.image

import ai.saniou.nmb.workflow.image.ImagePreviewContract.Effect
import ai.saniou.nmb.workflow.image.ImagePreviewContract.Event
import ai.saniou.nmb.workflow.image.ImagePreviewContract.State
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.factory

/**
 * 图片预览数据类
 */
data class ImageInfo(
    val imgPath: String,
    val ext: String,
    val isThumb: Boolean = false,
)

class ImagePreviewViewModel(
    private val imageProvider: ImageProvider?,
    initialImages: List<ImageInfo>,
    initialIndex: Int,
) : ScreenModel, ImagePreviewContract {

    private val _state = MutableStateFlow(
        State(
            images = initialImages,
            initialIndex = initialIndex,
            endReached = imageProvider == null

        )
    )
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    private var currentPage = 1
    private var isInitialLoad = true

    init {
        // If the initial list is empty, start loading immediately.
        if (initialImages.isEmpty()) {
            loadMoreImages()
        } else {
            // Determine the page of the initial image to continue loading from there.
            // This logic assumes a fixed number of items per page and might need adjustment.
            // For simplicity, we'll just start loading from page 2 if the initial list is not empty.
            currentPage = 2
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.LoadMore -> loadMoreImages()
            is Event.SaveImage -> {
                // TODO: 保存图片
            }
        }
    }

    private fun loadMoreImages() {
        if (_state.value.isLoading || _state.value.endReached) return

        imageProvider ?: return

        _state.update { it.copy(isLoading = true) }

        screenModelScope.launch {
            imageProvider.load(currentPage)
                .onSuccess { newImages ->
                    if (newImages.isEmpty()) {
                        _state.update { it.copy(isLoading = false, endReached = true) }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                images = it.images + newImages,
                            )
                        }
                        currentPage++
                    }
                    isInitialLoad = false
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}

val imagePreviewViewModelModule = DI.Module("ImagePreviewViewModelModule") {
    bind<ImagePreviewViewModel>() with factory { params: ImagePreviewViewModelParams ->
        ImagePreviewViewModel(
            imageProvider = params.imageProvider,
            initialImages = params.initialImages,
            initialIndex = params.initialIndex
        )
    }
}

data class ImagePreviewViewModelParams(
    val initialImages: List<ImageInfo>,
    val imageProvider: ImageProvider? = null,
    val initialIndex: Int = 0,
)
