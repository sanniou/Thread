package ai.saniou.nmb.workflow.history

import ai.saniou.nmb.domain.HistoryUseCase
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope

class HistoryViewModel(
    historyUseCase: HistoryUseCase
) : ScreenModel {
    val historyThreads = historyUseCase.getHistoryThreads().cachedIn(screenModelScope)
}
