package ai.saniou.feature.feed.workflow

import ai.saniou.thread.domain.refresh.RefreshTaskState
import ai.saniou.thread.domain.model.workspace.ListAnchor
import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialPost

interface FeedContract {
    data class SourceOption(
        val id: String,
        val name: String,
    )

    data class SocialSourceOption(
        val id: String,
        val name: String,
        val host: String,
    )

    data class State(
        val sources: List<SourceOption> = emptyList(),
        val selectedSourceIds: Set<String> = emptySet(),
        val includeReader: Boolean = true,
        val socialSources: List<SocialSourceOption> = emptyList(),
        val selectedSocialSourceIds: Set<String> = emptySet(),
        val includeSocial: Boolean = true,
        val isRefreshing: Boolean = false,
        val message: String? = null,
        val refreshFailures: List<RefreshTaskState> = emptyList(),
        val listAnchor: ListAnchor? = null,
    )

    sealed interface Event {
        data class ToggleSource(val sourceId: String) : Event
        data object ToggleReader : Event
        data class ToggleSocialSource(val sourceId: String) : Event
        data object ToggleSocial : Event
        data object SelectAllSocialSources : Event
        data object ClearSocialSources : Event
        data object SelectAllSources : Event
        data object ClearForumSources : Event
        data object Refresh : Event
        data object LoadOlderSocial : Event
        data class InteractSocial(
            val post: SocialPost,
            val interaction: SocialInteraction,
            val enabled: Boolean,
        ) : Event
        data class ListPositionChanged(val contextKey: String, val index: Int, val offset: Int) : Event
        data object MessageShown : Event
    }

    sealed interface Effect {
        data object RefreshPaging : Effect
    }
}
