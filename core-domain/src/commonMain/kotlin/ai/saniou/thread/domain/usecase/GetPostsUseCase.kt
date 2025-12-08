package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.repository.FeedRepository

/**
 * 获取指定来源、指定板块的帖子列表
 */
class GetPostsUseCase(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(sourceId: String, forumId: String, page: Int): Result<List<Post>> {
        return feedRepository.getPosts(sourceId, forumId, page)
    }
}