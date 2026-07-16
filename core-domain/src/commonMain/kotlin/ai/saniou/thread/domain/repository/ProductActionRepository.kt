package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.activity.ProductActionRecord
import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.model.activity.ProductActionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ProductActionHistoryRepository {
    fun observe(): Flow<List<ProductActionRecord>>
    suspend fun upsert(record: ProductActionRecord)
    suspend fun clearCompleted()
}

interface ProductActionExecutor {
    val runningConflictKeys: StateFlow<Set<String>>

    suspend fun execute(request: ProductActionRequest): Result<ProductActionResult>
}
