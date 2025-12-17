package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.history.HistoryItem
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistory(): Flow<PagingData<HistoryItem>>
    suspend fun addToHistory(item: HistoryItem)
    suspend fun clearHistory()
}