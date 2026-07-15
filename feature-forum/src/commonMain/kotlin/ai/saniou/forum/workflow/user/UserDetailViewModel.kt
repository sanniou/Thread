package ai.saniou.forum.workflow.user

import ai.saniou.thread.domain.repository.UserContentRepository
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserDetailViewModel(
    private val sourceId: String,
    private val userHash: String,
    private val userContentRepository: UserContentRepository
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
        val threads = userContentRepository.getUserTopics(sourceId, userHash)
        val replies = userContentRepository.getUserComments(sourceId, userHash)
        _state.update {
            it.copy(
                topics = threads,
                comments = replies
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
