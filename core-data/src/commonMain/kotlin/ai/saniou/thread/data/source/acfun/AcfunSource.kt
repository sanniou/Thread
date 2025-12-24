package ai.saniou.thread.data.source.acfun

import ai.saniou.thread.data.source.acfun.remote.AcfunApi
import ai.saniou.thread.domain.model.forum.Forum
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.model.forum.ThreadReply
import ai.saniou.thread.domain.model.forum.Trend
import ai.saniou.thread.domain.model.forum.TrendResult
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SourceCapabilities
import ai.saniou.thread.network.SaniouResponse
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class AcfunSource(
    private val acfunApi: AcfunApi
) : Source {
    override val id: String = "acfun"
    override val name: String = "AcFun"
    override val isInitialized: Flow<Boolean> = flowOf(true)

    override val capabilities: SourceCapabilities = SourceCapabilities(
        supportsTrend = true,
        supportsTrendHistory = false,
        supportsPagination = false
    )

    override suspend fun getForums(): Result<List<Forum>> {
        // TODO: Implement getForums
        return Result.success(emptyList())
    }

    override suspend fun getPosts(forumId: String, page: Int): Result<List<Post>> {
        // TODO: Implement getPosts
        return Result.success(emptyList())
    }

    override fun getThreadsPager(
        forumId: String,
        isTimeline: Boolean,
        initialPage: Int
    ): Flow<PagingData<Post>> {
        return flowOf(PagingData.empty())
    }

    override suspend fun getThreadDetail(threadId: String, page: Int): Result<Post> {
        // TODO: Implement getThreadDetail
        return Result.failure(NotImplementedError())
    }

    override fun getThreadRepliesPager(
        threadId: String,
        initialPage: Int,
        isPoOnly: Boolean
    ): Flow<PagingData<ThreadReply>> {
        return flowOf(PagingData.empty())
    }

    override fun getForum(forumId: String): Flow<Forum?> {
        return flowOf(null)
    }

    override suspend fun getTrendList(forceRefresh: Boolean, dayOffset: Int): Result<TrendResult> {
        if (dayOffset != 0) {
            return Result.failure(IllegalArgumentException("AcFun does not support historical trends"))
        }
        return try {
            when (val response = acfunApi.getArticleHotRank()) {
                is SaniouResponse.Success -> {
                    val rankList = response.data.rankList
                    val trends = rankList.mapIndexed { index, item ->
                        Trend(
                            rank = (index + 1).toString().padStart(2, '0'),
                            trendNum = "围观: ${item.viewCountShow ?: item.viewCount ?: 0}",
                            forum = "[${item.channelName ?: "文章"}]",
                            isNew = false, // Acfun API doesn't seem to provide "isNew" flag directly
                            threadId = item.resourceId,
                            contentPreview = "${item.contentTitle ?: ""}\n${item.userName ?: ""}"
                        )
                    }
                    // AcFun doesn't provide a specific trend date, so we use the current date
                    val trendResult = TrendResult(
                        date = "", // Or current date if needed by UI
                        items = trends
                    )
                    Result.success(trendResult)
                }
                is SaniouResponse.Error -> {
                    Result.failure(response.ex)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
