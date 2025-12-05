package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.repository.FeedRepository
import ai.saniou.thread.domain.repository.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.ExperimentalTime

class FeedRepositoryImpl(
    private val sources: Set<Source>
) : FeedRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }

    override suspend fun getForums(sourceId: String): Result<List<Forum>> {
        val source = sourceMap[sourceId] ?: return Result.failure(IllegalArgumentException("Source not found: $sourceId"))
        return source.getForums()
    }

    override suspend fun getPosts(sourceId: String, forumId: String, page: Int): Result<List<Post>> {
        val source = sourceMap[sourceId] ?: return Result.failure(IllegalArgumentException("Source not found: $sourceId"))
        return source.getPosts(forumId, page)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun getAggregatedFeed(page: Int): Result<List<Post>> = coroutineScope {
        // 简单聚合：并行获取所有来源的第一页数据，然后合并排序
        // 实际应用中需要更复杂的策略，例如根据用户配置、分页等
        val results = sources.map { source ->
            async {
                // 注意：这里为简化，聚合流暂时只获取每个来源的第一个板块的帖子
                source.getForums().getOrNull()?.firstOrNull()?.let { forum ->
                    source.getPosts(forum.id, page)
                }
            }
        }.mapNotNull { it.await() }

        if (results.all { it.isSuccess }) {
            val allPosts = results.flatMap { it.getOrThrow() }.sortedByDescending { it.createdAt }
            Result.success(allPosts)
        } else {
            val firstError = results.first { it.isFailure }.exceptionOrNull()
            Result.failure(firstError ?: IllegalStateException("Unknown error during aggregation"))
        }
    }
}
