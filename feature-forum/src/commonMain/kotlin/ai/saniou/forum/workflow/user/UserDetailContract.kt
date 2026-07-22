package ai.saniou.forum.workflow.user

import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.source.UserRelationProfile
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface UserDetailContract {
    data class State(
        val userHash: String,
        val topics: Flow<PagingData<Topic>>? = null,
        val comments: Flow<PagingData<Comment>>? = null,
        val currentTab: Tab = Tab.Topics,
        val supportsUserFollow: Boolean = false,
        val profile: UserRelationProfile? = null,
        val isProfileLoading: Boolean = false,
        val isFollowBusy: Boolean = false,
        val actionMessage: String? = null,
    )

    sealed interface Event {
        data class SwitchTab(val tab: Tab) : Event
        data object Back : Event
        data object ToggleFollow : Event
        data object ConsumeActionMessage : Event
    }

    sealed interface Effect {
        data object NavigateBack : Effect
    }

    enum class Tab {
        Topics,
        Comments
    }
}
