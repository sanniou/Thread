package ai.saniou.thread.data.source.acfun

import ai.saniou.thread.data.source.acfun.remote.AcfunApi
import ai.saniou.thread.data.source.acfun.remote.AcfunTokenManager
import ai.saniou.thread.domain.model.forum.Channel as Forum
import ai.saniou.thread.domain.model.forum.Topic as Post
import ai.saniou.thread.domain.model.forum.Comment as ThreadReply
import ai.saniou.thread.domain.model.forum.Trend
import ai.saniou.thread.domain.model.forum.TrendResult
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SourceCapabilities
import ai.saniou.thread.network.SaniouResponse
import ai.saniou.thread.domain.model.forum.Author
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.PagingSource
import app.cash.paging.PagingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Clock

class AcfunSource(
    private val acfunApi: AcfunApi,
    private val acfunTokenManager: AcfunTokenManager
) : Source {
    override val id: String = "acfun"
    override val name: String = "AcFun"

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: Flow<Boolean> = _isInitialized

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initialize()
        }
    }

    private suspend fun initialize() {
        acfunTokenManager.loadTokens()
        if (acfunTokenManager.getToken() == null) {
            when (val response = acfunApi.visitorLogin()) {
                is SaniouResponse.Success -> {
                    val data = response.data
                    if (data.result == 0 && data.acSecurity != null && data.userId != null && data.serviceToken != null) {
                        val cookie = acfunTokenManager.buildVisitorCookie(
                            data.acSecurity,
                            data.userId,
                            data.serviceToken
                        )
                        acfunTokenManager.setTokens(
                            cookie = cookie,
                            token = null,
                            acSecurity = data.acSecurity,
                            userId = data.userId
                        )
                    }
                }

                is SaniouResponse.Error -> {
                    // Handle initialization error silently or log it
                }
            }
        }
        _isInitialized.value = true
    }

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
                val data = response.data
                if (data.result == 0) {
                    val post = Post(
                        id = data.articleId?.toString() ?: threadId.toString(),
                        sourceName = "acfun",
                        sourceUrl = "https://www.acfun.cn/a/ac${data.articleId}",
                        title = data.title?:"",
                        content = data.parts?.joinToString("\n") { it.content } ?: data.description
                        ?: "",
                        author = Author(id = data.user?.id?.toString() ?: "", name = data.user?.name ?: "Unknown"),
                        createdAt = Instant.fromEpochMilliseconds(data.createTimeMillis ?: 0),
                        channelName = data.channel?.name ?: "文章",
                        commentCount = (data.commentCount ?: 0).toLong(),
                        images = emptyList(), // TODO: extract images from content or coverUrl
                        isSage = false,
                        isAdmin = false,
                        isHidden = false,
                        isLocal = false,
                        channelId = data.channel?.id?.toString() ?: "0",
                        lastViewedCommentId = "",
                    )
                    Result.success(post)
                } else {
                    Result.failure(Exception("Get article info failed: result=${data.result}"))
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
                                        author = Author(
                                            id = comment.user?.id?.toString() ?: "",
                                            name = comment.user?.name ?: "Unknown",
                                            avatar = comment.user?.headUrl
                                        ),
                                        createdAt = Instant.fromEpochMilliseconds(
                                            comment.timestamp ?: 0
                                        ),
                                        images = emptyList(),
                                        isAdmin = false,
                                        title = "No.${comment.floor}",
                                        topicId = threadId,
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
                            channel = "[${item.channelName ?: "文章"}]",
                            isNew = false,
                            topicId = item.resourceId.toString(),
                            contentPreview = "${item.contentTitle ?: ""}\n${item.userName ?: ""}"
                        )
                    }
                    val trendResult = TrendResult(
                        date = Clock.System.now(),
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
