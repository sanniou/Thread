package ai.saniou.thread.domain.usecase.reader

import ai.saniou.thread.domain.repository.ReaderRepository
import kotlinx.coroutines.flow.Flow

class GetArticleCountsUseCase(private val repository: ReaderRepository) {
    operator fun invoke(feedSourceId: String): Flow<Pair<Int, Int>> = repository.getArticleCounts(feedSourceId)
}