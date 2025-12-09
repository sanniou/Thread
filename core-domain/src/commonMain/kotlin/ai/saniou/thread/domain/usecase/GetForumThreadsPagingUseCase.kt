package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.repository.ForumRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetForumThreadsPagingUseCase(
    private val forumRepository: ForumRepository
) {
    operator fun invoke(
        fid: Long,
        isTimeline: Boolean,
        initialPage: Int = 1
    ): Flow<PagingData<Post>> {
        return forumRepository.getForumThreadsPaging(
            fid = fid,
            isTimeline = isTimeline,
            initialPage = initialPage
        )
    }
}