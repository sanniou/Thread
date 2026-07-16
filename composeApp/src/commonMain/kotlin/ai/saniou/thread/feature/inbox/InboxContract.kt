package ai.saniou.thread.feature.inbox

import ai.saniou.thread.domain.model.inbox.InboxFilter
import ai.saniou.thread.domain.model.inbox.InboxKind
import ai.saniou.thread.domain.model.inbox.InboxSummary

interface InboxContract {
    data class State(
        val summary: InboxSummary = InboxSummary(),
        val filter: InboxFilter = InboxFilter(),
        val message: String? = null,
    )

    sealed interface Event {
        data class QueryChanged(val value: String) : Event
        data class UnreadOnlyChanged(val value: Boolean) : Event
        data class IncludeMutedChanged(val value: Boolean) : Event
        data class SourceChanged(val sourceId: String?) : Event
        data class KindChanged(val kind: InboxKind?) : Event
        data class MarkRead(val id: String, val read: Boolean = true) : Event
        object MarkAllRead : Event
        data class SetSourceMuted(val sourceId: String, val muted: Boolean) : Event
        data class Delete(val id: String) : Event
        object MessageShown : Event
    }
}
