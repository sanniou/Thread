package ai.saniou.nmb.workflow.forum

import ai.saniou.nmb.data.entity.Forum
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 定义 Forum 功能模块的 UI 状态和事件
 */
interface ForumContract {
    /**
     * UI 状态，承载页面的所有数据和状态
     *
     * @property isLoading 是否正在加载
     * @property error 错误信息
     * @property forumName 板块名称
     * @property threads 帖子列表的分页数据流
     */
    data class State(
        val isLoading: Boolean = true,
        val error: String? = null,
        val forumName: String = "",
        val threads: Flow<PagingData<Forum>> = emptyFlow()
    )

    /**
     * UI 事件，代表所有用户交互或生命周期事件
     */
    sealed interface Event {
        /**
         * 加载指定的板块
         * @param fid 板块 ID
         * @param fgroup 板块分组 ID
         */
        data class LoadForum(val fid: Long, val fgroup: Long) : Event

        /**
         * 刷新当前板块
         */
        object Refresh : Event
    }
}