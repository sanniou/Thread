package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Forum
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.model.forum.ThreadReply
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
     * 获取所有板块
     */
    suspend fun getForums(): Result<List<Forum>>

    /**
     * 获取指定板块的帖子列表
     * @param forumId 板块ID
     * @param page 页码
     */
    suspend fun getPosts(forumId: String, page: Int): Result<List<Post>>

    /**
     * 获取板块帖子分页数据
     */
    fun getThreadsPager(
        forumId: String,
        isTimeline: Boolean,
        initialPage: Int = 1,
    ): Flow<PagingData<Post>>

    suspend fun getThreadDetail(threadId: String, page: Int): Result<Post>

    fun getThreadRepliesPager(
        threadId: String,
        initialPage: Int,
    ): Flow<PagingData<ThreadReply>>

    /**
     * 获取板块详情
     */
    fun getForum(forumId: String): Flow<Forum?>
}

/**
 * 信息流仓库接口，定义了领域层需要的数据操作
 */
interface SourceRepository {
    /**
     * 从指定的信息源获取板块列表
     * @param sourceId 信息源ID
     */
    suspend fun getForums(sourceId: String): Result<List<Forum>>

    /**
     * 从指定信息源的板块获取帖子列表
     * @param sourceId 信息源ID
     * @param forumId 板块ID
     * @param page 页码
     */
    suspend fun getPosts(sourceId: String, forumId: String, page: Int): Result<List<Post>>

    /**
     * 获取聚合的信息流
     * @param page 页码
     */
    suspend fun getAggregatedFeed(page: Int): Result<List<Post>>

    /**
     * 获取所有可用的信息源
     */
    fun getAvailableSources(): List<Source>
}
