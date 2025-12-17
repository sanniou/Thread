package ai.saniou.thread.domain.usecase.history

import ai.saniou.thread.domain.model.history.HistoryItem
import ai.saniou.thread.domain.repository.HistoryRepository

class AddHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(item: HistoryItem) {
        historyRepository.addToHistory(item)
    }
}