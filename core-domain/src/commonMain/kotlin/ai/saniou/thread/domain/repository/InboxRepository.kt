package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.inbox.InboxEvent
import ai.saniou.thread.domain.model.inbox.InboxFilter
import ai.saniou.thread.domain.model.inbox.InboxSummary
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface InboxRepository {
    fun getInbox(filter: InboxFilter = InboxFilter()): Flow<PagingData<InboxEvent>>
    fun observeSummary(): Flow<InboxSummary>
    suspend fun upsert(event: InboxEvent)
    suspend fun markRead(id: String, read: Boolean = true)
    suspend fun markAllRead(filter: InboxFilter = InboxFilter())
    suspend fun setSourceMuted(sourceId: String, muted: Boolean)
    suspend fun delete(id: String)
}
