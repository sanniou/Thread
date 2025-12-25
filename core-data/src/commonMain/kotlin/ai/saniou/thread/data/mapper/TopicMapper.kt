package ai.saniou.thread.data.mapper


import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.db.table.forum.ImageQueries
import ai.saniou.thread.db.table.forum.TopicQueries
import ai.saniou.thread.db.table.forum.Topic as EntityTopic
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.ImageType
import kotlin.time.Instant

fun EntityTopic.toDomain(imageQueries: ImageQueries): Topic {
    val author = Author(
        id = userHash ?: "",
        name = authorName ?: "",
        sourceName = sourceId ?: "nmb"
    )

    val images = imageQueries.getImagesByParent(
        sourceId = sourceId,
        parentId = id,
        parentType = ImageType.Topic
    ).executeAsList().map { it.toDomain() }

    return Topic(
        id = id,
        sourceName = sourceId ?: "nmb",
        sourceUrl = "https://nmb.ai/thread/$id", // TODO: Move URL generation to Source logic
        title = title,
        content = content ?: "",
        author = author,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        channelId = channelId,
        channelName = "", // DB Topic table usually doesn't store channel name, need Join or lookup
        commentCount = commentCount,
        images = images,
        isSage = (sage ?: 0) > 0,
        isAdmin = (admin ?: 0) > 0,
        isHidden = (hide ?: 0) > 0,
        isLocal = false,
        // fixme  后续处理 lastReadCommentId 和 comments
        lastReadCommentId = null,
        comments = emptyList(),
        remainingCount = null // DB doesn't store remaining count in EntityTopic, only in TopicInformation. This mapper handles EntityTopic.
    )
}

fun ai.saniou.thread.db.table.forum.Image.toDomain(): Image {
    return Image(
        originalUrl = originalUrl,
        thumbnailUrl = thumbnailUrl ?: originalUrl,
        name = name,
        extension = extension,
        width = width?.toInt(),
        height = height?.toInt()
    )
}

fun Topic.toEntity(page: Int = 1): EntityTopic {
    return EntityTopic(
        id = id,
        sourceId = sourceName,
        channelId = channelId,
        commentCount = commentCount,
        createdAt = createdAt.toEpochMilliseconds(),
        userHash = author.id,
        authorName = author.name,
        title = title,
        content = content,
        sage = if (isSage) 1 else 0,
        admin = if (isAdmin) 1 else 0,
        hide = if (isHidden) 1 else 0,
        page = page.toLong()
    )
}
