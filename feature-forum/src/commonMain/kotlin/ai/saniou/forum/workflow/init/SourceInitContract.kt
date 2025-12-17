package ai.saniou.forum.workflow.init

import ai.saniou.coreui.state.UiStateWrapper

interface SourceInitContract {
    data class State(
        val sourceName: String = "",
        val isInitialized: Boolean = false,
        val uiState: UiStateWrapper<Unit> = UiStateWrapper.Success(Unit),
        
        // NMB specific
        val nmbSubscriptionKey: String = "",
        val nmbCookie: String = "",
        
        // Discourse specific
        val discourseApiKey: String = "",
        val discourseUsername: String = ""
    )

    sealed interface Event {
        data class UpdateNmbSubscriptionKey(val key: String) : Event
        data class UpdateNmbCookie(val cookie: String) : Event
        data class UpdateDiscourseApiKey(val key: String) : Event
        data class UpdateDiscourseUsername(val username: String) : Event
        data object CompleteInitialization : Event
    }
}