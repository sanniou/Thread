package ai.saniou.thread.domain.model.search

enum class GlobalSearchType {
    TOPIC,
    COMMENT,
    ARTICLE,
    SOCIAL,
}

/** Lightweight navigation-safe projection from SQLDelight's offline caches. */
data class GlobalSearchResult(
    val type: GlobalSearchType,
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val title: String,
    val snippet: String,
    val author: String? = null,
    val publishedAtEpochMillis: Long,
    /** Parent topic for a comment result; null for topics and articles. */
    val contextId: String? = null,
)

data class GlobalSearchResponse(
    val query: String,
    val results: List<GlobalSearchResult>,
    val topicCount: Long,
    val commentCount: Long,
    val articleCount: Long,
    val socialCount: Long = 0,
) {
    val totalCount: Long get() = topicCount + commentCount + articleCount + socialCount
}
