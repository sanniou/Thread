package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.domain.model.Bookmark
import ai.saniou.thread.domain.repository.BookmarkRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * BookmarkRepository 的具体实现，负责与本地数据库交互。
 */
class BookmarkRepositoryImpl(
    private val db: Database
) : BookmarkRepository {
    override fun getBookmarks(): Flow<List<Bookmark>> {
        return db.bookmarkQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun addBookmark(postId: String, content: String, tag: String?) {
        db.bookmarkQueries.insert(
            postId = postId,
            content = content,
            tag = tag,
            createdAt = Clock.System.now().epochSeconds
        )
    }

    override suspend fun removeBookmark(postId: String) {
        db.bookmarkQueries.delete(postId)
    }

    override fun isBookmarked(postId: String): Flow<Boolean> {
        return db.bookmarkQueries.getById(postId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.isNotEmpty() }
    }
}