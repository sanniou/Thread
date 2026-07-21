package ai.saniou.thread.domain.model.social

import kotlinx.serialization.Serializable

/** Open contracts for future social/stream connectors; no platform is named in the UI layer. */
@Serializable
data class SocialCursor(
    val value: String,
    val direction: CursorDirection = CursorDirection.OLDER,
)

@Serializable
enum class CursorDirection { NEWER, OLDER }

@Serializable
data class SocialIdentity(
    val id: String,
    val displayName: String,
    val handle: String? = null,
    val avatarUrl: String? = null,
    val verified: Boolean = false,
)

@Serializable
enum class SocialInteraction { REPLY, REPOST, LIKE, BOOKMARK, SHARE }

@Serializable
data class SocialCapabilities(
    val interactions: Set<SocialInteraction> = emptySet(),
    val supportsThreads: Boolean = true,
    val supportsContentWarnings: Boolean = true,
    val maxMediaItems: Int = 4,
)

@Serializable
enum class SocialMediaKind { IMAGE, VIDEO, AUDIO, LINK }

@Serializable
data class SocialMedia(
    val id: String,
    val kind: SocialMediaKind,
    val url: String,
    val previewUrl: String? = null,
    val altText: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class SocialPost(
    val id: String,
    val sourceId: String,
    val author: SocialIdentity,
    val body: String,
    val createdAtEpochMillis: Long,
    val contentWarning: String? = null,
    val media: List<SocialMedia> = emptyList(),
    val interactionCounts: Map<SocialInteraction, Long> = emptyMap(),
    val permittedInteractions: Set<SocialInteraction> = emptySet(),
    val activeInteractions: Set<SocialInteraction> = emptySet(),
    val canonicalUrl: String? = null,
    val replyToId: String? = null,
    val repostOfId: String? = null,
)

@Serializable
data class SocialTimelinePage(
    val items: List<SocialPost>,
    val newerCursor: SocialCursor? = null,
    val olderCursor: SocialCursor? = null,
)

@Serializable
data class SocialSourceDescriptor(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val enabled: Boolean = true,
) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9_-]{1,63}")))
        require(displayName.isNotBlank())
        require(baseUrl.startsWith("https://") || baseUrl.startsWith("http://"))
    }
}

data class SocialRefreshReport(
    val refreshedSourceIds: Set<String> = emptySet(),
    val failures: Map<String, String> = emptyMap(),
) {
    val isSuccess: Boolean get() = failures.isEmpty()
    val hasAnySuccess: Boolean get() = refreshedSourceIds.isNotEmpty()
}
