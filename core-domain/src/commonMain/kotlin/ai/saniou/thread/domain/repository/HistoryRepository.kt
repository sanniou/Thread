package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Post
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistoryThreads(): Flow<PagingData<Post>>
}