package ai.saniou.thread.domain.model.activity

import ai.saniou.thread.domain.model.forum.PostDraftKey
import ai.saniou.thread.domain.model.forum.SavedPostDraft
import ai.saniou.thread.domain.model.operations.OperationsSnapshot
import ai.saniou.thread.domain.model.workspace.WorkspaceDestination

enum class ActivityKind {
    AUTHENTICATION,
    REFRESH,
    DRAFT,
    TRANSFER,
    SOURCE,
}

enum class ActivityState {
    RUNNING,
    ACTION_REQUIRED,
    FAILED,
    READY,
    COMPLETED,
}

data class ActivityDeepLink(
    val destination: WorkspaceDestination,
    val sourceId: String? = null,
    val contentId: String? = null,
    val draftKey: PostDraftKey? = null,
)

data class ActivityItem(
    val id: String,
    val kind: ActivityKind,
    val state: ActivityState,
    val title: String,
    val summary: String,
    val occurredAtEpochMillis: Long,
    val sourceId: String? = null,
    val deepLink: ActivityDeepLink? = null,
    val primaryAction: ProductActionRequest? = null,
    val secondaryAction: ProductActionRequest? = null,
)

data class ActivityCenterSnapshot(
    val operations: OperationsSnapshot = OperationsSnapshot(),
    val identities: List<SourceIdentityStatus> = emptyList(),
    val drafts: List<SavedPostDraft> = emptyList(),
    val actionRecords: List<ProductActionRecord> = emptyList(),
    val items: List<ActivityItem> = emptyList(),
) {
    val runningCount: Int get() = items.count { it.state == ActivityState.RUNNING }
    val actionRequiredCount: Int get() = items.count { it.state == ActivityState.ACTION_REQUIRED }
    val failedCount: Int get() = items.count { it.state == ActivityState.FAILED }
    val draftCount: Int get() = drafts.size
}
