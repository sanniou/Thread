package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.ThreadReply
import ai.saniou.thread.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.Flow

class GetThreadRepliesUseCase(private val threadRepository: ThreadRepository) {
    operator fun invoke(threadId: Long, isPoOnly: Boolean): Flow<List<ThreadReply>> {
        return threadRepository.getThreadReplies(threadId, isPoOnly)
    }
}