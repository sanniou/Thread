package ai.saniou.forum.workflow.reference

import ai.saniou.thread.domain.model.forum.Comment

interface ReferenceContract {
    data class State(
        val isLoading: Boolean = false,
        val reply: Comment? = null,
        val error: String? = null
    )

    sealed interface Event {
        data class GetReference(val refId: Long) : Event
        object Clear : Event
    }
}
