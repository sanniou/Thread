package ai.saniou.nmb.workflow.forum

import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.model.ForumDetail
import ai.saniou.thread.domain.model.Post
import app.cash.paging.PagingData
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
        val forumName: String = "",
        val forumDetail: Forum? = null,
        val threads: Flow<PagingData<Post>> = emptyFlow(),
        val showInfoDialog: Boolean = false
    )

    /**
     * UI 事件，代表所有用户交互或生命周期事件
     */
    sealed interface Event {
        /**
         * 刷新当前板块
         */
        object Refresh : Event

        /**
         * 滚动到顶部
         */
        object ScrollToTop : Event

        /**
         * 跳转到指定页码
         */
        data class JumpToPage(val page: Int) : Event

        /**
         * 切换板块信息弹窗的显示状态
         */
        data class ToggleInfoDialog(val show: Boolean) : Event
    }

    /**
     * UI 副作用，用于处理一次性事件，如导航、Toast 等
     */
    sealed interface Effect {
        /**
         * 滚动到顶部
         */
        object ScrollToTop : Effect
    }
}
