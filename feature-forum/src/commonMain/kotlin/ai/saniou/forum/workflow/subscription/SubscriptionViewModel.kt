package ai.saniou.forum.workflow.subscription

import ai.saniou.coreui.state.toAppError
import ai.saniou.forum.workflow.subscription.SubscriptionContract.Effect
import ai.saniou.forum.workflow.subscription.SubscriptionContract.Event
import ai.saniou.forum.workflow.subscription.SubscriptionContract.State
import ai.saniou.thread.data.source.nmb.DataPolicy
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

class SubscriptionViewModel(
    private val getSubscriptionFeedUseCase: GetSubscriptionFeedUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val syncLocalSubscriptionsUseCase: SyncLocalSubscriptionsUseCase,
    private val observeActiveSubscriptionKeyUseCase: ObserveActiveSubscriptionKeyUseCase,
    private val saveSubscriptionKeyUseCase: SaveSubscriptionKeyUseCase
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
                                error = IllegalStateException("请先设置订阅ID").toAppError()
                            )
                        }
                    } else {
                        loadFeeds(key)
                    }
                }
        }
    }

    private suspend fun loadFeeds(id: String, policy: DataPolicy = DataPolicy.CACHE_ELSE_NETWORK) {
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
                it.copy(isLoading = false, error = e.toAppError { screenModelScope.launch { loadFeeds(id, policy) } })
            }
        }
    }


    fun onEvent(event: Event) {
        when (event) {
            is Event.OnUnsubscribe -> unsubscribe(event.threadId.toString())
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
            loadFeeds(id, DataPolicy.NETWORK_ELSE_CACHE)
        }
    }

    private fun push() {
        val id = state.value.subscriptionId ?: return
        screenModelScope.launch {
            try {
                syncLocalSubscriptionsUseCase(id)
                _effect.send(Effect.OnPushResult(true, "推送成功"))
                loadFeeds(id, DataPolicy.NETWORK_ELSE_CACHE)
            } catch (e: Exception) {
                _effect.send(Effect.OnPushResult(false, "推送失败: ${e.message}"))
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
        // This logic should ideally be in a UseCase as well
        val randomId = (1..10).map { ('a'..'z').random() }.joinToString("")
        setSubscriptionId(randomId)
    }

    private fun unsubscribe(threadId: String) {
        val id = state.value.subscriptionId ?: return
        screenModelScope.launch {
            val result = toggleSubscriptionUseCase(id, threadId, true)
            result.onSuccess {
                _effect.send(Effect.OnUnsubscribeResult(true, it))
            }.onFailure {
                _effect.send(Effect.OnUnsubscribeResult(false, "取消订阅失败: ${it.message}"))
            }
        }
    }
}
