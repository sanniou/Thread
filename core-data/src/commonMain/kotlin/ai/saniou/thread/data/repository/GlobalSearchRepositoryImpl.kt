package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.search.GlobalSearchResponse
import ai.saniou.thread.domain.model.search.GlobalSearchResult
import ai.saniou.thread.domain.model.search.GlobalSearchType
import ai.saniou.thread.domain.repository.GlobalSearchRepository
import ai.saniou.thread.domain.source.SourceCatalog
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class GlobalSearchRepositoryImpl(
    private val database: Database,
    private val sourceCatalog: SourceCatalog,
) : GlobalSearchRepository {
    override suspend fun search(
        query: String,
        types: Set<GlobalSearchType>,
        limitPerType: Long,
    ): GlobalSearchResponse = supervisorScope {
        val normalized = query.trim().take(MAX_QUERY_LENGTH)
        if (normalized.length < MIN_QUERY_LENGTH || types.isEmpty()) {
            return@supervisorScope GlobalSearchResponse(normalized, emptyList(), 0, 0, 0)
        }
        require(limitPerType in 1..MAX_LIMIT_PER_TYPE) { "limitPerType must be between 1 and $MAX_LIMIT_PER_TYPE" }

        val searches = types.map { type ->
            async(ioDispatcher) { type to searchType(type, normalized, limitPerType) }
        }.awaitAll().toMap()
        val counts = types.map { type ->
            async(ioDispatcher) { type to countType(type, normalized) }
        }.awaitAll().toMap()

        GlobalSearchResponse(
            query = normalized,
            results = searches.values.flatten()
                .sortedByDescending(GlobalSearchResult::publishedAtEpochMillis),
            topicCount = counts[GlobalSearchType.TOPIC] ?: 0,
            commentCount = counts[GlobalSearchType.COMMENT] ?: 0,
            articleCount = counts[GlobalSearchType.ARTICLE] ?: 0,
            socialCount = counts[GlobalSearchType.SOCIAL] ?: 0,
        )
    }

    private suspend fun searchType(type: GlobalSearchType, query: String, limit: Long) =
        withContext(ioDispatcher) {
            when (type) {
                GlobalSearchType.TOPIC -> database.topicQueries.searchTopics(query, limit, 0).executeAsList().map { row ->
                    GlobalSearchResult(
                        type = type,
                        id = row.id,
                        sourceId = row.sourceId,
                        sourceName = sourceName(row.sourceId),
                        title = row.title?.takeIf(String::isNotBlank) ?: "主题 #${row.id}",
                        snippet = excerpt(row.summary ?: row.content.orEmpty()),
                        author = row.authorName,
                        publishedAtEpochMillis = maxOf(row.lastReplyAt, row.createdAt),
                    )
                }
                GlobalSearchType.COMMENT -> database.commentQueries.searchComments(query, limit, 0).executeAsList().map { row ->
                    GlobalSearchResult(
                        type = type,
                        id = row.id,
                        sourceId = row.sourceId,
                        sourceName = sourceName(row.sourceId),
                        title = row.title?.takeIf(String::isNotBlank) ?: "${row.floor} 楼回复",
                        snippet = excerpt(row.content),
                        author = row.authorName,
                        publishedAtEpochMillis = row.createdAt,
                        contextId = row.topicId,
                    )
                }
                GlobalSearchType.ARTICLE -> database.articleQueries.searchArticles(query, limit, 0).executeAsList().map { row ->
                    GlobalSearchResult(
                        type = type,
                        id = row.id,
                        sourceId = row.feedSourceId,
                        sourceName = row.sourceName,
                        title = row.title,
                        snippet = excerpt(row.description.ifBlank { row.content }),
                        author = row.author,
                        publishedAtEpochMillis = row.publishDate,
                    )
                }
                GlobalSearchType.SOCIAL -> database.socialQueries.searchSocialPosts(query, limit, 0).executeAsList().map { row ->
                    GlobalSearchResult(
                        type = type,
                        id = row.id,
                        sourceId = row.sourceId,
                        sourceName = row.sourceName,
                        title = row.authorName,
                        snippet = excerpt(row.body),
                        author = row.authorHandle ?: row.authorName,
                        publishedAtEpochMillis = row.createdAt,
                    )
                }
            }
        }

    private fun countType(type: GlobalSearchType, query: String): Long = when (type) {
        GlobalSearchType.TOPIC -> database.topicQueries.countSearchTopics(query).executeAsOne()
        GlobalSearchType.COMMENT -> database.commentQueries.countSearchComments(query).executeAsOne()
        GlobalSearchType.ARTICLE -> database.articleQueries.countSearchArticles(query).executeAsOne()
        GlobalSearchType.SOCIAL -> database.socialQueries.countSearchSocialPosts(query).executeAsOne()
    }

    private fun sourceName(sourceId: String): String =
        sourceCatalog.descriptors.value.firstOrNull { it.id == sourceId }?.displayName ?: sourceId

    private fun excerpt(value: String): String = value
        .replace(HTML_TAG, " ")
        .replace(WHITESPACE, " ")
        .trim()
        .take(MAX_EXCERPT_LENGTH)

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val MAX_QUERY_LENGTH = 240
        const val MAX_LIMIT_PER_TYPE = 50L
        const val MAX_EXCERPT_LENGTH = 220
        val HTML_TAG = Regex("<[^>]+>")
        val WHITESPACE = Regex("\\s+")
    }
}
