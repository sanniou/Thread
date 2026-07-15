package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.feed.AggregatedFeedPage
import ai.saniou.thread.domain.model.feed.SourceFeedFailure
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SourceRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

class SourceRepositoryImpl(
    private val sources: Set<Source>,
) : SourceRepository {

    private val orderedSources = sources.sortedBy { it.id }
    private val sourceMap = orderedSources.associateBy { it.id }.also { indexed ->
        require(indexed.size == sources.size) { "Source ids must be unique" }
    }

    override suspend fun getAggregatedFeed(
        page: Int,
        sourceIds: Set<String>?,
    ): Result<AggregatedFeedPage> = supervisorScope {
        require(page > 0) { "page must be positive" }

        val selectedSources = orderedSources.filter { source ->
            source.capabilities.supportsFeedAggregation &&
                (sourceIds == null || source.id in sourceIds)
        }
        if (selectedSources.isEmpty()) {
            return@supervisorScope Result.success(AggregatedFeedPage(emptyList(), hasMore = false))
        }

        val results = selectedSources.map { source ->
            async {
                source to runCatching {
                    source.getFeedPage(source.getFeedCursor(page)).getOrThrow()
                }
            }
        }.awaitAll()

        val failures = results.mapNotNull { (source, result) ->
            result.exceptionOrNull()?.let { error ->
                SourceFeedFailure(
                    sourceId = source.id,
                    sourceName = source.name,
                    message = error.message ?: error::class.simpleName ?: "Unknown error",
                )
            }
        }
        val successfulPages = results.mapNotNull { (_, result) -> result.getOrNull() }
        val topics = successfulPages
            .flatMap { it.data }
            .distinctBy { "${it.sourceId}:${it.id}" }
            .sortedByDescending { it.createdAt }

        if (topics.isEmpty() && failures.size == selectedSources.size) {
            val summary = failures.joinToString { "${it.sourceName}: ${it.message}" }
            return@supervisorScope Result.failure(IllegalStateException(summary))
        }

        Result.success(
            AggregatedFeedPage(
                topics = topics,
                failures = failures,
                hasMore = successfulPages.any { it.nextCursor != null },
            )
        )
    }

    override fun getAvailableSources(): List<Source> {
        return orderedSources
    }

    override fun getSource(sourceId: String): Source? {
        return sourceMap[sourceId]
    }
}
