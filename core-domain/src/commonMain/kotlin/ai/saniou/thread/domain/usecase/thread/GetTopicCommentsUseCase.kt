package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow

class GetTopicCommentsUseCase(private val topicRepository: TopicRepository) {
    operator fun invoke(threadId: Long, isPoOnly: Boolean): Flow<List<Comment>> {
        return topicRepository.getTopicComments(threadId, isPoOnly)
    }
}
