package ai.saniou.nmb.workflow.user

import ai.saniou.thread.data.source.nmb.NmbSource
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserDetailViewModel(
    private val userHash: String,
    private val nmbRepository: NmbSource
) : ScreenModel {

    private val _state = MutableStateFlow(
        UserDetailContract.State(
            userHash = userHash
        )
    )
    val state = _state.asStateFlow()

    private val _effect = Channel<UserDetailContract.Effect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val threads = nmbRepository.getUserThreadsPager(userHash)
        val replies = nmbRepository.getUserRepliesPager(userHash)
        _state.update {
            it.copy(
                threads = threads,
                replies = replies
            )
        }
    }

    fun handleEvent(event: UserDetailContract.Event) {
        when (event) {
            is UserDetailContract.Event.SwitchTab -> {
                _state.update { it.copy(currentTab = event.tab) }
            }

            UserDetailContract.Event.Back -> {
                screenModelScope.launch {
                    _effect.send(UserDetailContract.Effect.NavigateBack)
                }
            }
        }
    }
}
