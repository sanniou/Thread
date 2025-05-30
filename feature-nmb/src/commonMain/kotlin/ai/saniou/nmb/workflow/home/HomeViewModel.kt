package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.db.table.Notice
import ai.saniou.nmb.domain.NoticeUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val noticeUseCase: NoticeUseCase) : ViewModel() {
    private val _noticeState = MutableStateFlow<Notice?>(null)
    val noticeState: StateFlow<Notice?> = _noticeState

    init {
        fetchNotice()
    }

    private fun fetchNotice() {
        viewModelScope.launch {
            noticeUseCase().collect { result ->
                _noticeState.value = result
            }
        }
    }
}
