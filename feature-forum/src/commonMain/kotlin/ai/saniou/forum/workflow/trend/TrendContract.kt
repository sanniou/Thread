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
        val availableTabs: List<TrendTab> = emptyList(),
        /** Topic IDs dismissed via「不感兴趣」within this session (client-side filter). */
        val dismissedTopicIds: Set<String> = emptySet(),
        val notInterestedInFlight: Set<String> = emptySet(),
    )

    /**
     * UI 事件
     */
    sealed interface Event {
        object Refresh : Event
        data class OnTrendItemClick(val item: TrendItem) : Event
        data class SelectSource(val sourceId: String) : Event
        data class SelectTab(val tabId: String) : Event
        data class SelectDate(val dayOffset: Int) : Event
        /** 推荐流「不感兴趣」 */
        data class NotInterested(val item: TrendItem) : Event
    }

    /**
     * 副作用
     */
    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
        data class NavigateToThread(val topicId: String, val sourceId: String) : Effect
    }
}
