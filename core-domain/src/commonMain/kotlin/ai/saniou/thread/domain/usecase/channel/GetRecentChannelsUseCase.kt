package ai.saniou.thread.domain.usecase.channel

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow

class GetRecentChannelsUseCase(
    private val channelRepository: ChannelRepository,
) {
    operator fun invoke(sourceId: String, limit: Long = 5): Flow<List<Channel>> =
        channelRepository.getRecentChannels(sourceId, limit)
}
