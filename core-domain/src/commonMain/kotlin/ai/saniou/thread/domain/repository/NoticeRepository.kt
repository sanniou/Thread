package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.Notice
import kotlinx.coroutines.flow.Flow

interface NoticeRepository {
    suspend fun getLatestNotice(): Flow<Notice?>
    suspend fun fetchAndCacheNotice()
    fun markAsRead(id: String)
}