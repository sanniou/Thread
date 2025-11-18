package ai.saniou.nmb.data.repository

import ai.saniou.nmb.data.entity.ThreadWithInformation
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistoryThreads(): Flow<PagingData<ThreadWithInformation>>
}
