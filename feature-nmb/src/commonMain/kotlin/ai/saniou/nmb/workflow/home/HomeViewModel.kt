package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.db.table.Notice
import ai.saniou.nmb.domain.NoticeUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val noticeUseCase: NoticeUseCase) : ScreenModel {
    private val _noticeState = MutableStateFlow<Notice?>(null)
    val noticeState: StateFlow<Notice?> = _noticeState

    init {
        fetchNotice()
    }

    private fun fetchNotice() {
        screenModelScope.launch {
            noticeUseCase().collect { result ->
                _noticeState.value = result
            }
        }
    }

    fun markAsRead() {
        _noticeState.value?.let {
            noticeUseCase.markAsRead(it.id)
        }
    }
}
