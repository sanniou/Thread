package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.Flow

/**
 * 获取帖子详情的用例。
 *
 * 这是一个纯粹的业务逻辑组件，它依赖于 [ThreadRepository] 接口来获取数据，
 * 而不关心数据的具体来源（网络、数据库等）。
 */
class GetThreadDetailUseCase(
    private val threadRepository: ThreadRepository
) {
    /**
     * @param id 帖子 ID
     * @param forceRefresh 是否强制从网络刷新
     * @return 包含帖子详情的 Flow
     */
    operator fun invoke(id: Long, forceRefresh: Boolean = false): Flow<Post> {
        return threadRepository.getThreadDetail(id, forceRefresh)
    }
}
