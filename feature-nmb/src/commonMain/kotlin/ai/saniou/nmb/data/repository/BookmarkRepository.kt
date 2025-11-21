package ai.saniou.nmb.data.repository

import ai.saniou.nmb.db.table.Bookmark
import ai.saniou.nmb.db.Database
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class BookmarkRepository(
    private val db: Database
) {
    fun getBookmarks(): Flow<List<Bookmark>> {
        return db.bookmarkQueries.selectAll().asFlow().mapToList(Dispatchers.IO)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun addBookmark(postId: String, content: String, tag: String? = null) {
        db.bookmarkQueries.insert(
            postId = postId,
            content = content,
            tag = tag,
            createdAt = Clock.System.now().epochSeconds
        )
    }

    suspend fun removeBookmark(postId: String) {
        db.bookmarkQueries.delete(postId)
    }

    fun isBookmarked(postId: String): Flow<Boolean> {
        return db.bookmarkQueries.getById(postId).asFlow().mapToList(Dispatchers.IO).map { it.isNotEmpty() }
    }
}
