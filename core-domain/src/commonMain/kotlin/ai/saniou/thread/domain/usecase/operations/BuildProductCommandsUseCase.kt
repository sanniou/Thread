package ai.saniou.thread.domain.usecase.operations

import ai.saniou.thread.domain.model.activity.ActivityCenterSnapshot
import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.model.activity.ProductActionStatus
import ai.saniou.thread.domain.model.activity.ProductActionType
import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.model.operations.ProductCommandAction
import ai.saniou.thread.domain.model.operations.ProductCommandDescriptor
import ai.saniou.thread.domain.model.operations.SourceOperationalState
import ai.saniou.thread.domain.model.reader.ReaderSubscriptionFormat

class BuildProductCommandsUseCase {
    operator fun invoke(snapshot: ActivityCenterSnapshot): List<ProductCommandDescriptor> = buildList {
        val operations = snapshot.operations
        val runningKeys = snapshot.actionRecords
            .filter { it.status == ProductActionStatus.RUNNING }
            .mapTo(mutableSetOf()) { it.conflictKey }

        add(
            ProductCommandDescriptor(
                id = "activity:open",
                label = "打开活动中心",
                description = "查看刷新、身份、草稿和数据转移队列",
                action = ProductCommandAction.OPEN_ACTIVITY_CENTER,
            )
        )
        if (operations.sources.any { it.kind == ContentSourceKind.READER && it.enabled }) {
            addAction(
                id = "refresh:all-readers",
                label = "刷新全部 Reader 来源",
                description = "按照各订阅刷新策略同步文章缓存",
                request = ProductActionRequest(ProductActionType.REFRESH_ALL_READERS),
                runningKeys = runningKeys,
            )
        }
        addAction(
            id = "operations:export-diagnostic",
            label = "复制脱敏运行诊断",
            description = "导出连接器状态，不包含账号、正文或本地路径",
            request = ProductActionRequest(ProductActionType.EXPORT_DIAGNOSTIC),
            runningKeys = runningKeys,
        )
        ReaderSubscriptionFormat.entries.forEach { format ->
            addAction(
                id = "reader:export:${format.name.lowercase()}",
                label = "导出 Reader ${format.name}",
                description = "生成可跨平台迁移的订阅清单",
                request = ProductActionRequest(ProductActionType.EXPORT_READER_SUBSCRIPTIONS, readerFormat = format),
                runningKeys = runningKeys,
            )
        }
        ReaderSubscriptionFormat.entries.forEach { format ->
            add(
                ProductCommandDescriptor(
                    id = "reader:import:${format.name.lowercase()}",
                    label = "导入 Reader ${format.name}",
                    description = "打开 ${format.name} 订阅导入工作流",
                    action = ProductCommandAction.OPEN_READER_IMPORT,
                    request = ProductActionRequest(ProductActionType.IMPORT_READER_SUBSCRIPTIONS, readerFormat = format),
                )
            )
        }
        addAction(
            id = "user-data:export",
            label = "导出用户数据快照",
            description = "备份来源、订阅、收藏和阅读状态",
            request = ProductActionRequest(ProductActionType.EXPORT_USER_DATA),
            runningKeys = runningKeys,
        )
        add(
            ProductCommandDescriptor(
                id = "user-data:import",
                label = "导入用户数据快照",
                description = "打开版本化恢复工作流",
                action = ProductCommandAction.OPEN_USER_DATA_IMPORT,
                request = ProductActionRequest(ProductActionType.IMPORT_USER_DATA),
            )
        )
        addAction(
            id = "webdav:backup",
            label = "立即备份到 WebDAV",
            description = "使用当前数据与同步配置创建远端快照",
            request = ProductActionRequest(ProductActionType.BACKUP_TO_WEBDAV),
            runningKeys = runningKeys,
        )
        addAction(
            id = "webdav:restore",
            label = "从 WebDAV 恢复",
            description = "合并远端来源、订阅、收藏和阅读状态",
            request = ProductActionRequest(ProductActionType.RESTORE_FROM_WEBDAV),
            runningKeys = runningKeys,
        )

        operations.sources.forEach { source ->
            if (source.enabled) {
                addAction(
                    id = "refresh:${source.kind.name.lowercase()}:${source.id}",
                    label = "刷新 ${source.name}",
                    description = "${source.kind.name} · ${source.state.name.lowercase().replace('_', ' ')}",
                    request = ProductActionRequest(
                        ProductActionType.REFRESH_SOURCE,
                        sourceId = source.id,
                        sourceKind = source.kind,
                    ),
                    sourceId = source.id,
                    sourceKind = source.kind,
                    runningKeys = runningKeys,
                )
            }
            if (source.kind == ContentSourceKind.FORUM) {
                val request = ProductActionRequest(
                    ProductActionType.SET_SOURCE_ENABLED,
                    sourceId = source.id,
                    sourceKind = source.kind,
                    enabledValue = !source.enabled,
                )
                addAction(
                    id = "source:${if (source.enabled) "disable" else "enable"}:${source.id}",
                    label = "${if (source.enabled) "停用" else "启用"} ${source.name}",
                    description = "更改连接器在全局导航和聚合流中的可用状态",
                    request = request,
                    sourceId = source.id,
                    sourceKind = source.kind,
                    enabledValue = !source.enabled,
                    runningKeys = runningKeys,
                )
            }
        }
        snapshot.identities.filter { it.isActionable }.forEach { identity ->
            add(
                ProductCommandDescriptor(
                    id = "source:login:${identity.sourceId}",
                    label = if (identity.requiresAuthentication) "重新登录 ${identity.sourceName}" else "登录 ${identity.sourceName}",
                    description = "${identity.loginKind.name.lowercase()} · 打开身份与凭据界面",
                    action = ProductCommandAction.OPEN_SOURCE_LOGIN,
                    sourceId = identity.sourceId,
                    sourceKind = ContentSourceKind.FORUM,
                )
            )
        }
        snapshot.drafts.forEach { draft ->
            add(
                ProductCommandDescriptor(
                    id = "draft:resume:${draft.key.stableKey}",
                    label = "继续草稿 · ${draft.key.targetId}",
                    description = "${draft.key.sourceId} · ${draft.draft.content.take(60).replace('\n', ' ')}",
                    action = ProductCommandAction.RESUME_DRAFT,
                    sourceId = draft.key.sourceId,
                    sourceKind = ContentSourceKind.FORUM,
                    draftKey = draft.key,
                )
            )
            addAction(
                id = "draft:discard:${draft.key.stableKey}",
                label = "丢弃草稿 · ${draft.key.targetId}",
                description = "永久删除此设备上的未发布草稿",
                request = ProductActionRequest(ProductActionType.DISCARD_DRAFT, draftKey = draft.key),
                sourceId = draft.key.sourceId,
                sourceKind = ContentSourceKind.FORUM,
                runningKeys = runningKeys,
            )
        }
    }

    private fun MutableList<ProductCommandDescriptor>.addAction(
        id: String,
        label: String,
        description: String,
        request: ProductActionRequest,
        runningKeys: Set<String>,
        sourceId: String? = request.sourceId,
        sourceKind: ContentSourceKind? = request.sourceKind,
        enabledValue: Boolean? = request.enabledValue,
    ) {
        add(
            ProductCommandDescriptor(
                id = id,
                label = label,
                description = description,
                action = ProductCommandAction.EXECUTE_PRODUCT_ACTION,
                sourceId = sourceId,
                sourceKind = sourceKind,
                enabledValue = enabledValue,
                request = request,
                enabled = request.conflictKey !in runningKeys,
            )
        )
    }
}
