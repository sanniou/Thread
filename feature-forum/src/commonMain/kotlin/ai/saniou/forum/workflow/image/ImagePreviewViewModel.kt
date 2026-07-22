package ai.saniou.forum.workflow.image

import ai.saniou.forum.workflow.image.ImagePreviewContract.Effect
import ai.saniou.forum.workflow.image.ImagePreviewContract.Event
import ai.saniou.forum.workflow.image.ImagePreviewContract.State
import ai.saniou.thread.domain.model.forum.Image
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.factory

class ImagePreviewViewModel(
    private val imageProvider: ImageProvider?,
    initialImages: List<Image>,
    initialIndex: Int,
) : ScreenModel, ImagePreviewContract {

    private val _state = MutableStateFlow(
        State(
            images = initialImages,
            initialIndex = initialIndex,
            endReached = imageProvider == null || (imageProvider.supportsPaging.not() && initialImages.isNotEmpty()),
        )
    )
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    private var initialLoadDone = initialImages.isNotEmpty()

    init {
        if (initialImages.isEmpty()) {
            loadImages()
        } else if (imageProvider?.supportsPaging == true) {
            // 有初始图 + 分页：不 endReached，允许滑到末尾再 LoadMore
            _state.update { it.copy(endReached = false) }
            initialLoadDone = true
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.LoadMore -> {
                if (imageProvider?.supportsPaging == true && initialLoadDone) {
                    loadMorePaged()
                } else {
                    loadImages()
                }
            }
            is Event.SaveImage -> {
                // UI handles saving directly for now via ImageSaver
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
            .onEach { newImages ->
                initialLoadDone = true
                _state.update {
                    it.copy(
                        isLoading = false,
                        images = if (newImages.isNotEmpty()) newImages else it.images,
                        endReached = !imageProvider.supportsPaging || newImages.isEmpty(),
                    )
                }
            }
            .catch { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
            .launchIn(screenModelScope)
    }

    private fun loadMorePaged() {
        val provider = imageProvider ?: return
        if (_state.value.isLoading || _state.value.endReached) return
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val more = provider.loadMore(_state.value.images)
                _state.update {
                    it.copy(
                        isLoading = false,
                        images = if (more.isEmpty()) it.images else it.images + more,
                        endReached = more.isEmpty(),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}

val imagePreviewViewModelModule = DI.Module("ImagePreviewViewModelModule") {
    bind<ImagePreviewViewModel>() with factory { params: ImagePreviewViewModelParams ->
        ImagePreviewViewModel(
            imageProvider = params.imageProvider,
            initialImages = params.initialImages,
            initialIndex = params.initialIndex,
        )
    }
}

data class ImagePreviewViewModelParams(
    val initialImages: List<Image>,
    val imageProvider: ImageProvider? = null,
    val initialIndex: Int = 0,
)
