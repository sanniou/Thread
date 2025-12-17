package ai.saniou.thread.data.cache

import ai.saniou.thread.db.table.forum.Forum
import ai.saniou.thread.db.table.forum.GetThreadsInForumOffset
import ai.saniou.thread.db.table.forum.Thread
import ai.saniou.thread.db.table.forum.ThreadReply
import app.cash.paging.PagingSource
import kotlinx.coroutines.flow.Flow

/**
 * 统一的数据缓存接口，屏蔽底层数据库操作
 */
interface SourceCache {
    /**
     * 观察指定帖子的详情
     */
    fun observeThread(sourceId: String, threadId: String): Flow<Thread?>

    /**
     * 观察指定帖子的回复列表（分页）
     */
    fun getThreadRepliesPagingSource(
        sourceId: String,
        threadId: String,
        userHash: String? = null
    ): PagingSource<Int, ThreadReply>

    /**
     * 观察指定板块的帖子列表（分页）
     */
    fun getForumThreadsPagingSource(
        sourceId: String,
        fid: String
    ): PagingSource<Int, GetThreadsInForumOffset>

    /**
     * 保存帖子详情
     */
    suspend fun saveThread(thread: Thread)

    /**
     * 批量保存帖子
     */
    suspend fun saveThreads(threads: List<Thread>)

    /**
     * 保存回复列表
     */
    suspend fun saveReplies(replies: List<ThreadReply>)

    /**
     * 清除指定板块的缓存
     */
    suspend fun clearForumCache(sourceId: String, fid: String)

    /**
     * 清除指定帖子的回复缓存
     */
    suspend fun clearThreadRepliesCache(sourceId: String, threadId: String)

    /**
     * 更新帖子最后访问时间
     */
    suspend fun updateThreadLastAccessTime(sourceId: String, threadId: String, time: Long)

    /**
     * 更新帖子最后阅读的回复ID
     */
    suspend fun updateThreadLastReadReplyId(sourceId: String, threadId: String, replyId: Long)

    /**
     * 获取指定来源的所有板块
     */
    suspend fun getForums(sourceId: String): List<Forum>

    /**
     * 批量保存板块
     */
    suspend fun saveForums(forums: List<Forum>)
}
