package ai.saniou.thread.domain.model.feed

import ai.saniou.thread.domain.model.forum.Topic

data class SourceFeedFailure(
    val sourceId: String,
    val sourceName: String,
    val message: String,
)

data class AggregatedFeedPage(
    val topics: List<Topic>,
    val failures: List<SourceFeedFailure> = emptyList(),
    val hasMore: Boolean = topics.isNotEmpty(),
)

data class FeedRefreshReport(
    val refreshedSourceIds: Set<String> = emptySet(),
    val sourceFailures: List<SourceFeedFailure> = emptyList(),
    val refreshedReaderSourceIds: Set<String> = emptySet(),
    val readerFailures: Map<String, String> = emptyMap(),
) {
    val isSuccess: Boolean
        get() = sourceFailures.isEmpty() && readerFailures.isEmpty()

    val hasAnySuccess: Boolean
        get() = refreshedSourceIds.isNotEmpty() || refreshedReaderSourceIds.isNotEmpty()
}
