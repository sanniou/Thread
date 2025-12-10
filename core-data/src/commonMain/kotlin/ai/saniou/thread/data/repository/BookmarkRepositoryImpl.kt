package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.domain.model.Bookmark
import ai.saniou.thread.domain.repository.BookmarkRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BookmarkRepositoryImpl(
    private val db: Database
) : BookmarkRepository {
    override fun getBookmarks(): Flow<List<Bookmark>> {
        return db.bookmarkQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addBookmark(bookmark: Bookmark) {
        withContext(Dispatchers.Default) {
            db.bookmarkQueries.insert(bookmark.toEntity())
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