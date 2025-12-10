package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.ThreadReply
import ai.saniou.thread.domain.repository.ThreadRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetThreadRepliesPagingUseCase(
    private val threadRepository: ThreadRepository
) {
    operator fun invoke(
        threadId: Long,
        isPoOnly: Boolean,
        initialPage: Int = 1
    ): Flow<PagingData<ThreadReply>> {
        return threadRepository.getThreadRepliesPaging(
            threadId = threadId,
            isPoOnly = isPoOnly,
            initialPage = initialPage
        )
    }
}
