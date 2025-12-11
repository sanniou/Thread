package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.source.nmb.remote.dto.FavoriteForumType
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.repository.FavoriteRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class FavoriteRepositoryImpl(
    private val db: Database
) : FavoriteRepository {

    override fun getFavoriteForums(sourceId: String): Flow<List<Forum>> {
        // Currently, only nmb is supported
        if (sourceId != "nmb") return kotlinx.coroutines.flow.flowOf(emptyList())

        return db.favoriteForumQueries.getAllFavoriteForum()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { forums -> forums.map { it.toDomain()} }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun toggleFavorite(sourceId: String, forum: Forum) {
        if (sourceId != "nmb") return

        if (db.favoriteForumQueries.countFavoriteForum(forum.id.toLong()).executeAsOne() < 1L) {
            db.favoriteForumQueries.insertFavoriteForum(
                id = forum.id.toLong(),
                favoriteTime = Clock.System.now().toEpochMilliseconds(),
                type = if (forum.tag == "timeline") FavoriteForumType.TIMELINE else FavoriteForumType.FORUM
            )
        } else {
            db.favoriteForumQueries.deleteFavoriteForum(forum.id.toLong())
        }
    }

    override fun isFavorite(sourceId: String, forumId: String): Flow<Boolean> {
        if (sourceId != "nmb") return kotlinx.coroutines.flow.flowOf(false)
        return db.favoriteForumQueries.getFavoriteForum(forumId.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.isNotEmpty() }
    }
}
