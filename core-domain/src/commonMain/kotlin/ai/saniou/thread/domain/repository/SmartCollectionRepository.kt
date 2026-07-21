package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.collection.SmartCollection
import kotlinx.coroutines.flow.Flow
import ai.saniou.thread.domain.model.search.GlobalSearchResult
import androidx.paging.PagingData

interface SmartCollectionRepository {
    fun observeCollections(): Flow<List<SmartCollection>>
    suspend fun save(collection: SmartCollection)
    suspend fun delete(id: String)
    suspend fun setPinned(id: String, pinned: Boolean)
    suspend fun reorder(orderedIds: List<String>)
    suspend fun resolve(id: String, limit: Int = 200): List<GlobalSearchResult>
    fun resolvePaging(id: String): Flow<PagingData<GlobalSearchResult>>
}
