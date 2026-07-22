package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.repository.TopicRepository

class FetchTopicImagePageUseCase(
    private val topicRepository: TopicRepository,
) {
    suspend operator fun invoke(
        sourceId: String,
        threadId: String,
        channelId: String,
        channelName: String,
        picId: String = "",
        picIndex: String = "1",
        seeLz: Boolean = false,
        forward: Boolean = true,
        batchSize: Int = 10,
    ): Result<List<Image>> {
        return topicRepository.fetchTopicImagePage(
            sourceId = sourceId,
            threadId = threadId,
            channelId = channelId,
            channelName = channelName,
            picId = picId,
            picIndex = picIndex,
            seeLz = seeLz,
            forward = forward,
            batchSize = batchSize,
        )
    }
}
