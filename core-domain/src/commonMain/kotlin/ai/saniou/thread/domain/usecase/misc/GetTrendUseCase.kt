package ai.saniou.thread.domain.usecase.misc

import ai.saniou.thread.domain.model.Trend
import ai.saniou.thread.domain.repository.TrendRepository

class GetTrendUseCase(
    private val trendRepository: TrendRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean): Result<Pair<String, List<Trend>>> {
        return trendRepository.getTrendItems(forceRefresh)
    }
}
