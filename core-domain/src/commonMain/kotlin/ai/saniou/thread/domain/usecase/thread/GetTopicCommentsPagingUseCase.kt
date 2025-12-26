package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.Comment as ThreadReply
import ai.saniou.thread.domain.repository.TopicRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetTopicCommentsPagingUseCase(
    private val topicRepository: TopicRepository
) {
    operator fun invoke(
        sourceId: String = "nmb",
        threadId: String,
        isPoOnly: Boolean,
        initialPage: Int = 1
    ): Flow<PagingData<ThreadReply>> {
        return topicRepository.getTopicCommentsPaging(
            sourceId = sourceId,
            threadId = threadId,
            isPoOnly = isPoOnly,
            initialPage = initialPage
        )
    }
}
