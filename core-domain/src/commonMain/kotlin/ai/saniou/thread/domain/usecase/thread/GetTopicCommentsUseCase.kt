package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.repository.TopicRepository
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetTopicCommentsUseCase(private val topicRepository: TopicRepository) {
    operator fun invoke(
        sourceId: String,
        threadId: String,
        isPoOnly: Boolean,
        initialPage: Int = 1,
    ): Flow<PagingData<Comment>> {
        return topicRepository.getTopicCommentsPager(sourceId, threadId, isPoOnly)
    }
}
