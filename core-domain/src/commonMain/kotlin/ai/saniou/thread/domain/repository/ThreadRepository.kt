package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.Image
import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.model.ThreadReply
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
}
