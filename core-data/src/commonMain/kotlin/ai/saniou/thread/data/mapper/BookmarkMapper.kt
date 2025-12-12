package ai.saniou.thread.data.mapper

import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.model.bookmark.Tag

fun Bookmark.toEntity(): ai.saniou.thread.db.table.Bookmark {
    return when (this) {
        is Bookmark.Text -> ai.saniou.thread.db.table.Bookmark(
            id = id,
            type = "TEXT",
            createdAt = createdAt,
            content = content,
            url = null,
            sourceId = null,
            sourceType = null,
            title = null,
            description = null,
            favicon = null,
            width = null,
            height = null,
            mimeType = null,
            duration = null
        )
        is Bookmark.Quote -> ai.saniou.thread.db.table.Bookmark(
            id = id,
            type = "QUOTE",
            createdAt = createdAt,
            content = content,
            url = null,
            sourceId = sourceId,
            sourceType = sourceType,
            title = null,
            description = null,
            favicon = null,
            width = null,
            height = null,
            mimeType = null,
            duration = null
        )
        is Bookmark.Link -> ai.saniou.thread.db.table.Bookmark(
            id = id,
            type = "LINK",
            createdAt = createdAt,
            content = null,
            url = url,
            sourceId = null,
            sourceType = null,
            title = title,
            description = description,
            favicon = favicon,
            width = null,
            height = null,
            mimeType = null,
            duration = null
        )
        is Bookmark.Image -> ai.saniou.thread.db.table.Bookmark(
            id = id,
            type = "IMAGE",
            createdAt = createdAt,
            content = null,
            url = url,
            sourceId = null,
            sourceType = null,
            title = null,
            description = null,
            favicon = null,
            width = width?.toLong(),
            height = height?.toLong(),
            mimeType = null,
            duration = null
        )
        is Bookmark.Media -> ai.saniou.thread.db.table.Bookmark(
            id = id,
            type = "MEDIA",
            createdAt = createdAt,
            content = null,
            url = url,
            sourceId = null,
            sourceType = null,
            title = null,
            description = null,
            favicon = null,
            width = null,
            height = null,
            mimeType = mimeType,
            duration = duration
        )
    }
}

fun ai.saniou.thread.db.table.Bookmark.toDomain(tags: List<Tag>): Bookmark {
    return when (type) {
        "TEXT" -> Bookmark.Text(
            id = id,
            createdAt = createdAt,
            tags = tags,
            content = content!!
        )
        "QUOTE" -> Bookmark.Quote(
            id = id,
            createdAt = createdAt,
            tags = tags,
            content = content!!,
            sourceId = sourceId!!,
            sourceType = sourceType!!
        )
        "LINK" -> Bookmark.Link(
            id = id,
            createdAt = createdAt,
            tags = tags,
            url = url!!,
            title = title,
            description = description,
            favicon = favicon
        )
        "IMAGE" -> Bookmark.Image(
            id = id,
            createdAt = createdAt,
            tags = tags,
            url = url!!,
            width = width?.toInt(),
            height = height?.toInt()
        )
        "MEDIA" -> Bookmark.Media(
            id = id,
            createdAt = createdAt,
            tags = tags,
            url = url!!,
            mimeType = mimeType!!,
            duration = duration
        )
        else -> throw IllegalArgumentException("Unknown bookmark type: $type")
    }
}
