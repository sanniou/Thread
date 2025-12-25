package ai.saniou.thread.domain.usecase.forum

import ai.saniou.thread.domain.model.forum.Channel as Forum
import ai.saniou.thread.domain.repository.SourceRepository

/**
 * 获取指定来源的板块列表
 */
class GetForumsUseCase(
    private val sourceRepository: SourceRepository
) {
    suspend operator fun invoke(sourceId: String): Result<List<Forum>> {
        return sourceRepository.getForums(sourceId)
    }
}
