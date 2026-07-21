package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.SmartCollectionIndexEntity
import ai.saniou.thread.domain.model.collection.SmartCollection
import ai.saniou.thread.domain.model.collection.SmartCollectionRules
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.search.GlobalSearchResult
import ai.saniou.thread.domain.model.search.GlobalSearchType
import ai.saniou.thread.domain.paging.threadPagingConfig
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.SmartCollectionRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.domain.repository.saveValue
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Persistent collection definitions backed by a SQL materialized projection.
 * Rebuilding the projection uses four INSERT…SELECT statements instead of a Kotlin full-cache scan;
 * every subsequent collection page is served by SQLDelight's official QueryPagingSource.
 */
class SmartCollectionRepositoryImpl(
    private val settings: SettingsRepository,
    private val db: Database,
) : SmartCollectionRepository {
    private val mutex = Mutex()

    override fun observeCollections(): Flow<List<SmartCollection>> =
        settings.observeValue<List<SmartCollection>>(KEY)
            .map { values -> values.orEmpty().ordered() }
            .catch { emit(emptyList()) }

    override suspend fun save(collection: SmartCollection) = mutex.withLock {
        val values = settings.getValue<List<SmartCollection>>(KEY).orEmpty()
        val existing = values.firstOrNull { it.id == collection.id }
        val normalized = collection.copy(position = existing?.position ?: values.size)
        // Bound the catalog by recency so concurrent creates keep the newest MAX_COLLECTIONS.
        val bounded = (values.filterNot { it.id == collection.id } + normalized)
            .sortedByDescending(SmartCollection::updatedAtEpochMillis)
            .take(MAX_COLLECTIONS)
            .mapIndexed { index, value -> value.copy(position = index) }
        settings.saveValue(KEY, bounded)
    }

    override suspend fun delete(id: String) = mutex.withLock {
        val updated = settings.getValue<List<SmartCollection>>(KEY).orEmpty()
            .filterNot { it.id == id }
            .mapIndexed { index, value -> value.copy(position = index) }
        settings.saveValue(KEY, updated)
    }

    override suspend fun setPinned(id: String, pinned: Boolean) = mutate { values ->
        values.map { value ->
            if (value.id == id) value.copy(
                pinned = pinned,
                updatedAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            ) else value
        }
    }

    override suspend fun reorder(orderedIds: List<String>) = mutate { values ->
        val ranks = orderedIds.distinct().withIndex().associate { it.value to it.index }
        values.map { value -> value.copy(position = ranks[value.id] ?: (ranks.size + value.position)) }
    }

    override suspend fun resolve(id: String, limit: Int): List<GlobalSearchResult> {
        require(limit in 1..500)
        val context = prepare(id) ?: return emptyList()
        return withContext(ioDispatcher) {
            db.smartCollectionIndexQueries.resolveSmartCollectionPaging(
                contentKinds = context.contentKinds,
                sourceIds = context.sourceIds,
                query = context.rules.query.trim(),
                author = context.rules.author.trim(),
                tag1 = context.tags[0],
                tag2 = context.tags[1],
                tag3 = context.tags[2],
                tag4 = context.tags[3],
                unreadOnly = context.rules.unreadOnly.asLong(),
                bookmarkedOnly = context.rules.bookmarkedOnly.asLong(),
                hasMedia = context.rules.hasMedia.asFilterLong(),
                includeContentWarnings = context.rules.includeContentWarnings.asLong(),
                groupMode = context.collection.groupBy.name,
                sortMode = context.collection.sort.name,
                limit = limit.toLong(),
                offset = 0,
            ).executeAsList().map(SmartCollectionIndexEntity::toSearchResult)
        }
    }

    override fun resolvePaging(id: String): Flow<PagingData<GlobalSearchResult>> = flow {
        val context = prepare(id)
        if (context == null) {
            emit(PagingData.empty())
            return@flow
        }
        emitAll(
            Pager(
                config = threadPagingConfig(pageSize = 40),
                pagingSourceFactory = {
                    QueryPagingSource(
                        transacter = db.smartCollectionIndexQueries,
                        context = Dispatchers.Default,
                        countQuery = db.smartCollectionIndexQueries.countResolvedSmartCollection(
                            contentKinds = context.contentKinds,
                            sourceIds = context.sourceIds,
                            query = context.rules.query.trim(),
                            author = context.rules.author.trim(),
                            tag1 = context.tags[0],
                            tag2 = context.tags[1],
                            tag3 = context.tags[2],
                            tag4 = context.tags[3],
                            unreadOnly = context.rules.unreadOnly.asLong(),
                            bookmarkedOnly = context.rules.bookmarkedOnly.asLong(),
                            hasMedia = context.rules.hasMedia.asFilterLong(),
                            includeContentWarnings = context.rules.includeContentWarnings.asLong(),
                        ),
                        queryProvider = { limit, offset ->
                            db.smartCollectionIndexQueries.resolveSmartCollectionPaging(
                                contentKinds = context.contentKinds,
                                sourceIds = context.sourceIds,
                                query = context.rules.query.trim(),
                                author = context.rules.author.trim(),
                                tag1 = context.tags[0],
                                tag2 = context.tags[1],
                                tag3 = context.tags[2],
                                tag4 = context.tags[3],
                                unreadOnly = context.rules.unreadOnly.asLong(),
                                bookmarkedOnly = context.rules.bookmarkedOnly.asLong(),
                                hasMedia = context.rules.hasMedia.asFilterLong(),
                                includeContentWarnings = context.rules.includeContentWarnings.asLong(),
                                groupMode = context.collection.groupBy.name,
                                sortMode = context.collection.sort.name,
                                limit = limit,
                                offset = offset,
                            )
                        },
                    )
                },
            ).flow.map { paging -> paging.map(SmartCollectionIndexEntity::toSearchResult) },
        )
    }

    private suspend fun prepare(id: String): QueryContext? {
        val collection = settings.getValue<List<SmartCollection>>(KEY).orEmpty()
            .firstOrNull { it.id == id } ?: return null
        rebuildIndex()
        val allSources = withContext(ioDispatcher) {
            db.smartCollectionIndexQueries.getSmartCollectionIndexSources().executeAsList()
                .mapTo(linkedSetOf()) { it }
        }
        val sources = collection.rules.sourceIds.ifEmpty { allSources }
        if (sources.isEmpty()) return null
        val kinds = collection.rules.contentKinds
            .ifEmpty { INDEXED_KINDS }
            .mapTo(linkedSetOf()) { it.name }
        val tags = collection.rules.tags.map(String::trim).filter(String::isNotBlank).take(MAX_TAG_RULES)
            .let { it + List(MAX_TAG_RULES - it.size) { "" } }
        return QueryContext(collection, collection.rules, kinds, sources, tags)
    }

    private suspend fun rebuildIndex() = withContext(ioDispatcher) {
        db.transaction {
            db.smartCollectionIndexQueries.clearSmartCollectionIndex()
            db.smartCollectionIndexQueries.indexCachedTopics()
            db.smartCollectionIndexQueries.indexCachedComments()
            db.smartCollectionIndexQueries.indexCachedArticles()
            db.smartCollectionIndexQueries.indexCachedSocialPosts()
        }
    }

    private suspend fun mutate(transform: (List<SmartCollection>) -> List<SmartCollection>) = mutex.withLock {
        val bounded = transform(settings.getValue<List<SmartCollection>>(KEY).orEmpty())
            .sortedByDescending(SmartCollection::updatedAtEpochMillis)
            .take(MAX_COLLECTIONS)
            .mapIndexed { index, value -> value.copy(position = index) }
        settings.saveValue(KEY, bounded)
    }

    private fun List<SmartCollection>.ordered() = sortedWith(
        compareByDescending<SmartCollection> { it.pinned }
            .thenBy(SmartCollection::position)
            .thenByDescending(SmartCollection::updatedAtEpochMillis),
    )

    private data class QueryContext(
        val collection: SmartCollection,
        val rules: SmartCollectionRules,
        val contentKinds: Set<String>,
        val sourceIds: Set<String>,
        val tags: List<String>,
    )

    private companion object {
        const val KEY = "smart_collections_v1"
        const val MAX_COLLECTIONS = 50
        const val MAX_TAG_RULES = 4
        val INDEXED_KINDS = setOf(
            ContentReferenceKind.TOPIC,
            ContentReferenceKind.COMMENT,
            ContentReferenceKind.ARTICLE,
            ContentReferenceKind.SOCIAL_POST,
        )
    }
}

private fun SmartCollectionIndexEntity.toSearchResult() = GlobalSearchResult(
    type = when (contentKind) {
        ContentReferenceKind.TOPIC.name -> GlobalSearchType.TOPIC
        ContentReferenceKind.COMMENT.name -> GlobalSearchType.COMMENT
        ContentReferenceKind.ARTICLE.name -> GlobalSearchType.ARTICLE
        ContentReferenceKind.SOCIAL_POST.name -> GlobalSearchType.SOCIAL
        else -> GlobalSearchType.ARTICLE
    },
    id = contentId,
    sourceId = sourceId,
    sourceName = sourceName,
    title = title,
    snippet = body.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim().take(220),
    author = author.takeIf(String::isNotBlank),
    publishedAtEpochMillis = publishedAt,
    contextId = contextId,
)

private fun Boolean.asLong() = if (this) 1L else 0L
private fun Boolean?.asFilterLong() = when (this) { null -> -1L; true -> 1L; false -> 0L }
