package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.repository.ThreadRepository

class UpdateThreadLastReadReplyIdUseCase(private val threadRepository: ThreadRepository) {
    suspend operator fun invoke(threadId: String, replyId: String) {
        threadRepository.updateThreadLastReadReplyId(threadId, replyId)
    }
}
