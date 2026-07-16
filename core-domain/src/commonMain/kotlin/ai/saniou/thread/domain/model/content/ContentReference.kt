package ai.saniou.thread.domain.model.content

import kotlinx.serialization.Serializable

/** A platform-neutral pointer that can be routed without carrying a UI screen. */
@Serializable
data class ContentReference(
    val kind: ContentReferenceKind,
    val id: String,
    val sourceId: String? = null,
    val parentId: String? = null,
    val canonicalUrl: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Content id must not be blank" }
        if (kind.requiresSource) require(!sourceId.isNullOrBlank()) {
            "$kind requires a source id"
        }
        if (kind == ContentReferenceKind.COMMENT) require(!parentId.isNullOrBlank()) {
            "A comment reference requires its parent topic id"
        }
    }
}

@Serializable
enum class ContentReferenceKind(val requiresSource: Boolean) {
    TOPIC(true),
    COMMENT(true),
    ARTICLE(false),
    SOCIAL_POST(true),
    EXTERNAL_URL(false),
}

sealed interface LinkResolution {
    data class Internal(
        val reference: ContentReference,
        val availableOffline: Boolean,
    ) : LinkResolution

    data class External(val url: String) : LinkResolution
    data class Unsupported(val reason: String) : LinkResolution
}
