package ai.saniou.nmb.workflow.subscription

import ai.saniou.nmb.data.storage.SubscriptionStorage
import ai.saniou.nmb.workflow.subscription.SubscriptionContract.Effect
import ai.saniou.nmb.workflow.subscription.SubscriptionContract.Event
import ai.saniou.nmb.workflow.subscription.SubscriptionContract.State
import ai.saniou.thread.data.source.nmb.DataPolicy
import ai.saniou.thread.domain.usecase.GetSubscriptionFeedUseCase
import ai.saniou.thread.domain.usecase.SyncLocalSubscriptionsUseCase
import ai.saniou.thread.domain.usecase.ToggleSubscriptionUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val getSubscriptionFeedUseCase: GetSubscriptionFeedUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val syncLocalSubscriptionsUseCase: SyncLocalSubscriptionsUseCase,
    private val subscriptionStorage: SubscriptionStorage
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    init {
        screenModelScope.launch {
            subscriptionStorage.loadLastSubscriptionId()
            if (subscriptionStorage.subscriptionId.value == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isShowSubscriptionIdDialog = true,
                        error = IllegalStateException("请先设置订阅ID")
                    )
                }
            }
        }

        screenModelScope.launch {
            subscriptionStorage.subscriptionId
                .filterNotNull()
                .distinctUntilChanged()
                .collect { id ->
                    loadFeeds(id)
                }
        }
    }

    private suspend fun loadFeeds(id: String, policy: DataPolicy = DataPolicy.CACHE_FIRST) {
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
                it.copy(isLoading = false, error = e)
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
            loadFeeds(id, DataPolicy.API_FIRST)
        }
    }

    private fun push() {
        val id = state.value.subscriptionId ?: return
        screenModelScope.launch {
            try {
                syncLocalSubscriptionsUseCase(id)
                _effect.send(Effect.OnPushResult(true, "推送成功"))
                loadFeeds(id, DataPolicy.API_FIRST)
            } catch (e: Exception) {
                _effect.send(Effect.OnPushResult(false, "推送失败: ${e.message}"))
            }
        }
    }

    private fun setSubscriptionId(id: String) {
        screenModelScope.launch {
            subscriptionStorage.addSubscriptionId(id)
            _state.update { it.copy(isShowSubscriptionIdDialog = false) }
        }
    }

    private fun generateRandomSubscriptionId() {
        val randomId = subscriptionStorage.generateRandomSubscriptionId()
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
