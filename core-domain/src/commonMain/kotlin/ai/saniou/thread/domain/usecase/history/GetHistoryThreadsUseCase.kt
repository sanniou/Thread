package ai.saniou.thread.domain.usecase.history

import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.repository.HistoryRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetHistoryThreadsUseCase(
    private val historyRepository: HistoryRepository
) {
    fun invoke(): Flow<PagingData<Post>> {
        return historyRepository.getHistoryThreads()
    }
}
