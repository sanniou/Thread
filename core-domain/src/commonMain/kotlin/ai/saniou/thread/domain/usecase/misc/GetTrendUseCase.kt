package ai.saniou.thread.domain.usecase.misc

import ai.saniou.thread.domain.model.forum.TrendResult
import ai.saniou.thread.domain.repository.TrendRepository

class GetTrendUseCase(
    private val trendRepository: TrendRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean, dayOffset: Int = 0): Result<TrendResult> {
        return trendRepository.getTrendItems(forceRefresh, dayOffset)
    }
}
