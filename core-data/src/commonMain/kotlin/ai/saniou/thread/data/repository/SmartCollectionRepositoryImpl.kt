package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.collection.SmartCollection
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.SmartCollectionRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.domain.repository.saveValue
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.domain.model.search.GlobalSearchResult
import ai.saniou.thread.domain.model.search.GlobalSearchType
import ai.saniou.corecommon.coroutines.ioDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SmartCollectionRepositoryImpl(
    private val settings: SettingsRepository,
    private val db: Database,
) : SmartCollectionRepository {
    private val mutex = Mutex()

    override fun observeCollections(): Flow<List<SmartCollection>> =
        settings.observeValue<List<SmartCollection>>(KEY)
            .map { values -> values.orEmpty().sortedWith(compareByDescending<SmartCollection> { it.pinned }.thenByDescending { it.updatedAtEpochMillis }) }
            .catch { emit(emptyList()) }

    override suspend fun save(collection: SmartCollection) = mutex.withLock {
        val values = settings.getValue<List<SmartCollection>>(KEY).orEmpty()
        val updated = (values.filterNot { it.id == collection.id } + collection)
            .sortedByDescending(SmartCollection::updatedAtEpochMillis)
            .take(MAX_COLLECTIONS)
        settings.saveValue(KEY, updated)
    }

    override suspend fun delete(id: String) = mutex.withLock {
        val updated = settings.getValue<List<SmartCollection>>(KEY).orEmpty().filterNot { it.id == id }
        settings.saveValue(KEY, updated)
    }

    override suspend fun resolve(id: String, limit: Int): List<GlobalSearchResult> {
        require(limit in 1..500)
        val collection = settings.getValue<List<SmartCollection>>(KEY).orEmpty()
            .firstOrNull { it.id == id } ?: return emptyList()
        val rules = collection.rules
        val scanLimit = (limit * 5).coerceAtMost(MAX_SCAN).toLong()
        return withContext(ioDispatcher) {
            buildList {
                if (rules.accepts(ContentReferenceKind.TOPIC)) {
                    db.topicQueries.getRecentTopicsForCollection(scanLimit, 0).executeAsList()
                        .asSequence()
                        .filter { row -> rules.sourceIds.isEmpty() || row.sourceId in rules.sourceIds }
                        .filter { row -> rules.author.isBlank() || row.authorName.contains(rules.author, true) || row.authorId.contains(rules.author, true) }
                        .filter { row -> rules.query.matches(row.title, row.summary, row.content, row.id) }
                        .filter { row -> !rules.unreadOnly || row.lastVisitedAt == null }
                        .filter { row -> !rules.bookmarkedOnly || row.isCollected == true }
                        .filter { row ->
                            val images = if (rules.hasMedia != null) {
                                db.imageQueries.getImagesByParent(row.sourceId, row.id, ImageType.Topic).executeAsList().isNotEmpty()
                            } else false
                            rules.hasMedia == null || rules.hasMedia == images
                        }
                        .filter { row ->
                            rules.tags.isEmpty() || db.topicTagQueries.getTagsForTopic(row.sourceId, row.id)
                                .executeAsList().map { it.name }.toSet().containsAll(rules.tags)
                        }
                        .mapTo(this) { row ->
                            GlobalSearchResult(
                                type = GlobalSearchType.TOPIC,
                                id = row.id,
                                sourceId = row.sourceId,
                                sourceName = row.sourceId,
                                title = row.title?.takeIf(String::isNotBlank) ?: "主题 #${row.id}",
                                snippet = excerpt(row.summary ?: row.content.orEmpty()),
                                author = row.authorName,
                                publishedAtEpochMillis = maxOf(row.lastReplyAt, row.createdAt),
                            )
                        }
                }
                if (rules.accepts(ContentReferenceKind.COMMENT) && !rules.bookmarkedOnly && rules.tags.isEmpty()) {
                    db.commentQueries.getRecentCommentsForCollection(scanLimit, 0).executeAsList()
                        .asSequence()
                        .filter { row -> rules.sourceIds.isEmpty() || row.sourceId in rules.sourceIds }
                        .filter { row -> rules.author.isBlank() || row.authorName.contains(rules.author, true) || row.userHash.contains(rules.author, true) }
                        .filter { row -> rules.query.matches(row.title, row.content, row.id) }
                        .filter { row -> !rules.unreadOnly || db.topicQueries.getTopic(row.sourceId, row.topicId).executeAsOneOrNull()?.lastVisitedAt == null }
                        .filter { row ->
                            val images = if (rules.hasMedia != null) {
                                db.imageQueries.getImagesByParent(row.sourceId, row.id, ImageType.Comment).executeAsList().isNotEmpty()
                            } else false
                            rules.hasMedia == null || rules.hasMedia == images
                        }
                        .mapTo(this) { row ->
                            GlobalSearchResult(
                                type = GlobalSearchType.COMMENT,
                                id = row.id,
                                sourceId = row.sourceId,
                                sourceName = row.sourceId,
                                title = row.title?.takeIf(String::isNotBlank) ?: "${row.floor} 楼回复",
                                snippet = excerpt(row.content),
                                author = row.authorName,
                                publishedAtEpochMillis = row.createdAt,
                                contextId = row.topicId,
                            )
                        }
                }
                if (rules.accepts(ContentReferenceKind.ARTICLE) && rules.tags.isEmpty()) {
                    db.articleQueries.getRecentArticlesWithSource(scanLimit, 0).executeAsList()
                        .asSequence()
                        .filter { row -> rules.sourceIds.isEmpty() || row.feedSourceId in rules.sourceIds }
                        .filter { row -> rules.author.isBlank() || row.author?.contains(rules.author, true) == true }
                        .filter { row -> rules.query.matches(row.title, row.description, row.content) }
                        .filter { row -> !rules.unreadOnly || row.isRead == 0L }
                        .filter { row -> !rules.bookmarkedOnly || row.isBookmarked != 0L }
                        .filter { row -> rules.hasMedia == null || rules.hasMedia == !row.imageUrl.isNullOrBlank() }
                        .mapTo(this) { row ->
                            GlobalSearchResult(
                                type = GlobalSearchType.ARTICLE,
                                id = row.id,
                                sourceId = row.feedSourceId,
                                sourceName = row.sourceName,
                                title = row.title,
                                snippet = excerpt(row.description.ifBlank { row.content }),
                                author = row.author,
                                publishedAtEpochMillis = row.publishDate,
                            )
                        }
                }
            }.sortedByDescending(GlobalSearchResult::publishedAtEpochMillis).take(limit)
        }
    }

    private fun ai.saniou.thread.domain.model.collection.SmartCollectionRules.accepts(kind: ContentReferenceKind) =
        contentKinds.isEmpty() || kind in contentKinds

    private fun String.matches(vararg values: String?): Boolean = isBlank() || values.any { value ->
        value?.contains(this, ignoreCase = true) == true
    }

    private fun excerpt(value: String): String = value
        .replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(220)

    private companion object {
        const val KEY = "smart_collections_v1"
        const val MAX_COLLECTIONS = 50
        const val MAX_SCAN = 1_000
    }
}
