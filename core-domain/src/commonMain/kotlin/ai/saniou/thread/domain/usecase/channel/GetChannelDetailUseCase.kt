package ai.saniou.thread.domain.usecase.channel

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow

class GetChannelDetailUseCase(
    private val channelRepository: ChannelRepository
) {
    operator fun invoke(sourceId: String, fid: String): Flow<Channel?> {
        return channelRepository.getChannelDetail(sourceId, fid)
    }
}
