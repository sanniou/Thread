package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.TrendResult
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

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
     * 观察所有板块
     */
    fun observeChannels(): Flow<List<Channel>>

    /**
     * 刷新板块数据
     */
    suspend fun fetchChannels(): Result<Unit>

    /**
     * 获取板块帖子分页数据
     */
    fun getTopicsPager(
        channelId: String,
        isTimeline: Boolean,
        initialPage: Int = 1,
    ): Flow<PagingData<Topic>>

    suspend fun getTopicDetail(threadId: String, page: Int): Result<Topic>

    fun getTopicCommentsPager(
        threadId: String,
        initialPage: Int,
        isPoOnly: Boolean = false,
    ): Flow<PagingData<Comment>>

    /**
     * 获取板块详情
     */
    fun getChannel(channelId: String): Flow<Channel?>

    /**
     * 获取热门榜单
     */
    suspend fun getTrendList(forceRefresh: Boolean, dayOffset: Int): Result<TrendResult> =
        Result.failure(NotImplementedError("Trend not supported for this source"))
}

data class SourceCapabilities(
    val supportsTrend: Boolean = false,
    val supportsTrendHistory: Boolean = false,
    val supportsPagination: Boolean = true,
)

/**
 * 信息流仓库接口，定义了领域层需要的数据操作
 */
interface SourceRepository {
    /**
     * 获取聚合的信息流
     * @param page 页码
     */
    suspend fun getAggregatedFeed(page: Int): Result<List<Topic>>

    /**
     * 获取所有可用的信息源
     */
    fun getAvailableSources(): List<Source>

    /**
     * 获取指定 ID 的 Source
     */
    fun getSource(sourceId: String): Source?
}
