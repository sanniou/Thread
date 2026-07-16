package ai.saniou.thread.domain.model.activity

import ai.saniou.thread.domain.model.forum.PostDraftKey
import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.model.reader.ReaderSubscriptionFormat

enum class ProductActionType {
    REFRESH_SOURCE,
    REFRESH_ALL_READERS,
    SET_SOURCE_ENABLED,
    CLEAR_SOURCE_DIAGNOSTIC,
    EXPORT_DIAGNOSTIC,
    EXPORT_READER_SUBSCRIPTIONS,
    IMPORT_READER_SUBSCRIPTIONS,
    EXPORT_USER_DATA,
    IMPORT_USER_DATA,
    BACKUP_TO_WEBDAV,
    RESTORE_FROM_WEBDAV,
    DISCARD_DRAFT,
}

enum class ProductActionDanger {
    NORMAL,
    CAUTION,
    DESTRUCTIVE,
}

enum class ProductActionStatus {
    RUNNING,
    SUCCEEDED,
    FAILED,
}

data class ProductActionRequest(
    val type: ProductActionType,
    val sourceId: String? = null,
    val sourceKind: ContentSourceKind? = null,
    val enabledValue: Boolean? = null,
    val readerFormat: ReaderSubscriptionFormat? = null,
    val payload: String? = null,
    val draftKey: PostDraftKey? = null,
) {
    val conflictKey: String get() = when (type) {
        ProductActionType.REFRESH_SOURCE,
        ProductActionType.SET_SOURCE_ENABLED,
        ProductActionType.CLEAR_SOURCE_DIAGNOSTIC -> "source:${requireNotNull(sourceId)}"
        ProductActionType.REFRESH_ALL_READERS,
        ProductActionType.EXPORT_READER_SUBSCRIPTIONS,
        ProductActionType.IMPORT_READER_SUBSCRIPTIONS -> "reader-library"
        ProductActionType.EXPORT_USER_DATA,
        ProductActionType.IMPORT_USER_DATA,
        ProductActionType.BACKUP_TO_WEBDAV,
        ProductActionType.RESTORE_FROM_WEBDAV -> "user-data"
        ProductActionType.EXPORT_DIAGNOSTIC -> "diagnostic-export"
        ProductActionType.DISCARD_DRAFT -> "draft:${requireNotNull(draftKey).stableKey}"
    }

    val danger: ProductActionDanger get() = when (type) {
        ProductActionType.SET_SOURCE_ENABLED -> if (enabledValue == false) ProductActionDanger.CAUTION else ProductActionDanger.NORMAL
        ProductActionType.IMPORT_READER_SUBSCRIPTIONS,
        ProductActionType.IMPORT_USER_DATA,
        ProductActionType.RESTORE_FROM_WEBDAV -> ProductActionDanger.CAUTION
        ProductActionType.DISCARD_DRAFT -> ProductActionDanger.DESTRUCTIVE
        else -> ProductActionDanger.NORMAL
    }
}

data class ProductActionResult(
    val message: String,
    val output: String? = null,
    val outputMediaType: String? = null,
    val affectedIds: Set<String> = emptySet(),
)

data class ProductActionRecord(
    val id: String,
    val type: ProductActionType,
    val conflictKey: String,
    val label: String,
    val status: ProductActionStatus,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long? = null,
    val sourceId: String? = null,
    val message: String? = null,
)

class ProductActionConflictException(val conflictingKey: String) :
    IllegalStateException("动作正在执行：$conflictingKey")
