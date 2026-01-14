package ai.saniou.thread.domain.usecase.channel

import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.ChannelRepository
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetChannelTopicsPagingUseCase(
    private val channelRepository: ChannelRepository,
) {
    operator fun invoke(
        sourceId: String,
        channelId: String,
        isTimeline: Boolean,
        initialPage: Int = 1,
    ): Flow<PagingData<Topic>> {
        return channelRepository.getChannelTopicsPaging(
            sourceId = sourceId,
            channelId = channelId,
            isTimeline = isTimeline,
            initialPage = initialPage
        )
    }
}
