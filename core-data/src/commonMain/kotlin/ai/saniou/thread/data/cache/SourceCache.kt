package ai.saniou.thread.data.cache

import ai.saniou.thread.db.table.forum.Channel
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset
import ai.saniou.thread.db.table.forum.Topic
import ai.saniou.thread.db.table.forum.Comment
import app.cash.paging.PagingSource
import kotlinx.coroutines.flow.Flow

/**
 * 统一的数据缓存接口，屏蔽底层数据库操作
 */
interface SourceCache {
    /**
     * 观察指定帖子的详情
     */
    fun observeTopic(sourceId: String, topicId: String): Flow<Topic?>

    /**
     * 观察指定帖子的回复列表（分页）
     */
    fun getTopicCommentsPagingSource(
        sourceId: String,
        topicId: String,
        userHash: String? = null,
    ): PagingSource<Int, Comment>

    /**
     * 观察指定板块的帖子列表（分页）
     */
    fun getChannelTopicPagingSource(
        sourceId: String,
        channelId: String,
    ): PagingSource<Int, GetTopicsInChannelOffset>

    /**
     * 保存帖子详情
     */
    suspend fun saveTopic(topic: Topic)

    /**
     * 批量保存帖子
     */
    suspend fun saveTopics(topics: List<Topic>)

    /**
     * 保存回复列表
     */
    suspend fun saveComments(comments: List<Comment>)

    /**
     * 清除指定板块的缓存
     */
    suspend fun clearChannelCache(sourceId: String, channelId: String)

    /**
     * 清除指定帖子的回复缓存
     */
    suspend fun clearTopicCommentsCache(sourceId: String, topicId: String)

    /**
     * 更新帖子最后访问时间
     */
    suspend fun updateTopicLastAccessTime(sourceId: String, topicId: String, time: Long)

    /**
     * 更新帖子最后阅读的回复ID
     */
    suspend fun updateTopicLastReadCommentId(sourceId: String, topicId: String, commentId: String)

    /**
     * 获取指定来源的所有板块
     */
    suspend fun getChannels(sourceId: String): List<Channel>

    /**
     * 观察指定来源的所有板块
     */
    fun observeChannels(sourceId: String): Flow<List<Channel>>

    /**
     * 批量保存板块
     */
    suspend fun saveChannels(forums: List<Channel>)
}
