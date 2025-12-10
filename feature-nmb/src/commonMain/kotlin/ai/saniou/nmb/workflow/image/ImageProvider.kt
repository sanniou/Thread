package ai.saniou.nmb.workflow.image

import ai.saniou.thread.domain.model.Image
import ai.saniou.thread.domain.usecase.thread.GetThreadImagesUseCase
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
    private val getThreadImagesUseCase: GetThreadImagesUseCase
) : ImageProvider {
    override fun load(): Flow<List<Image>> {
        return getThreadImagesUseCase(threadId)
    }
}
