package ai.saniou.thread.domain.model.reader

data class ArticleWithSource(
    val article: Article,
    val sourceName: String,
    val sourceIconUrl: String?,
)

data class ReaderRefreshReport(
    val refreshedSourceIds: Set<String> = emptySet(),
    val failures: Map<String, String> = emptyMap(),
) {
    val isSuccess: Boolean
        get() = failures.isEmpty()
}
