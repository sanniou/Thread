package ai.saniou.thread.data.source.nga

import ai.saniou.thread.domain.model.forum.Forum
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.model.forum.ThreadReply
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * NGA 信息源的骨架实现
 *
 * TODO: 对接真实的 NGA API
 */
class NgaSource : Source {
    override val id: String = "nga"
    override val name: String = "NGA"

    override val isInitialized: Flow<Boolean> = flowOf(true)

    override suspend fun getForums(): Result<List<Forum>> {
        // 骨架实现，返回空列表
        return Result.success(emptyList())
    }

    override suspend fun getPosts(forumId: String, page: Int): Result<List<Post>> {
        // 骨架实现，返回空列表
        return Result.success(emptyList())
    }

    override fun getThreadsPager(
        forumId: String,
        isTimeline: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Post>> {
        return flowOf(PagingData.empty())
    }

    override suspend fun getThreadDetail(threadId: String, page: Int): Result<Post> {
        return Result.failure(NotImplementedError("NGA source not implemented"))
    }

    override fun getThreadRepliesPager(
        threadId: String,
        initialPage: Int,
        isPoOnly: Boolean,
    ): Flow<PagingData<ThreadReply>> {
        return flowOf(PagingData.empty())
    }

    override fun getForum(forumId: String): Flow<Forum?> {
        TODO("Not yet implemented")
    }
}
