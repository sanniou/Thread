package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.nmb.remote.dto.FavoriteChannelType
import ai.saniou.thread.data.source.tieba.model.UserLikeForumBean
import ai.saniou.thread.data.source.tieba.remote.MiniTiebaApi
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Channel
import kotlin.time.Clock

/**
 * Syncs a user's followed forums into local Channel + FavoriteChannel tables.
 * Used for the current account and for other users' like-forum lists (read cache).
 */
class TiebaUserLikeForumSync(
    private val api: MiniTiebaApi,
    private val database: Database,
    private val parameterProvider: TiebaParameterProvider,
) {
    val sourceId: String = TiebaMapper.SOURCE_ID

    suspend fun syncUserForums(userId: String, markFavorite: Boolean): List<Channel> {
        val response = api.userLikeForum(
            page = 1,
            pageSize = 50,
            uid = userId,
            friendUid = null,
            is_guest = if (userId == parameterProvider.getUid()) null else "1",
        )
        if (response.errorCode.isNotBlank() && response.errorCode != "0") {
            throw IllegalStateException(response.errorMsg.ifBlank { "关注吧列表失败 (${response.errorCode})" })
        }
        val forums = response.forumList.forumList + response.commonForumList.forumList
        val channels = forums.mapNotNull { it.toChannel() }
        val now = Clock.System.now().toEpochMilliseconds()
        database.transaction {
            channels.forEach { channel ->
                database.channelQueries.insertChannel(TiebaMapper.mapChannelToEntity(channel))
                if (markFavorite) {
                    val exists = database.favoriteChannelQueries
                        .countFavoriteChannel(sourceId, channel.id)
                        .executeAsOne()
                    if (exists < 1L) {
                        database.favoriteChannelQueries.insertFavoriteChannel(
                            id = channel.id,
                            sourceId = sourceId,
                            favoriteTime = now,
                            type = FavoriteChannelType.CHANNEL,
                        )
                    }
                }
            }
        }
        return channels
    }

    suspend fun syncCurrentUserFavorites(): List<Channel> {
        val uid = parameterProvider.getUid().takeIf(String::isNotBlank) ?: return emptyList()
        return syncUserForums(userId = uid, markFavorite = true)
    }
}

internal fun UserLikeForumBean.ForumBean.toChannel(): Channel? {
    val forumId = id.takeIf(String::isNotBlank) ?: return null
    val forumName = name?.takeIf(String::isNotBlank) ?: return null
    return Channel(
        id = forumId,
        name = forumName,
        displayName = forumName,
        description = slogan.orEmpty(),
        descriptionText = slogan,
        groupId = "tieba_fav",
        groupName = levelName?.takeIf(String::isNotBlank)?.let { "Lv.$levelId $it" } ?: "关注的吧",
        sourceName = TiebaMapper.SOURCE_NAME,
        sourceId = TiebaMapper.SOURCE_ID,
        sort = 0,
        topicCount = null,
        postCount = null,
        autoDelete = null,
        logoUrl = avatar,
        icon = "font-awesome:fa-comments",
    )
}
