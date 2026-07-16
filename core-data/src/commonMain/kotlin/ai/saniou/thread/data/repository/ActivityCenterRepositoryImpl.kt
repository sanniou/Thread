package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.activity.ActivityCenterSnapshot
import ai.saniou.thread.domain.model.activity.ActivityDeepLink
import ai.saniou.thread.domain.model.activity.ActivityItem
import ai.saniou.thread.domain.model.activity.ActivityKind
import ai.saniou.thread.domain.model.activity.ActivityState
import ai.saniou.thread.domain.model.activity.IdentityValidity
import ai.saniou.thread.domain.model.activity.ProductActionRecord
import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.model.activity.ProductActionStatus
import ai.saniou.thread.domain.model.activity.ProductActionType
import ai.saniou.thread.domain.model.activity.SourceIdentityStatus
import ai.saniou.thread.domain.model.forum.PostDraftTargetKind
import ai.saniou.thread.domain.model.forum.SavedPostDraft
import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.model.operations.SourceHealth
import ai.saniou.thread.domain.model.operations.SourceOperationalState
import ai.saniou.thread.domain.model.workspace.WorkspaceDestination
import ai.saniou.thread.domain.repository.ActivityCenterRepository
import ai.saniou.thread.domain.repository.IdentityRepository
import ai.saniou.thread.domain.repository.OperationsRepository
import ai.saniou.thread.domain.repository.PostDraftRepository
import ai.saniou.thread.domain.repository.ProductActionExecutor
import ai.saniou.thread.domain.repository.ProductActionHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock

