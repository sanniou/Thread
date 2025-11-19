package ai.saniou.nmb.workflow.image

/**
 * 图片预览数据类
 */
data class ImageInfo(
    val imgPath: String,
    val ext: String,
    val isThumb: Boolean = false,
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
    val endReached: Boolean = false,
)

