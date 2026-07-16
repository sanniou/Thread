package ai.saniou.thread.feature.operations

import ai.saniou.thread.domain.model.operations.OperationsSnapshot
import ai.saniou.thread.domain.model.operations.SourceHealth

interface OperationsContract {
    data class State(
        val snapshot: OperationsSnapshot = OperationsSnapshot(),
        val filter: Filter = Filter.ALL,
        val workingSourceIds: Set<String> = emptySet(),
        val message: String? = null,
    ) {
        val visibleSources: List<SourceHealth> get() = snapshot.sources.filter { filter.accepts(it) }
    }

    enum class Filter {
        ALL,
        ATTENTION,
        FORUM,
        READER;

        fun accepts(source: SourceHealth): Boolean = when (this) {
            ALL -> true
            ATTENTION -> source.state !in setOf(
                ai.saniou.thread.domain.model.operations.SourceOperationalState.READY,
                ai.saniou.thread.domain.model.operations.SourceOperationalState.DISABLED,
            )
            FORUM -> source.kind == ai.saniou.thread.domain.model.operations.ContentSourceKind.FORUM
            READER -> source.kind == ai.saniou.thread.domain.model.operations.ContentSourceKind.READER
        }
    }

    sealed interface Event {
        data class FilterChanged(val filter: Filter) : Event
        data class Retry(val source: SourceHealth) : Event
        data class ClearDiagnostic(val sourceId: String) : Event
        data object MessageShown : Event
    }
}
