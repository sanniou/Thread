package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.repository.TopicRepository

class UpdateTopicLastReadCommentIdUseCase(private val topicRepository: TopicRepository) {
    suspend operator fun invoke(threadId: String, replyId: String) {
        topicRepository.updateTopicLastReadCommentId(threadId, replyId)
    }
}
