package ai.saniou.forum.workflow.image

import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.usecase.thread.GetTopicImagesUseCase
import kotlinx.coroutines.flow.Flow


/**
 * A generic interface for providing images page by page.
 * This allows the ImagePreviewViewModel to be agnostic of the image source.
 */
interface ImageProvider {
    fun load(): Flow<List<Image>>
}

/**
 * An implementation of ImageProvider that fetches images from a specific thread.
 */
class ThreadImageProvider(
    private val threadId: Long,
    private val getTopicImagesUseCase: GetTopicImagesUseCase
) : ImageProvider {
    override fun load(): Flow<List<Image>> {
        return getTopicImagesUseCase(threadId)
    }
}
