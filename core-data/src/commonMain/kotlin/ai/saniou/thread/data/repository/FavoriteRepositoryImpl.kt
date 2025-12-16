package ai.saniou.thread.data.repository

import ai.saniou.thread.db.Database
import ai.saniou.thread.data.source.nmb.remote.dto.FavoriteForumType
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.forum.Forum
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
        return db.favoriteForumQueries.getAllFavoriteForum(sourceId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { forums -> forums.map { it.toDomain() } }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun toggleFavorite(sourceId: String, forum: Forum) {
        if (db.favoriteForumQueries.countFavoriteForum(sourceId, forum.id).executeAsOne() < 1L) {
            db.favoriteForumQueries.insertFavoriteForum(
                id = forum.id,
                sourceId = sourceId,
                favoriteTime = Clock.System.now().toEpochMilliseconds(),
                type = if (forum.tag == "timeline") FavoriteForumType.TIMELINE else FavoriteForumType.FORUM
            )
        } else {
            db.favoriteForumQueries.deleteFavoriteForum(sourceId, forum.id)
        }
    }

    override fun isFavorite(sourceId: String, forumId: String): Flow<Boolean> {
        return db.favoriteForumQueries.getFavoriteForum(sourceId, forumId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.isNotEmpty() }
    }
}
