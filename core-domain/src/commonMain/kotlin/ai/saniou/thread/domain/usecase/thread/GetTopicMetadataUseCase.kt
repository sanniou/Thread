package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.TopicMetadata
import ai.saniou.thread.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow

class GetTopicMetadataUseCase(
    private val topicRepository: TopicRepository
) {
    operator fun invoke(
        sourceId: String,
        topicId: String,
        forceRefresh: Boolean = false
    ): Flow<TopicMetadata> {
        return topicRepository.getTopicMetadata(sourceId, topicId, forceRefresh)
    }
}
