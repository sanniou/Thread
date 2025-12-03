package ai.saniou.nmb.workflow.user

import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.data.entity.ThreadWithInformation
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface UserDetailContract {
    data class State(
        val userHash: String,
        val threads: Flow<PagingData<ThreadWithInformation>>? = null,
        val replies: Flow<PagingData<ThreadReply>>? = null,
        val currentTab: Tab = Tab.Threads
    )

    sealed interface Event {
        data class SwitchTab(val tab: Tab) : Event
        data object Back : Event
    }

    sealed interface Effect {
        data object NavigateBack : Effect
    }

    enum class Tab {
        Threads,
        Replies
    }
}
