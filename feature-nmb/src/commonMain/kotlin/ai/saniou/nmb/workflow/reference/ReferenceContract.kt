package ai.saniou.nmb.workflow.reference

import ai.saniou.thread.data.source.nmb.remote.dto.ThreadReply

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
