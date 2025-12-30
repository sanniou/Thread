package ai.saniou.thread.data.source.nga

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.model.forum.Comment
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

    override fun observeChannels(): Flow<List<Channel>> {
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }

    override suspend fun fetchChannels(): Result<Unit> {
        return Result.success(Unit)
    }

    override fun getThreadsPager(
        forumId: String,
        isTimeline: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Topic>> {
        return flowOf(PagingData.empty())
    }

    override suspend fun getThreadDetail(threadId: String, page: Int): Result<Topic> {
        return Result.failure(NotImplementedError("NGA source not implemented"))
    }

    override fun getThreadRepliesPager(
        threadId: String,
        initialPage: Int,
        isPoOnly: Boolean,
    ): Flow<PagingData<Comment>> {
        return flowOf(PagingData.empty())
    }

    override fun getForum(forumId: String): Flow<Channel?> {
        TODO("Not yet implemented")
    }
}
