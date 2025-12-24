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
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.PagingSource
import app.cash.paging.PagingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant

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
        val articleId =
            threadId.toLongOrNull() ?: return Result.failure(IllegalArgumentException("Invalid threadId"))

        return when (val response = acfunApi.getArticleInfo(articleId)) {
            is SaniouResponse.Success -> {
                val data = response.data.data
                if (data != null) {
                    val post = Post(
                        id = data.articleId.toString(),
                        sourceName = "acfun",
                        sourceUrl = "https://www.acfun.cn/a/ac${data.articleId}",
                        title = data.contentTitle,
                        content = data.parts?.joinToString("\n") { it.content } ?: data.description
                        ?: "",
                        author = data.user?.userName ?: "Unknown",
                        userHash = data.user?.userId?.toString() ?: "",
                        createdAt = Instant.fromEpochMilliseconds(data.createTimeMillis ?: 0),
                        forumName = data.channel?.name ?: "文章",
                        replyCount = data.commentCount ?: 0,
                        img = data.coverUrl,
                        ext = null,
                        isSage = false,
                        isAdmin = false,
                        isHidden = false,
                        now = "",
                        name = data.user?.userName ?: "Unknown",
                        sage = 0,
                        fid = data.channel?.id?.toLong() ?: 0,
                        admin = 0,
                        hide = 0,
                        lastReadReplyId = ""
                    )
                    Result.success(post)
                } else {
                    Result.failure(Exception(response.data.message ?: "Unknown error"))
                }
            }

            is SaniouResponse.Error -> Result.failure(response.ex)
        }
    }

    override fun getThreadRepliesPager(
        threadId: String,
        initialPage: Int,
        isPoOnly: Boolean
    ): Flow<PagingData<ThreadReply>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                object : PagingSource<String, ThreadReply>() {
                    override suspend fun load(params: LoadParams<String>): LoadResult<String, ThreadReply> {
                        val pcursor = params.key ?: "0"
                        val sourceId = threadId.toLongOrNull()
                            ?: return LoadResult.Error(IllegalArgumentException("Invalid threadId"))

                        return when (val response = acfunApi.getCommentList(
                            sourceId,
                            1,
                            pcursor,
                            20,
                            if (pcursor == "0") 1 else 0
                        )) { // sourceType 1 for article
                            is SaniouResponse.Success -> {
                                val list = response.data.rootComments ?: emptyList()
                                val nextKey =
                                    if (response.data.pcursor == "nomore" || list.isEmpty()) null else response.data.pcursor

                                val replies = list.map { comment ->
                                    ThreadReply(
                                        id = comment.commentId.toString(),
                                        content = comment.content ?: "",
//                                        author = comment.user?.userName ?: "Unknown",
                                        userHash = comment.user?.userId?.toString() ?: "",
                                        createdAt = Instant.fromEpochMilliseconds(
                                            comment.timestamp ?: 0
                                        ),
                                        img = "",
                                        ext = "",
                                        now = comment.postDate ?: "",
                                        name = comment.user?.userName ?: "Unknown",
                                        admin = 0,
                                        title = "No.${comment.floor}",
                                        threadId = threadId,
                                    )
                                }
                                LoadResult.Page(data = replies, prevKey = null, nextKey = nextKey)
                            }

                            is SaniouResponse.Error -> LoadResult.Error(response.ex)
                        }
                    }

                    override fun getRefreshKey(state: PagingState<String, ThreadReply>): String? {
                        return null
                    }
                }
            }
        ).flow
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
                            isNew = false,
                            threadId = item.resourceId,
                            contentPreview = "${item.contentTitle ?: ""}\n${item.userName ?: ""}"
                        )
                    }
                    val trendResult = TrendResult(
                        date = "",
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
