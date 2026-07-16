package ai.saniou.thread.domain.model.operations

import ai.saniou.thread.domain.model.activity.ProductActionDanger
import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.model.forum.PostDraftKey

enum class ProductCommandAction {
    EXECUTE_PRODUCT_ACTION,
    OPEN_SOURCE_LOGIN,
    OPEN_ACTIVITY_CENTER,
    OPEN_READER_IMPORT,
    OPEN_USER_DATA_IMPORT,
    RESUME_DRAFT,
}

/** UI-agnostic command contract generated from the currently registered connector capabilities. */
data class ProductCommandDescriptor(
    val id: String,
    val label: String,
    val description: String,
    val action: ProductCommandAction,
    val sourceId: String? = null,
    val sourceKind: ContentSourceKind? = null,
    val enabledValue: Boolean? = null,
    val request: ProductActionRequest? = null,
    val draftKey: PostDraftKey? = null,
    val danger: ProductActionDanger = request?.danger ?: ProductActionDanger.NORMAL,
    val enabled: Boolean = true,
)
