package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.search.GlobalSearchResponse
import ai.saniou.thread.domain.model.search.GlobalSearchType

interface GlobalSearchRepository {
    suspend fun search(
        query: String,
        types: Set<GlobalSearchType> = GlobalSearchType.entries.toSet(),
        limitPerType: Long = 12,
    ): GlobalSearchResponse
}
