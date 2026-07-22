package ai.saniou.thread.domain.usecase.channel

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.ForumRuleDetail
import ai.saniou.thread.domain.repository.ChannelActionRepository

class SignChannelUseCase(
    private val repository: ChannelActionRepository,
) {
    suspend operator fun invoke(sourceId: String, channel: Channel): String =
        repository.signChannel(sourceId, channel)
}

class SignFavoriteChannelsUseCase(
    private val repository: ChannelActionRepository,
) {
    suspend operator fun invoke(sourceId: String): String =
        repository.signFavoriteChannels(sourceId)
}

class GetForumRulesUseCase(
    private val repository: ChannelActionRepository,
) {
    suspend operator fun invoke(sourceId: String, channelId: String): ForumRuleDetail =
        repository.getForumRules(sourceId, channelId)
}
