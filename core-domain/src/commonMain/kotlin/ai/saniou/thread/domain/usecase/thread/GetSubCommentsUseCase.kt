package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.repository.TopicRepository

class GetSubCommentsUseCase(
    private val topicRepository: TopicRepository
) {
    suspend operator fun invoke(sourceId: String, topicId: String, commentId: String, page: Int): Result<List<Comment>> {
        return topicRepository.getSubComments(sourceId, topicId, commentId, page)
    }
}