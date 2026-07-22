package ai.saniou.forum.workflow.user

import ai.saniou.thread.domain.repository.SourceRepository
import ai.saniou.thread.domain.repository.UserContentRepository
import ai.saniou.thread.domain.usecase.user.FollowUserUseCase
import ai.saniou.thread.domain.usecase.user.GetUserRelationProfileUseCase
import ai.saniou.thread.domain.usecase.user.UnfollowUserUseCase
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
    private val userContentRepository: UserContentRepository,
    private val getUserRelationProfileUseCase: GetUserRelationProfileUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val sourceRepository: SourceRepository,
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
        loadRelation()
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

    private fun loadRelation() {
        screenModelScope.launch {
            val supports = sourceRepository.getSource(sourceId)
                ?.capabilities
                ?.supportsUserFollow == true
            _state.update { it.copy(supportsUserFollow = supports) }
            if (!supports) return@launch
            _state.update { it.copy(isProfileLoading = true) }
            getUserRelationProfileUseCase(sourceId, userHash)
                .onSuccess { profile ->
                    _state.update {
                        it.copy(
                            profile = profile,
                            isProfileLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isProfileLoading = false,
                            actionMessage = error.message,
                        )
                    }
                }
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

            UserDetailContract.Event.ToggleFollow -> toggleFollow()
            UserDetailContract.Event.ConsumeActionMessage -> {
                _state.update { it.copy(actionMessage = null) }
            }
        }
    }

    private fun toggleFollow() {
        val current = state.value
        if (!current.supportsUserFollow || current.isFollowBusy) return
        screenModelScope.launch {
            _state.update { it.copy(isFollowBusy = true) }
            val following = current.profile?.isFollowing == true
            val result = if (following) {
                unfollowUserUseCase(sourceId, userHash)
            } else {
                followUserUseCase(sourceId, userHash)
            }
            result
                .onSuccess { message ->
                    _state.update {
                        val profile = it.profile?.copy(isFollowing = !following)
                        it.copy(
                            profile = profile,
                            isFollowBusy = false,
                            actionMessage = message,
                        )
                    }
                    // Refresh profile for accurate counts / portrait-backed state.
                    getUserRelationProfileUseCase(sourceId, userHash)
                        .onSuccess { profile ->
                            _state.update { it.copy(profile = profile) }
                        }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isFollowBusy = false,
                            actionMessage = error.message,
                        )
                    }
                }
        }
    }
}
