package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.Comment as ThreadReply
import ai.saniou.thread.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow

class GetTopicCommentsUseCase(private val topicRepository: TopicRepository) {
    operator fun invoke(threadId: Long, isPoOnly: Boolean): Flow<List<ThreadReply>> {
        return topicRepository.getTopicComments(threadId, isPoOnly)
    }
}
