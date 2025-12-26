package ai.saniou.thread.domain.usecase.channel

import ai.saniou.thread.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow

class GetChannelNameUseCase(
    private val channelRepository: ChannelRepository
) {
    operator fun invoke(sourceId: String, fid: String): Flow<String?> {
        return channelRepository.getChannelName(sourceId, fid)
    }
}
