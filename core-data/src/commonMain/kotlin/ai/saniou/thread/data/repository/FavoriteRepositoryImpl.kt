package ai.saniou.thread.data.repository

import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.db.Database
import ai.saniou.thread.data.source.nmb.remote.dto.FavoriteChannelType
import ai.saniou.thread.domain.model.forum.Channel
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

    override fun getFavoriteChannels(sourceId: String): Flow<List<Channel>> {
        return db.favoriteChannelQueries.getAllFavoriteChannel(sourceId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { forums -> forums.map { it.toDomain(db.channelQueries) } }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun toggleFavorite(sourceId: String, channel: Channel) {
        if (db.favoriteChannelQueries.countFavoriteChannel(sourceId, channel.id).executeAsOne() < 1L) {
            db.favoriteChannelQueries.insertFavoriteChannel(
                id = channel.id,
                sourceId = sourceId,
                favoriteTime = Clock.System.now().toEpochMilliseconds(),
                type = if (channel.tag == "timeline") FavoriteChannelType.TIMELINE else FavoriteChannelType.CHANNEL
            )
        } else {
            db.favoriteChannelQueries.deleteFavoriteChannel(sourceId, channel.id)
        }
    }

    override fun isFavorite(sourceId: String, channel: String): Flow<Boolean> {
        return db.favoriteChannelQueries.getFavoriteChannel(sourceId, channel)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.isNotEmpty() }
    }
}
