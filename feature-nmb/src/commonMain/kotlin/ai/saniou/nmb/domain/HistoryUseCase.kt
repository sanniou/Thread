package ai.saniou.nmb.domain

import ai.saniou.thread.data.source.nmb.remote.dto.ThreadWithInformation
import ai.saniou.thread.data.source.nmb.NmbSource
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class HistoryUseCase(
    private val historyRepository: NmbSource
) {
    fun getHistoryThreads(): Flow<PagingData<ThreadWithInformation>> {
        return historyRepository.getHistoryThreads()
    }
}
