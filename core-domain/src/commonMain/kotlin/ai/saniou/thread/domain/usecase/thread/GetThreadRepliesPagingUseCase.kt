package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.ThreadReply
import ai.saniou.thread.domain.repository.ThreadRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetThreadRepliesPagingUseCase(
    private val threadRepository: ThreadRepository
) {
    operator fun invoke(
        sourceId: String = "nmb",
        threadId: String,
        isPoOnly: Boolean,
        initialPage: Int = 1
    ): Flow<PagingData<ThreadReply>> {
        return threadRepository.getThreadRepliesPaging(
            sourceId = sourceId,
            threadId = threadId,
            isPoOnly = isPoOnly,
            initialPage = initialPage
        )
    }
}
