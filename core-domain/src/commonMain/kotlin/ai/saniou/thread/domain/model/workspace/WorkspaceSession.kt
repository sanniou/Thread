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
    val updatedAtEpochMillis: Long = 0,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
