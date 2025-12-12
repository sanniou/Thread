package ai.saniou.forum.workflow.reference

import ai.saniou.thread.domain.model.forum.ThreadReply

interface ReferenceContract {
    data class State(
        val isLoading: Boolean = false,
        val reply: ThreadReply? = null,
        val error: String? = null
    )

    sealed interface Event {
        data class GetReference(val refId: Long) : Event
        object Clear : Event
    }
}
