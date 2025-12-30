package ai.saniou.thread.domain.usecase.channel

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow

/**
 * 获取指定来源的板块列表
 */
class GetChannelsUseCase(
    private val channelRepository: ChannelRepository
) {
    operator fun invoke(sourceId: String): Flow<List<Channel>> {
        return channelRepository.getChannels(sourceId)
    }
}
