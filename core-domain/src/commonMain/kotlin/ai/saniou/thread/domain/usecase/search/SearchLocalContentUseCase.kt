package ai.saniou.thread.domain.usecase.search

import ai.saniou.thread.domain.model.search.GlobalSearchType
import ai.saniou.thread.domain.repository.GlobalSearchRepository

class SearchLocalContentUseCase(
    private val repository: GlobalSearchRepository,
) {
    suspend operator fun invoke(
        query: String,
        types: Set<GlobalSearchType> = GlobalSearchType.entries.toSet(),
        limitPerType: Long = 12,
    ) = repository.search(query, types, limitPerType)
}
