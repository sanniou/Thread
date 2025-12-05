package ai.saniou.thread.data.source.nga

import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.repository.Source

/**
 * NGA 信息源的骨架实现
 *
 * TODO: 对接真实的 NGA API
 */
class NgaSource : Source {
    override val id: String = "nga"

    override suspend fun getForums(): Result<List<Forum>> {
        // 骨架实现，返回空列表
        return Result.success(emptyList())
    }

    override suspend fun getPosts(forumId: String, page: Int): Result<List<Post>> {
        // 骨架实现，返回空列表
        return Result.success(emptyList())
    }
}