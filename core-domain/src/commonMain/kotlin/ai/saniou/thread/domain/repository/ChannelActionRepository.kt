package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.ForumRuleDetail

/** Optional channel-side actions (sign / rules). Unsupported sources throw. */
interface ChannelActionRepository {
    suspend fun signChannel(sourceId: String, channel: Channel): String

    suspend fun signFavoriteChannels(sourceId: String): String

    suspend fun getForumRules(sourceId: String, channelId: String): ForumRuleDetail
}
