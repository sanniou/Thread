package ai.saniou.nmb.domain

import ai.saniou.thread.data.source.nmb.remote.dto.ThreadWithInformation
import ai.saniou.nmb.data.repository.HistoryRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class HistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    fun getHistoryThreads(): Flow<PagingData<ThreadWithInformation>> {
        return historyRepository.getHistoryThreads()
    }
}
