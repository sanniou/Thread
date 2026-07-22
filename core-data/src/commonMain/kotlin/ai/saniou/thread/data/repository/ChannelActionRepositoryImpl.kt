package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.data.source.tieba.TiebaChannelSign
import ai.saniou.thread.data.source.tieba.TiebaForumRuleService
import ai.saniou.thread.data.source.tieba.TiebaMapper
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.ForumRuleDetail
import ai.saniou.thread.domain.repository.ChannelActionRepository
import kotlinx.coroutines.withContext

class ChannelActionRepositoryImpl(
    private val tiebaSign: TiebaChannelSign,
    private val tiebaRules: TiebaForumRuleService,
) : ChannelActionRepository {
    override suspend fun signChannel(sourceId: String, channel: Channel): String =
        withContext(ioDispatcher) {
            require(sourceId == TiebaMapper.SOURCE_ID) { "当前源不支持签到" }
            tiebaSign.sign(channel)
        }

    override suspend fun signFavoriteChannels(sourceId: String): String =
        withContext(ioDispatcher) {
            require(sourceId == TiebaMapper.SOURCE_ID) { "当前源不支持一键签到" }
            tiebaSign.signFavorites()
        }

    override suspend fun getForumRules(sourceId: String, channelId: String): ForumRuleDetail =
        withContext(ioDispatcher) {
            require(sourceId == TiebaMapper.SOURCE_ID) { "当前源不支持吧规" }
            tiebaRules.load(channelId)
        }
}
