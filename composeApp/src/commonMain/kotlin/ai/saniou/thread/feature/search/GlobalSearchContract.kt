package ai.saniou.thread.feature.search

import ai.saniou.thread.domain.model.search.GlobalSearchResponse
import ai.saniou.thread.domain.model.search.GlobalSearchResult
import ai.saniou.thread.domain.model.search.GlobalSearchType

interface GlobalSearchContract {
    data class State(
        val query: String = "",
        val selectedTypes: Set<GlobalSearchType> = GlobalSearchType.entries.toSet(),
        val response: GlobalSearchResponse? = null,
        val isSearching: Boolean = false,
        val message: String? = null,
    )

    sealed interface Event {
        data class QueryChanged(val value: String) : Event
        data class TypeToggled(val type: GlobalSearchType) : Event
        data object Retry : Event
        data object Clear : Event
        data object MessageShown : Event
    }

    sealed interface Effect {
        data class OpenResult(val result: GlobalSearchResult) : Effect
    }
}
