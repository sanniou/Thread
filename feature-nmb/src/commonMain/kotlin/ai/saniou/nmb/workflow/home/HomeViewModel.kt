package ai.saniou.nmb.workflow.home

import ai.saniou.thread.domain.model.Notice
import ai.saniou.thread.domain.usecase.notice.GetNoticeUseCase
import ai.saniou.thread.domain.usecase.notice.MarkNoticeAsReadUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getNoticeUseCase: GetNoticeUseCase,
    private val markNoticeAsReadUseCase: MarkNoticeAsReadUseCase,
) : ScreenModel {
    private val _noticeState = MutableStateFlow<Notice?>(null)
    val noticeState: StateFlow<Notice?> = _noticeState

    init {
        fetchNotice()
    }

    private fun fetchNotice() {
        screenModelScope.launch {
            getNoticeUseCase().collect { result ->
                _noticeState.value = result
            }
        }
    }

    fun markAsRead() {
        _noticeState.value?.let {
            markNoticeAsReadUseCase(it.id)
        }
    }
}
