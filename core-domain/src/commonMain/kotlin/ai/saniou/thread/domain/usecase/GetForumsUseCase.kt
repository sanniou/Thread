package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.repository.FeedRepository

/**
 * 获取指定来源的板块列表
 */
class GetForumsUseCase(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(sourceId: String): Result<List<Forum>> {
        return feedRepository.getForums(sourceId)
    }
}