package ai.saniou.thread.domain.usecase.forum

import ai.saniou.thread.domain.model.forum.Topic as Post
import ai.saniou.thread.domain.repository.ForumRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetForumThreadsPagingUseCase(
    private val forumRepository: ForumRepository
) {
    operator fun invoke(
        sourceId: String,
        fid: String,
        isTimeline: Boolean,
        initialPage: Int = 1
    ): Flow<PagingData<Post>> {
        return forumRepository.getForumThreadsPaging(
            sourceId = sourceId,
            fid = fid,
            isTimeline = isTimeline,
            initialPage = initialPage
        )
    }
}
