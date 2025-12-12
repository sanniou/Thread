package ai.saniou.thread.data.repository

import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.BookmarkTag
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.repository.BookmarkRepository
import app.cash.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class BookmarkRepositoryImpl(
    private val db: Database
) : BookmarkRepository {
    override fun getBookmarks(query: String?, tags: List<String>?): PagingSource<Int, Bookmark> {
        val hasQuery = !query.isNullOrBlank()
        val hasTags = !tags.isNullOrEmpty()

        val countQuery = when {
            hasQuery && hasTags -> db.bookmarkQueries.searchByQueryAndTagsCount(query!!, tags!!)
            hasQuery -> db.bookmarkQueries.searchByQueryCount(query!!)
            hasTags -> db.bookmarkQueries.searchByTagsCount(tags!!)
            else -> db.bookmarkQueries.searchAllCount()
        }

        return QueryPagingSource(
            countQuery = countQuery,
            transacter = db.bookmarkQueries,
            context = Dispatchers.Default,
            queryProvider = { limit, offset ->
                when {
                    hasQuery && hasTags -> db.bookmarkQueries.searchByQueryAndTags(
                        query!!,
                        tags!!,
                        limit,
                        offset,
                        ::mapBookmark
                    )
                    hasQuery -> db.bookmarkQueries.searchByQuery(
                        query!!,
                        limit,
                        offset,
                        ::mapBookmark
                    )
                    hasTags -> db.bookmarkQueries.searchByTags(tags!!, limit, offset, ::mapBookmark)
                    else -> db.bookmarkQueries.searchAll(limit, offset, ::mapBookmark)
                }
            }
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
        duration: Long?
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
            duration
        ).toDomain(bookmarkTags)
    }

    override suspend fun addBookmark(bookmark: Bookmark) {
        withContext(Dispatchers.Default) {
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
            db.bookmarkQueries.deleteById(id)
        }
    }

    override fun isBookmarked(id: String): Flow<Boolean> {
        return db.bookmarkQueries.getById(id)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.isNotEmpty() }
    }
}
