package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.Flow

class GetThreadImagesUseCase(
    private val threadRepository: ThreadRepository
) {
    operator fun invoke(threadId: Long): Flow<List<Image>> {
        return threadRepository.getTopicImages(threadId)
    }
}
