package ai.saniou.nmb.domain

import ai.saniou.nmb.data.repository.NmbRepository
import ai.saniou.nmb.workflow.image.ImageInfo

class GetThreadImagesUseCase(
    private val nmbRepository: NmbRepository
) {
    suspend operator fun invoke(threadId: Long, page: Int): Result<List<ImageInfo>> {
        return nmbRepository.getThreadRepliesByPage(threadId, page).map { replies ->
            replies.filter { it.img.isNotBlank() }
                .map { ImageInfo(it.img, it.ext) }
        }
    }
}