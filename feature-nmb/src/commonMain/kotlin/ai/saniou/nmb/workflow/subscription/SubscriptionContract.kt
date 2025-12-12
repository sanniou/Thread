package ai.saniou.nmb.workflow.subscription

import ai.saniou.thread.domain.model.forum.Post
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface SubscriptionContract {
    data class State(
        val subscriptionId: String? = null,
        val feeds: Flow<PagingData<Post>> = emptyFlow(),
        val isShowSubscriptionIdDialog: Boolean = false,
        val isLoading: Boolean = true,
        val error: Throwable? = null,
        val isPushEnabled: Boolean = false
    )

    sealed interface Event {
        data class OnUnsubscribe(val threadId: Long) : Event
        data class OnSetSubscriptionId(val id: String) : Event
        object OnShowSubscriptionIdDialog : Event
        object OnHideSubscriptionIdDialog : Event
        object OnGenerateRandomSubscriptionId : Event
        object OnPull : Event
        object OnPush : Event
    }

    sealed interface Effect {
        data class OnUnsubscribeResult(val isSuccess: Boolean, val message: String?) : Effect
        data class OnPushResult(val isSuccess: Boolean, val message: String?) : Effect
    }
}
