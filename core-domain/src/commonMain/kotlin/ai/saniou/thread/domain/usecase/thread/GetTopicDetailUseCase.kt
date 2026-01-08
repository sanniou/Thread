package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow

class GetTopicDetailUseCase(
    private val topicRepository: TopicRepository
) {
    operator fun invoke(sourceId: String, topicId: String, forceRefresh: Boolean = false): Flow<Topic> {
        return topicRepository.getTopicDetail(sourceId, topicId, forceRefresh)
    }
}