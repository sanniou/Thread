package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow

class GetTopicImagesUseCase(
    private val topicRepository: TopicRepository
) {
    operator fun invoke(threadId: Long): Flow<List<Image>> {
        return topicRepository.getTopicImages(threadId)
    }
}
