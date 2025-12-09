package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Image
import ai.saniou.thread.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.Flow

class GetThreadImagesUseCase(
    private val threadRepository: ThreadRepository
) {
    operator fun invoke(threadId: Long): Flow<List<Image>> {
        return threadRepository.getThreadImages(threadId)
    }
}