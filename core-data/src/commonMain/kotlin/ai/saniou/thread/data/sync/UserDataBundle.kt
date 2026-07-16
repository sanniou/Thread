package ai.saniou.thread.data.sync

import ai.saniou.thread.domain.model.Tag
import ai.saniou.thread.domain.model.TagType
import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.model.reader.FeedType
import ai.saniou.thread.domain.model.source.SourceDescriptor
import ai.saniou.thread.domain.model.source.SourceType
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
internal data class UserDataBundle(
    val format: String = FORMAT,
    val version: Int = CURRENT_VERSION,
    val exportedAtEpochMillis: Long,
    val sources: List<SourceSnapshot> = emptyList(),
    val feedSources: List<FeedSourceSnapshot> = emptyList(),
    val bookmarks: List<BookmarkSnapshot> = emptyList(),
    val articleStates: List<ArticleStateSnapshot> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
) {
    init {
        require(format == FORMAT) { "Unsupported user data format: $format" }
        require(version in 1..CURRENT_VERSION) { "Unsupported user data version: $version" }
    }

    companion object {
        const val FORMAT = "thread-user-data"
        const val CURRENT_VERSION = 2
    }
}

@Serializable
internal data class SourceSnapshot(
    val id: String,
    val type: String,
    val displayName: String,
    val baseUrl: String? = null,
    val enabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val options: Map<String, String> = emptyMap(),
)

@Serializable
internal data class FeedSourceSnapshot(
    val id: String,
    val name: String,
    val url: String,
    val type: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val lastUpdate: Long = 0,
    val selectorConfig: Map<String, String> = emptyMap(),
    val autoRefresh: Boolean = true,
    val refreshInterval: Long = 3_600_000,
)

@Serializable
internal data class TagSnapshot(
    val id: String,
    val name: String,
    val color: String? = null,
    val icon: String? = null,
    val url: String? = null,
    val type: String,
)

@Serializable
internal data class BookmarkSnapshot(
    val id: String,
    val type: String,
    val createdAtEpochMillis: Long,
    val tags: List<TagSnapshot> = emptyList(),
    val content: String? = null,
    val url: String? = null,
    val sourceId: String? = null,
    val sourceType: String? = null,
    val title: String? = null,
    val description: String? = null,
    val favicon: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String? = null,
    val duration: Long? = null,
)

@Serializable
internal data class ArticleStateSnapshot(
    val articleId: String,
    val isRead: Boolean,
    val isBookmarked: Boolean,
    val updatedAtEpochMillis: Long,
)

internal fun SourceDescriptor.toSnapshot() = SourceSnapshot(
    id = id,
    type = type.value,
    displayName = displayName,
    baseUrl = baseUrl,
    enabled = enabled,
    isBuiltIn = isBuiltIn,
    options = options,
)

internal fun SourceSnapshot.toDomain() = SourceDescriptor(
    id = id,
    type = SourceType(type),
    displayName = displayName,
    baseUrl = baseUrl,
    enabled = enabled,
    isBuiltIn = isBuiltIn,
    options = options,
)

internal fun FeedSource.toSnapshot() = FeedSourceSnapshot(
    id = id,
    name = name,
    url = url,
    type = type.name,
    description = description,
    iconUrl = iconUrl,
    lastUpdate = lastUpdate,
    selectorConfig = selectorConfig,
    autoRefresh = autoRefresh,
    refreshInterval = refreshInterval,
)

internal fun FeedSourceSnapshot.toDomain() = FeedSource(
    id = id,
    name = name,
    url = url,
    type = FeedType.valueOf(type),
    description = description,
    iconUrl = iconUrl,
    lastUpdate = lastUpdate,
    selectorConfig = selectorConfig,
    autoRefresh = autoRefresh,
    refreshInterval = refreshInterval,
).also {
    require(it.id.isNotBlank()) { "Feed source id must not be blank" }
    require(it.name.isNotBlank()) { "Feed source name must not be blank" }
    require(it.url.startsWith("https://") || it.url.startsWith("http://")) {
        "Feed source URL must use http:// or https://"
    }
    require(it.refreshInterval >= 60_000) { "Refresh interval must be at least one minute" }
}

internal fun Tag.toSnapshot() = TagSnapshot(id, name, color, icon, url, type.name)

internal fun TagSnapshot.toDomain() = Tag(id, name, color, icon, url, TagType.valueOf(type))

internal fun Bookmark.toSnapshot(): BookmarkSnapshot = when (this) {
    is Bookmark.Text -> BookmarkSnapshot(
        id, "TEXT", createdAt.toEpochMilliseconds(), tags.map(Tag::toSnapshot), content = content,
    )
    is Bookmark.Quote -> BookmarkSnapshot(
        id, "QUOTE", createdAt.toEpochMilliseconds(), tags.map(Tag::toSnapshot),
        content = content, sourceId = sourceId, sourceType = sourceType,
    )
    is Bookmark.Link -> BookmarkSnapshot(
        id, "LINK", createdAt.toEpochMilliseconds(), tags.map(Tag::toSnapshot),
        url = url, title = title, description = description, favicon = favicon,
    )
    is Bookmark.Image -> BookmarkSnapshot(
        id, "IMAGE", createdAt.toEpochMilliseconds(), tags.map(Tag::toSnapshot),
        url = url, width = width, height = height,
    )
    is Bookmark.Media -> BookmarkSnapshot(
        id, "MEDIA", createdAt.toEpochMilliseconds(), tags.map(Tag::toSnapshot),
        url = url, mimeType = mimeType, duration = duration,
    )
}

internal fun BookmarkSnapshot.toDomain(): Bookmark {
    require(id.isNotBlank()) { "Bookmark id must not be blank" }
    val createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis)
    val tags = tags.map(TagSnapshot::toDomain)
    return when (type) {
        "TEXT" -> Bookmark.Text(id, createdAt, tags, requireNotNull(content))
        "QUOTE" -> Bookmark.Quote(
            id, createdAt, tags, requireNotNull(content), requireNotNull(sourceId), requireNotNull(sourceType),
        )
        "LINK" -> Bookmark.Link(id, createdAt, tags, requireNotNull(url), title, description, favicon)
        "IMAGE" -> Bookmark.Image(id, createdAt, tags, requireNotNull(url), width, height)
        "MEDIA" -> Bookmark.Media(id, createdAt, tags, requireNotNull(url), requireNotNull(mimeType), duration)
        else -> throw IllegalArgumentException("Unknown bookmark type: $type")
    }
}
