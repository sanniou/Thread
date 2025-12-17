package ai.saniou.thread.domain.usecase.history

import ai.saniou.thread.domain.model.history.HistoryItem
import ai.saniou.thread.domain.repository.HistoryRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    operator fun invoke(): Flow<PagingData<HistoryItem>> {
        return historyRepository.getHistory()
    }
}