package ai.saniou.nmb.data.repository

import ai.saniou.thread.data.source.nmb.remote.dto.ThreadWithInformation
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistoryThreads(): Flow<PagingData<ThreadWithInformation>>
}
