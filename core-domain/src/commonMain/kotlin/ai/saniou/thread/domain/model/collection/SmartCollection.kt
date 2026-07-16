package ai.saniou.thread.domain.model.collection

import ai.saniou.thread.domain.model.content.ContentReferenceKind
import kotlinx.serialization.Serializable

@Serializable
data class SmartCollection(
    val id: String,
    val name: String,
    val description: String = "",
    val rules: SmartCollectionRules = SmartCollectionRules(),
    val pinned: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
        require(rules.isMeaningful) { "A smart collection needs at least one rule" }
    }
}

@Serializable
data class SmartCollectionRules(
    val contentKinds: Set<ContentReferenceKind> = emptySet(),
    val sourceIds: Set<String> = emptySet(),
    val query: String = "",
    val author: String = "",
    val tags: Set<String> = emptySet(),
    val unreadOnly: Boolean = false,
    val bookmarkedOnly: Boolean = false,
    val hasMedia: Boolean? = null,
    val includeContentWarnings: Boolean = true,
) {
    val isMeaningful: Boolean
        get() = contentKinds.isNotEmpty() || sourceIds.isNotEmpty() || query.isNotBlank() ||
            author.isNotBlank() || tags.isNotEmpty() || unreadOnly || bookmarkedOnly ||
            hasMedia != null || !includeContentWarnings
}
