package ai.saniou.thread.domain.usecase.channel

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.repository.ChannelRepository

class SaveLastOpenedChannelUseCase(
    private val channelRepository: ChannelRepository
) {
    suspend operator fun invoke(channel: Channel?) {
        channelRepository.saveLastOpenedChannel(channel)
    }
}