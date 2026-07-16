package ai.saniou.thread.feature.activity

import ai.saniou.thread.domain.model.activity.ActivityCenterSnapshot
import ai.saniou.thread.domain.model.activity.ActivityItem
import ai.saniou.thread.domain.model.activity.ActivityKind
import ai.saniou.thread.domain.model.activity.ActivityState
import ai.saniou.thread.domain.model.activity.ProductActionRequest

interface ActivityCenterContract {
    data class State(
        val snapshot: ActivityCenterSnapshot = ActivityCenterSnapshot(),
        val filter: Filter = Filter.ATTENTION,
        val query: String = "",
        val runningConflictKeys: Set<String> = emptySet(),
        val pendingDangerAction: ProductActionRequest? = null,
        val outputTitle: String? = null,
        val outputPayload: String? = null,
        val message: String? = null,
    ) {
        val visibleItems: List<ActivityItem> get() {
            val needle = query.trim()
            return snapshot.items.filter { item ->
                filter.accepts(item) && (
                    needle.isEmpty() || item.title.contains(needle, ignoreCase = true) ||
                        item.summary.contains(needle, ignoreCase = true) ||
                        item.sourceId?.contains(needle, ignoreCase = true) == true
                    )
            }
        }
    }

    enum class Filter {
        ATTENTION,
        ALL,
        RUNNING,
        DRAFTS,
        HISTORY;

        fun accepts(item: ActivityItem): Boolean = when (this) {
            ATTENTION -> item.state in setOf(ActivityState.ACTION_REQUIRED, ActivityState.FAILED, ActivityState.READY)
            ALL -> true
            RUNNING -> item.state == ActivityState.RUNNING
            DRAFTS -> item.kind == ActivityKind.DRAFT && item.state != ActivityState.COMPLETED
            HISTORY -> item.state == ActivityState.COMPLETED
        }
    }

    sealed interface Event {
        data class FilterChanged(val value: Filter) : Event
        data class QueryChanged(val value: String) : Event
        data class Execute(val request: ProductActionRequest) : Event
        data object ConfirmDangerAction : Event
        data object DismissDangerAction : Event
        data object ClearCompleted : Event
        data object DismissOutput : Event
        data object MessageShown : Event
    }
}
