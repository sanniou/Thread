package ai.saniou.nmb.workflow.history

import ai.saniou.thread.domain.repository.HistoryRepository
import app.cash.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope

class HistoryViewModel(
    historyUseCase: HistoryRepository
) : ScreenModel {
    val historyThreads = historyUseCase.getHistoryThreads().cachedIn(screenModelScope)
}
