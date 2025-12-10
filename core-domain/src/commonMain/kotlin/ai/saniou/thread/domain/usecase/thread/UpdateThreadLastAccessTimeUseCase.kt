package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.repository.ThreadRepository

class UpdateThreadLastAccessTimeUseCase(private val threadRepository: ThreadRepository) {
    suspend operator fun invoke(threadId: Long, time: Long) {
        threadRepository.updateThreadLastAccessTime(threadId, time)
    }
}