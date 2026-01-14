package ai.saniou.thread.domain.usecase.channel

import ai.saniou.thread.domain.repository.ChannelRepository

class SetChannelFallbackModeUseCase(
    private val channelRepository: ChannelRepository,
) {
    suspend operator fun invoke(
        sourceId: String,
        channelId: String,
        enabled: Boolean,
    ) {
        channelRepository.setFallbackMode(sourceId, channelId, enabled)
    }
}