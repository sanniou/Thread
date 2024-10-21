package ai.saniou.nmb.workflow.home

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.data.entity.ShowF
import ai.saniou.nmb.domain.ForumUserCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForumViewModel(private val forumUserCase: ForumUserCase) : ViewModel() {

    private val dataUiState = MutableStateFlow(
        ShowForumUiState(
            showF = emptyList(),
            page = 1,
            id = 0,
            onUpdateForumId = {
                refreshForum(it)
            },
            onClickThread = {

            },
        )
    )

    fun refreshForum(fid: Long) {
        viewModelScope.launch {
            try {
                _uiState.emit(UiStateWrapper.Loading)
                val dataList = forumUserCase(fid, 1)
                updateUiState { state ->
                    state.copy(
                        id = fid,
                        showF = dataList,
                        page = 1,
                    )
                }
                _uiState.emit(dataUiState.value)
            } catch (e: Throwable) {
                _uiState.emit(UiStateWrapper.Error(IllegalStateException(), ""))
            }
        }
    }


    private fun updateUiState(invoke: (ShowForumUiState) -> ShowForumUiState) {
        dataUiState.update(invoke)
    }

//    val uiState = _uiState.asStateFlow()

    private val _uiState = MutableStateFlow<UiStateWrapper>(
        UiStateWrapper.Loading
    )

    val uiState = _uiState.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(3000), UiStateWrapper.Loading
    )
}

data class ShowForumUiState(
    var showF: List<ShowF>,
    var page: Long = 1,
    var id: Long = 0,
    var onUpdateForumId: (Long) -> Unit,
    var onClickThread: (Long) -> Unit,
) : UiStateWrapper
