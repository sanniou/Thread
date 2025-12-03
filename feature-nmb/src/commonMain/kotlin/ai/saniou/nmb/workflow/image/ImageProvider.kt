package ai.saniou.nmb.workflow.image

import ai.saniou.nmb.domain.GetThreadImagesUseCase

/**
 * A generic interface for providing images page by page.
 * This allows the ImagePreviewViewModel to be agnostic of the image source.
 */
interface ImageProvider {
    suspend fun load(page: Int): Result<List<ImageInfo>>
}

/**
 * An implementation of ImageProvider that fetches images from a specific thread.
 */
class ThreadImageProvider(
    private val threadId: Long,
    private val getThreadImagesUseCase: GetThreadImagesUseCase
) : ImageProvider {
    override suspend fun load(page: Int): Result<List<ImageInfo>> {
        return getThreadImagesUseCase(threadId, page)
    }
}
