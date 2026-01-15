package ai.saniou.thread.data.source.acfun

import ai.saniou.thread.data.model.CommentKey
import ai.saniou.thread.data.source.acfun.remote.AcfunApi
import ai.saniou.thread.data.source.acfun.remote.AcfunTokenManager
import ai.saniou.thread.domain.model.PagedResult
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.Trend
import ai.saniou.thread.domain.model.forum.TrendResult
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SourceCapabilities
import ai.saniou.thread.network.SaniouResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

class AcfunSource(
    private val acfunApi: AcfunApi,
    private val acfunTokenManager: AcfunTokenManager,
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
                is SaniouResult.Success -> {
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

                is SaniouResult.Error -> {
                    // Handle initialization error silently or log it
                }
            }
        }
        _isInitialized.value = true
    }

    override val capabilities: SourceCapabilities = SourceCapabilities(
        supportsPagination = false
    )

    override val loginStrategy: LoginStrategy = LoginStrategy.Api(
        title = "AcFun 登录"
    )

    override fun observeChannels(): Flow<List<Channel>> {
        return flowOf(emptyList())
    }

    override suspend fun fetchChannels(): Result<Unit> {
        // TODO: Implement fetchChannels
        return Result.success(Unit)
    }

    override suspend fun getChannelTopics(
        channelId: String,
        cursor: String?,
        isTimeline: Boolean,
    ): Result<PagedResult<Topic>> {
        return Result.failure(NotImplementedError("AcFun channel topics not implemented"))
    }


    override suspend fun getTopicDetail(threadId: String, page: Int): Result<Topic> {
        val articleId =
            threadId.toLongOrNull()
                ?: return Result.failure(IllegalArgumentException("Invalid threadId"))

        return when (val response = acfunApi.getArticleInfo(articleId)) {
            is SaniouResult.Success -> {
                val data = response.data
                if (data.result == 0) {
                    val post = Topic(
                        id = data.articleId?.toString() ?: threadId.toString(),
                        sourceName = name,
                        sourceId = id,
                        sourceUrl = "https://www.acfun.cn/a/ac${data.articleId}",
                        title = data.title ?: "",
                        content = data.parts?.joinToString("\n") { it.content } ?: data.description
                        ?: "",
                        author = Author(
                            id = data.user?.id?.toString() ?: "",
                            name = data.user?.name ?: "Unknown"
                        ),
                        createdAt = Instant.fromEpochMilliseconds(data.createTimeMillis ?: 0),
                        channelName = data.channel?.name ?: "文章",
                        commentCount = (data.commentCount ?: 0).toLong(),
                        images = emptyList(), // TODO: extract images from content or coverUrl
                        isLocal = false,
                        channelId = data.channel?.id?.toString() ?: "0",
                        summary = null,
                        lastViewedCommentId = "",
                        receiveDate = TODO(),
                        receiveOrder = TODO(),
                    )
                    Result.success(post)
                } else {
                    Result.failure(Exception("Get article info failed: result=${data.result}"))
                }
            }

            is SaniouResult.Error -> Result.failure(response.ex)
        }
    }

    override suspend fun getTopicComments(
        threadId: String,
        cursor: String?,
        isPoOnly: Boolean,
    ): Result<PagedResult<Comment>> {
        // AcFun uses "pcursor" which is a string.
        // Initial cursor is usually "0".
        val pcursor = cursor ?: "0"

        val sourceId = threadId.toLongOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid threadId"))

        return when (val response = acfunApi.getCommentList(
            sourceId,
            1,
            pcursor,
            20,
            if (pcursor == "0") 1 else 0
        )) {
            is SaniouResult.Success -> {
                val list = response.data.rootComments ?: emptyList()
                val replies = list.map { comment ->
                    Comment(
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
                        sourceId = id,
                        floor = comment.floor?.toLong() ?: 0L,
                        replyToId = null,
                        agreeCount = 0,
                        disagreeCount = 0,
                        subCommentCount = 0,
                        authorLevel = 0,
                        isPo = false,
                        subCommentsPreview = emptyList()
                    )
                }

                // AcFun API should return next cursor. Assuming response.data.pcursor is the next one.
                // If pcursor is "no_more" or similar, nextCursor is null.
                // Let's assume response.data.pcursor is the next cursor.
                val nextCursor =
                    if (response.data.pcursor == "no_more" || list.isEmpty()) null else response.data.pcursor

                Result.success(PagedResult(replies, null, nextCursor))
            }

            is SaniouResult.Error -> Result.failure(response.ex)
        }
    }

    override fun getChannel(channelId: String): Flow<Channel?> {
        return flowOf(null)
    }

    suspend fun getTrendList(forceRefresh: Boolean, dayOffset: Int): Result<TrendResult> {
        if (dayOffset != 0) {
            return Result.failure(IllegalArgumentException("AcFun does not support historical trends"))
        }
        return try {
            when (val response = acfunApi.getArticleHotRank()) {
                is SaniouResult.Success -> {
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

                is SaniouResult.Error -> {
                    Result.failure(response.ex)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