class ActivityCenterRepositoryImpl(
    private val operationsRepository: OperationsRepository,
    private val identityRepository: IdentityRepository,
    private val postDraftRepository: PostDraftRepository,
    private val actionHistoryRepository: ProductActionHistoryRepository,
    private val actionExecutor: ProductActionExecutor,
) : ActivityCenterRepository {
    override fun observe(): Flow<ActivityCenterSnapshot> = combine(
        operationsRepository.observe(),
        identityRepository.observe(),
        postDraftRepository.observeAll(),
        actionHistoryRepository.observe(),
        actionExecutor.runningConflictKeys,
    ) { operations, identities, drafts, records, runningKeys ->
        val now = Clock.System.now().toEpochMilliseconds()
        val normalizedRecords = records.map { record ->
            if (record.status == ProductActionStatus.RUNNING && record.conflictKey !in runningKeys) {
                record.copy(
                    status = ProductActionStatus.FAILED,
                    finishedAtEpochMillis = record.finishedAtEpochMillis ?: now,
                    message = "上次运行在完成前中断，可安全重试",
                )
            } else record
        }
        val items = buildList {
            identities.mapNotNullTo(this) { it.toActivity(now) }
            operations.sources.mapNotNullTo(this) { it.toActivity(now) }
            val availableSourceIds = operations.sources.mapTo(mutableSetOf(), SourceHealth::id)
            drafts.mapTo(this) { it.toActivity(availableSourceIds) }
            normalizedRecords.take(MAX_ACTION_ITEMS).mapTo(this) { it.toActivity() }
        }.distinctBy(ActivityItem::id).sortedWith(
            compareBy<ActivityItem> { it.state.rank }
                .thenByDescending(ActivityItem::occurredAtEpochMillis)
        )
        ActivityCenterSnapshot(
            operations = operations,
            identities = identities,
            drafts = drafts,
            actionRecords = normalizedRecords,
            items = items,
        )
    }

    override suspend fun clearCompletedActions() = actionHistoryRepository.clearCompleted()

    private fun SourceIdentityStatus.toActivity(now: Long): ActivityItem? {
        if (validity !in setOf(IdentityValidity.EXPIRED, IdentityValidity.ANONYMOUS)) return null
        if (!supportsLogin) return null
        val expired = validity == IdentityValidity.EXPIRED
        return ActivityItem(
            id = "identity:$sourceId",
            kind = ActivityKind.AUTHENTICATION,
            state = if (expired) ActivityState.ACTION_REQUIRED else ActivityState.READY,
            title = if (expired) "$sourceName 需要重新认证" else "$sourceName 尚未登录",
            summary = message ?: if (expired) "凭证已失效，刷新和发布动作将暂停" else "可匿名浏览，登录后解锁账号能力",
            occurredAtEpochMillis = lastValidatedAtEpochMillis ?: now,
            sourceId = sourceId,
            deepLink = ActivityDeepLink(WorkspaceDestination.FORUM, sourceId = sourceId),
        )
    }

    private fun SourceHealth.toActivity(now: Long): ActivityItem? {
        if (state == SourceOperationalState.AUTHENTICATION_REQUIRED) return null
        val activityState = when (state) {
            SourceOperationalState.REFRESHING -> ActivityState.RUNNING
            SourceOperationalState.OFFLINE,
            SourceOperationalState.RATE_LIMITED,
            SourceOperationalState.DEGRADED -> ActivityState.FAILED
            SourceOperationalState.DISABLED -> ActivityState.READY
            SourceOperationalState.READY,
            SourceOperationalState.AUTHENTICATION_REQUIRED -> return null
        }
        val request = when (state) {
            SourceOperationalState.DISABLED -> ProductActionRequest(
                ProductActionType.SET_SOURCE_ENABLED,
                sourceId = id,
                sourceKind = kind,
                enabledValue = true,
            )
            SourceOperationalState.REFRESHING -> null
            else -> ProductActionRequest(ProductActionType.REFRESH_SOURCE, sourceId = id, sourceKind = kind)
        }
        return ActivityItem(
            id = "source:${kind.name.lowercase()}:$id",
            kind = if (state == SourceOperationalState.DISABLED) ActivityKind.SOURCE else ActivityKind.REFRESH,
            state = activityState,
            title = when (state) {
                SourceOperationalState.REFRESHING -> "$name 正在刷新"
                SourceOperationalState.DISABLED -> "$name 已停用"
                SourceOperationalState.OFFLINE -> "$name 等待网络"
                SourceOperationalState.RATE_LIMITED -> "$name 暂时受限"
                else -> "$name 刷新失败"
            },
            summary = message ?: "缓存 ${primaryItemCount} 条，可在恢复后重试",
            occurredAtEpochMillis = lastRefreshAtEpochMillis ?: lastContentAtEpochMillis ?: now,
            sourceId = id,
            deepLink = ActivityDeepLink(WorkspaceDestination.OPERATIONS, sourceId = id),
            primaryAction = request,
            secondaryAction = message?.let {
                ProductActionRequest(ProductActionType.CLEAR_SOURCE_DIAGNOSTIC, sourceId = id, sourceKind = kind)
            },
        )
    }

    private fun SavedPostDraft.toActivity(availableSourceIds: Set<String>): ActivityItem {
        val target = if (key.targetKind == PostDraftTargetKind.TOPIC) "回复" else "新主题"
        val preview = draft.title?.takeIf(String::isNotBlank) ?: draft.content.trim().replace('\n', ' ').take(90)
        val sourceAvailable = key.sourceId in availableSourceIds
        return ActivityItem(
            id = "draft:${key.stableKey}",
            kind = ActivityKind.DRAFT,
            state = if (sourceAvailable) ActivityState.READY else ActivityState.ACTION_REQUIRED,
            title = if (sourceAvailable) "继续$target" else "草稿来源已不可用",
            summary = if (sourceAvailable) {
                preview.ifBlank { "包含附件或发布选项的未完成草稿" }
            } else {
                "${key.sourceId} 已移除或未安装；草稿仍安全保留，可选择丢弃"
            },
            occurredAtEpochMillis = updatedAtEpochMillis,
            sourceId = key.sourceId,
            deepLink = ActivityDeepLink(
                destination = WorkspaceDestination.FORUM,
                sourceId = key.sourceId,
                contentId = key.targetId,
                draftKey = key,
            ).takeIf { sourceAvailable },
            secondaryAction = ProductActionRequest(ProductActionType.DISCARD_DRAFT, draftKey = key),
        )
    }

    private fun ProductActionRecord.toActivity(): ActivityItem {
        val activityState = when (status) {
            ProductActionStatus.RUNNING -> ActivityState.RUNNING
            ProductActionStatus.SUCCEEDED -> ActivityState.COMPLETED
            ProductActionStatus.FAILED -> ActivityState.FAILED
        }
        val destination = when (type) {
            ProductActionType.EXPORT_READER_SUBSCRIPTIONS,
            ProductActionType.IMPORT_READER_SUBSCRIPTIONS,
            ProductActionType.EXPORT_USER_DATA,
            ProductActionType.IMPORT_USER_DATA,
            ProductActionType.BACKUP_TO_WEBDAV,
            ProductActionType.RESTORE_FROM_WEBDAV -> WorkspaceDestination.SETTINGS
            else -> WorkspaceDestination.OPERATIONS
        }
        return ActivityItem(
            id = "action:$id",
            kind = type.activityKind,
            state = activityState,
            title = label,
            summary = message ?: if (status == ProductActionStatus.RUNNING) "动作执行中" else "动作已完成",
            occurredAtEpochMillis = finishedAtEpochMillis ?: startedAtEpochMillis,
            sourceId = sourceId,
            deepLink = ActivityDeepLink(destination, sourceId = sourceId),
        )
    }

    private val ProductActionType.activityKind: ActivityKind get() = when (this) {
        ProductActionType.REFRESH_SOURCE,
        ProductActionType.REFRESH_ALL_READERS,
        ProductActionType.CLEAR_SOURCE_DIAGNOSTIC,
        ProductActionType.EXPORT_DIAGNOSTIC -> ActivityKind.REFRESH
        ProductActionType.SET_SOURCE_ENABLED -> ActivityKind.SOURCE
        ProductActionType.DISCARD_DRAFT -> ActivityKind.DRAFT
        else -> ActivityKind.TRANSFER
    }

    private val ActivityState.rank: Int get() = when (this) {
        ActivityState.RUNNING -> 0
        ActivityState.ACTION_REQUIRED -> 1
        ActivityState.FAILED -> 2
        ActivityState.READY -> 3
        ActivityState.COMPLETED -> 4
    }

    private companion object {
        const val MAX_ACTION_ITEMS = 40
    }
}
