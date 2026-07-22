package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.MiniTiebaApi
import ai.saniou.thread.data.source.tieba.remote.WebTiebaApi
import ai.saniou.thread.domain.model.forum.Channel

/**
 * Remote follow/unfollow for Tieba forums (likeForum / unlikeForum).
 * Local favorite rows still live in FavoriteRepository; this is the network side-effect.
 */
class TiebaChannelMembership(
    private val api: MiniTiebaApi,
    private val webApi: WebTiebaApi,
    private val parameterProvider: TiebaParameterProvider,
) {
    val sourceId: String = TiebaMapper.SOURCE_ID

    suspend fun follow(channel: Channel) {
        val tbs = parameterProvider.ensureTbs(webApi)
        val response = api.likeForum(
            forumId = channel.id,
            forumName = channel.name,
            tbs = tbs,
        )
        if (response.errorCode.isNotBlank() && response.errorCode != "0") {
            val msg = response.error?.usermsg?.takeIf(String::isNotBlank)
                ?: response.error?.errmsg?.takeIf(String::isNotBlank)
                ?: "关注贴吧失败 (${response.errorCode})"
            throw IllegalStateException(msg)
        }
    }

    suspend fun unfollow(channel: Channel) {
        val tbs = parameterProvider.ensureTbs(webApi)
        val response = api.unlikeForum(
            forumId = channel.id,
            forumName = channel.name,
            tbs = tbs,
        )
        if (response.errorCode != 0) {
            throw IllegalStateException(response.errorMsg.ifBlank { "取消关注贴吧失败 (${response.errorCode})" })
        }
    }
}
