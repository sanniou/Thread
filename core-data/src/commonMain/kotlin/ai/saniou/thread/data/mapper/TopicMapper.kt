package ai.saniou.thread.data.mapper


import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.data.source.nmb.remote.dto.toThreadReply
import ai.saniou.thread.db.table.forum.CommentQueries
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset
import ai.saniou.thread.db.table.forum.ImageQueries
import ai.saniou.thread.db.table.forum.SearchTopics
import ai.saniou.thread.db.table.forum.Topic as EntityTopic
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.ImageType
import kotlin.time.Instant


fun SearchTopics.toDomain(
    query: CommentQueries? = null,
    imageQueries: ImageQueries? = null,
): Topic {
    val author = Author(
        id = userHash,
        name = authorName,
        sourceName = sourceId
    )

    val images = imageQueries?.getImagesByParent(
        sourceId = sourceId,
        parentId = id,
        parentType = ImageType.Topic
    )?.executeAsList()?.map { it.toDomain() }
        ?: emptyList()

    return Topic(
        id = id,
        sourceName = sourceId,
        sourceUrl = "https://nmb.ai/thread/$id", // TODO: Move URL generation to Source logic
        title = title,
        content = content ?: summary ?: "", // If content is null, fallback to summary, then empty
        summary = summary,
        author = author,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        channelId = channelId,
        channelName = "", // DB Topic table usually doesn't store channel name, need Join or lookup
        commentCount = commentCount,
        images = images,
        isSage = sage > 0,
        isAdmin = admin > 0,
        isHidden = hide > 0,
        isLocal = false,
        lastViewedCommentId = lastViewedCommentId,
        comments = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain()
        } ?: emptyList(),
        remainingCount = remainingCount
    )
}

fun GetTopicsInChannelOffset.toDomain(
    query: CommentQueries? = null,
    imageQueries: ImageQueries? = null,
): Topic {
    val author = Author(
        id = userHash,
        name = authorName,
        sourceName = sourceId
    )

    val images = imageQueries?.getImagesByParent(
        sourceId = sourceId,
        parentId = id,
        parentType = ImageType.Topic
    )?.executeAsList()?.map { it.toDomain() }
        ?: emptyList()

    return Topic(
        id = id,
        sourceName = sourceId,
        sourceUrl = "https://nmb.ai/thread/$id", // TODO: Move URL generation to Source logic
        title = title,
        content = content ?: summary ?: "",
        summary = summary,
        author = author,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        channelId = channelId,
        channelName = "", // DB Topic table usually doesn't store channel name, need Join or lookup
        commentCount = commentCount,
        images = images,
        isSage = sage > 0,
        isAdmin = admin > 0,
        isHidden = hide > 0,
        isLocal = false,
        lastViewedCommentId = lastViewedCommentId,
        comments = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain()
        } ?: emptyList(),
        remainingCount = remainingCount
    )
}

fun EntityTopic.toDomain(
    query: CommentQueries? = null,
    imageQueries: ImageQueries? = null,
): Topic {
    val author = Author(
        id = userHash,
        name = authorName,
        sourceName = sourceId
    )

    val images = imageQueries?.getImagesByParent(
        sourceId = sourceId,
        parentId = id,
        parentType = ImageType.Topic
    )?.executeAsList()?.map { it.toDomain() }
        ?: emptyList()

    return Topic(
        id = id,
        sourceName = sourceId,
        sourceUrl = "https://nmb.ai/thread/$id", // TODO: Move URL generation to Source logic
        title = title,
        content = content ?: summary ?: "",
        summary = summary,
        author = author,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        channelId = channelId,
        channelName = "", // DB Topic table usually doesn't store channel name, need Join or lookup
        commentCount = commentCount,
        images = images,
        isSage = sage > 0,
        isAdmin = admin > 0,
        isHidden = hide > 0,
        isLocal = false,
        lastViewedCommentId = null,
        comments = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toDomain()
        } ?: emptyList(),
        remainingCount = null
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
        // path is not exposed to domain yet, but available in DB if needed
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
        summary = summary,
        sage = if (isSage) 1 else 0,
        admin = if (isAdmin) 1 else 0,
        hide = if (isHidden) 1 else 0,
        page = page.toLong()
    )
}
