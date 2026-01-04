package ai.saniou.thread.domain.usecase.channel

import ai.saniou.thread.domain.repository.ChannelRepository

class FetchChannelsUseCase(
    private val channelRepository: ChannelRepository
) {
    suspend operator fun invoke(sourceId: String): Result<Unit> {
        return channelRepository.fetchChannels(sourceId)
    }
}