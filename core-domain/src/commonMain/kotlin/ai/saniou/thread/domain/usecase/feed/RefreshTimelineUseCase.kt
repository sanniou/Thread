package ai.saniou.thread.domain.usecase.feed

import ai.saniou.thread.domain.model.feed.FeedRefreshReport
import ai.saniou.thread.domain.repository.FeedRepository

class RefreshTimelineUseCase(
    private val repository: FeedRepository,
) {
    suspend operator fun invoke(
        sourceIds: Set<String>? = null,
        includeReader: Boolean = true,
        socialSourceIds: Set<String>? = null,
        includeSocial: Boolean = true,
    ): FeedRefreshReport = repository.refreshTimeline(
        sourceIds = sourceIds,
        includeReader = includeReader,
        socialSourceIds = socialSourceIds,
        includeSocial = includeSocial,
    )
}
