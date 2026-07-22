package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.db.Database
import ai.saniou.thread.data.source.nmb.remote.dto.FavoriteChannelType
import ai.saniou.thread.data.source.tieba.TiebaChannelMembership
import ai.saniou.thread.data.source.tieba.TiebaMapper
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.repository.FavoriteRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class FavoriteRepositoryImpl(
    private val db: Database,
    private val tiebaMembership: TiebaChannelMembership,
) : FavoriteRepository {

    override fun getFavoriteChannels(sourceId: String): Flow<List<Channel>> {
        return db.favoriteChannelQueries.getAllFavoriteChannel(sourceId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { forums -> forums.map { it.toDomain(db.channelQueries) } }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun toggleFavorite(sourceId: String, channel: Channel) {
        withContext(ioDispatcher) {
            val currentlyFavorite =
                db.favoriteChannelQueries.countFavoriteChannel(sourceId, channel.id).executeAsOne() >= 1L
            if (sourceId == TiebaMapper.SOURCE_ID) {
                // Remote-first so failed network doesn't leave a false local star.
                if (currentlyFavorite) tiebaMembership.unfollow(channel) else tiebaMembership.follow(channel)
            }
            if (currentlyFavorite) {
                db.favoriteChannelQueries.deleteFavoriteChannel(sourceId, channel.id)
            } else {
                db.favoriteChannelQueries.insertFavoriteChannel(
                    id = channel.id,
                    sourceId = sourceId,
                    favoriteTime = Clock.System.now().toEpochMilliseconds(),
                    type = if (channel.tag == "timeline") FavoriteChannelType.TIMELINE else FavoriteChannelType.CHANNEL,
                )
                // Ensure channel row exists for join queries after remote-only follow.
                if (sourceId == TiebaMapper.SOURCE_ID) {
                    val existing = db.channelQueries.getChannel(sourceId, channel.id).executeAsOneOrNull()
                    if (existing == null) {
                        db.channelQueries.insertChannel(
                            ai.saniou.thread.data.source.tieba.TiebaMapper.mapChannelToEntity(channel),
                        )
                    }
                }
            }
        }
    }

    override fun isFavorite(sourceId: String, channel: String): Flow<Boolean> {
        return db.favoriteChannelQueries.getFavoriteChannel(sourceId, channel)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { it.isNotEmpty() }
    }
}
