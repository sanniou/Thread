package ai.saniou.thread.domain.model.workspace

/** Stable workspace identifiers are persisted across every platform implementation. */
enum class WorkspaceDestination(val key: String) {
    FORUM("forum"),
    READER("reader"),
    FEED("feed"),
    SEARCH("search"),
    BOOKMARKS("bookmarks"),
    HISTORY("history"),
    OPERATIONS("operations"),
    LAB("lab"),
    SETTINGS("settings");

    companion object {
        fun fromKey(key: String?): WorkspaceDestination =
            entries.firstOrNull { it.key == key } ?: FORUM
    }
}

/**
 * A deliberately small, versioned restoration contract. Detail routes are not
 * restored blindly because their backing content may have been removed while
 * the app was closed; workspace, source and discovery context are always safe.
 */
data class WorkspaceSession(
    val version: Int = CURRENT_VERSION,
    val destination: WorkspaceDestination = WorkspaceDestination.FORUM,
    val forumSourceId: String? = null,
    val globalSearchQuery: String = "",
    val forum: ForumWorkspaceState = ForumWorkspaceState(),
    val reader: ReaderWorkspaceState = ReaderWorkspaceState(),
    val feed: FeedWorkspaceState = FeedWorkspaceState(),
    val lastContent: RestorableContentReference? = null,
    val updatedAtEpochMillis: Long = 0,
) {
    companion object {
        const val CURRENT_VERSION = 2
    }
}

data class ListAnchor(
    val contextKey: String,
    val index: Int,
    val offset: Int,
) {
    init {
        require(contextKey.isNotBlank())
        require(index >= 0)
        require(offset >= 0)
    }
}

data class ForumWorkspaceState(
    val sourceId: String? = null,
    val channelId: String? = null,
    val expandedGroupId: String? = null,
    val listAnchor: ListAnchor? = null,
)

data class ReaderWorkspaceState(
    val feedSourceId: String? = null,
    val articleFilter: String = "ALL",
    val searchQuery: String = "",
    val previewArticleId: String? = null,
    val listAnchor: ListAnchor? = null,
)

data class FeedWorkspaceState(
    val selectedSourceIds: Set<String> = emptySet(),
    val hasExplicitSourceSelection: Boolean = false,
    val includeReader: Boolean = true,
    val listAnchor: ListAnchor? = null,
)

enum class RestorableContentKind { TOPIC, ARTICLE }

data class RestorableContentReference(
    val kind: RestorableContentKind,
    val id: String,
    val sourceId: String? = null,
    val workspace: WorkspaceDestination,
) {
    init {
        require(id.isNotBlank())
        if (kind == RestorableContentKind.TOPIC) require(!sourceId.isNullOrBlank())
    }
}
