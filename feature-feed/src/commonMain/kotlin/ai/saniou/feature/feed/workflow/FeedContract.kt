package ai.saniou.feature.feed.workflow

interface FeedContract {
    data class SourceOption(
        val id: String,
        val name: String,
    )

    data class State(
        val sources: List<SourceOption> = emptyList(),
        val selectedSourceIds: Set<String> = emptySet(),
        val includeReader: Boolean = true,
        val isRefreshing: Boolean = false,
        val message: String? = null,
    )

    sealed interface Event {
        data class ToggleSource(val sourceId: String) : Event
        data object ToggleReader : Event
        data object SelectAllSources : Event
        data object ClearForumSources : Event
        data object Refresh : Event
        data object MessageShown : Event
    }

    sealed interface Effect {
        data object RefreshPaging : Effect
    }
}
