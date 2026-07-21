package ai.saniou.thread.domain.usecase.feed

import ai.saniou.thread.domain.model.feed.TimelineItem
import ai.saniou.thread.domain.repository.FeedRepository
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetTimelineUseCase(
    private val repository: FeedRepository,
) {
    operator fun invoke(
        sourceIds: Set<String>? = null,
        includeReader: Boolean = true,
        socialSourceIds: Set<String>? = null,
        includeSocial: Boolean = true,
    ): Flow<PagingData<TimelineItem>> = repository.getTimelinePaging(
        sourceIds = sourceIds,
        includeReader = includeReader,
        socialSourceIds = socialSourceIds,
        includeSocial = includeSocial,
    )
}
