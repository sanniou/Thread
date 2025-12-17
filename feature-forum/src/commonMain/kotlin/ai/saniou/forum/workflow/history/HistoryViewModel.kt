package ai.saniou.forum.workflow.history

import ai.saniou.thread.domain.usecase.history.GetHistoryUseCase
import app.cash.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope

class HistoryViewModel(
    getHistoryUseCase: GetHistoryUseCase
) : ScreenModel {
    val historyItems = getHistoryUseCase().cachedIn(screenModelScope)
}
