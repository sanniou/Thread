package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.model.forum.ThreadReply
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * 帖子相关的仓库接口，定义了帖子数据的契约
 */
interface ThreadRepository {

    /**
     * 获取帖子详情
     *
     * @param id 帖子ID
     * @param forceRefresh 是否强制从网络刷新
     * @return 包含帖子详情的 Flow
     */
    fun getThreadDetail(id: Long, forceRefresh: Boolean = false): Flow<Post>

    /**
     * 获取帖子回复的分页数据
     *
     * @param threadId 帖子ID
     * @param isPoOnly 是否只看PO主
     * @param initialPage 初始页码
     * @return 包含帖子回复分页数据的 Flow
     */
    fun getThreadRepliesPaging(
        threadId: Long,
        isPoOnly: Boolean,
        initialPage: Int = 1
    ): Flow<PagingData<ThreadReply>>

    /**
     * 获取帖子中的所有图片
     *
     * @param threadId 帖子ID
     * @return 包含图片列表的 Flow
     */
    fun getThreadImages(threadId: Long): Flow<List<Image>>

    /**
     * 获取帖子的所有回复（非分页）
     *
     * @param threadId 帖子ID
     * @param isPoOnly 是否只看PO主
     * @return 包含帖子回复列表的 Flow
     */
    fun getThreadReplies(threadId: Long, isPoOnly: Boolean): Flow<List<ThreadReply>>

    /**
     * 更新帖子的最后访问时间
     *
     * @param threadId 帖子ID
     * @param time 时间戳
     */
    suspend fun updateThreadLastAccessTime(threadId: Long, time: Long)

    /**
     * 更新帖子最后已读的回复ID
     *
     * @param threadId 帖子ID
     * @param replyId 回复ID
     */
    suspend fun updateThreadLastReadReplyId(threadId: Long, replyId: Long)
}
