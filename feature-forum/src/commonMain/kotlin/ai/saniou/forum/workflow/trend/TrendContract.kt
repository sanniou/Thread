package ai.saniou.forum.workflow.trend

import ai.saniou.coreui.state.AppError
import ai.saniou.thread.data.source.nmb.remote.dto.Thread
import ai.saniou.thread.domain.model.forum.Trend
import kotlinx.serialization.Serializable

/**
 * 趋势页面的 UI 状态和事件定义
 */
interface TrendContract {

    /**
     * 解析后的趋势条目
     */
    @Serializable
    data class TrendItem(
        val rank: String,      // 排名，如 "01"
        val trendNum: String,  // 趋势值，如 "Trend 34"
        val forum: String,     // 版块，如 "[综合版1]"
        val isNew: Boolean,    // 是否是 New
        val threadId: Long,    // 串 ID
        val content: String    // 内容预览
    )

    /**
     * UI 状态
     * @property isLoading 是否正在加载
     * @property error 错误信息
     * @property trendDate 趋势数据的日期，如 "2025-12-01(一)01:04:01"
     * @property items 解析后的趋势列表
     */
    data class State(
        val isLoading: Boolean = false,
        val error: AppError? = null,
        val trendDate: String = "",
        val items: List<TrendItem> = emptyList(),
        val rawThread: Thread? = null // 保留原始数据以备不时之需
    )

    /**
     * UI 事件
     */
    sealed interface Event {
        /**
         * 刷新数据
         */
        object Refresh : Event

        /**
         * 点击趋势条目
         */
        data class OnTrendItemClick(val threadId: Long) : Event

        /**
         * 点击信息按钮（显示源地址）
         */
        object OnInfoClick : Event
    }

    /**
     * 副作用
     */
    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
        data class NavigateToThread(val threadId: Long) : Effect
        data class ShowInfoDialog(val url: String) : Effect
    }
}

fun Trend.toUI() = TrendContract.TrendItem(
    rank = rank,
    trendNum = trendNum,
    forum = forum,
    isNew = isNew,
    threadId = threadId,
    content = contentPreview
)
