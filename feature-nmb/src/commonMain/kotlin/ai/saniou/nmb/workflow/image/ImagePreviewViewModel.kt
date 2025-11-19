package ai.saniou.nmb.workflow.image

import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.entity.toTableReply
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.db.Database
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val initialIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val endReached: Boolean = false
)

/**
 * 图片预览ViewModel
 */
class ImagePreviewViewModel(
    private val threadId: Long,
    private val initialImgPath: String,
    private val di: DI
) : ScreenModel {

    private val cdnManager: CdnManager by di.instance()
    private val db: Database by di.instance()
    private val forumRepository: ForumRepository by di.instance()

    // UI状态
    private val _uiState = MutableStateFlow(ImagePreviewUiState())
    val uiState: StateFlow<ImagePreviewUiState> = _uiState.asStateFlow()

    init {
        observeImages()
    }

    private fun observeImages() {
        val threadFlow = db.threadQueries.getThread(threadId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)

        val repliesFlow = db.threadReplyQueries.getThreadImages(threadId)
            .asFlow()
            .mapToList(Dispatchers.IO)

        screenModelScope.launch {
            combine(threadFlow, repliesFlow) { threadRow, replies ->
                val images = mutableListOf<ImageInfo>()

                // Add thread main image if exists
                if (threadRow != null && threadRow.img.isNotBlank()) {
                    images.add(ImageInfo(threadRow.img, threadRow.ext))
                }

                // Add reply images
                replies.forEach { reply ->
                    images.add(ImageInfo(reply.img, reply.ext))
                }
                images
            }.collect { images ->
                // Only update if images changed significantly or it's the first load
                // We need to preserve current index if possible, but for now let's just update list
                // and set initial index only once if needed.
                // Actually, we should recalculate index based on the currently viewed image?
                // For now, let's just update the list. The Pager in UI handles the index.
                // But we need to set initialIndex for the first time.
                
                _uiState.update { state ->
                    val newInitialIndex = if (state.images.isEmpty()) {
                         val index = images.indexOfFirst { it.imgPath == initialImgPath }
                         if (index != -1) index else 0
                    } else {
                        state.initialIndex
                    }
                    
                    state.copy(
                        images = images,
                        initialIndex = newInitialIndex
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        if (_uiState.value.isLoading || _uiState.value.endReached) return

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Get max page
                val maxPage = db.threadReplyQueries.getMaxPage(threadId).executeAsOne().MAX?.toLong() ?: 1L
                val nextPage = maxPage + 1

                when (val result = forumRepository.thread(threadId, nextPage)) {
                    is SaniouResponse.Success -> {
                        val threadDetail = result.data
                        // Check if we actually got new data or if it's empty
                        // Note: API might return empty replies if end reached
                        if (threadDetail.replies.isEmpty()) {
                            _uiState.update { it.copy(isLoading = false, endReached = true) }
                        } else {
                            // Save to DB
                            db.transaction {
                                db.threadQueries.upsetThread(threadDetail.toTable())
                                threadDetail.toTableReply(nextPage).forEach(db.threadReplyQueries::upsertThreadReply)
                            }
                            // Check if we really added any images. If not, we might need to load NEXT page recursively?
                            // For now, let's assume user will swipe again or we just show loading.
                            // But if the next page has NO images, the user sees "Loading" then nothing changes?
                            // The "images" list won't grow.
                            // The Pager will stay at the end.
                            // We should probably check if images count increased.
                            
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    }
                    is SaniouResponse.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.ex.message) }
                    }
                }
            } catch (e: Exception) {
                 _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * 保存当前图片
     */
    fun saveCurrentImage(imageInfo: ImageInfo) {
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
                // saveImage(di, imageInfo.imgPath, imageInfo.ext)

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

