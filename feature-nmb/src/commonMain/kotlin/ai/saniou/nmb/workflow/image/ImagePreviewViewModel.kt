package ai.saniou.nmb.workflow.image

import ai.saniou.nmb.data.manager.CdnManager
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

/**
 * 图片预览数据类
 */
data class ImageInfo(
    val imgPath: String,
    val ext: String,
    val isThumb: Boolean = false
)

/**
 * 图片预览页面状态
 */
data class ImagePreviewUiState(
    val images: List<ImageInfo> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null
)

/**
 * 图片预览ViewModel
 */
class ImagePreviewViewModel(
    private val di: DI
) : ScreenModel {

    private val cdnManager: CdnManager by di.instance()

    // UI状态
    private val _uiState = MutableStateFlow(ImagePreviewUiState())
    val uiState: StateFlow<ImagePreviewUiState> = _uiState.asStateFlow()

    /**
     * 设置图片列表
     */
    fun setImages(images: List<ImageInfo>, initialIndex: Int = 0) {
        _uiState.update { state ->
            state.copy(
                images = images,
                currentIndex = initialIndex.coerceIn(0, images.size - 1)
            )
        }
    }

    /**
     * 设置当前图片
     */
    fun setCurrentImage(imgPath: String, ext: String) {
        val currentState = _uiState.value
        val images = listOf(ImageInfo(imgPath, ext, false))

        _uiState.update { state ->
            state.copy(
                images = images,
                currentIndex = 0
            )
        }
    }

    /**
     * 切换到下一张图片
     */
    fun nextImage() {
        val currentState = _uiState.value
        if (currentState.currentIndex < currentState.images.size - 1) {
            _uiState.update { state ->
                state.copy(currentIndex = state.currentIndex + 1)
            }
        }
    }

    /**
     * 切换到上一张图片
     */
    fun previousImage() {
        val currentState = _uiState.value
        if (currentState.currentIndex > 0) {
            _uiState.update { state ->
                state.copy(currentIndex = state.currentIndex - 1)
            }
        }
    }

    /**
     * 保存当前图片
     */
    fun saveCurrentImage() {
        val currentState = _uiState.value
        if (currentState.images.isEmpty() || currentState.currentIndex >= currentState.images.size) {
            return
        }

        val currentImage = currentState.images[currentState.currentIndex]

        _uiState.update { state ->
            state.copy(
                isSaving = true,
                saveSuccess = false,
                saveError = null
            )
        }

        screenModelScope.launch {
            try {
                // 这里实现保存图片的逻辑
                // 由于平台差异，具体实现会在平台特定的代码中完成
                saveImage(di, currentImage.imgPath, currentImage.ext)

                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        saveError = e.message ?: "保存图片失败"
                    )
                }
            }
        }
    }

}

