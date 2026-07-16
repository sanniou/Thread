package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.activity.ProductActionConflictException
import ai.saniou.thread.domain.model.activity.ProductActionRecord
import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.model.activity.ProductActionResult
import ai.saniou.thread.domain.model.activity.ProductActionStatus
import ai.saniou.thread.domain.model.activity.ProductActionType
import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.refresh.DiagnosticSanitizer
import ai.saniou.thread.domain.repository.ChannelRepository
import ai.saniou.thread.domain.repository.OperationsRepository
import ai.saniou.thread.domain.repository.PostDraftRepository
import ai.saniou.thread.domain.repository.ProductActionExecutor
import ai.saniou.thread.domain.repository.ProductActionHistoryRepository
import ai.saniou.thread.domain.repository.ReaderRepository
import ai.saniou.thread.domain.repository.SyncRepository
import ai.saniou.thread.domain.source.SourceCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

class DefaultProductActionExecutor(
    private val channelRepository: ChannelRepository,
    private val readerRepository: ReaderRepository,
    private val sourceCatalog: SourceCatalog,
    private val operationsRepository: OperationsRepository,
    private val syncRepository: SyncRepository,
    private val postDraftRepository: PostDraftRepository,
    private val historyRepository: ProductActionHistoryRepository,
) : ProductActionExecutor {
    private val gate = Mutex()
    private val mutableRunningConflictKeys = MutableStateFlow<Set<String>>(emptySet())
    override val runningConflictKeys: StateFlow<Set<String>> = mutableRunningConflictKeys.asStateFlow()

    override suspend fun execute(request: ProductActionRequest): Result<ProductActionResult> {
        val conflictKey = runCatching(request::conflictKey).getOrElse { return Result.failure(it) }
        val acquired = gate.withLock {
            if (conflictKey in mutableRunningConflictKeys.value) false else {
                mutableRunningConflictKeys.value += conflictKey
                true
            }
        }
        if (!acquired) return Result.failure(ProductActionConflictException(conflictKey))

        val startedAt = Clock.System.now().toEpochMilliseconds()
        val id = "$startedAt:${request.type.name.lowercase()}:$conflictKey"
        val label = request.label()
        return try {
            historyRepository.upsert(
                ProductActionRecord(
                    id = id,
                    type = request.type,
                    conflictKey = conflictKey,
                    label = label,
                    status = ProductActionStatus.RUNNING,
                    startedAtEpochMillis = startedAt,
                    sourceId = request.sourceId,
                )
            )

            val result = runCatching { perform(request) }
            val finishedAt = Clock.System.now().toEpochMilliseconds()
            result.fold(
                onSuccess = { value ->
                    historyRepository.upsert(
                        ProductActionRecord(
                            id = id,
                            type = request.type,
                            conflictKey = conflictKey,
                            label = label,
                            status = ProductActionStatus.SUCCEEDED,
                            startedAtEpochMillis = startedAt,
                            finishedAtEpochMillis = finishedAt,
                            sourceId = request.sourceId,
                            message = DiagnosticSanitizer.sanitize(value.message)?.take(500),
                        )
                    )
                },
                onFailure = { error ->
                    historyRepository.upsert(
                        ProductActionRecord(
                            id = id,
                            type = request.type,
                            conflictKey = conflictKey,
                            label = label,
                            status = ProductActionStatus.FAILED,
                            startedAtEpochMillis = startedAt,
                            finishedAtEpochMillis = finishedAt,
                            sourceId = request.sourceId,
                            message = DiagnosticSanitizer.sanitize(error.message ?: label)?.take(500),
                        )
                    )
                },
            )
            result
        } catch (error: Throwable) {
            Result.failure(error)
        } finally {
            gate.withLock { mutableRunningConflictKeys.value -= conflictKey }
        }
    }

    private suspend fun perform(request: ProductActionRequest): ProductActionResult = when (request.type) {
        ProductActionType.REFRESH_SOURCE -> {
            val sourceId = requireNotNull(request.sourceId) { "缺少来源" }
            when (requireNotNull(request.sourceKind) { "缺少来源类型" }) {
                ContentSourceKind.FORUM -> channelRepository.fetchChannels(sourceId, forceRefresh = true).getOrThrow()
                ContentSourceKind.READER -> readerRepository.refreshFeed(sourceId, forceRefresh = true).getOrThrow()
            }
            ProductActionResult("${request.label()}完成", affectedIds = setOf(sourceId))
        }
        ProductActionType.REFRESH_ALL_READERS -> {
            val report = readerRepository.refreshAllFeeds()
            val message = "Reader 刷新完成：${report.refreshedSourceIds.size} 成功" +
                if (report.failures.isEmpty()) "" else "，${report.failures.size} 失败"
            if (report.refreshedSourceIds.isEmpty() && report.failures.isNotEmpty()) {
                error(message)
            }
            ProductActionResult(message, affectedIds = report.refreshedSourceIds)
        }
        ProductActionType.SET_SOURCE_ENABLED -> {
            val sourceId = requireNotNull(request.sourceId) { "缺少来源" }
            val enabled = requireNotNull(request.enabledValue) { "缺少目标状态" }
            sourceCatalog.setEnabled(sourceId, enabled)
            ProductActionResult("已${if (enabled) "启用" else "停用"} $sourceId", affectedIds = setOf(sourceId))
        }
        ProductActionType.CLEAR_SOURCE_DIAGNOSTIC -> {
            val sourceId = requireNotNull(request.sourceId) { "缺少来源" }
            operationsRepository.clearRefreshDiagnostic(sourceId)
            ProductActionResult("已清除 $sourceId 的刷新诊断", affectedIds = setOf(sourceId))
        }
        ProductActionType.EXPORT_DIAGNOSTIC -> {
            val export = operationsRepository.exportDiagnostic()
            ProductActionResult(
                message = "已生成 ${export.sourceCount} 个来源的脱敏诊断",
                output = export.payload,
                outputMediaType = "application/json",
            )
        }
        ProductActionType.EXPORT_READER_SUBSCRIPTIONS -> {
            val format = requireNotNull(request.readerFormat) { "缺少订阅格式" }
            val payload = readerRepository.exportSubscriptions(format).getOrThrow()
            ProductActionResult(
                message = "Reader ${format.name} 订阅已导出",
                output = payload,
                outputMediaType = if (format.name == "OPML") "text/x-opml" else "application/json",
            )
        }
        ProductActionType.IMPORT_READER_SUBSCRIPTIONS -> {
            val format = requireNotNull(request.readerFormat) { "缺少订阅格式" }
            val report = readerRepository.importSubscriptions(requireNotNull(request.payload), format).getOrThrow()
            ProductActionResult(
                message = "Reader 导入完成：${report.added} 新增，${report.updated} 更新，${report.skipped} 跳过",
                affectedIds = report.feedSourceIds,
            )
        }
        ProductActionType.EXPORT_USER_DATA -> {
            val export = syncRepository.exportUserData().getOrThrow()
            ProductActionResult(
                message = "用户数据已导出：${export.summary.sourceCount} 个来源、${export.summary.feedSourceCount} 个订阅",
                output = export.payload,
                outputMediaType = "application/json",
            )
        }
        ProductActionType.IMPORT_USER_DATA -> {
            val report = syncRepository.importUserData(requireNotNull(request.payload)).getOrThrow()
            ProductActionResult(
                message = "用户数据已恢复：${report.summary.sourceCount} 个来源、${report.summary.feedSourceCount} 个订阅",
                affectedIds = report.sourceIds + report.feedSourceIds,
            )
        }
        ProductActionType.BACKUP_TO_WEBDAV -> {
            val export = syncRepository.backupToWebDav().getOrThrow()
            ProductActionResult("WebDAV 备份完成：${export.summary.bookmarkCount} 个收藏")
        }
        ProductActionType.RESTORE_FROM_WEBDAV -> {
            val report = syncRepository.restoreFromWebDav().getOrThrow()
            ProductActionResult("WebDAV 恢复完成：${report.summary.feedSourceCount} 个订阅")
        }
        ProductActionType.DISCARD_DRAFT -> {
            val key = requireNotNull(request.draftKey) { "缺少草稿标识" }
            postDraftRepository.discard(key)
            ProductActionResult("草稿已丢弃", affectedIds = setOf(key.stableKey))
        }
    }

    private fun ProductActionRequest.label(): String = when (type) {
        ProductActionType.REFRESH_SOURCE -> "刷新 ${sourceId.orEmpty()}"
        ProductActionType.REFRESH_ALL_READERS -> "刷新全部 Reader"
        ProductActionType.SET_SOURCE_ENABLED -> "${if (enabledValue == true) "启用" else "停用"} ${sourceId.orEmpty()}"
        ProductActionType.CLEAR_SOURCE_DIAGNOSTIC -> "清除来源诊断"
        ProductActionType.EXPORT_DIAGNOSTIC -> "导出脱敏诊断"
        ProductActionType.EXPORT_READER_SUBSCRIPTIONS -> "导出 Reader ${readerFormat?.name.orEmpty()}"
        ProductActionType.IMPORT_READER_SUBSCRIPTIONS -> "导入 Reader ${readerFormat?.name.orEmpty()}"
        ProductActionType.EXPORT_USER_DATA -> "导出用户数据"
        ProductActionType.IMPORT_USER_DATA -> "导入用户数据"
        ProductActionType.BACKUP_TO_WEBDAV -> "备份到 WebDAV"
        ProductActionType.RESTORE_FROM_WEBDAV -> "从 WebDAV 恢复"
        ProductActionType.DISCARD_DRAFT -> "丢弃草稿"
    }
}
