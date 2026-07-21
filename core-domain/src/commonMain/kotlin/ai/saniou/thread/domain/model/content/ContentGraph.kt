package ai.saniou.thread.domain.model.content

import kotlin.time.Instant

enum class ContentRelationKind {
    REPLY_TO,
    REFERENCES,
    REPOST_OF,
    SAME_AUTHOR,
    SAME_TAG,
    CANONICAL_DUPLICATE,
}

data class ContentEdge(
    val from: ContentReference,
    val to: ContentReference,
    val relation: ContentRelationKind,
    val weight: Float = 1f,
    val createdAt: Instant,
) {
    init {
        require(weight in 0f..1f)
        require(from != to) { "Content graph cannot contain self edges" }
    }
}

data class RelatedContent(
    val reference: ContentReference,
    val relation: ContentRelationKind,
    val title: String,
    val summary: String = "",
    val sourceName: String,
    val publishedAt: Instant,
    val weight: Float,
)
