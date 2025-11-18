package ai.saniou.nmb.workflow.thread

import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.ThreadReply
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 定义 Thread 功能模块的 UI 状态和事件
 */
interface ThreadContract {
    /**
     * UI 状态，承载页面的所有数据和状态
     *
     * @property isLoading 是否正在加载主内容
     * @property error 全局错误信息
     * @property thread 帖子主楼内容
     * @property replies 回复列表的分页数据流
     * @property currentPage 当前页码
     * @property totalPages 总页数
     * @property isPoOnlyMode 是否开启“只看PO”模式
     * @property isSubscribed 是否已订阅
     * @property forumName 板块名称
     * @property snackbarMessage 用于显示 Snackbar 的临时消息
     */
    data class State(
        val isLoading: Boolean = true,
        val error: String? = null,
        val thread: Thread? = null,
        val replies: Flow<PagingData<ThreadReply>> = emptyFlow(),
        val currentPage: Int = 1,
        val totalPages: Int = 1,
        val isPoOnlyMode: Boolean = false,
        val isSubscribed: Boolean = false,
        val forumName: String = "",
        val snackbarMessage: String? = null
    )

    /**
     * UI 事件，代表所有用户交互或生命周期事件
     */
    sealed interface Event {
        /**
         * 加载指定的帖子
         * @param id 帖子 ID
         */
        data class LoadThread(val id: Long) : Event

        /**
         * 刷新当前帖子
         */
        object Refresh : Event

        /**
         * 跳转到指定页码
         * @param page 目标页码
         */
        data class JumpToPage(val page: Int) : Event

        /**
         * 切换“只看PO”模式
         */
        object TogglePoOnlyMode : Event

        /**
         * 切换订阅状态
         */
        object ToggleSubscription : Event

        /**
         * Snackbar 消息已显示，用于清空消息状态
         */
        object SnackbarMessageShown : Event

        /**
         * 更新最后阅读的回复ID
         * @param id 回复 ID
         */
        data class UpdateLastReadReplyId(val id: Long) : Event
    }
}