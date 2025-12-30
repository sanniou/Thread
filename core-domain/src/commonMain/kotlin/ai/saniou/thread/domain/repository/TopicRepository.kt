package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.Comment
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * 帖子相关的仓库接口，定义了帖子数据的契约
 */
interface TopicRepository {

    /**
     * 获取帖子详情
     *
     * @param sourceId 来源ID
     * @param id 帖子ID
     * @param forceRefresh 是否强制从网络刷新
     * @return 包含帖子详情的 Flow
     */
    fun getTopicDetail(sourceId: String, id: String, forceRefresh: Boolean = false): Flow<Topic>

    /**
     * 获取帖子回复的分页数据
     *
     * @param sourceId 来源ID
     * @param threadId 帖子ID
     * @param isPoOnly 是否只看PO主
     * @param initialPage 初始页码
     * @return 包含帖子回复分页数据的 Flow
     */
    fun getTopicCommentsPaging(
        sourceId: String,
        threadId: String,
        isPoOnly: Boolean,
        initialPage: Int = 1
    ): Flow<PagingData<Comment>>

    /**
     * 获取帖子中的所有图片
     *
     * @param threadId 帖子ID
     * @return 包含图片列表的 Flow
     */
    fun getTopicImages(threadId: Long): Flow<List<Image>>

    /**
     * 获取帖子的所有回复（非分页）
     *
     * @param threadId 帖子ID
     * @param isPoOnly 是否只看PO主
     * @return 包含帖子回复列表的 Flow
     */
    fun getTopicComments(threadId: Long, isPoOnly: Boolean): Flow<List<Comment>>

    /**
     * 更新帖子最后已读的回复ID
     *
     * @param threadId 帖子ID
     * @param replyId 回复ID
     */
    suspend fun updateTopicLastReadCommentId(threadId: String, replyId: String)
}
