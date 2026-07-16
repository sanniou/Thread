package ai.saniou.thread.domain.model.operations

enum class ProductCommandAction {
    REFRESH_SOURCE,
    SET_SOURCE_ENABLED,
    OPEN_SOURCE_LOGIN,
    REFRESH_ALL_READERS,
    EXPORT_DIAGNOSTIC,
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
)
