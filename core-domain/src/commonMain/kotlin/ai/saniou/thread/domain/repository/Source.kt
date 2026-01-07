package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.FeedType
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.TrendResult
import ai.saniou.thread.domain.model.user.LoginStrategy
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class SourceCapabilities(
    val supportsTrend: Boolean = false,
    val supportsTrendHistory: Boolean = false,
    val supportsPagination: Boolean = true,
)

/**
 * 信息源的标准接口，所有具体的信息源（如NMB, NGA）都需要实现它
 */
interface Source {
    /**
     * 来源的唯一标识，如 "nmb", "nga"
     */
    val id: String

    /**
     * 来源的显示名称，如 "A岛", "NGA"
     */
    val name: String

    /**
     * 是否已初始化
     */
    val isInitialized: Flow<Boolean>

    /**
     * 功能能力标志
     */
    val capabilities: SourceCapabilities get() = SourceCapabilities()

    /**
     * 获取登录策略
     */
    val loginStrategy: LoginStrategy

    /**
     * 观察所有板块
     */
    fun observeChannels(): Flow<List<Channel>>

    /**
     * 刷新板块数据
     */
    suspend fun fetchChannels(): Result<Unit>

    /**
     * 获取板块下的帖子列表（分页）
     * 替代 getTopicsPager，配合 RemoteMediator 使用
     */
    suspend fun getChannelTopics(
        channelId: String,
        page: Int,
        isTimeline: Boolean
    ): Result<List<Topic>> = Result.failure(NotImplementedError("Not implemented"))

    suspend fun getTopicDetail(threadId: String, page: Int): Result<Topic>

    /**
     * 获取指定帖子的回复列表（分页）
     * 替代 getTopicCommentsPager，配合 RemoteMediator 使用
     */
    suspend fun getTopicComments(
        threadId: String,
        page: Int,
        isPoOnly: Boolean = false,
    ): Result<List<Comment>> = Result.failure(NotImplementedError("Not implemented"))

    /**
     * 获取板块详情
     */
    fun getChannel(channelId: String): Flow<Channel?>

    /**
     * 获取热门榜单
     */
    suspend fun getTrendList(forceRefresh: Boolean, dayOffset: Int): Result<TrendResult> =
        Result.failure(NotImplementedError("Trend not supported for this source"))

    /**
     * 获取 Feed 流（分页）
     */
    fun getFeedFlow(feedType: FeedType): Flow<PagingData<Topic>> = emptyFlow()
}
