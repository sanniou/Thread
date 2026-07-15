package ai.saniou.forum.workflow.init

import ai.saniou.coreui.state.UiStateWrapper

interface SourceInitContract {
    data class State(
        val sourceName: String = "",
        val isInitialized: Boolean = false,
        val uiState: UiStateWrapper<Unit> = UiStateWrapper.Success(Unit),
    )

    sealed interface Event {
        data object CompleteInitialization : Event
    }
}
