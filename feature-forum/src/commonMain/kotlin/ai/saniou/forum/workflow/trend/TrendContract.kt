package ai.saniou.forum.workflow.trend

import ai.saniou.coreui.state.AppError
import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.model.TrendTab
import ai.saniou.thread.domain.source.TrendSource

/**
 * 趋势页面的 UI 状态和事件定义
 */
interface TrendContract {

    /**
     * UI 状态
     */
    data class State(
        val isLoading: Boolean = false,
        val error: AppError? = null,
        val selectedSource: TrendSource? = null,
        val selectedTab: TrendTab? = null,
        val trendParams: TrendParams = TrendParams(),
        val availableSources: List<TrendSource> = emptyList(),
        val availableTabs: List<TrendTab> = emptyList()
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
        data class OnTrendItemClick(val item: TrendItem) : Event

        /**
         * 选择数据源
         */
        data class SelectSource(val sourceId: String) : Event

        /**
         * 选择 Tab
         */
        data class SelectTab(val tabId: String) : Event

        /**
         * 选择日期 (用于历史回溯)
         */
        data class SelectDate(val dayOffset: Int) : Event
    }

    /**
     * 副作用
     */
    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
        data class NavigateToThread(val topicId: String, val sourceId: String) : Effect
    }
}