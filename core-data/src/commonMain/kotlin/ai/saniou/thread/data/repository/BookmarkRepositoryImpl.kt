package ai.saniou.thread.data.repository

import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.BookmarkTag
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.data.source.tieba.TiebaMapper
import ai.saniou.thread.data.source.tieba.TiebaThreadStoreSync
import ai.saniou.thread.data.source.tieba.parseTiebaTopicBookmarkId
import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.repository.BookmarkRepository
import androidx.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class BookmarkRepositoryImpl(
    private val db: Database,
    private val tiebaThreadStore: TiebaThreadStoreSync,
) : BookmarkRepository {
    override fun getBookmarks(query: String?, tags: List<String>?): PagingSource<Int, Bookmark> {
        val normalizedQuery = query.orEmpty()
        val normalizedTags = tags.orEmpty()
        val hasQuery = normalizedQuery.isNotBlank()
        val hasTags = normalizedTags.isNotEmpty()

        val countQuery = when {
            hasQuery && hasTags -> db.bookmarkQueries.searchByQueryAndTagsCount(
                query = normalizedQuery,
                tags = normalizedTags,
                tagCount = normalizedTags.size.toLong(),
            )
            hasQuery -> db.bookmarkQueries.searchByQueryCount(normalizedQuery)
            hasTags -> db.bookmarkQueries.searchByTagsCount(normalizedTags, normalizedTags.size.toLong())
            else -> db.bookmarkQueries.searchAllCount()
        }

        return QueryPagingSource(
            countQuery = countQuery,
            transacter = db.bookmarkQueries,
            context = Dispatchers.Default,
            queryProvider = { limit, offset ->
                when {
                    hasQuery && hasTags -> db.bookmarkQueries.searchByQueryAndTags(
                        normalizedQuery,
                        normalizedTags,
                        normalizedTags.size.toLong(),
                        limit,
                        offset,
                        ::mapBookmark,
                    )
                    hasQuery -> db.bookmarkQueries.searchByQuery(
                        normalizedQuery,
                        limit,
                        offset,
                        ::mapBookmark,
                    )
                    hasTags -> db.bookmarkQueries.searchByTags(
                        normalizedTags,
                        normalizedTags.size.toLong(),
                        limit,
                        offset,
                        ::mapBookmark,
                    )
                    else -> db.bookmarkQueries.searchAll(limit, offset, ::mapBookmark)
                }
            },
        )
    }

    private fun mapBookmark(
        id: String,
        type: String,
        createdAt: Instant,
        content: String?,
        url: String?,
        sourceId: String?,
        sourceType: String?,
        title: String?,
        description: String?,
        favicon: String?,
        width: Long?,
        height: Long?,
        mimeType: String?,
        duration: Long?,
    ): Bookmark {
        val bookmarkTags = db.tagQueries.getTagsForBookmark(id).executeAsList().map { it.toDomain() }
        return ai.saniou.thread.db.table.Bookmark(
            id,
            type,
            createdAt,
            content,
            url,
            sourceId,
            sourceType,
            title,
            description,
            favicon,
            width,
            height,
            mimeType,
            duration,
        ).toDomain(bookmarkTags)
    }

    override suspend fun addBookmark(bookmark: Bookmark) {
        withContext(Dispatchers.Default) {
            syncTiebaStoreOnAdd(bookmark)
            db.transaction {
                db.bookmarkQueries.insert(bookmark.toEntity())
                bookmark.tags.forEach { tag ->
                    db.tagQueries.insert(tag.toEntity())
                    db.bookmarkTagQueries.insert(BookmarkTag(bookmark.id, tag.id))
                }
            }
        }
    }

    override suspend fun removeBookmark(id: String) {
        withContext(Dispatchers.Default) {
            parseTiebaTopicBookmarkId(id)?.let { threadId ->
                // Best-effort remote rmstore; local remove still proceeds if already uncollected.
                runCatching { tiebaThreadStore.removeStore(threadId) }
            }
            db.bookmarkQueries.deleteById(id)
        }
    }

    override fun isBookmarked(id: String): Flow<Boolean> {
        return db.bookmarkQueries.getById(id)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.isNotEmpty() }
    }

    private suspend fun syncTiebaStoreOnAdd(bookmark: Bookmark) {
        val quote = bookmark as? Bookmark.Quote ?: return
        if (!quote.sourceType.startsWith("${TiebaMapper.SOURCE_ID}.")) return
        val threadId = when {
            quote.sourceType == "${TiebaMapper.SOURCE_ID}.Topic" -> quote.sourceId
            quote.sourceType == "${TiebaMapper.SOURCE_ID}.Comment" -> {
                // Comment bookmarks store the post id in sourceId; official store needs tid.
                // Prefer bookmark id shape tieba.Comment.* only when parent is unknown — skip remote.
                return
            }
            else -> parseTiebaTopicBookmarkId(quote.id) ?: return
        }
        if (threadId.isBlank()) return
        // Remote-first: failed collect should surface to the UI instead of a local-only star.
        tiebaThreadStore.addStore(threadId = threadId, postId = threadId)
    }
}
