package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.repository.TopicRepository
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetTopicCommentsPagerUseCase(
    private val topicRepository: TopicRepository
) {
    operator fun invoke(sourceId: String, topicId: String, isPoOnly: Boolean): Flow<PagingData<Comment>> {
        return topicRepository.getTopicCommentsPager(sourceId, topicId, isPoOnly)
    }
}
