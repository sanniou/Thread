package ai.saniou.thread.domain.usecase.channel

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.repository.SourceRepository

/**
 * 获取指定来源的板块列表
 */
class GetChannelsUseCase(
    private val sourceRepository: SourceRepository
) {
    suspend operator fun invoke(sourceId: String): Result<List<Channel>> {
        return sourceRepository.getChannels(sourceId)
    }
}
