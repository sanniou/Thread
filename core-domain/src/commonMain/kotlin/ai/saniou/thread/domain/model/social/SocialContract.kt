package ai.saniou.thread.domain.model.social

/** Open contracts for future social/stream connectors; no platform is named in the UI layer. */
data class SocialCursor(
    val value: String,
    val direction: CursorDirection = CursorDirection.OLDER,
)

enum class CursorDirection { NEWER, OLDER }

data class SocialIdentity(
    val id: String,
    val displayName: String,
    val handle: String? = null,
    val avatarUrl: String? = null,
    val verified: Boolean = false,
)

enum class SocialInteraction { REPLY, REPOST, LIKE, BOOKMARK, SHARE }

data class SocialCapabilities(
    val interactions: Set<SocialInteraction> = emptySet(),
    val supportsThreads: Boolean = true,
    val supportsContentWarnings: Boolean = true,
    val maxMediaItems: Int = 4,
)

enum class SocialMediaKind { IMAGE, VIDEO, AUDIO, LINK }

data class SocialMedia(
    val id: String,
    val kind: SocialMediaKind,
    val url: String,
    val previewUrl: String? = null,
    val altText: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

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
)

data class SocialTimelinePage(
    val items: List<SocialPost>,
    val newerCursor: SocialCursor? = null,
    val olderCursor: SocialCursor? = null,
)
