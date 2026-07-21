package ai.saniou.forum.workflow.subscription

import ai.saniou.coreui.state.toAppError
import ai.saniou.forum.workflow.subscription.SubscriptionContract.Effect
import ai.saniou.forum.workflow.subscription.SubscriptionContract.Event
import ai.saniou.forum.workflow.subscription.SubscriptionContract.State
import ai.saniou.thread.domain.usecase.subscription.GenerateRandomSubscriptionIdUseCase
import ai.saniou.thread.domain.usecase.subscription.GetSubscriptionFeedUseCase
import ai.saniou.thread.domain.usecase.subscription.ObserveActiveSubscriptionKeyUseCase
import ai.saniou.thread.domain.usecase.subscription.SaveSubscriptionKeyUseCase
import ai.saniou.thread.domain.usecase.subscription.SyncLocalSubscriptionsUseCase
import ai.saniou.thread.domain.usecase.subscription.ToggleSubscriptionUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.s_6ae7f0c15b
import thread.feature_forum.generated.resources.s_7a88fa73fc
import thread.feature_forum.generated.resources.s_90b3eb11f7
import thread.feature_forum.generated.resources.s_cdab65f5b9

class SubscriptionViewModel(
    private val getSubscriptionFeedUseCase: GetSubscriptionFeedUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val syncLocalSubscriptionsUseCase: SyncLocalSubscriptionsUseCase,
    private val observeActiveSubscriptionKeyUseCase: ObserveActiveSubscriptionKeyUseCase,
    private val saveSubscriptionKeyUseCase: SaveSubscriptionKeyUseCase,
    private val generateRandomSubscriptionIdUseCase: GenerateRandomSubscriptionIdUseCase,
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    init {
        screenModelScope.launch {
            observeActiveSubscriptionKeyUseCase()
                .distinctUntilChanged()
                .collect { key ->
                    if (key == null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isShowSubscriptionIdDialog = true,
                                error = IllegalStateException(getString(Res.string.s_90b3eb11f7)).toAppError()
                            )
                        }
                    } else {
                        loadFeeds(key)
                    }
                }
        }
    }

    private suspend fun loadFeeds(id: String) {
        _state.update { it.copy(subscriptionId = id, isLoading = true) }
        try {
            val feeds = getSubscriptionFeedUseCase(id)
            _state.update {
                it.copy(
                    feeds = feeds,
                    isLoading = false,
                    error = null,
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(isLoading = false, error = e.toAppError { screenModelScope.launch { loadFeeds(id) } })
            }
        }
    }


    fun onEvent(event: Event) {
        when (event) {
            is Event.OnUnsubscribe -> unsubscribe(event.threadId)
            is Event.OnSetSubscriptionId -> setSubscriptionId(event.id)
            Event.OnGenerateRandomSubscriptionId -> generateRandomSubscriptionId()
            Event.OnShowSubscriptionIdDialog -> _state.update { it.copy(isShowSubscriptionIdDialog = true) }
            Event.OnHideSubscriptionIdDialog -> _state.update { it.copy(isShowSubscriptionIdDialog = false) }
            Event.OnPull -> pull()
            Event.OnPush -> push()
        }
    }

    private fun pull() {
        val id = state.value.subscriptionId ?: return
        screenModelScope.launch {
            loadFeeds(id)
        }
    }

    private fun push() {
        val id = state.value.subscriptionId ?: return
        screenModelScope.launch {
            try {
                syncLocalSubscriptionsUseCase(id)
                _effect.send(Effect.OnPushResult(true, getString(Res.string.s_6ae7f0c15b)))
                loadFeeds(id)
            } catch (e: Exception) {
                _effect.send(Effect.OnPushResult(false, getString(Res.string.s_7a88fa73fc, e.message.orEmpty())))
            }
        }
    }

    private fun setSubscriptionId(id: String) {
        screenModelScope.launch {
            saveSubscriptionKeyUseCase(id)
            _state.update { it.copy(isShowSubscriptionIdDialog = false) }
            loadFeeds(id)
        }
    }

    private fun generateRandomSubscriptionId() {
        val randomId = generateRandomSubscriptionIdUseCase()
        setSubscriptionId(randomId)
    }

    private fun unsubscribe(threadId: String) {
        val id = state.value.subscriptionId ?: return
        screenModelScope.launch {
            val result = toggleSubscriptionUseCase(id, threadId, true)
            result.onSuccess {
                _effect.send(Effect.OnUnsubscribeResult(true, it))
            }.onFailure {
                _effect.send(Effect.OnUnsubscribeResult(false, getString(Res.string.s_cdab65f5b9, it.message.orEmpty())))
            }
        }
    }
}
