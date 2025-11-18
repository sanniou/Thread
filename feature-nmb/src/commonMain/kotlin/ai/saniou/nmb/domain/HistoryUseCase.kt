package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.ThreadWithInformation
import ai.saniou.nmb.data.repository.HistoryRepository
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class HistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    fun getHistoryThreads(): Flow<PagingData<ThreadWithInformation>> {
        return historyRepository.getHistoryThreads()
    }
}