package ai.saniou.forum.workflow.image

import ai.saniou.forum.workflow.image.ImagePreviewContract.Effect
import ai.saniou.forum.workflow.image.ImagePreviewContract.Event
import ai.saniou.forum.workflow.image.ImagePreviewContract.State
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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

    init {
        if (initialImages.isEmpty()) {
            loadImages()
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.LoadMore -> loadImages() // Renamed from loadMoreImages
            is Event.SaveImage -> {
                // TODO: 保存图片
            }
        }
    }

    private fun loadImages() {
        if (_state.value.isLoading || _state.value.endReached) return
        imageProvider ?: return

        imageProvider.load()
            .onStart {
                _state.update { it.copy(isLoading = true) }
            }
            .map { images ->
                images.map { ImageInfo(it.name, it.ext) }
            }
            .onEach { newImages ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        images = newImages,
                        endReached = true // Since we load all images at once
                    )
                }
            }
            .catch { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
            .launchIn(screenModelScope)
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
