package ai.saniou.nmb.workflow.history

import ai.saniou.nmb.domain.HistoryUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn

class HistoryViewModel(
    historyUseCase: HistoryUseCase
) : ViewModel() {
    val historyThreads = historyUseCase.getHistoryThreads().cachedIn(viewModelScope)
}
