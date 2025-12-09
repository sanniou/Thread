package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.model.ForumDetail
import ai.saniou.thread.domain.model.Post
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * 板块相关的仓库接口，定义了板块数据的契约
 */
interface ForumRepository {

    /**
     * 获取板块下的帖子分页数据
     *
     * @param fid 板块ID
     * @param isTimeline 是否为时间线模式
     * @param initialPage 初始页码
     * @return 包含帖子分页数据的 Flow
     */
    fun getForumThreadsPaging(
        fid: Long,
        isTimeline: Boolean,
        initialPage: Int = 1
    ): Flow<PagingData<Post>>

    /**
     * 获取板块名称
     *
     * @param fid 板块ID
     * @return 包含板块名称的 Flow
     */
    fun getForumName(fid: Long): Flow<String>

    /**
     * 获取板块详情
     *
     * @param fid 板块ID
     * @return 包含板块详情的 Flow
     */
    fun getForumDetail(fid: Long): Flow<Forum?>
}
